package com.example.payment.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 결제 요청 생성 입력. amount는 원 단위 정수.
 * 누락(null)과 0을 구분하기 위해 박싱 타입을 쓴다.
 */
public record PaymentCreateRequest(
		@NotBlank @Size(max = 64) String orderId,
		@NotNull @Positive Long amount) {
}
