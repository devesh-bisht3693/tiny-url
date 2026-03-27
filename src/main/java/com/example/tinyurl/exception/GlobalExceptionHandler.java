package com.example.tinyurl.exception;

import com.example.tinyurl.service.id.IdAllocationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.findFirst()
				.orElse("Validation failed");
		return json(HttpStatus.BAD_REQUEST, msg, req);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> constraint(ConstraintViolationException ex, HttpServletRequest req) {
		return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> badRequest(IllegalArgumentException ex, HttpServletRequest req) {
		return json(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex, HttpServletRequest req) {
		if (log.isDebugEnabled()) {
			log.debug("Data integrity: {}", ex.getMessage());
		}
		return json(HttpStatus.CONFLICT, "Slug or URL hash already exists", req);
	}

	@ExceptionHandler(IdAllocationException.class)
	public ResponseEntity<ApiError> idUnavailable(IdAllocationException ex, HttpServletRequest req) {
		if (log.isWarnEnabled()) {
			log.warn("Id allocation failed: {}", ex.toString());
		}
		return json(HttpStatus.SERVICE_UNAVAILABLE, "Temporary failure allocating id; retry later", req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> fallback(Exception ex, HttpServletRequest req) {
		if (log.isErrorEnabled()) {
			log.error("Unhandled error", ex);
		}
		return json(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", req);
	}

	private static ResponseEntity<ApiError> json(HttpStatus status, String message, HttpServletRequest req) {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
