package com.example.tinyurl.web;

import com.example.tinyurl.config.AppProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(50)
public class RateLimitFilter extends OncePerRequestFilter {

	private final AppProperties appProperties;
	private final RateLimiterRegistry rateLimiterRegistry;

	public RateLimitFilter(AppProperties appProperties, RateLimiterRegistry rateLimiterRegistry) {
		this.appProperties = appProperties;
		this.rateLimiterRegistry = rateLimiterRegistry;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!appProperties.rateLimit().enabled() || !request.getRequestURI().startsWith("/api/")) {
			filterChain.doFilter(request, response);
			return;
		}
		RateLimiter limiter = rateLimiterRegistry.rateLimiter("api");
		boolean allowed = limiter.acquirePermission();
		if (!allowed) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			return;
		}
		filterChain.doFilter(request, response);
	}
}
