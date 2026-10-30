-- 금액은 원 단위 정수(long)로 다룬다.
ALTER TABLE payment DROP CONSTRAINT IF EXISTS payment_amount_check;
ALTER TABLE payment ALTER COLUMN amount TYPE BIGINT USING amount::BIGINT;
ALTER TABLE payment ADD CONSTRAINT payment_amount_positive CHECK (amount > 0);
