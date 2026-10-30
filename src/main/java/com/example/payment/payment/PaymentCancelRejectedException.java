package com.example.payment.payment;

public class PaymentCancelRejectedException extends RuntimeException {

	public PaymentCancelRejectedException(String reason) {
		super("PG사가 결제 취소를 거절했습니다: " + reason);
	}

}
