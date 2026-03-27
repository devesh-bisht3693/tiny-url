package com.example.tinyurl.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
		@DefaultValue("http://localhost:8080") String shortUrlBase,
		IdAllocation idAllocation,
		Cache cache,
		BloomFilterSettings bloomFilter,
		Zookeeper zookeeper,
		RateLimit rateLimit
) {
	public record IdAllocation(@DefaultValue("1000") int batchSize) {}

	public record Cache(@DefaultValue("P7D") Duration slugTtl) {}

	public record BloomFilterSettings(
			@DefaultValue("1000000") long expectedInsertions,
			@DefaultValue("0.01") double fpp,
			@DefaultValue("true") boolean enabled
	) {}

	public record Zookeeper(
			String connectString,
			@DefaultValue("60000") int sessionTimeoutMs,
			@DefaultValue("15000") int connectionTimeoutMs,
			@DefaultValue("/tinyurl/id-counter") String idCounterPath
	) {}

	public record RateLimit(@DefaultValue("false") boolean enabled, @DefaultValue("120") int permitsPerMinute) {}
}
