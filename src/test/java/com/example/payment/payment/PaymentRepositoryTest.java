package com.example.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.payment.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PaymentRepositoryTest {

	@Autowired
	PaymentRepository repository;

	@Test
	void saveAndFindByOrderId() {
		repository.saveAndFlush(new Payment("order-1", 1000L));

		Payment found = repository.findByOrderId("order-1").orElseThrow();

		assertThat(found.getStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(found.getAmount()).isEqualTo(1000L);
		assertThat(found.getCurrency()).isEqualTo("KRW");
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void duplicateOrderIdIsRejected() {
		repository.saveAndFlush(new Payment("order-dup", 100L));

		assertThatThrownBy(() -> repository.saveAndFlush(new Payment("order-dup", 200L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findByIdForUpdateReturnsPayment() {
		Payment saved = repository.saveAndFlush(new Payment("order-lock", 100L));

		assertThat(repository.findByIdForUpdate(saved.getId())).isPresent();
		assertThat(repository.findByIdForUpdate(-1L)).isEmpty();
	}

}
