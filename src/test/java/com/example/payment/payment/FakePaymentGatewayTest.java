package com.example.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FakePaymentGatewayTest {

	FakePaymentGateway gateway = new FakePaymentGateway();

	@Test
	void approvesAmountWithinLimit() {
		assertThat(gateway.approve("order-1", FakePaymentGateway.LIMIT).approved()).isTrue();
	}

	@Test
	void rejectsAmountOverLimit() {
		PaymentGatewayResult result = gateway.approve("order-1", FakePaymentGateway.LIMIT + 1);

		assertThat(result.approved()).isFalse();
		assertThat(result.reason()).isNotBlank();
	}

	@Test
	void cancelSucceeds() {
		assertThat(gateway.cancel("order-1", 1_000L).approved()).isTrue();
	}

}
