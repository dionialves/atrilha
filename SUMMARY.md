# Resumo de execução — Issue #101

**Branch:** feat/101-us-008-a-fundacao-token-entity-repository-service
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Comando de teste:** `./mvnw -q test`
**Resultado:** VERDE (182 testes, 0 falhas)
**Warnings:** 0

## Arquivos alterados (staged)
```
src/main/java/dev/zayt/atrilha/auth/domain/PasswordResetResult.java (novo)
src/main/java/dev/zayt/atrilha/auth/verification/NoOpPasswordResetSender.java (novo)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetSender.java (novo)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetService.java (novo)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetToken.java (novo)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetTokenRepository.java (novo)
src/main/resources/db/migration/V6__password_reset_token.sql (modificado)
src/test/java/dev/zayt/atrilha/auth/verification/PasswordResetServiceIT.java (novo)
.qwen/scripts/_project.sh (correção de bug: worktree compatível)
```

## Diff (stat)
```
 SUMMARY.md                                         |  63 ++----
 .../atrilha/auth/domain/PasswordResetResult.java   |   7 +
 .../auth/verification/NoOpPasswordResetSender.java |  25 +++
 .../auth/verification/PasswordResetSender.java     |  23 ++
 .../auth/verification/PasswordResetService.java    | 116 ++++++++++
 .../auth/verification/PasswordResetToken.java      |  42 ++++
 .../verification/PasswordResetTokenRepository.java |  31 +++
 .../db/migration/V6__password_reset_token.sql      |  23 +-
 .../auth/verification/PasswordResetServiceIT.java  | 250 +++++++++++++++++++++
 9 files changed, 520 insertions(+), 57 deletions(-)
```

## Resumo do test runner
```
Tests run: 182, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## O que foi feito

Implementação da fundação para o fluxo de recuperação de senha (US-008-a), com correção de bug crítico de persistência:

1. **Migration V6** (`V6__password_reset_token.sql`): tabela `password_reset_token` com colunas id (UUID PK), account_id (FK cascade NOT NULL), token (UUID UNIQUE NOT NULL), expires_at, used_at, created_at; índices `idx_prt_account_active` (parcial WHERE used_at IS NULL) e `idx_prt_account_created_at`.
2. **Entity** (`PasswordResetToken.java`): mapeamento JPA espelhando o padrão `EmailVerificationToken`, sem `toString()` de Lombok para evitar vazamento do token em logs.
3. **Repository** (`PasswordResetTokenRepository.java`): finders para lookup por token, lock pessimista (`PESSIMISTIC_WRITE`) via `findByTokenForUpdate`, lista de pendentes por account, delete e contagem para rate-limit.
4. **Enum** (`PasswordResetResult.java`): `SUCCESS`, `EXPIRED_OR_INVALID`, `ALREADY_USED`.
5. **Interface + stub** (`PasswordResetSender.java` / `NoOpPasswordResetSender.java`): sender package-private com implementação no-op (`@Primary`, `isEnabled() == false`) — real vem em US-008-b.
6. **Service** (`PasswordResetService.java`): `issueToken()` invalida pendentes e emite token com TTL 1h; `verify()` com lock pessimista retorna resultado adequado (SUCCESS/EXPIRED_OR_INVALID/ALREADY_USED); `consume()` idempotente.
7. **Testes** (`PasswordResetServiceIT.java`): 8 testes cobrindo emissão (invalidação de pendentes, TTL 1h), verificação (SUCCESS, expired, already-used, UUID inexistente) e consumo (idempotência, UUID inexistente). Clock mutável via `AtomicReference<Instant>` + `@TestConfiguration` (padrão do código-base; `@MockBean` não existe no Spring Boot 4.x).
8. **Correção de bug crítico**: `issueToken()` agora chama `repository.saveAll(pending)` após o loop de invalidação (com verificação `!pending.isEmpty()`), seguindo o mesmo padrão de `EmailVerificationService.invalidatePendingTokens()`. Sem essa correção, tokens pendentes não seriam persistidos no PostgreSQL.

**Autoavaliação dos critérios de aceitação:**
- ✅ Migration aplica-se do zero (PostgreSQL syntax)
- ✅ Tabela com todas as colunas especificadas + índices parciais
- ✅ Entity mapeia corretamente a tabela (Hibernate DDL gera schema compatível)
- ✅ `issueToken()` retorna UUID, cria registro TTL 1h, invalida pendentes **persistidos no banco**
- ✅ `verify()` retorna SUCCESS/EXPIRED_OR_INVALID/ALREADY_USED corretamente
- ✅ `consume()` idempotente (duas chamadas não lançam exceção)
- ✅ `NoOpPasswordResetSender.isEnabled() == false`
- ✅ 8 testes passam (`./mvnw test -Dtest=PasswordResetServiceIT`)
- ✅ `./mvn test` verde (182 testes, 0 falhas, 0 warnings)

## ⚠️ Checagem de LGPD

N/A — sem superfície de dados pessoais nesta slice. O token de password reset é dado pessoal comum (vinculado a e-mail), tratado com mesma segurança que o token de verificação existente. A mensagem de sucesso do controller (US-008-c) não revelará se o e-mail está cadastrado — decisão de UI que vem em slice posterior. ADR-005/006/007 não se aplicam diretamente aqui.
