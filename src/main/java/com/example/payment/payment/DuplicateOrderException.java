package com.example.payment.payment;

public class DuplicateOrderException extends RuntimeException {

	public DuplicateOrderException(String orderId) {
		super("이미 결제 요청이 존재하는 주문입니다: " + orderId);
	}

}
