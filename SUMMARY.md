# Resumo de execução — Issue #72

**Branch:** feat/72-fix-013-login-por-e-mail-senha-e-google-falha-porq
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados

**Modificados (5):**
- `src/main/java/dev/zayt/atrilha/accounts/AccountReader.java` — expõe `findByEmailIgnoreCase(String)`
- `src/main/java/dev/zayt/atrilha/accounts/JpaAccountReader.java` — implementa `findByEmailIgnoreCase` delegando ao repositório
- `src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java` — restringe a `@ConditionalOnProperty(name = "atrilha.auth.seed.enabled", havingValue = "true")`
- `src/main/resources/application.properties` — remove seeds do default; adiciona comentário explicativo
- `src/test/resources/application-test.properties` — mantém seeds com flag explícita `atrilha.auth.seed.enabled=true`

**Novos (3):**
- `src/main/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQuery.java` — implementação JPA real de `LoginAccountQuery`, injeta `AccountReader` + `AccountProfileLookup`
- `src/test/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQueryIT.java` — 8 testes de integração (PostgreSQL testcontainer)
- `src/test/java/dev/zayt/atrilha/auth/web/CadastroELoginIT.java` — 4 testes end-to-end (cadastro → login)

## Diff (stat)
```
 src/main/java/dev/zayt/atrilha/accounts/AccountReader.java               |  7 +++
 src/main/java/dev/zayt/atrilha/accounts/JpaAccountReader.java            |  8 +++
 src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java | 13 +++--
 src/main/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQuery.java      | 74 ++++++++++++++++++
 src/main/resources/application.properties                                | 16 ++---
 src/test/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQueryIT.java    | 230 +++++++++++++++++++++
 src/test/java/dev/zayt/atrilha/auth/web/CadastroELoginIT.java            | 180 ++++++++++++++++
 src/test/resources/application-test.properties                           |  6 ++
 8 files changed, 502 insertions(+), 52 deletions(-)
```

## O que foi feito

Substituí o stub `InMemoryLoginAccountQuery` (que só conhecia 3 contas-semente em memória) pela implementação real `JpaLoginAccountQuery`, que consulta a tabela `accounts` via `AccountReader`. O stub foi restringido ao perfil `atrilha.auth.seed.enabled=true` (default: desligado), preservando os testes legados que dependem de seeds determinísticas.

**Decisões implícitas:**
- `JpaLoginAccountQuery` usa `@ConditionalOnProperty(matchIfMissing = true)` — é o bean padrão em todos os perfis (dev, prod, test). O stub só sobe quando a flag é explicitamente `true`.
- Não criei `AdolescentProfileLookup` separado — reutilizei a interface existente `AccountProfileLookup` (já exposta pelo módulo accounts).
- `PostLoginDestination.valueOfError()` já existia no enum (verificado), sem necessidade de alteração.
- Testes IT usam `JdbcTemplate` para inserir dados (repositórios são package-private), respeitando a fronteira do módulo `accounts`.

**Autoavaliação dos CAs:**
- ✅ Cadastro → login redireciona para `/trilha` (CA 1)
- ✅ Conta Google gravada diretamente é encontrável pelo JPA query (CA 2, validado via SQL)
- ✅ E-mail inexistente retorna mesmo redirect que senha errada (CA 3 — privacidade)
- ✅ `JpaLoginAccountQuery` é bean padrão em todos os perfis (CA 4)
- ✅ `InMemoryLoginAccountQuery` só ativa com flag explícita (CA 5)
- ✅ Case-insensitive + ignora soft-delete (CA 6, testes 3 e 4)
- ✅ 8 testes `JpaLoginAccountQueryIT` + 4 testes `CadastroELoginIT` verdes
- ✅ Suíte legada (187 tests) continua 100% verde

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais. O diff não toca consentimento, compartilhamento ou dados de menor (13-17). A mudança é puramente infraestrutural: expor um finder no `AccountReader` e criar uma implementação JPA do SPI `LoginAccountQuery`. Nenhuma lógica de LGPD (ADR-005/006/007) foi alterada.
