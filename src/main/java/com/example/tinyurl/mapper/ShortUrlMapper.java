package com.example.tinyurl.mapper;

import com.example.tinyurl.config.AppProperties;
import com.example.tinyurl.dto.CreateShortUrlResponse;
import com.example.tinyurl.dto.UrlStatsResponse;
import com.example.tinyurl.repository.ShortUrl;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlMapper {

	private final AppProperties appProperties;

	public ShortUrlMapper(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	public CreateShortUrlResponse toCreateResponse(ShortUrl entity) {
		String base = trimTrailingSlash(appProperties.shortUrlBase());
		return new CreateShortUrlResponse(base + "/" + entity.getSlug(), entity.getSlug());
	}

	public UrlStatsResponse toStats(ShortUrl entity) {
		return new UrlStatsResponse(entity.getSlug(), entity.getClickCount());
	}

	private static String trimTrailingSlash(String base) {
		if (base.endsWith("/")) {
			return base.substring(0, base.length() - 1);
		}
		return base;
	}
}
