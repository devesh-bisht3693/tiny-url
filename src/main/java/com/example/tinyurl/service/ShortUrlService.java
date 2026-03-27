package com.example.tinyurl.service;

import com.example.tinyurl.cache.SlugBloomFilter;
import com.example.tinyurl.cache.SlugCache;
import com.example.tinyurl.dto.CreateShortUrlRequest;
import com.example.tinyurl.dto.CreateShortUrlResponse;
import com.example.tinyurl.dto.UrlStatsResponse;
import com.example.tinyurl.mapper.ShortUrlMapper;
import com.example.tinyurl.repository.ShortUrl;
import com.example.tinyurl.repository.ShortUrlRepository;
import com.example.tinyurl.service.id.Base62Encoder;
import com.example.tinyurl.service.id.IdAllocator;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

	private final ShortUrlRepository shortUrlRepository;
	private final UrlHasher urlHasher;
	private final IdAllocator idAllocator;
	private final SlugCache slugCache;
	private final SlugBloomFilter slugBloomFilter;
	private final SlugPolicy slugPolicy;
	private final ShortUrlMapper shortUrlMapper;

	public ShortUrlService(
			ShortUrlRepository shortUrlRepository,
			UrlHasher urlHasher,
			IdAllocator idAllocator,
			SlugCache slugCache,
			SlugBloomFilter slugBloomFilter,
			SlugPolicy slugPolicy,
			ShortUrlMapper shortUrlMapper) {
		this.shortUrlRepository = shortUrlRepository;
		this.urlHasher = urlHasher;
		this.idAllocator = idAllocator;
		this.slugCache = slugCache;
		this.slugBloomFilter = slugBloomFilter;
		this.slugPolicy = slugPolicy;
		this.shortUrlMapper = shortUrlMapper;
	}

	@Transactional
	public CreateShortUrlResponse create(CreateShortUrlRequest request) {
		String raw = request.longUrl().trim();
		validateHttpUrl(raw);
		byte[] hash = urlHasher.sha256Normalized(raw);

		Optional<ShortUrl> existing = shortUrlRepository.findByUrlHash(hash);
		if (existing.isPresent()) {
			return shortUrlMapper.toCreateResponse(existing.get());
		}

		long id = idAllocator.nextId();
		String slug = resolveSlug(request.customAlias(), id);
		Instant now = Instant.now();
		ShortUrl entity = new ShortUrl(id, slug, raw, hash, now, 0L);
		ShortUrl saved;
		try {
			saved = shortUrlRepository.save(entity);
		} catch (DataIntegrityViolationException e) {
			return shortUrlRepository
					.findByUrlHash(hash)
					.map(shortUrlMapper::toCreateResponse)
					.orElseThrow(() -> e);
		}
		slugCache.put(saved.getSlug(), saved.getLongUrl());
		slugBloomFilter.put(saved.getSlug());
		return shortUrlMapper.toCreateResponse(saved);
	}

	private String resolveSlug(String customAlias, long idForGeneratedSlug) {
		if (customAlias == null || customAlias.isBlank()) {
			return Base62Encoder.encode(idForGeneratedSlug);
		}
		String slug = customAlias.trim();
		if (!slugPolicy.isValidCustomSlug(slug)) {
			throw new IllegalArgumentException("Invalid custom alias");
		}
		if (slugPolicy.isReserved(slug)) {
			throw new IllegalArgumentException("Reserved slug");
		}
		if (shortUrlRepository.findBySlug(slug).isPresent()) {
			throw new IllegalArgumentException("Alias already taken");
		}
		return slug;
	}

	@Transactional(readOnly = true)
	public Optional<UrlStatsResponse> getStats(String slug) {
		return shortUrlRepository.findBySlug(slug).map(shortUrlMapper::toStats);
	}

	/**
	 * Resolves long URL for redirect and increments click count when found.
	 */
	@Transactional
	public Optional<String> resolveLongUrlAndRecordClick(String slug) {
		Optional<String> fromCache = slugCache.getLongUrl(slug);
		if (fromCache.isPresent()) {
			shortUrlRepository.incrementClickCountBySlug(slug);
			return fromCache;
		}
		Optional<ShortUrl> row = shortUrlRepository.findBySlug(slug);
		if (row.isEmpty()) {
			return Optional.empty();
		}
		ShortUrl entity = row.get();
		slugCache.put(entity.getSlug(), entity.getLongUrl());
		shortUrlRepository.incrementClickCountBySlug(slug);
		return Optional.of(entity.getLongUrl());
	}

	private static void validateHttpUrl(String url) {
		final URI u;
		try {
			u = URI.create(url);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid URL", ex);
		}
		if (!isHttpOrHttpsScheme(u.getScheme())) {
			throw new IllegalArgumentException("URL must use http or https");
		}
		final String host = u.getHost();
		if (host == null || host.isEmpty()) {
			throw new IllegalArgumentException("URL must include a host");
		}
	}

	private static boolean isHttpOrHttpsScheme(String scheme) {
		return scheme != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
	}
}
