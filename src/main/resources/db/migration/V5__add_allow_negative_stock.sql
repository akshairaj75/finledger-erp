ALTER TABLE companies
    ADD COLUMN allow_negative_stock TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active;

