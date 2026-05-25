-- REF-003: Remove integracao Google OAuth (decisao do produto apos 3 fixes falhas).
-- Pre-condicao: nenhuma conta ativa pode estar usando oauth_provider.
-- Se houver, esta migration FALHA propositalmente — humano precisa decidir
-- (converter para senha temporaria? soft-delete? exportar?) antes de aplicar.

DO $$
DECLARE
    google_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO google_count
    FROM accounts
    WHERE oauth_provider IS NOT NULL
      AND deleted_at IS NULL;

    IF google_count > 0 THEN
        RAISE EXCEPTION
            'REF-003: % conta(s) ativa(s) ainda usam oauth_provider. Migracao abortada — resolva manualmente antes de re-aplicar.',
            google_count;
    END IF;
END $$;

-- Constraint XOR fica obsoleta — credencial passa a ser obrigatoriamente password_hash.
ALTER TABLE accounts DROP CONSTRAINT accounts_credential_chk;

-- Drop da coluna oauth_provider.
ALTER TABLE accounts DROP COLUMN oauth_provider;

-- Novo CHECK: toda conta ativa precisa ter password_hash.
-- (Contas soft-deletadas podem ter password_hash NULL — preserva historia.)
ALTER TABLE accounts ADD CONSTRAINT accounts_credential_chk
    CHECK (password_hash IS NOT NULL OR deleted_at IS NOT NULL);
