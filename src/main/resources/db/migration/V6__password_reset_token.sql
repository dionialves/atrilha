-- V6: tabela de token de recuperação de senha (US-008-a)
--
-- Cada linha armazena um UUID v4 de uso único com TTL de 1 hora.
-- O service invalida tokens pendentes ao emitir um novo (resend).

create table password_reset_token (
    id          uuid not null primary key,
    account_id  uuid   not null references accounts(id),
    token       uuid   not null unique,
    expires_at  timestamp with time zone not null,
    used_at     timestamp with time zone,
    created_at  timestamp with time zone not null
);
