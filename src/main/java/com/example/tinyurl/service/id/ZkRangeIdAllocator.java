package com.example.tinyurl.service.id;

import com.example.tinyurl.config.AppProperties;
import com.example.tinyurl.config.ResilienceConfiguration;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Reserves disjoint ID ranges from Zookeeper via {@link DistributedAtomicLong}, then hands out
 * IDs locally to minimize ZK round-trips.
 */
@Component
@Profile("!test")
public class ZkRangeIdAllocator implements IdAllocator {

	private static final Logger log = LoggerFactory.getLogger(ZkRangeIdAllocator.class);

	private final CuratorFramework curator;
	private final AppProperties appProperties;
	private final CircuitBreaker zkCircuitBreaker;
	private DistributedAtomicLong counter;

	private final Object rangeLock = new Object();
	private long nextLocal;
	private long rangeEndExclusive;

	public ZkRangeIdAllocator(
			CuratorFramework curator,
			AppProperties appProperties,
			CircuitBreakerRegistry circuitBreakerRegistry) {
		this.curator = curator;
		this.appProperties = appProperties;
		this.zkCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ResilienceConfiguration.ZK_ID_ALLOCATOR);
	}

	@PostConstruct
	void initCounter() throws Exception {
		String path = appProperties.zookeeper().idCounterPath();
		counter = new DistributedAtomicLong(curator, path, new ExponentialBackoffRetry(1000, 3));
		boolean created = counter.initialize(0L);
		if (!created && log.isDebugEnabled()) {
			log.debug("ZK counter already present at path {}", path);
		}
	}

	@Override
	public long nextId() {
		synchronized (rangeLock) {
			while (nextLocal >= rangeEndExclusive) {
				refillRange();
			}
			return nextLocal++;
		}
	}

	private void refillRange() {
		int batch = Math.max(1, appProperties.idAllocation().batchSize());
		try {
			zkCircuitBreaker.executeRunnable(() -> reserveBatch(batch));
		} catch (CallNotPermittedException e) {
			throw new IdAllocationException("Zookeeper id allocator circuit open; try again later", e);
		}
	}

	@SuppressWarnings("PMD.AvoidCatchingGenericException")
	private void reserveBatch(int batch) {
		final AtomicValue<Long> atomic;
		try {
			atomic = counter.add((long) batch);
		} catch (Exception e) {
			throw new IdAllocationException("Failed to allocate id range from Zookeeper", e);
		}
		if (!atomic.succeeded()) {
			throw new IdAllocationException("ZK atomic add failed: compare-and-set conflict", null);
		}
		long start = atomic.preValue();
		long end = atomic.postValue();
		if (end - start != batch) {
			throw new IdAllocationException("Unexpected ZK counter delta: " + start + " -> " + end, null);
		}
		this.nextLocal = start;
		this.rangeEndExclusive = end;
		if (log.isDebugEnabled()) {
			log.debug("Reserved ZK id range [{}, {})", start, end);
		}
	}
}
