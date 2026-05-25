# Changelog — atrilha

## [Unreleased]

### Bug Fixes

- **fix-015 · cadastro-novo-via-google-parou-de-funcionar-apos-fix-014** (#78) — FIX-014 transformou o `GoogleOAuth2UserService` em validador estrito, lançando `account_not_found` quando o e-mail Google não tinha conta — quebrando a US-002 (cadastro novo via Google). Introduzido `OAuthDispatcherSuccessHandler` que decide entre login (delega a `RoleBasedAuthenticationSuccessHandler`) ou cadastro novo (delega a `OAuthSuccessHandler`); `AtrilhaOAuth2User` ganhou estado `PENDING_SIGNUP` via factory `pendingSignup(...)`. `OAuth2GoogleSignupChainIT` cobre a chain end-to-end com Testcontainers e stubs de `AccessTokenResponseClient` + `OAuth2UserService`.
- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel) porque o principal `OAuth2User` não recebia `ROLE_TEEN`/`ROLE_GUARDIAN`. Introduzida interface `AuthenticatedPrincipal` comum a `AtrilhaUserDetails` e nova classe `AtrilhaOAuth2User`, que injeta `LoginAccountQuery` para resolver authorities a partir da conta no banco. Página 403 amigável adicionada.

### Refactors

- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor, usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`.
