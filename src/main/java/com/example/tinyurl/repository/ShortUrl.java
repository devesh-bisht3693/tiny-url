package com.example.tinyurl.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(
		name = "short_urls",
		indexes = @Index(name = "idx_short_urls_created_at", columnList = "created_at"))
public class ShortUrl {

	@Id
	private Long id;

	@Column(nullable = false, length = 32, unique = true)
	private String slug;

	@Column(name = "long_url", nullable = false, columnDefinition = "text")
	private String longUrl;

	@Column(name = "url_hash", nullable = false, unique = true, columnDefinition = "bytea")
	private byte[] urlHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "click_count", nullable = false)
	private long clickCount;

	protected ShortUrl() {}

	public ShortUrl(Long id, String slug, String longUrl, byte[] urlHash, Instant createdAt, long clickCount) {
		this.id = id;
		this.slug = slug;
		this.longUrl = longUrl;
		this.urlHash = Arrays.copyOf(Objects.requireNonNull(urlHash, "urlHash"), urlHash.length);
		this.createdAt = createdAt;
		this.clickCount = clickCount;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getLongUrl() {
		return longUrl;
	}

	public byte[] getUrlHash() {
		return urlHash == null ? null : Arrays.copyOf(urlHash, urlHash.length);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public long getClickCount() {
		return clickCount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ShortUrl shortUrl = (ShortUrl) o;
		return Objects.equals(id, shortUrl.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "ShortUrl{"
				+ "id="
				+ id
				+ ", slug='"
				+ slug
				+ '\''
				+ ", longUrl='"
				+ longUrl
				+ '\''
				+ ", urlHash="
				+ Arrays.toString(urlHash)
				+ ", createdAt="
				+ createdAt
				+ ", clickCount="
				+ clickCount
				+ '}';
	}
}
