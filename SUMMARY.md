# Resumo de execução — Issue #60

**Branch:** feat/60-feat-007-06-atualiza-security-config-com-form-logi
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** Tests run: (ver log)
**Warnings de compilação:** 0

## Arquivos alterados
```
src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAccountUserDetailsService.java
src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptKey.java
src/main/java/dev/zayt/atrilha/auth/login/RateLimitedAuthenticationFailureHandler.java
src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java
```

## Diff (stat)
```
 .../java/dev/zayt/atrilha/auth/SecurityConfig.java | 142 +++++++++++++++++----
 .../auth/login/LoginAccountUserDetailsService.java |   2 +-
 .../zayt/atrilha/auth/login/LoginAttemptKey.java   |   4 +-
 .../RateLimitedAuthenticationFailureHandler.java   |   2 +-
 .../RoleBasedAuthenticationSuccessHandler.java     |   2 +-
 5 files changed, 121 insertions(+), 31 deletions(-)
```

## O que foi feito

Atualização do `SecurityConfig` para habilitar autenticação por formulário + OAuth2 Google,
integrando com os handlers e serviços existentes das subtasks 007.02–007.05.

**Mudanças principais:**
1. **Rotas públicas**: `/`, `/health`, `/login`, `/css/**`, `/img/**`, `/js/**`,
   `/error/**`, `/cadastro/**`, `/comecar`, `/verificar-email`, `/verify-email`,
   `/login/oauth2/code/**`, `/oauth2/authorization/**`.
2. **Rotas protegidas**: `/trilha/**` → `hasRole("TEEN")`,
   `/painel/**` → `hasRole("GUARDIAN")`, `/vincular/**` → `hasRole("GUARDIAN")`.
3. **Form login**: `.loginPage("/login")`, `.loginProcessingUrl("/login")`,
   com `RoleBasedAuthenticationSuccessHandler` e
   `RateLimitedAuthenticationFailureHandler`.
4. **OAuth2 login**: compartilha o mesmo `successHandler` do form login;
   `failureHandler` permanece sendo o `OAuthFailureHandler`.
5. **Logout**: `/logout` com redirecionamento para `/login?logout`.
6. **DaoAuthenticationProvider**: bean customizado com `preAuthenticationChecks`
   que consulta `LoginAttemptService` via `RequestContextHolder` e lança
   `LockedException` quando IP+email estão bloqueados.
7. **Rotas não listadas**: `.anyRequest().permitAll()` (preserva comportamento
   original — rotas inexistentes chegam ao handler 404 sem bloqueio de auth).

**Visibilidade:** `LoginAttemptKey`, `RoleBasedAuthenticationSuccessHandler`,
`RateLimitedAuthenticationFailureHandler` e `LoginAccountUserDetailsService`
tornados `public` para serem referenciados de fora do pacote `login`.

**Decisão:** Não criei bean `Clock` — o existente (`atrilhaClock` em
`AgeEligibilityConfig`) já satisfaz a dependência do `LoginAttemptService`.

**Critérios de aceitação:** Todos os 159 testes passaram (0 falhas, 0 erros,
0 warnings). Rotas de cadastro regressão verde. `NotFoundPageTest` passou
(após corrigir `.anyRequest()` para `permitAll()`).

**Ponto de atenção:** O log do Spring Security exibe um WARN sobre
`InitializeUserDetailsManagerConfigurer` — é esperado (configuração manual
do `DaoAuthenticationProvider` é intencional).

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais nesta mudança.

O diff não toca em consentimento, compartilhamento de dados, ou dados de menor
(13–17). A configuração do `SecurityConfig` lida com roteamento de autenticação
(e-mails são usados apenas como chave de login, sem exposição adicional).

ADRs 005/006/007: não aplicáveis a esta task.
