
## Devolução — 2026-05-28 15:23
**Veredito:** AJUSTES NECESSÁRIOS

### 🔴 BUG CRÍTICO — `issueToken()` não persiste invalidação de tokens pendentes

**Arquivo:** `src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetService.java`, método `issueToken()`, linhas 47–52

**Problema:** O código modifica tokens pendentes em memória (`t.setUsedAt(now)`) mas **nunca chama `repository.saveAll(pending)`** para persistir as mudanças.

```java
// Linha 47-52 — tokens são modificados mas NUNCA salvos
List<PasswordResetToken> pending = repository.findByAccountIdAndUsedAtIsNull(account.getId());
Instant now = clock.instant();
for (PasswordResetToken t : pending) {
    t.setUsedAt(now);  // ← modifica em memória apenas!
}
// ← FALTA: repository.saveAll(pending);
```

**Contraste com padrão existente:** `EmailVerificationService.invalidatePendingTokens()` (linha 193) faz exatamente o mesmo loop **e depois chama** `tokenRepository.saveAll(pending)` com verificação de `!pending.isEmpty()`.

**Impacto:** O teste `issueToken_invalidaPendentes()` passa porque H2 faz auto-flush dentro da mesma transação. Em produção com PostgreSQL, as atualizações seriam **perdidas silenciosamente** — tokens antigos permaneceriam válidos, permitindo uso concorrente do mesmo token de recuperação.

**Correção:** Adicionar após o loop:
```java
if (!pending.isEmpty()) {
    repository.saveAll(pending);
}
```

---

### 📋 Auditoria completa (4 camadas)

#### Layer A — Aderência ao plano
| Passo do plano | Status | Evidência |
|---|---|---|
| Migration V6 (colunas + índices) | ✅ | `V6__password_reset_token.sql` — id UUID PK, account_id FK cascade NOT NULL, token UUID UNIQUE, expires_at/used_at/created_at TIMESTAMPTZ; índices `idx_prt_account_active` (parcial) e `idx_prt_account_created_at` |
| Entity `PasswordResetToken` | ✅ | Mapeamento JPA espelhando `EmailVerificationToken`; sem `toString()` de Lombok (proteção contra vazamento) |
| Repository `PasswordResetTokenRepository` | ✅ | 6 finders: `findByToken`, `findByTokenForUpdate` (PESSIMISTIC_WRITE), `findByAccountIdAndUsedAtIsNull`, `deleteByAccountIdAndUsedAtIsNull`, `countByAccountIdAndCreatedAtAfter`, `findFirstByAccountIdOrderByCreatedAtDesc` |
| Enum `PasswordResetResult` | ✅ | 3 valores: SUCCESS, EXPIRED_OR_INVALID, ALREADY_USED — espelhando `VerificationResult` |
| Interface + stub sender | ✅ | `PasswordResetSender` (package-private) + `NoOpPasswordResetSender` (`@Component @Primary`, `isEnabled() == false`) |
| Service com 3 métodos | ✅ | `issueToken`, `verify` (com lock pessimista), `consume` (idempotente) |
| Testes (8 testes) | ✅ | 6 previstos no plano + 2 extras (`consume_idempotent`, `consume_nonExistent_doesNotThrow`) |
| Worktree via `start_task.sh` | ✅ | Branch `feat/101-us-008-a-fundacao-token-entity-repository-service` |
| SUMMARY.md preenchido | ✅ | Inclui seção "⚠️ Checagem de LGPD" com "N/A" |

#### Layer B — Qualidade técnica
| Critério | Status | Observação |
|---|---|---|
| Zero warnings de compilação | ✅ | `./mvnw compile -q` — zero warnings |
| Testes verdes (182) | ✅ | `./mvnw -q test` — 0 falhas, 0 erros |
| Convenções de nome/pacote | ✅ | `auth.verification` para tokens/services; `auth.domain` para enums de resultado |
| Logging (SLF4J) | ✅ | `NoOpPasswordResetSender` usa `LoggerFactory.getLogger(ClassName.class)` — padrão dominante no projeto |
| Separação de camadas | ✅ | Entity → Repository → Service — sem vazamento |
| Injeção de dependência | ✅ | Construtor com 3 deps (repository, sender, clock) — padrão do projeto |
| Clock bean override em teste | ✅ | `@TestConfiguration` com `@Primary` — padrão consistente com `EmailVerificationServiceIT` |
| **Persistência de tokens pendentes** | ❌ | **BUG: falta `saveAll(pending)` — ver detalhe acima** |

