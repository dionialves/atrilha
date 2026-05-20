-- Sprint 3 / US-006: tokens de verificação de e-mail.
-- A tabela `accounts` (US-001 / V2) já contém a coluna `email_verified_at`.
-- Esta migration apenas cria a tabela de tokens e seus índices.

CREATE TABLE email_verification_token (
    id           UUID         PRIMARY KEY,
    account_id   UUID         NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    token        UUID         NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ  NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Suporta lookups de tokens ativos por usuário (resend invalida pendentes).
CREATE INDEX idx_evt_account_active
    ON email_verification_token (account_id)
    WHERE used_at IS NULL;

-- Suporta contagem de tokens emitidos por hora (rate-limit /reenviar).
CREATE INDEX idx_evt_account_created_at
    ON email_verification_token (account_id, created_at DESC);
