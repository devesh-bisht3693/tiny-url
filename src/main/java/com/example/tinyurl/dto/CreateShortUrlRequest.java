package com.example.tinyurl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
		@NotBlank @Size(max = 2048) String longUrl,
		@Size(min = 2, max = 32) String customAlias
) {}
