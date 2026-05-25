# Release Notes — atrilha

## [Unreleased]

### Bug Fixes

- **fix-015 · cadastro-novo-via-google-parou-de-funcionar-apos-fix-014** (#78) — O PR #77 (FIX-014) transformou o `GoogleOAuth2UserService` em validador estrito que lançava `OAuth2AuthenticationException("account_not_found")` quando o e-mail Google não tinha conta no banco, quebrando a US-002 (cadastro novo via Google) — Julia caía em tela em branco. A correção (Opção A do plano):
  - **`OAuthDispatcherSuccessHandler` (novo)** assume o `oauth2Login.successHandler(...)` no `SecurityConfig` e decide: se o principal é `AtrilhaOAuth2User` em estado `PENDING_SIGNUP`, delega para `OAuthSuccessHandler` (cadastro novo → grava `pendingGoogleSignup` na sessão e redireciona `/cadastro/adolescente/complementar`); caso contrário, delega para `RoleBasedAuthenticationSuccessHandler` (login → `/trilha`, `/painel` ou `/vincular`).
  - **`AtrilhaOAuth2User` ganha estado `PENDING_SIGNUP`** via factory `pendingSignup(email, attrs)` com `account = null` e authorities vazias; `role()`/`displayName()`/`hasGuardianLink()`/`getAccount()` lançam `IllegalStateException` para forçar o chamador a checar `isPendingSignup()`.
  - **`GoogleOAuth2UserService` não lança mais `account_not_found`** — quando a conta não existe, retorna principal pendente.
  - **`OAuthSuccessHandler` aceita os dois tipos de principal** (`OAuth2AuthenticationToken` original ou `AtrilhaOAuth2User` pendente).
  - **`LoginController` e `RoleBasedAuthenticationSuccessHandler`** ganham defesa em profundidade: reconhecem `AuthenticatedPrincipal` genérico e tratam `PENDING_SIGNUP` corretamente (não redirecionam, logam warn).
  - **Template `adolescente_escolher_metodo.html`** ganha branch `no_account` (defesa em profundidade).
  - **Cobertura end-to-end:** `OAuth2GoogleSignupChainIT` (novo, em `auth/web/`) exercita a chain real do Spring Security (`OAuth2LoginAuthenticationFilter` → `OAuth2LoginAuthenticationProvider` → stub do `AccessTokenResponseClient` → stub do `OAuth2UserService` (que herda do real) → `OAuthDispatcherSuccessHandler`) com Testcontainers Postgres. Cobre CA1–CA8 incluindo cadastro novo, login TEEN, login GUARDIAN com/sem vínculo, e-mail não verificado, cancelamento no Google, normalização de e-mail em case misto, e routing do dispatcher. Pré-popula a sessão MockMvc com `OAuth2AuthorizationRequest` para satisfazer o validador de `state` do filtro. Usa scopes `email + profile` (sem `openid`) propositalmente para evitar a chain OIDC, que exigiria id_token JWT válido — a lógica de success/failure handler é idêntica nos dois caminhos.

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel Error Page) em produção porque o principal `OAuth2User` carregava apenas `[OAUTH2_USER, SCOPE_*]`, sem `ROLE_TEEN`. O AuthorizationManager barrava `/trilha` com 403. Agora `GoogleOAuth2UserService` consulta a conta no banco via `LoginAccountQuery` e retorna `AtrilhaOAuth2User` com `[ROLE_TEEN]` ou `[ROLE_GUARDIAN]`. Página 403 amigável substitui o Whitelabel.

### Refactors

- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor (lógica migrada para o service), usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`, eliminando branch por tipo de principal.
