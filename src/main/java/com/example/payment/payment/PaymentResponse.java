package com.example.payment.payment;

import java.time.OffsetDateTime;

public record PaymentResponse(
		Long id,
		String orderId,
		long amount,
		String currency,
		PaymentStatus status,
		OffsetDateTime createdAt) {

	public static PaymentResponse from(Payment payment) {
		return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(),
				payment.getCurrency(), payment.getStatus(), payment.getCreatedAt());
	}

}
