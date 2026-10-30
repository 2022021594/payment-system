package com.example.payment.payment;

/** PG 승인·취소 요청 결과. 거절 시 {@code reason}에 사유가 담긴다. */
public record PaymentGatewayResult(boolean approved, String reason) {

	public static PaymentGatewayResult success() {
		return new PaymentGatewayResult(true, null);
	}

	public static PaymentGatewayResult rejected(String reason) {
		return new PaymentGatewayResult(false, reason);
	}

}
