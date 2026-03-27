package com.example.tinyurl.service.id;

/**
 * Allocates monotonically increasing numeric IDs used for generated slugs (Base62).
 */
public interface IdAllocator {

	long nextId();
}
