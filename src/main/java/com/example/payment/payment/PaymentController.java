package com.example.payment.payment;

import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.payment.idempotency.IdempotencyService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

	private final PaymentService paymentService;

	private final IdempotencyService idempotencyService;

	public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
		this.paymentService = paymentService;
		this.idempotencyService = idempotencyService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentResponse create(
			@RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
			@Valid @RequestBody PaymentCreateRequest request) {
		return idempotent(idempotencyKey, "POST /payments|" + request.orderId() + "|" + request.amount(),
				() -> PaymentResponse.from(paymentService.create(request.orderId(), request.amount())));
	}

	@GetMapping("/{id}")
	public PaymentResponse get(@PathVariable Long id) {
		return PaymentResponse.from(paymentService.getById(id));
	}

	@GetMapping(params = "orderId")
	public PaymentResponse getByOrderId(@RequestParam String orderId) {
		return PaymentResponse.from(paymentService.getByOrderId(orderId));
	}

	@PostMapping("/{id}/approve")
	public PaymentResponse approve(
			@RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
			@PathVariable Long id) {
		return idempotent(idempotencyKey, "POST /payments/" + id + "/approve",
				() -> PaymentResponse.from(paymentService.approve(id)));
	}

	@PostMapping("/{id}/cancel")
	public PaymentResponse cancel(
			@RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
			@PathVariable Long id) {
		return idempotent(idempotencyKey, "POST /payments/" + id + "/cancel",
				() -> PaymentResponse.from(paymentService.cancel(id)));
	}

	/** 멱등성 키가 있으면 한 번만 실행하고 응답을 재사용하며, 없으면 그대로 실행한다. */
	private PaymentResponse idempotent(String idempotencyKey, String fingerprint, Supplier<PaymentResponse> action) {
		if (idempotencyKey == null) {
			return action.get();
		}
		return idempotencyService.execute(idempotencyKey, fingerprint, PaymentResponse.class, action);
	}

}
