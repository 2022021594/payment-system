package com.example.payment.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private final PaymentRepository paymentRepository;

	public PaymentService(PaymentRepository paymentRepository) {
		this.paymentRepository = paymentRepository;
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

}
