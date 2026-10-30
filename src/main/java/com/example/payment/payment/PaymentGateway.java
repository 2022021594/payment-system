package com.example.payment.payment;

/** 외부 PG사 연동 경계. 실제 PG가 정해지면 이 인터페이스의 구현체만 교체한다. */
public interface PaymentGateway {

	PaymentGatewayResult approve(String orderId, long amount);

}
