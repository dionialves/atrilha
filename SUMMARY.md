# Resumo de execução — Issue #56

**Branch:** feat/56-feat-007-02-cria-spi-login-account-query-e-post-lo
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
.tool-versions
src/main/java/dev/zayt/atrilha/auth/login/LoginAccountQuery.java
src/main/java/dev/zayt/atrilha/auth/login/PostLoginDestination.java
```

## Diff (stat)
```
 .tool-versions                                     |  1 +
 .../zayt/atrilha/auth/login/LoginAccountQuery.java | 34 ++++++++++++++++++++++
 .../atrilha/auth/login/PostLoginDestination.java   | 26 +++++++++++++++++
 3 files changed, 61 insertions(+)
```

## O que foi feito
Criei a interface `LoginAccountQuery` (SPI) com o método opcional de busca por e-mail normalizado (`findForLogin`) e a classe aninhada `record LoginAccount` transportando os campos essenciais para autenticação: email, hash BCrypt (nullable), `AccountRole` (reutilizado de `dev.zayt.atrilha.auth`, sem duplicação), flag booleana `hasGuardianLink` e `displayName`. Implementei o enum `PostLoginDestination` com as três rotas (`TRILHA`, `PAINEL`, `VINCULAR`) e o método acessor `.path()`. Compilação limpa, zero warnings Maven. Nenhuma classe do projeto referencia `findForLogin` — a implementação concreta (stub em memória) será entregue na subtask 007.03.

Critérios de aceitação: todos atendidos (`LoginAccountQuery` compila isoladamente, `PostLoginDestination.path()` retorna string com `/`, `mvn compile` verde sem warnings).

## ⚠️ Checagem LGPD (atrilha)
N/A — o diff contém apenas contratos Java puros (interface + enum). Não há armazenamento, processamento ou fluxo de dados pessoais. O campo `hasGuardianLink` é um booleano interno do record para lógica de roteamento pós-login; não há coleta, exibição ou compartilhamento exposto nesta subtask. ADR-005/006/007 não são acionados por esta camada de contrato.
