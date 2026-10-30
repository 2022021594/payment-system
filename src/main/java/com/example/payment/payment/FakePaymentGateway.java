package com.example.payment.payment;

import org.springframework.stereotype.Component;

/** 실제 PG 연동 전까지 쓰는 가짜 구현체. 한도({@value #LIMIT}원)를 넘는 금액만 거절한다. */
@Component
public class FakePaymentGateway implements PaymentGateway {

	static final long LIMIT = 10_000_000L;

	@Override
	public PaymentGatewayResult approve(String orderId, long amount) {
		if (amount > LIMIT) {
			return PaymentGatewayResult.rejected("결제 한도를 초과했습니다.");
		}
		return PaymentGatewayResult.success();
	}

	@Override
	public PaymentGatewayResult cancel(String orderId, long amount) {
		return PaymentGatewayResult.success();
	}

}
