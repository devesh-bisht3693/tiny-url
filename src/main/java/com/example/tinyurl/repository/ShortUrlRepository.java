package com.example.tinyurl.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

	Optional<ShortUrl> findBySlug(String slug);

	Optional<ShortUrl> findByUrlHash(byte[] urlHash);

	@Modifying
	@Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1 WHERE s.slug = :slug")
	int incrementClickCountBySlug(@Param("slug") String slug);
}
