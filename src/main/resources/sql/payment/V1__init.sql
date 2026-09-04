-- V1__init.sql
-- Initial schema for fret-payment service: fatourati_token + fatourati_callback_log
CREATE TABLE IF NOT EXISTS fatourati_token (
    id BIGSERIAL PRIMARY KEY,
    token_ref VARCHAR(64) NOT NULL UNIQUE,
    mouvement_id VARCHAR(100) NOT NULL,
    order_id VARCHAR(64),
    total_amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(8) DEFAULT '504',
    status VARCHAR(20) DEFAULT 'CREATED',
    qr_code TEXT,
    channels JSONB,
    expires_at TIMESTAMP,
    raw_response TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fatourati_callback_log (
    id BIGSERIAL PRIMARY KEY,
    token_ref VARCHAR(64),
    sys_pmt_code VARCHAR(64),
    num_trx_fatourati VARCHAR(64),
    num_trx_sys_pmt VARCHAR(64),
    total_amount NUMERIC(15,2),
    raw_body JSONB,
    signature_valid BOOLEAN NOT NULL,
    decision_code INT,
    error_message VARCHAR(500),
    received_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fatourati_token_mouvement ON fatourati_token(mouvement_id);
CREATE INDEX IF NOT EXISTS idx_fatourati_token_token_ref ON fatourati_token(token_ref);
CREATE INDEX IF NOT EXISTS idx_fatourati_callback_num_trx ON fatourati_callback_log(num_trx_fatourati);
CREATE INDEX IF NOT EXISTS idx_fatourati_callback_token ON fatourati_callback_log(token_ref);
