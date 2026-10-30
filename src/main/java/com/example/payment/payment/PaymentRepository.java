package com.example.payment.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByOrderId(String orderId);

	/** 승인·취소처럼 상태를 바꾸는 작업이 동시에 같은 결제를 처리하지 못하도록 행을 잠근다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Payment p where p.id = :id")
	Optional<Payment> findByIdForUpdate(@Param("id") Long id);

}
