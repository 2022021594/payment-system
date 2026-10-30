package com.example.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	PaymentRepository repository;

	@InjectMocks
	PaymentService service;

	@Test
	void createsReadyPaymentInKrw() {
		when(repository.findByOrderId("order-1")).thenReturn(Optional.empty());
		when(repository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

		Payment payment = service.create("order-1", 15_000L);

		assertThat(payment.getOrderId()).isEqualTo("order-1");
		assertThat(payment.getAmount()).isEqualTo(15_000L);
		assertThat(payment.getCurrency()).isEqualTo("KRW");
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
	}

	@Test
	void rejectsExistingOrderId() {
		when(repository.findByOrderId("order-1")).thenReturn(Optional.of(new Payment("order-1", 1_000L)));

		assertThatThrownBy(() -> service.create("order-1", 2_000L)).isInstanceOf(DuplicateOrderException.class);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void convertsUniqueConstraintViolationToDuplicate() {
		when(repository.findByOrderId("order-1")).thenReturn(Optional.empty());
		when(repository.saveAndFlush(any(Payment.class))).thenThrow(new DataIntegrityViolationException("unique"));

		assertThatThrownBy(() -> service.create("order-1", 1_000L)).isInstanceOf(DuplicateOrderException.class);
	}

	@Test
	void getsPaymentById() {
		Payment payment = new Payment("order-1", 1_000L);
		when(repository.findById(1L)).thenReturn(Optional.of(payment));

		assertThat(service.getById(1L)).isSameAs(payment);
	}

	@Test
	void throwsWhenPaymentIdNotFound() {
		when(repository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(PaymentNotFoundException.class);
	}

	@Test
	void getsPaymentByOrderId() {
		Payment payment = new Payment("order-1", 1_000L);
		when(repository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

		assertThat(service.getByOrderId("order-1")).isSameAs(payment);
	}

	@Test
	void throwsWhenOrderIdNotFound() {
		when(repository.findByOrderId("order-1")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getByOrderId("order-1")).isInstanceOf(PaymentNotFoundException.class);
	}

}
