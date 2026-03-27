package com.example.tinyurl.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UrlHasher {

	private static final HexFormat HEX = HexFormat.of();

	public byte[] sha256Normalized(String rawUrl) {
		String normalized = normalize(rawUrl);
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(normalized.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Normalizes URL string for stable hashing (idempotency).
	 */
	public String normalize(String rawUrl) {
		try {
			URI uri = URI.create(rawUrl.trim());
			return buildNormalizedString(uri);
		} catch (IllegalArgumentException e) {
			return rawUrl.trim();
		}
	}

	private static String buildNormalizedString(URI uri) {
		String scheme = schemeOrHttps(uri);
		String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
		int port = uri.getPort();
		String path = uri.getPath() == null || uri.getPath().isEmpty() ? "/" : uri.getPath();
		String query = uri.getRawQuery();
		StringBuilder builder = new StringBuilder();
		builder.append(scheme).append("://").append(host);
		if (port != -1 && !isDefaultPort(scheme, port)) {
			builder.append(':').append(port);
		}
		builder.append(path);
		if (query != null && !query.isEmpty()) {
			builder.append('?').append(query);
		}
		return builder.toString();
	}

	private static String schemeOrHttps(URI uri) {
		String s = uri.getScheme();
		return s != null ? s.toLowerCase(Locale.ROOT) : "https";
	}

	private static boolean isDefaultPort(String scheme, int port) {
		return ("http".equalsIgnoreCase(scheme) && port == 80)
				|| ("https".equalsIgnoreCase(scheme) && port == 443);
	}

	public String toHex(byte[] hash) {
		return HEX.formatHex(hash);
	}
}
