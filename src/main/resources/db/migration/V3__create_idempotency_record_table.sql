-- 멱등성 키로 처리한 요청의 응답을 저장해 같은 요청이 다시 오면 그대로 돌려준다.
CREATE TABLE idempotency_record (
    id                  BIGSERIAL   PRIMARY KEY,
    idempotency_key     VARCHAR(64) NOT NULL UNIQUE,
    request_fingerprint VARCHAR(64) NOT NULL,
    response_body       TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
