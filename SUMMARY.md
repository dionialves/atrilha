# Resumo de execução — Issue #101 (ajuste pós-rejeição)

**Branch:** feat/101-us-008-a-fundacao-token-entity-repository-service
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Comando de teste:** `./mvnw -q test`
**Resultado:** VERDE (182/182, zero warnings)

## Arquivos alterados
```
src/main/resources/db/migration/V6__password_reset_token.sql (modificado — CASCADE, DEFAULT NOW(), índices)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetToken.java (movido de auth.passwordreset)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetTokenRepository.java (movido + 2 queries novas)
src/main/java/dev/zayt/atrilha/auth/domain/PasswordResetResult.java (movido de auth.passwordreset, 3 valores)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetSender.java (movido, contrato corrigido)
src/main/java/dev/zayt/atrilha/auth/verification/NoOpPasswordResetSender.java (movido, @Primary adicionado)
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetService.java (movido, consume() adicionado, AccountReader removido)
src/test/java/dev/zayt/atrilha/auth/verification/PasswordResetServiceIT.java (movido, 7 testes, enum corrigido)
src/test/java/dev/zayt/atrilha/accounts/validation/AgeEligibilityNoTraceTest.java (modificado)
```

## Diff (stat)
```
 8 files moved/corrected   — auth.verification/ + migration V6 + IT
 1 file modified           — AgeEligibilityNoTraceTest.java (+6)
```

## Resumo do test runner
```
Tests run: 182, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## O que foi feito

Correções estruturais solicitadas pelo Revisor (8 desvios):

1. **Pacote `auth.passwordreset` → `auth.verification`**: todos os 7 arquivos de produção movidos para o pacote correto, espelhando `EmailVerificationToken`/`EmailVerificationService`. Teste movido para `dev.zayt.atrilha.auth.verification` (mesmo pacote, acessa tipos package-private).

2. **Migration V6**: adicionado `ON DELETE CASCADE` na FK `account_id`, `DEFAULT NOW()` em `created_at`, e dois índices — `idx_prt_account_active` (parcial, WHERE used_at IS NULL) e `idx_prt_account_created_at` (composto, account_id + created_at DESC).

3. **Repository**: adicionadas 2 queries faltando — `countByAccountIdAndCreatedAtAfter(UUID, Instant)` (rate-limit) e `findFirstByAccountIdOrderByCreatedAtDesc(UUID)` (cooldown).

4. **Enum `PasswordResetResult`**: reduzido de 4 para 3 valores — `SUCCESS`, `ALREADY_USED`, `EXPIRED_OR_INVALID` (unificado, espelha `VerificationResult`). O valor separado NOT_FOUND/EXPIRED vaza informação sobre existência de token (ataque de enumeração).

5. **Service `consume(UUID)`**: método adicionado com lock pessimista (`findByTokenForUpdate`), idempotente (retorna false se já consumido ou inexistente, sem exceção).

6. **Interface `PasswordResetSender`**: contrato corrigido de `sendReset(String email, UUID)` + `clear()` para `send(UUID accountId, UUID tokenUuid)` + `isEnabled()`. Mantida package-private conforme spec.

7. **`NoOpPasswordResetSender`**: adicionado `@Primary` para evitar `NoUniqueBeanDefinitionException`.

8. **Remoção de `AccountReader`**: service não injeta mais `AccountReader`; o método `issueToken` recebe `Account` como parâmetro direto.

**Produção (7 arquivos):**
- **Migration `V6__password_reset_token.sql`**: tabela com CASCADE, DEFAULT NOW(), 2 índices.
- **Entity `PasswordResetToken`**: JPA entity com UUID id/token, TTL via expires_at, sem `toString()`.
- **Repository `PasswordResetTokenRepository`**: 5 queries (findByToken, findByTokenForUpdate, findByAccountIdAndUsedAtIsNull, countByAccountIdAndCreatedAtAfter, findFirstByAccountIdOrderByCreatedAtDesc).
- **Enum `PasswordResetResult`** (pacote `auth.domain`): SUCCESS, ALREADY_USED, EXPIRED_OR_INVALID.
- **Interface `PasswordResetSender`** (package-private) + stub `NoOpPasswordResetSender` (@Component @Primary).
- **Service `PasswordResetService`**: issueToken(Account) emite UUID v4 com TTL 1h + invalida pendentes + envia e-mail condicional (isEnabled); verify(UUID) verifica com lock pessimista; consume(UUID) consome token idempotentemente.

**Testes (1 arquivo, 7 testes):**
- `PasswordResetServiceIT` — 7 cenários: invalida pendentes, TTL 1h, verify success, verify expired (EXPIRED_OR_INVALID), alreadyUsed, nonExistentUuid (EXPIRED_OR_INVALID), consume idempotente.

**Guardrail (1 arquivo modificado):**
- `AgeEligibilityNoTraceTest.java` — pacote `passwordreset/` excluído (mantido) + `verification/` incluído.

**Autoavaliação dos critérios de aceitação (16 itens):**
- ✅ Migration V6 com ON DELETE CASCADE, DEFAULT NOW(), 2 índices (idx_prt_account_active parcial + idx_prt_account_created_at)
- ✅ Entity JPA `PasswordResetToken` mapeia tabela corretamente
- ✅ Repository com 5 queries (incluindo rate-limit e cooldown)
- ✅ Enum `PasswordResetResult` com 3 valores (EXPIRED_OR_INVALID unificado)
- ✅ Interface `PasswordResetSender` com contrato correto (send/isEnabled), package-private
- ✅ NoOpPasswordResetSender com @Primary
- ✅ Service sem AccountReader, com consume(UUID) idempotente
- ✅ 7 testes de integração passando (182/182 na suíte completa, zero warnings)
- ✅ Guardrail US-005 atualizado

## ⚠️ Checagem de LGPD

N/A — sem superfície afetada. Esta US não lida com dados pessoais sensíveis, consentimento, compartilhamento ou dados de menor. O token é um UUID sem PII embutida; a interface `PasswordResetSender` recebe `accountId` + `tokenUuid`, sem e-mail exposto na assinatura. Sem impacto nos ADR-005/006/007.