#### Layer C — Critérios de aceitação
| Critério | Status | Evidência |
|---|---|---|
| V6 aplica-se do zero (PostgreSQL) | ✅ | Flyway valida e aplica V6 nos logs do test runner |
| Tabela com colunas + índices | ✅ | Migration contém todas as colunas e ambos os índices (parcial + composto) |
| Entity mapeia corretamente | ✅ | Hibernate DDL gera schema compatível; Flyway validate passa |
| `issueToken()` retorna UUID, TTL 1h, invalida pendentes | ❌ | **Retorna UUID ✅ + TTL 1h ✅ — mas NÃO invalida pendentes no banco ❌** |
| `verify()` retorna SUCCESS + marca used_at | ✅ | Teste `verify_success` passa |
| `verify()` retorna EXPIRED_OR_INVALID sem marcar used_at | ✅ | Teste `verify_expired` passa |
| `verify()` retorna ALREADY_USED para token consumido | ✅ | Teste `verify_alreadyUsed` passa |
| `verify()` retorna EXPIRED_OR_INVALID sem lançar exceção | ✅ | Teste `verify_nonExistentUuid` passa |
| `consume()` é idempotente | ✅ | Teste `consume_idempotent` passa (duas chamadas, mesmo used_at) |
| `NoOpPasswordResetSender.isEnabled() == false` | ✅ | Código direto; teste implícito via `if (sender.isEnabled())` no service |
| 8 testes de PasswordResetServiceIT passam | ✅ | `./mvnw test -Dtest=PasswordResetServiceIT` — 8/8 verdes |
| `./mvnw test` verde, zero warnings | ✅ | 182 testes, 0 falhas, 0 warnings |
| Worktree criada via start_task.sh | ✅ | Branch existe em `.qwen/worktrees/` |
| Testes escritos ANTES do código (TDD) | ✅ | SUMMARY.md afirma; diff confirma novos arquivos |
| Commit único no padrão conventional | ⏳ | Gerado pelo `approve.sh` (ainda não executado) |
| PR DRAFT com Closes #101 | ⏳ | Gerado pelo `approve.sh` (ainda não executado) |

#### Layer D — Coerência com padrões implícitos do projeto
| Frente | Status | Evidência |
|---|---|---|
| **1. Padrões análogos** | ❌ | `PasswordResetService.issueToken()` modifica entidades em memória sem persistir; 3+ análogos usam `saveAll` após modificação em lote: `EmailVerificationService.invalidatePendingTokens()` (linha 193), `EmailVerificationTokenRepository.saveAll()`, padrão JPA de batch update. **Sem justificativa no SUMMARY.** |
| **2. Cross-cutting concerns** | ✅ | Logging: `LoggerFactory.getLogger(ClassName.class)` — padrão dominante (4+ análogos em `auth/`). Clock: `@TestConfiguration` + `@Primary` — padrão consistente com `EmailVerificationServiceIT`. Entity sem `toString()`: padrão de segurança (mesmo padrão em `EmailVerificationToken`). |
| **3. Detecção de duplicação** | ✅ | Nenhum helper/util duplicado. `AccountTestFactory` reutilizado de `accounts/` — padrão existente (usado por `EmailVerificationServiceIT`). |
| **4. Dívida arrastada / refactor** | ✅ | Nenhuma API `@Deprecated` usada. Pacote `auth.verification` é consistente com estrutura existente. Nenhuma migração de pacote em curso afetando este delta. |

---

### ✅ Resumo do que está CORRETO
- Migration V6: schema, colunas, tipos e índices estão perfeitos
- Entity `PasswordResetToken`: mapeamento JPA idêntico ao padrão `EmailVerificationToken`
- Repository: todos os 6 finders corretos, incluindo `PESSIMISTIC_WRITE`
- Enum `PasswordResetResult`: valores e ordem consistentes com `VerificationResult`
- Interface + stub: package-private, `@Primary`, `isEnabled() == false` — correto
- Método `verify()`: lógica de verificação com lock pessimista está correta
- Método `consume()`: idempotente, safe para UUID inexistente — correto
- Testes: 8 testes cobrindo todos os cenários (SUCCESS, expired, already-used, non-existent, idempotência)
- Zero warnings de compilação, 182 testes verdes

### 🔴 O que precisa corrigir
1. **`PasswordResetService.issueToken()`** — adicionar `repository.saveAll(pending)` após o loop de invalidação (com verificação `!pending.isEmpty()` opcional, mas recomendado para evitar INSERT desnecessário)
2. **Teste `issueToken_invalidaPendentes()`** — após a correção, o teste já deve continuar passando (H2 auto-flush + `saveAll` explícito = dupamente seguro). Opcional: adicionar `repository.flush()` ou verificar via `em.clear()` + re-fetch para eliminar dependência do auto-flush.

---

### 📌 Caminho para re-submissão
1. Corrigir `PasswordResetService.java` (adicionar `saveAll`)
2. Re-executar `./mvnw test` — garantir 182 testes verdes + zero warnings
3. Atualizar `SUMMARY.md` com nota sobre a correção
4. Chamar `finish_task 101` novamente
