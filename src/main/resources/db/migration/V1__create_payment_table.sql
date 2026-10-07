CREATE TABLE payment (
    id              BIGSERIAL      PRIMARY KEY,
    order_id        VARCHAR(64)    NOT NULL UNIQUE,
    amount          NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)     NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_status ON payment (status);
