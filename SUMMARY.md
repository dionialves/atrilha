# Resumo de execução — Issue #58

**Branch:** feat/58-feat-007-04-cria-servico-de-rate-limit-de-login
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/java/dev/zayt/atrilha/auth/config/LoginRateLimitProperties.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptKey.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptService.java
src/test/java/dev/zayt/atrilha/auth/login/LoginAttemptServiceTest.java
```

## Diff (stat)
```
 .../auth/config/LoginRateLimitProperties.java      |  39 +++
 .../zayt/atrilha/auth/login/LoginAttemptKey.java   |  35 +++
 .../atrilha/auth/login/LoginAttemptService.java    | 101 ++++++++
 .../auth/login/LoginAttemptServiceTest.java        | 263 +++++++++++++++++++++
 4 files changed, 438 insertions(+)
```

## O que foi feito
Implementa o mecanismo de rate-limit in-memory para proteção contra brute-force no form login (Issue #58 / US-007.04).

**3 arquivos de produção + 1 de testes:**
- `LoginAttemptKey` — record imutável com factory `of(email, ip)` que normaliza e-mail (`trim().toLowerCase(Locale.ROOT)`). Equals/hashCode baseados nos campos normalizados.
- `LoginRateLimitProperties` — `@ConfigurationProperties(prefix = "atrilha.auth.login")` com 3 propriedades: `maxAttempts` (default 5), `attemptWindow` (default PT15M), `blockDuration` (default PT15M). Validação de defaults no initializer.
- `LoginAttemptService` — `@Service` com `ConcurrentHashMap<LoginAttemptKey, AttemptState>` + `Clock` injetável (bean existente em `AgeEligibilityConfig`). API: `isBlocked(key)`, `registerFailure(key)`, `registerSuccess(key)`. Lógica: reinicia contagem quando janela expira; bloqueia ao atingir `maxAttempts`; desbloqueia após `blockDuration`.

**12 testes unitários cobrindo:** normalização de chave, incremento progressivo até bloqueio, reset por sucesso, expiração de janela (Clock mutável), expiração de bloqueio (Clock mutável), isolamento por email/IP, noop em chave inexistente, e defaults de properties.

**Decisões:**
- `Clock` reutiliza bean existente (`AgeEligibilityConfig.atrilhaClock()`), sem duplicação.
- `LoginRateLimitProperties` é `public` (necessário porque `LoginAttemptService` está em package diferente).
- Sem logs com dados sensíveis — nenhuma chamada de log no service.

**Critérios de aceitação:** todos [x] satisfeitos (ver testes).

## ⚠️ Checagem LGPD (atrilha)
N/A — sem superfície de dados pessoais exposta. O serviço usa IP + e-mail como chave interna (hashmap in-memory), **nunca loga dados sensíveis** (sem email/IP em texto plano). Não há consentimento, compartilhamento ou dados de menor envolvidos. ADR-005/006/007 não se aplicam diretamente a esta subtask.
