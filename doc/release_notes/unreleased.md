# Release Notes — atrilha

## [Unreleased]

### Bug Fixes

- **fix-014 · login-google-em-producao-retorna-403-white** (#76) — Login OAuth Google retornava 403 (Whitelabel Error Page) em produção porque o principal `OAuth2User` carregava apenas `[OAUTH2_USER, SCOPE_*]`, sem `ROLE_TEEN`. O AuthorizationManager barrava `/trilha` com 403. Agora `GoogleOAuth2UserService` consulta a conta no banco via `LoginAccountQuery` e retorna `AtrilhaOAuth2User` com `[ROLE_TEEN]` ou `[ROLE_GUARDIAN]`. Página 403 amigável substitui o Whitelabel.

### Refactors

- **ref-015 · authenticatedprincipal-interface-comum-form-e-oauth** (#76) — `RoleBasedAuthenticationSuccessHandler` simplificado: remove `LoginAccountQuery` do construtor (lógica migrada para o service), usa pattern matching em `AuthenticatedPrincipal`. `PostLoginRedirectController` passa a usar `@AuthenticationPrincipal AuthenticatedPrincipal`, eliminando branch por tipo de principal.

## REFACTOR · Remover integração Google OAuth (descontinuada) (#81)

**Tipo:** Refactor (REF-003, sprint atual)
**Issue:** [#81](https://github.com/dionialves/atrilha/issues/81)
**Branch:** feat/81-ref-003-remover-google-oauth
**Data de conclusão:** 2026-05-25

### Comunicado aos usuários

**Login e cadastro via conta Google foram descontinuados.** O botão "Continuar com Google" permanece visível nas telas de login e de escolha de método de cadastro, mas está inativo (não leva a lugar nenhum) — clicar nele não tem efeito. **Use cadastro/login por e-mail e senha**, que continua funcionando normalmente. Contas previamente criadas exclusivamente via Google (se existirem) precisam ser tratadas manualmente pelo administrador antes da próxima migração do banco (ver "Riscos" abaixo).

### O que foi feito

- Migration `V4__remove_google_oauth.sql` dropa a coluna `accounts.oauth_provider` e substitui o CHECK XOR antigo (`password_hash` XOR `oauth_provider`) por um CHECK simplificado: `password_hash IS NOT NULL OR deleted_at IS NOT NULL` (contas ativas precisam de senha; contas soft-deletadas históricas preservam `password_hash NULL`).
- Migration tem guarda `RAISE EXCEPTION` que **aborta** a aplicação se houver contas ativas com `oauth_provider NOT NULL` — protege produção contra perda silenciosa de dado.
- 10 classes de produção deletadas (`GoogleOAuth2UserService`, `OAuthSuccessHandler`, `OAuthFailureHandler`, `PendingGoogleSignup`, `AtrilhaOAuth2User`, `AgeEligibilityResult`, `AdolescentGoogleSignupController`, `CompleteGoogleSignupForm`, `CompleteGoogleSignupRequest`) + template `adolescente_complementar.html`.
- `SecurityConfig` sem `.oauth2Login(...)`, sem parâmetros `googleOAuth2UserService` / `oauthFailureHandler`, sem permitAll de `/login/oauth2/code/**` e `/oauth2/authorization/**`.
- Entidade `Account` sem o campo `oauthProvider`; `RegisterAdolescentService` sem o método `registerFromGoogle(...)` nem a variante `Outcome.GoogleRegistered`.
- Dependência `spring-boot-starter-oauth2-client` removida do `pom.xml`.
- Properties `spring.security.oauth2.client.*` removidas de `application.properties`, `application-dev.properties`, `application-prod.properties`, `application-test.properties`.
- Variáveis `GOOGLE_OAUTH_CLIENT_ID` / `GOOGLE_OAUTH_CLIENT_SECRET` removidas de `infra/compose/docker-compose.prod.yml`, `infra/compose/.env.example` e `.env.example`.
- Templates `auth/login.html` e `cadastro/adolescente_escolher_metodo.html`: o `<a th:href="@{/oauth2/authorization/google}">` foi substituído por `<button type="button" disabled aria-disabled="true" data-test="cta-google-disabled">` (SVG e texto "Continuar com Google" preservados). Banner de erros OAuth removido de `escolher_metodo.html`.
- Adicionado handler GET mínimo `/cadastro/adolescente/escolher-metodo` em `AdolescentRegistrationController` (5 linhas, sem lógica) para preservar a rota — bookmarks e teste de regressão US-002 continuam respondendo.
- Suite de testes reduzida em 14 classes Google-only deletadas; 8 classes Google-adjacentes editadas para remover asserts Google. Adicionados 6 testes em `OAuthRoutesRemovedIT` (contratos pós-remoção) e 2 testes em `AccountsMigrationIT` cobrindo o CHECK pós-V4.

### Impacto

- Arquivos novos: `src/main/resources/db/migration/V4__remove_google_oauth.sql`, `src/test/java/dev/zayt/atrilha/auth/web/OAuthRoutesRemovedIT.java`.
- Arquivos deletados: 10 classes Java de produção, 1 template Thymeleaf, 12 classes Java de teste.
- Arquivos editados: 18 (entidade, serviço, controller, security, templates, properties, pom, infra, suite de teste).
- Migrations aplicadas do zero em ordem `V1 → V2 → V3 → V4` (validado por `AccountsMigrationIT` + `FlywayMigrationIT` em Testcontainers Postgres 18).
- `./mvnw verify` verde: 163 Surefire + 175 Failsafe = **338 testes**, 0 falhas, 0 erros, 0 skipped.

### Como testar

1. Subir o app em dev: `./mvnw spring-boot:run`.
2. Abrir `http://localhost:8084/login`: confirmar que o botão "Continuar com Google" aparece visualmente mas está `disabled` (cursor `not-allowed`) e clicar nele não dispara navegação nem request de rede.
3. Abrir `http://localhost:8084/cadastro/adolescente/escolher-metodo`: confirmar o mesmo comportamento do botão Google.
4. Tentar `http://localhost:8084/oauth2/authorization/google`: deve retornar não-200 (não inicia mais flow OAuth para `accounts.google.com`).
5. Tentar `http://localhost:8084/cadastro/adolescente/complementar`: deve retornar 404.
6. Verificar logs de boot: zero WARN/ERROR mencionando OAuth ou Google.
7. Submeter cadastro por e-mail/senha em `/cadastro/adolescente` e login em `/login`: fluxos US-001/US-007 intactos.

### Riscos e pré-requisitos de deploy

**Antes do deploy em produção**, o administrador deve rodar no banco de prod:

```sql
SELECT id, email, created_at FROM accounts
 WHERE oauth_provider IS NOT NULL AND deleted_at IS NULL;
```

Se retornar zero linhas, a migration `V4` aplica sem fricção. Se retornar contas, a migration **falha intencionalmente** (`RAISE EXCEPTION`) e exige decisão manual: soft-delete + senha temporária com reset por e-mail, exportar dados, ou outra estratégia que preserve o acesso do usuário.

### Pendências de outras tarefas (não bloqueantes deste PR)

- **PO**: atualizar `doc/Requisitos/UserStory.md` removendo US-002 e US-004 e ajustando US-007 para refletir que o botão Google é cosmético/desabilitado.
- **Designer**: atualizar `doc/UX/us-002-spec.md` (e quaisquer outros `doc/UX/*-spec.md` que referenciem o fluxo Google) refletindo o estado inerte do botão.
- **REF futuro (sugestão do Revisor)**: limpar CSS órfão `.btn-google` (linhas 473+) e `.google-account-card*` (linhas 565+) em `src/main/frontend/css/app.css` quando o cliente decidir remover o botão visual. Hoje preservados por decisão explícita do plano.
