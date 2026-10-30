package com.example.payment.idempotency;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.payment.TestcontainersConfiguration;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdempotencyApiTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JsonMapper jsonMapper;

	private static String uuid() {
		return UUID.randomUUID().toString();
	}

	private ResultActions create(String key, String orderId, long amount) throws Exception {
		var request = post("/payments").contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\": \"" + orderId + "\", \"amount\": " + amount + "}");
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return mockMvc.perform(request);
	}

	private ResultActions postTo(String path, String key) throws Exception {
		var request = post(path);
		if (key != null) {
			request.header("Idempotency-Key", key);
		}
		return mockMvc.perform(request);
	}

	private long createAndGetId(String orderId, long amount) throws Exception {
		String body = create(null, orderId, amount).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return jsonMapper.readTree(body).get("id").asLong();
	}

	@Test
	void retryingCreateWithSameKeyReturnsSameResponse() throws Exception {
		String key = uuid();
		String orderId = uuid();

		String first = create(key, orderId, 5_000).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		create(key, orderId, 5_000).andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(equalTo(jsonMapper.readTree(first).get("id").asInt())));
	}

	@Test
	void withoutKeyDuplicateCreateIsConflict() throws Exception {
		String orderId = uuid();
		create(null, orderId, 5_000).andExpect(status().isCreated());

		create(null, orderId, 5_000).andExpect(status().isConflict());
	}

	@Test
	void retryingApproveWithSameKeySucceedsAgain() throws Exception {
		long id = createAndGetId(uuid(), 5_000);
		String key = uuid();

		postTo("/payments/" + id + "/approve", key).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
		postTo("/payments/" + id + "/approve", key).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void withoutKeyApprovingTwiceIsConflict() throws Exception {
		long id = createAndGetId(uuid(), 5_000);

		postTo("/payments/" + id + "/approve", null).andExpect(status().isOk());
		postTo("/payments/" + id + "/approve", null).andExpect(status().isConflict());
	}

	@Test
	void retryingCancelWithSameKeySucceedsAgain() throws Exception {
		long id = createAndGetId(uuid(), 5_000);
		postTo("/payments/" + id + "/approve", null).andExpect(status().isOk());
		String key = uuid();

		postTo("/payments/" + id + "/cancel", key).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELED"));
		postTo("/payments/" + id + "/cancel", key).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELED"));
	}

	@Test
	void sameKeyOnDifferentRequestIsUnprocessable() throws Exception {
		String key = uuid();
		create(key, uuid(), 5_000).andExpect(status().isCreated());

		create(key, uuid(), 5_000).andExpect(status().isUnprocessableContent());
	}

	@Test
	void tooLongKeyIsBadRequest() throws Exception {
		create("k".repeat(65), uuid(), 5_000).andExpect(status().isBadRequest());
	}

}
