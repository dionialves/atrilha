# Resumo de execução — Issue #104

**Branch:** feat/104-us-008-c-fluxo-solicitar-reset-get-post-esqueci-se
**Estado:** APROVADO pelo revisor (4ª rodada) — pronto para commit/PR
**Comando de teste:** `./mvnw test`
**Resultado:** VERDE ✅ (suíte funcional 0 falhas; 5 testes novos do controller). Os 12 erros remanescentes são exclusivamente ambiente (Docker/testcontainers ausente para ITs de Postgres).
**Warnings:** (N/A — defina QWEN_WARNINGS_REGEX)

## Arquivos alterados
```
SUMMARY.md
src/main/java/dev/zayt/atrilha/auth/config/SecurityConfig.java
src/main/java/dev/zayt/atrilha/auth/web/PasswordResetRequestController.java
src/main/java/dev/zayt/atrilha/auth/verification/NoOpPasswordResetSender.java
src/main/java/dev/zayt/atrilha/auth/verification/PasswordResetService.java (novo)
src/main/resources/messages.properties
src/main/resources/templates/auth/esqueci-senha.html
src/test/java/dev/zayt/atrilha/auth/web/PasswordResetRequestControllerTest.java (novo)
```

## Diff (stat)
```
 SUMMARY.md                                         |  72 +++-------
 .../auth/config/SecurityConfig.java                |   2 +
 .../web/PasswordResetRequestController.java        |  94 +++++++++++---
 .../verification/NoOpPasswordResetSender.java      |   1 -
 .../verification/PasswordResetService.java         |  16 ---
 src/main/resources/messages.properties             |  13 ++
 .../templates/auth/esqueci-senha.html              |  87 +++++++++++++
 .../web/PasswordResetRequestControllerTest.java    | 205 ++++++++++++++++++++++
 7 files changed, 400 insertions(+), 76 deletions(-)
```

## Resumo do test runner
```
[INFO] Tests run: 5 (PasswordResetRequestControllerTest), Failures: 0, Errors: 0
```

## O que foi feito

Implementação da metade frontal do fluxo "solicitar reset" (US-008-c) com correções de revisão aplicadas:

1. **Bean Validation no DTO** — Substituída validação manual via regex por `@Valid` + `@NotBlank` + `@Email` no `ForgotPasswordRequest`, com `BindingResult` no handler POST.

2. **E-mail sem duplicação (B1)** — `PasswordResetService.issueToken()` foi simplificado para emitir token sem enviar e-mail. O controller injeta `PasswordResetSender` e chama `sendReset()` explicitamente uma única vez após `issueToken()`. Campos `sender` e `profileLookup` removidos do service (não são mais necessários).

3. **Erro de validação retorna 200 + view (B2 corrigido)** — Removido `HttpServletResponse` e `response.setStatus(SC_BAD_REQUEST)` do handler POST. O path de erro retorna `VIEW_FORM` direto (200 OK), consistente com `AdolescentRegistrationController` e os demais controllers de formulário. Testes 4 e 5 ajustados de `isBadRequest()` para `isOk()`, mantendo os asserts das mensagens de erro.

4. **Testes com cobertura completa (B3+B4)** — Teste 2 verifica `sendReset()` via Mockito (`verify(passwordResetSender).sendReset(...)`); teste 3 verifica que `sendReset` NUNCA foi chamado (`verify(passwordResetSender, never())`).

7. **Desambiguação dos senders por `@Profile` (B5 corrigido)** — Existiam dois beans `PasswordResetSender` (`NoOp` + `JavaMail`) sem distinção, o que quebrava a carga do contexto Spring ao injetar o sender no controller (`NoUniqueBeanDefinitionException`). Resolvido com `@Profile`: `JavaMailPasswordResetSender` → `@Profile("!test")` (real em dev/prod), `NoOpPasswordResetSender` → `@Profile("test")` (só em testes).

5. **Template com padrão do projeto** — Template usa `th:object="${request}"` + `th:field="*{email}"` + `th:errors="*{email}"`, seguindo o mesmo padrão dos templates de cadastro (`cadastro/adolescente.html`).

6. **Redirect String com redirect:** — POST retorna `String "redirect:/esqueci-senha?enviado=1"` com `RedirectAttributes.addFlashAttribute("sent", true)`, consistente com `EmailVerificationController`.

## ⚠️ Checagem de LGPD
**Aplicável.** A anti-enumeration é preservada: redirect idêntico independente de conta existir ou não, sem mensagens diferenciadas. Template usa frase condicional ("Se este e-mail estiver cadastrado…"). Sem logs ou métricas que vazem existência da conta. Conforme ADR-005/006/007.

## Nota ao Revisor
A suíte completa expôs uma falha de carga de contexto (`NoUniqueBeanDefinitionException` para `PasswordResetSender`) introduzida ao remover `@Primary` do `NoOpPasswordResetSender` — corrigida nesta rodada via `@Profile` (ver item 7 e REVIEW.md / B5). Após a correção, a suíte funcional fica verde; os 12 erros remanescentes são exclusivamente ambiente (Docker/testcontainers ausente nos ITs de Postgres) e falhariam de forma idêntica em `main`.
