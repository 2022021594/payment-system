package com.example.payment.payment;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	PaymentService paymentService;

	private ResultActions postJson(String body) throws Exception {
		return mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void createsPayment() throws Exception {
		when(paymentService.create("order-1", 15_000L)).thenReturn(new Payment("order-1", 15_000L));

		postJson("""
				{"orderId": "order-1", "amount": 15000}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderId").value("order-1"))
				.andExpect(jsonPath("$.amount").value(15000))
				.andExpect(jsonPath("$.currency").value("KRW"))
				.andExpect(jsonPath("$.status").value("READY"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"amount\": 1000}",
			"{\"orderId\": \"\", \"amount\": 1000}",
			"{\"orderId\": \"   \", \"amount\": 1000}",
			"{\"orderId\": \"order-1\"}",
			"{\"orderId\": \"order-1\", \"amount\": 0}",
			"{\"orderId\": \"order-1\", \"amount\": -100}"
	})
	void rejectsInvalidInput(String body) throws Exception {
		postJson(body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors").isMap());
		verify(paymentService, never()).create(anyString(), anyLong());
	}

	@Test
	void rejectsTooLongOrderId() throws Exception {
		postJson("{\"orderId\": \"" + "a".repeat(65) + "\", \"amount\": 1000}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.orderId").exists());
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		postJson("{not json").andExpect(status().isBadRequest());
	}

	@Test
	void returnsConflictForDuplicateOrder() throws Exception {
		when(paymentService.create(eq("order-1"), anyLong())).thenThrow(new DuplicateOrderException("order-1"));

		postJson("{\"orderId\": \"order-1\", \"amount\": 1000}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void getsPaymentById() throws Exception {
		when(paymentService.getById(1L)).thenReturn(new Payment("order-1", 15_000L));

		mockMvc.perform(get("/payments/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value("order-1"))
				.andExpect(jsonPath("$.amount").value(15000))
				.andExpect(jsonPath("$.status").value("READY"));
	}

	@Test
	void returnsNotFoundForUnknownId() throws Exception {
		when(paymentService.getById(99L)).thenThrow(new PaymentNotFoundException(99L));

		mockMvc.perform(get("/payments/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void getsPaymentByOrderId() throws Exception {
		when(paymentService.getByOrderId("order-1")).thenReturn(new Payment("order-1", 15_000L));

		mockMvc.perform(get("/payments").param("orderId", "order-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value("order-1"));
	}

	@Test
	void returnsNotFoundForUnknownOrderId() throws Exception {
		when(paymentService.getByOrderId("nope")).thenThrow(new PaymentNotFoundException("nope"));

		mockMvc.perform(get("/payments").param("orderId", "nope"))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsNonNumericId() throws Exception {
		mockMvc.perform(get("/payments/abc")).andExpect(status().isBadRequest());
	}

}
