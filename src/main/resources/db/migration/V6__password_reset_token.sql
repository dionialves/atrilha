-- V6: password_reset_token (US-008-a)
CREATE TABLE password_reset_token (
    id          UUID PRIMARY KEY,
    account_id  UUID NOT NULL REFERENCES accounts ON DELETE CASCADE,
    token       UUID NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_account_active ON password_reset_token (account_id) WHERE used_at IS NULL;
CREATE INDEX idx_prt_account_created_at ON password_reset_token (account_id, created_at DESC);
