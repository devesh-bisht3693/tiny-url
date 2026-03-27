package com.example.tinyurl.cache;

import com.example.tinyurl.config.AppProperties;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Probabilistic set of known slugs to reduce DB lookups after warm-up. False positives possible; never used to deny
 * without a confirming lookup when data may predate the filter.
 */
@Component
public class SlugBloomFilter {

	private final BloomFilter<CharSequence> bloom;
	private final boolean enabled;

	public SlugBloomFilter(AppProperties appProperties) {
		var bf = appProperties.bloomFilter();
		this.enabled = bf.enabled();
		if (enabled) {
			this.bloom = BloomFilter.create(
					Funnels.stringFunnel(StandardCharsets.UTF_8),
					Math.max(1000L, bf.expectedInsertions()),
					bf.fpp());
		} else {
			this.bloom = null;
		}
	}

	public void put(String slug) {
		if (enabled) {
			bloom.put(slug);
		}
	}

	/**
	 * @return true if the slug might exist; false if definitely not seen via {@link #put} in this process
	 */
	public boolean mightContain(String slug) {
		return !enabled || bloom.mightContain(slug);
	}

	public boolean isEnabled() {
		return enabled;
	}
}
