package com.example.tinyurl.cache;

import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
@Primary
public class NoopSlugCache implements SlugCache {

	@Override
	public Optional<String> getLongUrl(String slug) {
		return Optional.empty();
	}

	@Override
	public void put(String slug, String longUrl) {
		// no-op
	}
}
