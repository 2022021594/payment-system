package com.example.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PaymentTest {

	private Payment ready() {
		return new Payment("order-1", 1_000L);
	}

	private Payment approved() {
		Payment payment = ready();
		payment.approve();
		return payment;
	}

	@Test
	void readyCanBeApproved() {
		Payment payment = ready();
		payment.approve();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void readyCanFail() {
		Payment payment = ready();
		payment.fail();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
	}

	@Test
	void approvedCanBeCanceled() {
		Payment payment = approved();
		payment.cancel();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	void approvedCannotBeApprovedOrFailedAgain() {
		Payment payment = approved();
		assertThatThrownBy(payment::approve).isInstanceOf(InvalidPaymentStateException.class);
		assertThatThrownBy(payment::fail).isInstanceOf(InvalidPaymentStateException.class);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void readyCannotBeCanceled() {
		assertThatThrownBy(() -> ready().cancel()).isInstanceOf(InvalidPaymentStateException.class);
	}

	@Test
	void failedAndCanceledAreTerminal() {
		Payment failed = ready();
		failed.fail();
		assertThatThrownBy(failed::approve).isInstanceOf(InvalidPaymentStateException.class);
		assertThatThrownBy(failed::cancel).isInstanceOf(InvalidPaymentStateException.class);

		Payment canceled = approved();
		canceled.cancel();
		assertThatThrownBy(canceled::approve).isInstanceOf(InvalidPaymentStateException.class);
		assertThatThrownBy(canceled::cancel).isInstanceOf(InvalidPaymentStateException.class);
	}

}
