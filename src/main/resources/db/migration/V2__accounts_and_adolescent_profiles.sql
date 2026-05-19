-- Sprint 3 / US-001: contas polimórficas (ADOLESCENT | GUARDIAN) + perfil de adolescente.
-- Esquema seguindo PRD §10.1: polimorfismo por coluna `type`, perfis 1:1 com PK compartilhada.

CREATE TABLE accounts (
    id                  UUID         PRIMARY KEY,
    type                VARCHAR(16)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    email_verified_at   TIMESTAMPTZ  NULL,
    password_hash       VARCHAR(72)  NULL,        -- BCrypt = 60 chars; folga para outros encoders.
    oauth_provider      VARCHAR(32)  NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at       TIMESTAMPTZ  NULL,
    deleted_at          TIMESTAMPTZ  NULL,
    CONSTRAINT accounts_type_chk CHECK (type IN ('ADOLESCENT', 'GUARDIAN')),
    -- Credencial: exatamente uma estratégia de auth — XOR password_hash / oauth_provider.
    CONSTRAINT accounts_credential_chk CHECK (
        (password_hash IS NOT NULL AND oauth_provider IS NULL) OR
        (password_hash IS NULL     AND oauth_provider IS NOT NULL)
    )
);

-- Unicidade case-insensitive de e-mail, ignorando contas soft-deletadas.
CREATE UNIQUE INDEX accounts_email_unique
    ON accounts (LOWER(email))
    WHERE deleted_at IS NULL;

CREATE TABLE adolescent_profiles (
    account_id   UUID         PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    nickname     VARCHAR(20)  NOT NULL,
    birth_date   DATE         NOT NULL,
    avatar_url   VARCHAR(255) NULL,
    timezone     VARCHAR(64)  NOT NULL DEFAULT 'America/Sao_Paulo',
    CONSTRAINT adolescent_profiles_nickname_len_chk
        CHECK (CHAR_LENGTH(nickname) BETWEEN 3 AND 20)
);
