package com.example.payment.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;

	private final PaymentGateway paymentGateway;

	public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
		this.paymentRepository = paymentRepository;
		this.paymentGateway = paymentGateway;
	}

	@Transactional
	public Payment create(String orderId, long amount) {
		if (paymentRepository.findByOrderId(orderId).isPresent()) {
			throw new DuplicateOrderException(orderId);
		}
		try {
			// 동시 요청이 위 확인을 함께 통과해도 DB 유니크 제약이 막는다.
			return paymentRepository.saveAndFlush(new Payment(orderId, amount));
		}
		catch (DataIntegrityViolationException e) {
			throw new DuplicateOrderException(orderId);
		}
	}

	@Transactional(readOnly = true)
	public Payment getById(Long id) {
		return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Payment getByOrderId(String orderId) {
		return paymentRepository.findByOrderId(orderId).orElseThrow(() -> new PaymentNotFoundException(orderId));
	}

	/**
	 * 결제를 승인한다. PG가 거절하면 예외 없이 FAILED로 저장한다.
	 * 행 잠금을 잡은 채 PG를 호출하므로 같은 결제가 두 번 승인 요청되지 않는다.
	 */
	@Transactional
	public Payment approve(Long id) {
		Payment payment = paymentRepository.findByIdForUpdate(id).orElseThrow(() -> new PaymentNotFoundException(id));
		if (payment.getStatus() != PaymentStatus.READY) {
			throw new InvalidPaymentStateException(payment.getStatus(), PaymentStatus.APPROVED);
		}
		PaymentGatewayResult result = paymentGateway.approve(payment.getOrderId(), payment.getAmount());
		if (result.approved()) {
			payment.approve();
		}
		else {
			payment.fail();
		}
		return payment;
	}

	/**
	 * 승인된 결제를 전액 취소한다. PG가 취소에 성공했을 때만 CANCELED로 바꾸고,
	 * 거절되면 상태를 그대로 둔 채 {@link PaymentCancelRejectedException}을 던진다.
	 */
	@Transactional
	public Payment cancel(Long id) {
		Payment payment = paymentRepository.findByIdForUpdate(id).orElseThrow(() -> new PaymentNotFoundException(id));
		if (payment.getStatus() != PaymentStatus.APPROVED) {
			throw new InvalidPaymentStateException(payment.getStatus(), PaymentStatus.CANCELED);
		}
		PaymentGatewayResult result = paymentGateway.cancel(payment.getOrderId(), payment.getAmount());
		if (!result.approved()) {
			throw new PaymentCancelRejectedException(result.reason());
		}
		payment.cancel();
		return payment;
	}

}
