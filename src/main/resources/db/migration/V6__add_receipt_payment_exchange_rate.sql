ALTER TABLE receipts
    ADD COLUMN exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000 AFTER currency_code;

ALTER TABLE payments
    ADD COLUMN exchange_rate DECIMAL(18,6) NOT NULL DEFAULT 1.000000 AFTER currency_code;
