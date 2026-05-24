# Resumo de execução — Issue #70

**Branch:** fix/70-fix-012-testes-integracao-usam-localdate-now-emvez
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** 187 testes, 0 falhas, 0 erros
**Warnings de compilação:** 0

## Arquivos alterados
```
REVIEW.md
SUMMARY.md
src/test/java/dev/zayt/atrilha/accounts/AdolescentGoogleSignupControllerIT.java
src/test/java/dev/zayt/atrilha/accounts/AdolescentGoogleSignupEdgeCasesIT.java
src/test/java/dev/zayt/atrilha/accounts/AdolescentRegistrationControllerIT.java
src/test/java/dev/zayt/atrilha/accounts/AdolescentRegistrationEdgeCasesIT.java
src/test/java/dev/zayt/atrilha/accounts/RegressionUS001AndUS006IT.java
```

## Diff (stat)
```
 REVIEW.md                                          |  7 +++
 SUMMARY.md                                         | 50 ++++++++++++----------
 .../AdolescentGoogleSignupControllerIT.java        |  8 +++-
 .../AdolescentGoogleSignupEdgeCasesIT.java         | 16 ++++---
 .../AdolescentRegistrationControllerIT.java        | 16 ++++---
 .../AdolescentRegistrationEdgeCasesIT.java         | 13 ++++--
 .../accounts/RegressionUS001AndUS006IT.java        |  6 ++-
 7 files changed, 76 insertions(+), 40 deletions(-)
```

## O que foi feito

Fix de teste-integração: substituição de `LocalDate.now()` por `LocalDate.now(clock)` em 5 classes IT do pacote `accounts`, injetando `Clock clock` via `@Autowired` em cada uma.

**Causa:** CI do GitHub Actions (timezone UTC) falhava em bordas de idade (13 e 18 anos) porque `LocalDate.now()` do teste retornava "hoje" em UTC, enquanto `AgeEligibilityChecker` usa `Clock.system(America/Sao_Paulo)`. Divergência de 1 dia invertia o veredito de elegibilidade.

**Alterações:**
- `AdolescentGoogleSignupEdgeCasesIT`: 5 substituições (bordas exatas: 12, 13, 17, 18 anos + data futura)
- `AdolescentGoogleSignupControllerIT`: 2 substituições (idades 10 e 25 anos)
- `AdolescentRegistrationControllerIT`: 2 substituições (idades 10 e 25 anos)
- `AdolescentRegistrationEdgeCasesIT`: 2 substituições (idades 10 anos com erros simultâneos)
- `RegressionUS001AndUS006IT`: 1 substituição (idade 10 anos)

**Decisões:**
- Reutilização do bean `atrilhaClock` (America/Sao_Paulo) já existente em `AgeEligibilityConfig` — sem novo bean, sem configuração extra.
- O perfil `test` (`@ActiveProfiles("test")`) já carrega o contexto completo, incluindo `AgeEligibilityConfig`, então `@Autowired Clock clock` resolve sem `@Import`.
- Reorganização incidental de imports em 3 arquivos (static → third-party → project) — efeito colateral do editor, sem impacto funcional.

**Autoavaliação dos critérios de aceitação:**
- ✅ Zero `LocalDate.now()` sem clock nos 5 arquivos listados (12/12 substituições)
- ✅ Cada IT injeta `Clock clock` via `@Autowired`
- ✅ `mvn test` → 187 testes verdes, zero warnings
- ✅ Bordas 13 e 18 agora usam mesmo timezone do `AgeEligibilityChecker`
- ✅ Nenhum teste unitário (`AgeEligibilityCheckerTest`, `EligibleAgeValidatorTest`) modificado

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais. Alteração exclusiva em código de teste (substituição de `LocalDate.now()` por `LocalDate.now(clock)`). Não toca consentimento, compartilhamento ou dados de menor (13–17). ADR-005/006/007 não se aplicam.
