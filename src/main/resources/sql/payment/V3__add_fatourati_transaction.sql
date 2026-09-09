-- fatourati_transaction: per-callback transaction audit table
-- Captures the full detail of every CMI callback for a token
CREATE TABLE IF NOT EXISTS fatourati_transaction (
    id                                BIGSERIAL PRIMARY KEY,
    token_ref                         VARCHAR(64) NOT NULL,
    aggregator_code                   VARCHAR(64),
    channel                           VARCHAR(64),
    operator                          VARCHAR(64),
    terminal_id                       VARCHAR(64),
    fatourati_transaction_number      VARCHAR(100),
    payment_system_transaction_number VARCHAR(100),
    payment_mode                      VARCHAR(50),
    amount                            NUMERIC(15,2),
    currency                          VARCHAR(8),
    transaction_date                  TIMESTAMP,
    receipt_number                    VARCHAR(100),
    status                            VARCHAR(50),
    selected_items                    JSONB,
    raw_payload                       JSONB,
    created_at                        TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fatourati_tx_token_ref ON fatourati_transaction(token_ref);
CREATE INDEX IF NOT EXISTS idx_fatourati_tx_fatourati_trx ON fatourati_transaction(fatourati_transaction_number);
CREATE INDEX IF NOT EXISTS idx_fatourati_tx_created_at ON fatourati_transaction(created_at);
