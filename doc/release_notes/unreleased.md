# Release Notes — Unreleased

## US-005 · Bloqueio de cadastro por idade fora da faixa (#36)

**Tipo:** User Story (Sprint 3, marco M2 — Auth essencial)
**Issue:** [#36](https://github.com/dionialves/atrilha/issues/36)
**Branch:** `feat/36-bloqueio-cadastro-por-idade`
**Data de conclusão:** 2026-05-19

### O que foi feito
- Entregue o validador Jakarta Validation reusável `@EligibleAge(role = ...)` que será consumido pelas US-001/002/003/004 nos DTOs/records de cadastro.
- Implementado `AgeEligibilityChecker` como função pura: recebe `LocalDate birthDate` + `AccountRole`, consulta um `Clock` injetado e devolve `Optional<AgeEligibilityViolation>` cobrindo os três casos M1/M2/M3 do `doc/UX/us-005-spec.md`.
- Declarado o enum `AccountRole { TEEN, GUARDIAN }` (RF-E1-03), o enum `AgeEligibilityViolation { TEEN_TOO_YOUNG, TEEN_TOO_OLD, GUARDIAN_TOO_YOUNG }` (cada constante mapeia para uma chave de mensagem) e o record `AgeEligibilityResult` reservado para o caminho OAuth Google das US-002/004.
- Configurado bean `Clock` em `America/Sao_Paulo` (`AgeEligibilityConfig`) para que "hoje" tenha interpretação determinística independente da timezone do container.
- Adicionado `messages.properties` em UTF-8 com as três mensagens contratuais (M1/M2/M3) copiadas literalmente de `doc/UX/us-005-spec.md` §5 + a chave fallback `validation.age.invalid`.
- Habilitado `spring.messages.basename`, `spring.messages.encoding=UTF-8` e `spring.messages.fallback-to-system-locale=false` em `application.properties` para que o `MessageSource` autoconfigurado leia o arquivo corretamente em pt-BR independente do locale do sistema.
- Cobertura de testes: **54/54 verdes**, sem warnings. TDD seguido (Blocos A/B/C do plano da Issue #36 escritos antes do código de produção). QA expandiu com cenários de borda (ano bissexto, fusos), locale-resilience, coexistência `@NotNull` + `@EligibleAge`, contrato do record `AgeEligibilityResult` e guardrail estrutural `AgeEligibilityNoTraceTest` que blinda o CA-5 enquanto o escopo do pacote `auth/` for apenas o validador.

### Impacto
- **Módulo:** `auth` (validador reusável — primeira entrega funcional do módulo).
- **Migrations Flyway:** **nenhuma** (CA-5 obriga zero rastro persistido).
- **Mudanças no `pom.xml`:** nenhuma — `spring-boot-starter-validation` já estava presente.
- **Arquivos novos (produção):**
  - `src/main/java/dev/zayt/atrilha/auth/AccountRole.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityChecker.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityConfig.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityResult.java`
  - `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityViolation.java`
  - `src/main/java/dev/zayt/atrilha/auth/EligibleAge.java`
  - `src/main/java/dev/zayt/atrilha/auth/EligibleAgeValidator.java`
  - `src/main/resources/messages.properties`
- **Arquivos novos (testes):**
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityCheckerTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityCheckerEdgeCasesTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityMessagesTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityMessagesLocaleTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityNoTraceTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/AgeEligibilityResultTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/EligibleAgeValidatorTest.java`
  - `src/test/java/dev/zayt/atrilha/auth/EligibleAgeWithNotNullTest.java`
- **Arquivos editados:** `src/main/resources/application.properties` (somente: 3 linhas `spring.messages.*`).
- **Efeitos colaterais:** nenhum — esta US não monta endpoint HTTP, controller, template, persistência ou e-mail. Consumo do `@EligibleAge` virá nas US-001/002/003/004.

### Como testar
1. A partir do worktree (`/Users/dionia.oliveira/sources/atrilha/.claude/worktrees/agent-a03c82f5b7fa3f27e`), rodar `./mvnw clean test` — 54/54 verdes.
2. Inspeção dirigida (opcional) — rodar apenas a suíte da US: `./mvnw test -Dtest='AgeEligibility*,EligibleAge*'`.
3. Conferir que `messages.properties` está em UTF-8 e que `validation.age.teen.tooYoung`, `validation.age.teen.tooOld`, `validation.age.guardian.tooYoung` correspondem literalmente ao `doc/UX/us-005-spec.md` §5.
4. Validar o guardrail estrutural: `AgeEligibilityNoTraceTest` falhará deliberadamente quando a US-001 introduzir `@Entity`/JPA no pacote `auth/` — esta quebra é intencional (forçar revisão consciente de CA-5 no momento certo). Documentado no Javadoc do teste.

### Gaps visuais / manuais
- **Não há tela nesta US.** O comportamento visual do estado de erro inline (borda `--color-danger-700`, ícone, `aria-invalid`, `role="alert"`, foco no campo) será validado manualmente quando US-001/003 introduzirem o formulário de cadastro que consome `@EligibleAge`. Referência: `doc/UX/us-005-spec.md` §2 e §6.
