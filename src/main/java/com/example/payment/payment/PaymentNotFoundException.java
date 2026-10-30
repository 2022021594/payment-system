package com.example.payment.payment;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(Long id) {
		super("결제를 찾을 수 없습니다: id=" + id);
	}

	public PaymentNotFoundException(String orderId) {
		super("결제를 찾을 수 없습니다: orderId=" + orderId);
	}

}
