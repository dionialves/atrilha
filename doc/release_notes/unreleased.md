# Release Notes — atrilha

## [Unreleased]

### Bug Fixes

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel Error Page) em produção porque o principal `OAuth2User` carregava apenas `[OAUTH2_USER, SCOPE_*]`, sem `ROLE_TEEN`. O AuthorizationManager barrava `/trilha` com 403. Agora `GoogleOAuth2UserService` consulta a conta no banco via `LoginAccountQuery` e retorna `AtrilhaOAuth2User` com `[ROLE_TEEN]` ou `[ROLE_GUARDIAN]`. Página 403 amigável substitui o Whitelabel.

### Refactors

- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor (lógica migrada para o service), usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`, eliminando branch por tipo de principal.
