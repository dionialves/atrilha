# Changelog — atrilha

## [Unreleased]

### Bug Fixes

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel) porque o principal `OAuth2User` não recebia `ROLE_TEEN`/`ROLE_GUARDIAN`. Introduzida interface `AuthenticatedPrincipal` comum a `AtrilhaUserDetails` e nova classe `AtrilhaOAuth2User`, que injeta `LoginAccountQuery` para resolver authorities a partir da conta no banco. Página 403 amigável adicionada.

### Refactors

- **ref-003 · remover-integracao-google-oauth** ([#81](https://github.com/dionialves/atrilha/issues/81)) — Remoção total da integração Google OAuth (descontinuada pelo produto após FIX-013/014/015 falharem em produção). Migration `V4__remove_google_oauth.sql` dropa coluna `oauth_provider` e recria CHECK exigindo `password_hash NOT NULL` para contas ativas, com guarda `RAISE EXCEPTION` para proteger contas Google residuais em prod. Dependência `spring-boot-starter-oauth2-client`, 10 classes de produção (`GoogleOAuth2UserService`, `OAuthSuccessHandler/FailureHandler`, `PendingGoogleSignup`, `AtrilhaOAuth2User`, `AgeEligibilityResult`, `AdolescentGoogleSignupController`, `CompleteGoogleSignupForm/Request`) e o template `adolescente_complementar.html` removidos. Botão "Continuar com Google" preservado visualmente como `<button disabled aria-disabled="true">` (decisão do cliente). Properties OAuth removidas de todos os perfis e envs Google removidas de `infra/compose/`.
- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor, usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`.
