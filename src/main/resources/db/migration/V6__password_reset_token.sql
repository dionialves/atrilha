-- V6: tabela de token de recuperação de senha (US-008-a)
--
-- Cada linha armazena um UUID v4 de uso único com TTL de 1 hora.
-- O service invalida tokens pendentes ao emitir um novo (resend).

CREATE TABLE password_reset_token (
    id         UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token      UUID NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Suporta lookups de tokens ativos por usuário (resend invalida pendentes).
CREATE INDEX idx_prt_account_active
    ON password_reset_token (account_id)
    WHERE used_at IS NULL;

-- Suporta contagem de tokens emitidos por hora (rate-limit /reenviar).
CREATE INDEX idx_prt_account_created_at
    ON password_reset_token (account_id, created_at DESC);
