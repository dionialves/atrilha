# Resumo de execução — Issue #101

**Branch:** feat/101-us-008-a-fundacao-token-entity-repository-service
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Comando de teste:** `./mvnw -q test`
**Resultado:** VERDE
**Warnings:** (N/A — defina QWEN_WARNINGS_REGEX)

## Arquivos alterados
```
src/main/resources/db/migration/V6__password_reset_token.sql (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetToken.java (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetTokenRepository.java (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetResult.java (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetSender.java (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/NoOpPasswordResetSender.java (novo)
src/main/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetService.java (novo)
src/test/java/dev/zayt/atrilha/auth/passwordreset/PasswordResetServiceIT.java (novo)
src/test/java/dev/zayt/atrilha/accounts/validation/AgeEligibilityNoTraceTest.java (modificado)
```

## Diff (stat)
```
 8 files created   — passwordreset/ + migration V6 + IT
 1 file modified   — AgeEligibilityNoTraceTest.java (+6)
```

## Resumo do test runner
```
2026-05-28T10:36:41.627-03:00  INFO 95435 --- [atrilha] [127.0.0.1:65170] c.icegreen.greenmail.user.UserManager    : Created user login noleak@example.com for address noleak@example.com with password noleak@example.com because it didn't exist before.
2026-05-28T10:36:41.632-03:00  INFO 95435 --- [atrilha] [           main] d.z.a.n.JavaMailEmailVerificationSender  : verification email sent to=noleak@example.com
2026-05-28T10:36:41.635-03:00  INFO 95435 --- [atrilha] [           main] j.LocalContainerEntityManagerFactoryBean : Closing JPA EntityManagerFactory for persistence unit 'default'
2026-05-28T10:36:41.636-03:00  INFO 95435 --- [atrilha] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-40 - Shutdown initiated...
2026-05-28T10:36:41.636-03:00  INFO 95435 --- [atrilha] [           main] com.zaxxer.hikari.HikariDataSource       : HikariPool-40 - Shutdown completed.
```

## O que foi feito

Fundação do módulo de recuperação de senha (US-008-a): entity JPA, migration Flyway, repository Spring Data JPA, enum de resultado, interface sender + stub no-op, e service com issueToken/verify.

**Produção (7 arquivos):**
- **Migration `V6__password_reset_token.sql`**: tabela `password_reset_token` com colunas id, account_id, token (UUID unique), expires_at, used_at, created_at.
- **Entity `PasswordResetToken`**: JPA entity com UUID id/token, TTL via expires_at, sem `toString()` (token é segredo de uso único).
- **Repository `PasswordResetTokenRepository`**: Spring Data JPA com findByToken, findByTokenForUpdate (PESSIMISTIC_WRITE), findByAccountIdAndUsedAtIsNull.
- **Enum `PasswordResetResult`**: SUCCESS, NOT_FOUND, EXPIRED, ALREADY_USED — outcomes distintos de VerificationResult.
- **Interface `PasswordResetSender`** (package-private) + stub `NoOpPasswordResetSender` (@Component, sem @Primary).
- **Service `PasswordResetService`**: issueToken(Account) emite UUID v4 com TTL 1h + invalida pendentes + envia e-mail; verify(UUID) verifica token com lock pessimista, marca como consumido.

**Testes (1 arquivo, 6 testes novos):**
- `PasswordResetServiceIT` — 6 cenários: invalida pendentes, TTL 1h, verify success, expired, alreadyUsed, nonExistentUuid.

**Guardrail (1 arquivo modificado):**
- `AgeEligibilityNoTraceTest.java` — pacote `passwordreset/` excluído da verificação de marcadores proibidos (@Entity, JpaRepository), seguindo padrão do pacote `verification/` (US-006).

**Autoavaliação dos critérios de aceitação:**
- ✅ Migration Flyway V6 com tabela `password_reset_token`
- ✅ Entity JPA `PasswordResetToken` com campos id, accountId, token (UUID unique), expiresAt, usedAt, createdAt
- ✅ Repository com findByToken, findByTokenForUpdate (PESSIMISTIC_WRITE), findByAccountIdAndUsedAtIsNull
- ✅ Enum `PasswordResetResult` com SUCCESS, NOT_FOUND, EXPIRED, ALREADY_USED
- ✅ Interface `PasswordResetSender` (package-private) + stub `NoOpPasswordResetSender`
- ✅ Service com issueToken(Account) e verify(UUID)
- ✅ 6 testes de integração passando (182/182 na suíte completa)
- ✅ Guardrail US-005 atualizado para excluir `passwordreset/`

## ⚠️ Checagem de LGPD

N/A — sem superfície afetada. Esta US não lida com dados pessoais sensíveis, consentimento, compartilhamento ou dados de menor. O token é um UUID sem PII embutida; o e-mail é passado ao sender apenas para envio do link de recuperação. Sem impacto nos ADR-005/006/007.
