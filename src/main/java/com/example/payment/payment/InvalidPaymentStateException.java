package com.example.payment.payment;

public class InvalidPaymentStateException extends RuntimeException {

	public InvalidPaymentStateException(PaymentStatus current, PaymentStatus target) {
		super("현재 상태(" + current + ")에서는 " + target + " 상태로 변경할 수 없습니다.");
	}

}
