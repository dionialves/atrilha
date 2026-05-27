-- US-003: tabela de perfil do responsável (guardian).
-- Relação 1:1 com accounts via account_id (FK cascata).
-- full_name é obrigatório para atender RF-E1-11 (perfil mínimo do responsável).

CREATE TABLE guardian_profiles (
    account_id  UUID         PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    full_name   VARCHAR(100) NOT NULL
);
