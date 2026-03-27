package com.example.tinyurl.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SlugPolicy {

	private static final Set<String> RESERVED =
			Set.of(
					"api",
					"actuator",
					"swagger-ui",
					"swagger-ui.html",
					"api-docs",
					"v3",
					"favicon.ico",
					"robots.txt");

	public boolean isReserved(String slug) {
		return slug != null && RESERVED.contains(slug.toLowerCase(Locale.ROOT));
	}

	public boolean isValidCustomSlug(String slug) {
		if (slug == null || slug.length() < 2 || slug.length() > 32) {
			return false;
		}
		return slug.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-' || c == '_');
	}
}
