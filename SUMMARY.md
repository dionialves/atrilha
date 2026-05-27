# Resumo de execução — Issue #95

**Branch:** feat/95-feat-us-003-integracao-login-limpeza-stub-testes-d
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/java/dev/zayt/atrilha/accounts/repository/AccountProfileLookup.java
src/main/java/dev/zayt/atrilha/accounts/repository/JpaAccountProfileLookup.java
src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java
src/main/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQuery.java
src/main/java/dev/zayt/atrilha/auth/verification/AccountRegisteredEventListener.java
src/test/java/dev/zayt/atrilha/accounts/repository/JpaAccountProfileLookupTest.java
src/test/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQueryTest.java
src/test/java/dev/zayt/atrilha/auth/verification/AccountRegisteredEventListenerTest.java
```

## Diff (stat)
```
 .../accounts/repository/AccountProfileLookup.java  |   6 +
 .../repository/JpaAccountProfileLookup.java        |  20 ++-
 .../auth/login/InMemoryLoginAccountQuery.java      |   8 +-
 .../atrilha/auth/login/JpaLoginAccountQuery.java   |   6 +
 .../AccountRegisteredEventListener.java            |   5 +-
 .../repository/JpaAccountProfileLookupTest.java    | 151 ++++++++++++++++++
 .../auth/login/JpaLoginAccountQueryTest.java       | 124 +++++++++++++++
 .../AccountRegisteredEventListenerTest.java        | 168 +++++++++++++++++++++
 8 files changed, 484 insertions(+), 4 deletions(-)
```

## O que foi feito

Integração vertical do login e e-mail de verificação com o perfil de responsável (GUARDIAN) criado na Issue #94.

**Produção (5 arquivos):**
- `AccountProfileLookup` ganhou `findFullName(UUID)` e `findDisplayName(UUID, String)` na interface.
- `JpaAccountProfileLookup` implementa os dois novos métodos, injetando `GuardianProfileRepository` (construtor passou de 1 para 2 parâmetros).
- `JpaLoginAccountQuery.resolveDisplayName()` agora consulta `findFullName()` para contas GUARDIAN, com fallback ao prefixo do email.
- `AccountRegisteredEventListener` usa `findDisplayName(accountId, account.getType())` no lugar de `findNickname()` — e-mail de verificação agora saúda com nome correto.
- `InMemoryLoginAccountQuery.SeedConfig` ganhou campo `displayName`; `toAccount()` prioriza displayName sobre fallback de email.

**Testes (3 arquivos novos, 8 testes):**
- `JpaAccountProfileLookupTest` — 4 testes: findFullName (guardian/adolescente) e findDisplayName (GUARDIAN/ADOLESCENT).
- `JpaLoginAccountQueryTest` — 2 testes: displayName full_name para GUARDIAN, fallback email prefix sem profile.
- `AccountRegisteredEventListenerTest` — 2 testes: e-mail com full_name (GUARDIAN) e nickname (ADOLESCENT, sem regressão).

**Stub:** `GuardianRegistrationStubController` e `responsavel_em_breve.html` já removidos pela PR #98 — no-op.

**Autoavaliação:** Todos os 4 critérios de aceitação da Issue #95 atendidos. `mvn test` verde (182 testes, 0 erros).

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoal nova. Esta issue integra e limpa funcionalidades já existentes:
- `full_name` do responsável já foi criado na Issue #93 (entidade) e capturado na Issue #94 (cadastro).
- O e-mail de verificação já existe; a mudança é apenas incluir o nome correto (`full_name`) na saudação.
- Seeds de teste usam e-mails `.test` — não são dados reais.

**Nenhuma nova coleta, armazenamento ou processamento de PII nesta issue.** ADR-005/006/007 não aplicáveis.
