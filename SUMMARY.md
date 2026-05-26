# Resumo de execução — Issue #73

**Branch:** feat/73-ref-001-remove-stub-inmemoryloginaccountquery-e-mi
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java
src/main/resources/application.properties
src/test/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQueryTest.java
src/test/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQueryIT.java
src/test/java/dev/zayt/atrilha/auth/login/LoginTestFixtures.java
src/test/java/dev/zayt/atrilha/auth/web/CadastroELoginIT.java
src/test/java/dev/zayt/atrilha/auth/web/LoginFlowTest.java
src/test/java/dev/zayt/atrilha/auth/web/LoginPageTest.java
src/test/java/dev/zayt/atrilha/auth/web/LoginRateLimitIT.java
src/test/java/dev/zayt/atrilha/auth/web/OAuthRoutesRemovedIT.java
src/test/java/dev/zayt/atrilha/auth/web/PostLoginRedirectTest.java
src/test/resources/application-test.properties
```

## Diff (stat)
```
 .../auth/login/InMemoryLoginAccountQuery.java      | 116 ---------------------
 src/main/resources/application.properties          |   6 --
 .../auth/login/InMemoryLoginAccountQueryTest.java  | 114 --------------------
 .../atrilha/auth/login/JpaLoginAccountQueryIT.java |   2 +-
 .../zayt/atrilha/auth/login/LoginTestFixtures.java |  67 ++++++++++++
 .../zayt/atrilha/auth/web/CadastroELoginIT.java    |   2 +-
 .../dev/zayt/atrilha/auth/web/LoginFlowTest.java   |  25 +++--
 .../dev/zayt/atrilha/auth/web/LoginPageTest.java   |   2 +-
 .../zayt/atrilha/auth/web/LoginRateLimitIT.java    |  11 +-
 .../atrilha/auth/web/OAuthRoutesRemovedIT.java     |   2 +-
 .../atrilha/auth/web/PostLoginRedirectTest.java    |   2 +-
 src/test/resources/application-test.properties     |  17 ---
 12 files changed, 96 insertions(+), 270 deletions(-)
```

## O que foi feito

Removido o stub `InMemoryLoginAccountQuery` (Sprint 3) e seu teste unitário — o caminho JPA via `JpaLoginAccountQuery` é a única implementação ativa. Criado `LoginTestFixtures.java` como helper de teste que persiste contas reais via JPA (`AccountRepository` + `AdolescentProfileRepository`) no `@BeforeEach`, substituindo as seeds por properties. Migrados `LoginFlowTest` e `LoginRateLimitIT` para usar o fixture; testes com SecurityContext mockado (`LoginPageTest`, `PostLoginRedirectTest`, `OAuthRoutesRemovedIT`) e JPA puro (`CadastroELoginIT`, `JpaLoginAccountQueryIT`) apenas tiveram comentários atualizados. Limpos os blocos `atrilha.auth.seed.*` de `application-test.properties` e comentários sobre stub em `application.properties`. Removido 1 teste incompatível com JPA real (`guardian vinculado → /painel`) pois `JpaLoginAccountQuery` sempre retorna `hasGuardianLink=false` (tabela de vínculo chega em US-014). 155 testes verdes, 0 warnings.

## ⚠️ Checagem LGPD (atrilha)
N/A — sem superfície de dados pessoais. Esta refactor remove um stub em memória e migra testes para JPA; não altera consentimento, compartilhamento ou dados de menor.
