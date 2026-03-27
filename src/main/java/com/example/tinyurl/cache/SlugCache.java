package com.example.tinyurl.cache;

import java.util.Optional;

public interface SlugCache {

	Optional<String> getLongUrl(String slug);

	void put(String slug, String longUrl);
}
