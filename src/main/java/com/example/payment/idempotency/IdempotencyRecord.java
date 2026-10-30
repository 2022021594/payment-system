package com.example.payment.idempotency;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
	private String idempotencyKey;

	@Column(name = "request_fingerprint", nullable = false, length = 64)
	private String requestFingerprint;

	@Column(name = "response_body", nullable = false, columnDefinition = "text")
	private String responseBody;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected IdempotencyRecord() {
	}

	public IdempotencyRecord(String idempotencyKey, String requestFingerprint, String responseBody) {
		this.idempotencyKey = idempotencyKey;
		this.requestFingerprint = requestFingerprint;
		this.responseBody = responseBody;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRequestFingerprint() {
		return requestFingerprint;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

}
