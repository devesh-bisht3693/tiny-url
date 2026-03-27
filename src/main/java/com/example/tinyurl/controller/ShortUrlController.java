package com.example.tinyurl.controller;

import com.example.tinyurl.dto.CreateShortUrlRequest;
import com.example.tinyurl.dto.CreateShortUrlResponse;
import com.example.tinyurl.dto.UrlStatsResponse;
import com.example.tinyurl.service.ShortUrlService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

	private final ShortUrlService shortUrlService;
	private final Counter createsCounter;

	public ShortUrlController(ShortUrlService shortUrlService, MeterRegistry meterRegistry) {
		this.shortUrlService = shortUrlService;
		this.createsCounter = Counter.builder("tinyurl.short_urls.created")
				.description("Short URLs created")
				.register(meterRegistry);
	}

	@PostMapping
	public ResponseEntity<CreateShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
		CreateShortUrlResponse body = shortUrlService.create(request);
		createsCounter.increment();
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{slug}/stats")
	public ResponseEntity<UrlStatsResponse> stats(@PathVariable String slug) {
		return shortUrlService
				.getStats(slug)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}
}
