package com.example.tinyurl.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfiguration {

	public static final String ZK_ID_ALLOCATOR = "zkIdAllocator";
	public static final String REDIS_CACHE = "redisCache";

	@Bean
	public CircuitBreakerRegistry circuitBreakerRegistry() {
		CircuitBreakerConfig config = CircuitBreakerConfig.custom()
				.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
				.slidingWindowSize(10)
				.minimumNumberOfCalls(5)
				.failureRateThreshold(50)
				.waitDurationInOpenState(Duration.ofSeconds(30))
				.build();
		return CircuitBreakerRegistry.of(config);
	}

	@Bean
	public RateLimiterRegistry rateLimiterRegistry(AppProperties appProperties) {
		var rl = appProperties.rateLimit();
		RateLimiterConfig config = RateLimiterConfig.custom()
				.limitForPeriod(rl.permitsPerMinute())
				.limitRefreshPeriod(Duration.ofMinutes(1))
				.timeoutDuration(Duration.ZERO)
				.build();
		RateLimiterRegistry registry = RateLimiterRegistry.of(config);
		registry.rateLimiter("api", config);
		return registry;
	}
}
