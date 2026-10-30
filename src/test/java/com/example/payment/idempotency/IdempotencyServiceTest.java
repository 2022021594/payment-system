package com.example.payment.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.payment.TestcontainersConfiguration;
import com.example.payment.payment.PaymentResponse;
import com.example.payment.payment.PaymentStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IdempotencyServiceTest {

	@Autowired
	IdempotencyService service;

	private static String newKey() {
		return UUID.randomUUID().toString();
	}

	private static PaymentResponse response(long id) {
		return new PaymentResponse(id, "order-" + id, 1_000L, "KRW", PaymentStatus.APPROVED, null);
	}

	@Test
	void executesOnceAndReplaysStoredResponse() {
		String key = newKey();
		AtomicInteger calls = new AtomicInteger();

		PaymentResponse first = service.execute(key, "POST /a", PaymentResponse.class,
				() -> response(calls.incrementAndGet()));
		PaymentResponse second = service.execute(key, "POST /a", PaymentResponse.class,
				() -> response(calls.incrementAndGet()));

		assertThat(calls).hasValue(1);
		assertThat(second).isEqualTo(first);
	}

	@Test
	void rejectsSameKeyWithDifferentRequest() {
		String key = newKey();
		service.execute(key, "POST /a", PaymentResponse.class, () -> response(1));

		assertThatThrownBy(() -> service.execute(key, "POST /b", PaymentResponse.class, () -> response(2)))
				.isInstanceOf(IdempotencyKeyReuseException.class);
	}

	@Test
	void doesNotStoreResponseWhenActionFails() {
		String key = newKey();
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> service.execute(key, "POST /a", PaymentResponse.class, () -> {
			calls.incrementAndGet();
			throw new IllegalStateException("boom");
		})).isInstanceOf(IllegalStateException.class);
		PaymentResponse retry = service.execute(key, "POST /a", PaymentResponse.class, () -> {
			calls.incrementAndGet();
			return response(1);
		});

		assertThat(calls).hasValue(2);
		assertThat(retry.id()).isEqualTo(1);
	}

	@Test
	void differentKeysAreIndependent() {
		AtomicInteger calls = new AtomicInteger();

		service.execute(newKey(), "POST /a", PaymentResponse.class, () -> response(calls.incrementAndGet()));
		service.execute(newKey(), "POST /a", PaymentResponse.class, () -> response(calls.incrementAndGet()));

		assertThat(calls).hasValue(2);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void rejectsBlankKey(String key) {
		assertThatThrownBy(() -> service.execute(key, "POST /a", PaymentResponse.class, () -> response(1)))
				.isInstanceOf(InvalidIdempotencyKeyException.class);
	}

	@Test
	void rejectsTooLongKey() {
		assertThatThrownBy(() -> service.execute("k".repeat(65), "POST /a", PaymentResponse.class, () -> response(1)))
				.isInstanceOf(InvalidIdempotencyKeyException.class);
	}

}
