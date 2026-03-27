package com.example.tinyurl.controller;

import com.example.tinyurl.service.ShortUrlService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Order(Integer.MAX_VALUE)
public class RedirectController {

	private final ShortUrlService shortUrlService;
	private final Counter redirectsCounter;

	public RedirectController(ShortUrlService shortUrlService, MeterRegistry meterRegistry) {
		this.shortUrlService = shortUrlService;
		this.redirectsCounter = Counter.builder("tinyurl.redirects")
				.description("HTTP redirects served")
				.register(meterRegistry);
	}

	@GetMapping("/{slug}")
	public ResponseEntity<Void> redirect(@PathVariable String slug) {
		return shortUrlService
				.resolveLongUrlAndRecordClick(slug)
				.map(url -> {
					redirectsCounter.increment();
					return ResponseEntity.status(HttpStatus.FOUND)
							.location(URI.create(url))
							.<Void>build();
				})
				.orElse(ResponseEntity.<Void>notFound().build());
	}
}
