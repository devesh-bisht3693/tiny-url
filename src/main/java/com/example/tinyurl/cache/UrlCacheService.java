package com.example.tinyurl.cache;

import com.example.tinyurl.config.AppProperties;
import com.example.tinyurl.config.ResilienceConfiguration;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Read-through / write-through cache for slug → long URL. Fail-open on Redis errors so redirects still work via DB.
 */
@Service
@Profile("!test")
public class UrlCacheService implements SlugCache {

	private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);
	private static final String KEY_PREFIX = "tinyurl:url:";

	private final StringRedisTemplate redis;
	private final AppProperties appProperties;
	private final CircuitBreaker redisCircuitBreaker;

	public UrlCacheService(
			StringRedisTemplate redis,
			AppProperties appProperties,
			CircuitBreakerRegistry circuitBreakerRegistry) {
		this.redis = redis;
		this.appProperties = appProperties;
		this.redisCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ResilienceConfiguration.REDIS_CACHE);
	}

	@Override
	public Optional<String> getLongUrl(String slug) {
		try {
			return redisCircuitBreaker.executeSupplier(() -> Optional.ofNullable(redis.opsForValue().get(cacheKey(slug))));
		} catch (CallNotPermittedException e) {
			if (log.isWarnEnabled()) {
				log.warn("Redis get circuit open (fail-open)");
			}
			return Optional.empty();
		} catch (DataAccessException e) {
			if (log.isWarnEnabled()) {
				log.warn("Redis get failed for slug (fail-open): {}", e.toString());
			}
			return Optional.empty();
		}
	}

	@Override
	public void put(String slug, String longUrl) {
		try {
			redisCircuitBreaker.executeRunnable(() -> {
				Duration ttl = appProperties.cache().slugTtl();
				String key = cacheKey(slug);
				if (ttl == null || ttl.isZero() || ttl.isNegative()) {
					redis.opsForValue().set(key, longUrl);
				} else {
					redis.opsForValue().set(key, longUrl, ttl);
				}
			});
		} catch (CallNotPermittedException e) {
			if (log.isWarnEnabled()) {
				log.warn("Redis set circuit open (continuing)");
			}
		} catch (DataAccessException e) {
			if (log.isWarnEnabled()) {
				log.warn("Redis set failed for slug (continuing): {}", e.toString());
			}
		}
	}

	private static String cacheKey(String slug) {
		return KEY_PREFIX + slug;
	}
}
