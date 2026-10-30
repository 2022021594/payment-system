package com.example.payment.idempotency;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdempotencyExceptionHandler {

	@ExceptionHandler(InvalidIdempotencyKeyException.class)
	ProblemDetail handleInvalidKey(InvalidIdempotencyKeyException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(IdempotencyKeyReuseException.class)
	ProblemDetail handleKeyReuse(IdempotencyKeyReuseException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
	}

}
