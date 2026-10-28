package com.wallet.api.common.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest()
				.body(ApiError.validation("Request validation failed", request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(ApiError.validation("Malformed JSON request", request.getRequestURI(), Map.of()));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiError.of(HttpStatus.CONFLICT, "Request conflicts with existing data",
						request.getRequestURI()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiError.of(HttpStatus.UNAUTHORIZED, "Invalid phone or password", request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception for {}", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error",
						request.getRequestURI()));
	}
}
