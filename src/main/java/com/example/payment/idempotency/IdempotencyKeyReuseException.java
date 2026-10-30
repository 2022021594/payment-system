package com.example.payment.idempotency;

public class IdempotencyKeyReuseException extends RuntimeException {

	public IdempotencyKeyReuseException() {
		super("이미 다른 요청에 사용된 Idempotency-Key입니다.");
	}

}
