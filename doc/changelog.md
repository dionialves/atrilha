# Changelog — atrilha

## [Unreleased]

### Bug Fixes

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel) porque o principal `OAuth2User` não recebia `ROLE_TEEN`/`ROLE_GUARDIAN`. Introduzida interface `AuthenticatedPrincipal` comum a `AtrilhaUserDetails` e nova classe `AtrilhaOAuth2User`, que injeta `LoginAccountQuery` para resolver authorities a partir da conta no banco. Página 403 amigável adicionada.

### Refactors

- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor, usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`.
