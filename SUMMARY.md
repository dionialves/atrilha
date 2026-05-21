# Resumo de execução — Issue #57

**Branch:** feat/57-feat-007-03-cria-stub-inmemory-login-account-query
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java
src/main/resources/application.properties
src/test/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQueryTest.java
src/test/resources/application-test.properties
```

## Diff (stat)
```
 .../auth/login/InMemoryLoginAccountQuery.java      | 113 ++++++++++++++++++++
 src/main/resources/application.properties          |   3 +
 .../auth/login/InMemoryLoginAccountQueryTest.java  | 114 +++++++++++++++++++++
 src/test/resources/application-test.properties     |   3 +
 4 files changed, 233 insertions(+)
```

## O que foi feito

Implementei o stub `InMemoryLoginAccountQuery` (US-007.03) como bean Spring
`@Component` + `@ConfigurationProperties(prefix = "atrilha.auth.seed")`
limitado ao perfil `!prod`. A classe lê 3 seeds (`teen`, `guardian-linked`,
`guardian-unlinked`) das properties, codifica as senhas em claro para BCrypt
no `@PostConstruct` via `PasswordEncoder`, e expõe `findForLogin(email)` com
busca case-insensitive em um `Map<String, LoginAccount>` interno.

**Arquivos criados:**
- `InMemoryLoginAccountQuery.java` — implementação stub com inner class
  `SeedConfig` para binding das properties (sem Lombok, setters explícitos)
- `InMemoryLoginAccountQueryTest.java` — 7 testes: carregamento das 3 seeds
  (email lowercase, BCrypt válido, role, hasGuardianLink, displayName),
  lookup case-insensitive (uppercase/mixedCase) e retorno empty para email
  desconhecido.

**Arquivos alterados:**
- `application.properties` — adicionei campo `role` para cada seed
- `application-test.properties` — idem, com valores `.test`

**Critérios de aceitação:**
- [x] 3 seeds carregadas no perfil `test` (e `dev`, via application.properties)
- [x] Bean não instanciado em `prod` (`@Profile("!prod")`)
- [x] `findForLogin("ANA@TEST")` casa com seed (case-insensitive)
- [x] `findForLogin("nao-existe@x")` retorna `Optional.empty()`
- [x] Senhas armazenadas como BCrypt (`$2a$...`)
- [x] `mvn test` verde (111/111, 0 warnings)

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais. A classe `InMemoryLoginAccountQuery`
armazena apenas email, hash BCrypt de senha, role e flag `hasGuardianLink`
em memória para lookup de login. Não há consentimento, compartilhamento,
exposição de dados de menor (13–17), ou reflexão de menor tocada neste diff.
Os ADRs 005/006/007 não se aplicam a esta mudança.
