package com.example.payment.payment;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentResponse create(@Valid @RequestBody PaymentCreateRequest request) {
		return PaymentResponse.from(paymentService.create(request.orderId(), request.amount()));
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
	public PaymentResponse approve(@PathVariable Long id) {
		return PaymentResponse.from(paymentService.approve(id));
	}

	@PostMapping("/{id}/cancel")
	public PaymentResponse cancel(@PathVariable Long id) {
		return PaymentResponse.from(paymentService.cancel(id));
	}

}
