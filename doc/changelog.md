# Changelog — atrilha

## [Unreleased]

### Features

- **us-008-c · fluxo-solicitar-reset-esqueci-senha** ([#104](https://github.com/dionialves/atrilha/issues/104)) — Metade frontal de US-008: `GET /esqueci-senha` renderiza o formulário e `POST /esqueci-senha` faz lookup da conta, emite token (`PasswordResetService.issueToken`) e dispara o e-mail (`PasswordResetSender.sendReset`) quando o e-mail existe. Resposta idêntica para e-mail existente e inexistente (anti-enumeration / LGPD). Os dois beans `PasswordResetSender` passam a ser desambiguados por `@Profile`: `JavaMailPasswordResetSender` ativo em dev/prod e `NoOpPasswordResetSender` apenas em `test` — antes ambos eram `@Component` sem distinção, o que quebrava o contexto Spring ao injetar o sender no controller. Rota liberada no `SecurityConfig`; textos públicos em `messages.properties` (sem informação sensível).

### Refactors

- **ref-004 · remover-testes-cosmeticos-e-de-build-front** ([#84](https://github.com/dionialves/atrilha/issues/84)) — Remoção de 7 arquivos de teste que validam aspectos cosméticos, de build ou frontend (não funcionalidade de backend): `NotFoundPageTest`, `StaticAssetsCssIT`, `StaticAssetsCssCoverageIT`, `StaticAssetsFingerprintCoverageIT`, `StaticAssetsFingerprintProdIT`, `HomeControllerTest`, `Error403PageTest`. Comentários órfãos em `pom.xml` e `application-test.properties` atualizados para refletir os ITs removidos. Suíte de testes continua verde: 53 arquivos `.java` em `src/test/`, 159 testes, zero falhas.

### Chores

- **chore-016 · aplicar-prototipos-telas-publicas** ([#79](https://github.com/dionialves/atrilha/issues/79)) — Aplicação dos protótipos aprovados nas três telas públicas (`GET /`, `GET /comecar`, `GET /login`) preservando 100% do comportamento (rotas, form de login, CSRF, banners `data-error`/`data-state`, toggle de senha). Novo decorator enxuto `layout/public.html` separa o shell das telas públicas do `layout/base.html` (intocado). Fontes Bricolage Grotesque (600/700) e Inter (400/500/600) passam a ser servidas self-hosted via `@font-face` com `font-display: swap` — sem requisição a `fonts.googleapis.com`. CSS de página adicionado a `app.css` reaproveitando classes do design system (`.btn`, `.card`, `.input-field`, `.alert`, `.brand`, etc.), sem hex duplicado nem `@theme` redeclarado. Botão Google permanece `disabled` (consistente com REF-003 / PR #83).

### Bug Fixes

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel) porque o principal `OAuth2User` não recebia `ROLE_TEEN`/`ROLE_GUARDIAN`. Introduzida interface `AuthenticatedPrincipal` comum a `AtrilhaUserDetails` e nova classe `AtrilhaOAuth2User`, que injeta `LoginAccountQuery` para resolver authorities a partir da conta no banco. Página 403 amigável adicionada.

### Refactors

- **ref-003 · remover-integracao-google-oauth** ([#81](https://github.com/dionialves/atrilha/issues/81)) — Remoção total da integração Google OAuth (descontinuada pelo produto após FIX-013/014/015 falharem em produção). Migration `V4__remove_google_oauth.sql` dropa coluna `oauth_provider` e recria CHECK exigindo `password_hash NOT NULL` para contas ativas, com guarda `RAISE EXCEPTION` para proteger contas Google residuais em prod. Dependência `spring-boot-starter-oauth2-client`, 10 classes de produção (`GoogleOAuth2UserService`, `OAuthSuccessHandler/FailureHandler`, `PendingGoogleSignup`, `AtrilhaOAuth2User`, `AgeEligibilityResult`, `AdolescentGoogleSignupController`, `CompleteGoogleSignupForm/Request`) e o template `adolescente_complementar.html` removidos. Botão "Continuar com Google" preservado visualmente como `<button disabled aria-disabled="true">` (decisão do cliente). Properties OAuth removidas de todos os perfis e envs Google removidas de `infra/compose/`.
- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor, usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`.
