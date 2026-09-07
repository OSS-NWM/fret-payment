-- fatourati_token_status_history: audit trail for payment status transitions
CREATE TABLE IF NOT EXISTS fatourati_token_status_history (
    id                BIGSERIAL PRIMARY KEY,
    token_ref         VARCHAR(64) NOT NULL,
    previous_status   VARCHAR(20),
    new_status        VARCHAR(20) NOT NULL,
    reason            VARCHAR(64) NOT NULL,
    actor             VARCHAR(64),
    channel           VARCHAR(64),
    operator          VARCHAR(64),
    occurred_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_status_history_token_ref   ON fatourati_token_status_history(token_ref);
CREATE INDEX IF NOT EXISTS idx_status_history_occurred_at ON fatourati_token_status_history(occurred_at);

-- fatourati_token: add payment_channel and payment_operator columns
ALTER TABLE fatourati_token
    ADD COLUMN IF NOT EXISTS payment_channel VARCHAR(64),
    ADD COLUMN IF NOT EXISTS payment_operator VARCHAR(64);
