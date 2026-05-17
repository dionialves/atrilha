-- Migration sentinela que valida o pipeline Flyway.
-- Sera removida quando a primeira entidade real entrar (Sprint 3+).
CREATE TABLE schema_baseline (
    id           SMALLINT PRIMARY KEY,
    applied_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    description  TEXT NOT NULL
);
INSERT INTO schema_baseline (id, description) VALUES (1, 'atrilha baseline schema');
