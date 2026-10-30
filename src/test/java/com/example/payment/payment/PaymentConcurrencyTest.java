package com.example.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.payment.TestcontainersConfiguration;
import com.example.payment.idempotency.IdempotencyService;

/** 같은 결제에 요청이 동시에 몰려도 한 번만 처리되는지 실제 DB로 검증한다. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentConcurrencyTest {

	// 커넥션 풀(10)보다 작게 잡아, 잠금을 기다리는 스레드가 풀을 고갈시키지 않게 한다.
	private static final int THREADS = 8;

	@Autowired
	PaymentService paymentService;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	IdempotencyService idempotencyService;

	@MockitoSpyBean
	PaymentGateway gateway;

	private record Outcome(List<Object> successes, List<Throwable> failures) {
	}

	/** 모든 스레드를 한꺼번에 출발시켜 {@code task}를 동시에 실행한다. */
	private static <T> Outcome runConcurrently(Callable<T> task) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		CountDownLatch ready = new CountDownLatch(THREADS);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<T>> futures = new ArrayList<>();
			for (int i = 0; i < THREADS; i++) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return task.call();
				}));
			}
			ready.await();
			start.countDown();

			Outcome outcome = new Outcome(new ArrayList<>(), new ArrayList<>());
			for (Future<T> future : futures) {
				try {
					outcome.successes().add(future.get());
				}
				catch (ExecutionException e) {
					outcome.failures().add(e.getCause());
				}
			}
			return outcome;
		}
		finally {
			executor.shutdownNow();
		}
	}

	private Payment createReady() {
		return paymentService.create(UUID.randomUUID().toString(), 5_000L);
	}

	@Test
	void concurrentCreateWithSameOrderIdCreatesOnlyOne() throws Exception {
		String orderId = UUID.randomUUID().toString();

		Outcome outcome = runConcurrently(() -> paymentService.create(orderId, 5_000L));

		assertThat(outcome.successes()).hasSize(1);
		assertThat(outcome.failures()).hasSize(THREADS - 1).allMatch(e -> e instanceof DuplicateOrderException);
		assertThat(paymentRepository.findByOrderId(orderId)).isPresent();
	}

	@Test
	void concurrentApproveCallsGatewayOnce() throws Exception {
		Long id = createReady().getId();

		Outcome outcome = runConcurrently(() -> paymentService.approve(id));

		assertThat(outcome.successes()).hasSize(1);
		assertThat(outcome.failures()).hasSize(THREADS - 1).allMatch(e -> e instanceof InvalidPaymentStateException);
		verify(gateway, times(1)).approve(anyString(), anyLong());
		assertThat(paymentService.getById(id).getStatus()).isEqualTo(PaymentStatus.APPROVED);
	}

	@Test
	void concurrentCancelCallsGatewayOnce() throws Exception {
		Long id = createReady().getId();
		paymentService.approve(id);

		Outcome outcome = runConcurrently(() -> paymentService.cancel(id));

		assertThat(outcome.successes()).hasSize(1);
		assertThat(outcome.failures()).hasSize(THREADS - 1).allMatch(e -> e instanceof InvalidPaymentStateException);
		verify(gateway, times(1)).cancel(anyString(), anyLong());
		assertThat(paymentService.getById(id).getStatus()).isEqualTo(PaymentStatus.CANCELED);
	}

	@Test
	void concurrentApproveWithSameIdempotencyKeyReturnsSameResponseAndCallsGatewayOnce() throws Exception {
		Long id = createReady().getId();
		String key = UUID.randomUUID().toString();

		Outcome outcome = runConcurrently(() -> idempotencyService.execute(key, "POST /payments/" + id + "/approve",
				PaymentResponse.class, () -> PaymentResponse.from(paymentService.approve(id))));

		assertThat(outcome.failures()).isEmpty();
		assertThat(outcome.successes()).hasSize(THREADS)
				.allMatch(r -> ((PaymentResponse) r).status() == PaymentStatus.APPROVED);
		assertThat(outcome.successes()).containsOnly(outcome.successes().get(0));
		verify(gateway, times(1)).approve(anyString(), anyLong());
	}

}
