package com.example.tinyurl.service.id;

public class IdAllocationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public IdAllocationException(String message, Throwable cause) {
		super(message, cause);
	}
}
