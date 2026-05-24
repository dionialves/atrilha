# Resumo de execução — Issue #76

**Branch:** feat/76-fix-014-login-google-em-producao-retorna-403-white
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** 204 pass, 0 falhas
**Warnings de compilação:** 0

## Arquivos alterados (diff)
```
src/main/java/dev/zayt/atrilha/auth/GoogleOAuth2UserService.java
src/main/java/dev/zayt/atrilha/auth/OAuthFailureHandler.java
src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java
src/main/java/dev/zayt/atrilha/auth/login/AtrilhaUserDetails.java
src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java
src/main/java/dev/zayt/atrilha/auth/web/PostLoginRedirectController.java
src/test/java/dev/zayt/atrilha/auth/GoogleOAuth2UserServiceEdgeCasesTest.java
src/test/java/dev/zayt/atrilha/auth/GoogleOAuth2UserServiceTest.java
src/test/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandlerTest.java
src/test/java/dev/zayt/atrilha/auth/web/PostLoginRedirectTest.java
```

## Arquivos novos (não aparecem no diff)
```
src/main/java/dev/zayt/atrilha/auth/AuthenticatedPrincipal.java          (novo)
src/main/java/dev/zayt/atrilha/auth/login/AtrilhaOAuth2User.java         (novo)
src/main/resources/templates/error/403.html                               (novo)
src/test/java/dev/zayt/atrilha/auth/OAuthFailureHandlerTest.java         (novo)
src/test/java/dev/zayt/atrilha/auth/web/Error403PageTest.java            (novo)
```

## Diff (stat)
```
 .../zayt/atrilha/auth/GoogleOAuth2UserService.java |  50 +++++-
 .../dev/zayt/atrilha/auth/OAuthFailureHandler.java |   6 +
 .../java/dev/zayt/atrilha/auth/SecurityConfig.java |   2 +-
 .../atrilha/auth/login/AtrilhaUserDetails.java     |  15 +-
 .../RoleBasedAuthenticationSuccessHandler.java     |  53 ++-----
 .../auth/web/PostLoginRedirectController.java      |   8 +-
 .../auth/GoogleOAuth2UserServiceEdgeCasesTest.java |  91 ++++++++++-
 .../atrilha/auth/GoogleOAuth2UserServiceTest.java  | 168 ++++++++++++++++-----
 .../RoleBasedAuthenticationSuccessHandlerTest.java |  96 +++++-------
 .../atrilha/auth/web/PostLoginRedirectTest.java    |  65 ++++++++
 10 files changed, 413 insertions(+), 141 deletions(-)
```

## O que foi feito

Corrigiu-se o bug FIX-014: login Google em produção retornava 403 (Whitelabel)
porque o principal OAuth2User não recebia ROLE_TEEN/ROLE_GUARDIAN.

**Causa raiz:** `GoogleOAuth2UserService.loadUser()` devolvia o OAuth2User cru
do Google com authorities `[OAUTH2_USER, SCOPE_*]` — nenhuma era `ROLE_TEEN`.
O AuthorizationManager barrava a request a `/trilha` com 403.

**Solução em 5 mudanças:**

1. **Nova interface `AuthenticatedPrincipal`** — contrato comum a
   `AtrilhaUserDetails` (form login) e `AtrilhaOAuth2User` (OAuth), expondo
   `role()`, `displayName()`, `hasGuardianLink()`, `getAccount()`.
2. **Nova classe `AtrilhaOAuth2User`** — implementa `OAuth2User` +
   `AuthenticatedPrincipal`. Construído pelo `GoogleOAuth2UserService` com
   authorities derivadas da conta no banco (`[ROLE_TEEN]` ou `[ROLE_GUARDIAN]`).
3. **`GoogleOAuth2UserService` editado** — injeta `LoginAccountQuery`, consulta
   a conta pelo email normalizado, lança `account_not_found` se não existir,
   retorna `AtrilhaOAuth2User` com authorities corretas.
4. **`RoleBasedAuthenticationSuccessHandler` simplificado** — remove `LoginAccountQuery`
   do construtor (lógica migrou para o service). `resolveDestination` e
   `extractUsername` agora usam pattern matching em `AuthenticatedPrincipal`.
5. **Página 403 amigável** — novo template `templates/error/403.html` + correção
   do matcher `/error/**` para `"/error", "/error/**"` no SecurityConfig.

**Testes:** 204 pass, 0 falhas. Novos testes cobrem: OAuth principal com
`AtrilhaOAuth2User`, `account_not_found` → redirect, `no_account` variant no
failure handler, 403 com papel errado.

## ⚠️ Checagem LGPD (atrilha)

N/A — sem superfície de dados pessoais. Este fix não toca consentimento,
compartilhamento ou dados de menor (13-17). As mudanças são exclusivamente
na camada de autenticação/autorização Spring Security (authorities do principal,
template de erro 403). Os dados pessoais (email, displayName) já eram lidos
pelos templates existentes via `AtrilhaUserDetails` — agora também são acessíveis
via `AuthenticatedPrincipal.displayName()` no mesmo fluxo.
