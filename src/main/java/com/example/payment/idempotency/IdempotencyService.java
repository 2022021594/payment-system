package com.example.payment.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * 멱등성 키로 요청을 한 번만 실행하고, 같은 요청이 다시 오면 저장해 둔 응답을 돌려준다.
 * <p>
 * 요청 실행과 응답 저장은 한 트랜잭션이라 함께 커밋되거나 함께 롤백된다. 같은 키의 동시 요청은
 * 트랜잭션 단위 advisory lock으로 직렬화하므로, 뒤따르는 요청은 먼저 끝난 요청의 응답을 재사용한다.
 * 예외로 끝난 요청은 저장하지 않으므로 같은 키로 다시 실행된다.
 */
@Service
public class IdempotencyService {

	static final int MAX_KEY_LENGTH = 64;

	private final IdempotencyRecordRepository repository;

	private final EntityManager entityManager;

	private final JsonMapper jsonMapper;

	public IdempotencyService(IdempotencyRecordRepository repository, EntityManager entityManager,
			JsonMapper jsonMapper) {
		this.repository = repository;
		this.entityManager = entityManager;
		this.jsonMapper = jsonMapper;
	}

	/**
	 * @param fingerprint 요청의 의미를 나타내는 문자열(메서드, 경로, 본문 값). 같은 키에 다른 값이 오면 거절한다.
	 */
	@Transactional
	public <T> T execute(String key, String fingerprint, Class<T> responseType, Supplier<T> action) {
		if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
			throw new InvalidIdempotencyKeyException();
		}
		entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtext(?1))::text")
				.setParameter(1, key)
				.getSingleResult();

		String fingerprintHash = sha256(fingerprint);
		Optional<IdempotencyRecord> existing = repository.findByIdempotencyKey(key);
		if (existing.isPresent()) {
			if (!existing.get().getRequestFingerprint().equals(fingerprintHash)) {
				throw new IdempotencyKeyReuseException();
			}
			return jsonMapper.readValue(existing.get().getResponseBody(), responseType);
		}

		T response = action.get();
		repository.save(new IdempotencyRecord(key, fingerprintHash, jsonMapper.writeValueAsString(response)));
		return response;
	}

	private static String sha256(String value) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
