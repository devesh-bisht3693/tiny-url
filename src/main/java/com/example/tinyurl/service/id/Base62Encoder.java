package com.example.tinyurl.service.id;

/**
 * Base62 using [0-9a-zA-Z] — URL-safe, unambiguous for display in paths.
 */
public final class Base62Encoder {

	private static final String ALPHABET =
			"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final int BASE = ALPHABET.length();

	private Base62Encoder() {}

	public static String encode(long value) {
		if (value < 0) {
			throw new IllegalArgumentException("value must be non-negative");
		}
		if (value == 0) {
			return "0";
		}
		StringBuilder sb = new StringBuilder();
		long n = value;
		while (n > 0) {
			sb.append(ALPHABET.charAt((int) (n % BASE)));
			n /= BASE;
		}
		return sb.reverse().toString();
	}
}
