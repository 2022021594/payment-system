package com.example.payment.idempotency;

public class InvalidIdempotencyKeyException extends RuntimeException {

	public InvalidIdempotencyKeyException() {
		super("Idempotency-Key는 비어 있지 않은 " + IdempotencyService.MAX_KEY_LENGTH + "자 이하 문자열이어야 합니다.");
	}

}
