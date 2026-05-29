# Release Notes — atrilha

## [Unreleased]

### Features

## US-008-d · Fluxo "consumir token e nova senha" (GET/POST /reset-senha) (#105)

**Tipo:** User Story (US-008-d) — última slice de US-008 (recuperação de senha)
**Issue:** [#105](https://github.com/dionialves/atrilha/issues/105)
**Branch:** `feat/105-us-008-d-fluxo-consumir-token-e-nova-senha-get-pos`
**Data de conclusão:** 2026-05-28

### O que foi feito

- Novo `PasswordResetController` (package-private) com `@RequestMapping("/reset-senha")`:
  - `GET /reset-senha?token=<UUID>` — usa um helper read-only `peekOutcome` (via `PasswordResetTokenRepository.findByToken`) para classificar o estado do token sem consumi-lo, e renderiza `auth/reset-senha` com `outcome ∈ {SUCCESS, EXPIRED_OR_INVALID, ALREADY_USED}`.
  - `POST /reset-senha` — fluxo defensivo em 7 passos: (1) peek do token; (2) validação Bean Validation `@Size(min=8)` em `newPassword`; (3) `passwordResetService.verify(token)` consome atomicamente (lock `PESSIMISTIC_WRITE`); (4) deriva `accountId` server-side do `PasswordResetToken.getAccountId()` — **não** do form (defesa contra tampering); (5) persiste novo hash via `passwordEncoder.encode` (BCrypt 12 rounds); (6) invalida sessões pré-existentes via `sessionRegistry.getAllSessions(principal, false).expireNow()` (CA-4); (7) auto-login via `SessionAuthenticator.authenticate` + `redirect:/`.
- Novo template `templates/auth/reset-senha.html` (decorator `layout/public`, padrão visual de `login.html`/`esqueci-senha.html`):
  - Três estados mutuamente exclusivos (`SUCCESS` / `EXPIRED_OR_INVALID` / `ALREADY_USED`) controlados por `${outcome}`.
  - Formulário com token em hidden field (preservado entre GET e POST), toggle de visibilidade de senha via Alpine.js, exibição de erros de validação via `th:errors`.
  - CTAs de erro apontam para `/esqueci-senha` (link expirado) e `/login` (link já usado).
- `SecurityConfig`: bean novo `SessionRegistry` (`SessionRegistryImpl`); bean novo `HttpSessionEventPublisher` (necessário para o registry receber eventos de criação/expiração de sessão — sem ele a invalidação seria silenciosamente no-op em produção); `.sessionManagement` estendido com `.maximumSessions(-1).sessionRegistry(...)` (ilimitado — apenas rastreia, não bloqueia múltiplos dispositivos); `/reset-senha` adicionado a `permitAll`.
- `messages.properties`: 15 chaves novas `password.reset.*` em pt-BR (títulos, bodies, CTAs, labels, placeholder, hint, mensagem de validação).
- Novo `PasswordResetControllerIT` (10 testes, executado pelo failsafe — sufixo `IT`): GET com token válido / expirado / já usado / malformado / ausente; POST feliz (persiste hash + consome token + auto-login + redirect); POST com senha curta (validação `password.reset.form.password.min`, sem persistência); POST com token expirado (sem persistência); POST sem CSRF (403); POST sucesso invalida sessão pré-existente registrada manualmente via `sessionRegistry.registerNewSession`.

### Decisões tomadas (divergências do plano original, todas justificadas)

- **`PasswordResetService.verify` é destrutivo (não read-only)** — o método marca `usedAt = clock.instant()` na mesma transação. O controller usa `peekOutcome` via `findByToken` no GET e antes da validação no POST, e chama `verify` **uma única vez** antes da persistência. `consume(token)` não é mais necessário. Elimina a race condition que existiria se `verify` fosse chamado duas vezes.
- **`accountId` derivado server-side, não do form** — o link de e-mail só carrega o token. Após `verify` retornar SUCCESS, o `accountId` é obtido de `PasswordResetToken.getAccountId()`. Mitiga ataque em que um atacante envia POST com `accountId` arbitrário para resetar a senha de outra conta.
- **`HttpSessionEventPublisher` adicionado junto com `SessionRegistry`** — sem o publisher o `SessionRegistryImpl` não recebe eventos do servlet container e a invalidação seria no-op em produção. Pegadinha clássica do Spring Security.
- **Teste de invalidação de sessão (teste 10) sem Testcontainers** — usa `sessionRegistry.registerNewSession(oldSessionId, principal)` para simular sessão pré-existente e verifica que `getAllSessions(principal, false)` deixa de listá-la após o POST. Determinístico, sem Docker.

### Impacto

- Arquivos novos:
  - `src/main/java/dev/zayt/atrilha/auth/web/PasswordResetController.java`
  - `src/main/resources/templates/auth/reset-senha.html`
  - `src/test/java/dev/zayt/atrilha/auth/web/PasswordResetControllerIT.java`
- Arquivos editados:
  - `src/main/java/dev/zayt/atrilha/auth/config/SecurityConfig.java` (`SessionRegistry` + `HttpSessionEventPublisher` + `.maximumSessions(-1).sessionRegistry(...)` + `/reset-senha` no `permitAll`)
  - `src/main/resources/messages.properties` (+15 chaves `password.reset.*`)
- Sem nova migration Flyway (esta slice não altera schema). Sem nova dependência no `pom.xml`. `doc/Requisitos/**`, `doc/UX/**`, `AGENTS.md` intactos.
- `mvn test` (surefire) verde: 190 testes (zero falhas). `mvn verify` (surefire + failsafe) verde: 190 + 168 (incluindo `PasswordResetControllerIT` com 10 testes + `PasswordResetServiceIT` 8 testes + `LoginRateLimitIT` + `CadastroELoginIT` + `JpaLoginAccountQueryIT` + `SecurityConfigSessionRewritingIT` — sem regressão).

### Como testar

1. Subir o app em dev (`./mvnw spring-boot:run`).
2. Acessar `/esqueci-senha`, informar um e-mail cadastrado, enviar.
3. Em dev, o e-mail cai no Mailpit (configurado em `application-dev.properties`). Abrir o link `/reset-senha?token=<UUID>` do corpo do e-mail.
4. Verificar que o formulário aparece. Submeter senha curta → validação. Submeter senha ≥ 8 chars → redireciona para `/` autenticado.
5. Tentar usar o mesmo link novamente → tela "Link já utilizado". Aguardar > 1h e tentar → tela "Link expirado".
6. Em outra aba/navegador, fazer login com a conta. Solicitar novo reset e completar. Confirmar que a sessão da outra aba foi invalidada (próxima request volta para `/login`).

### Encerramento do épico US-008

Esta entrega fecha o épico de recuperação de senha — fluxo end-to-end:

- US-008-a (#101) — domain (`PasswordResetToken`, `PasswordResetTokenRepository`, `PasswordResetService.issueToken/verify/consume`, `PasswordResetResult`, migration V6).
- US-008-b (#103) — sender + templates (`JavaMailPasswordResetSender`, `email/password-reset.html`, `email/password-reset-plain.txt`).
- US-008-c (#104) — formulário de solicitação (`GET/POST /esqueci-senha`, anti-enumeration).
- US-008-d (#105) — esta entrega: consumo do token + nova senha + invalidação de sessões + auto-login.

### Gaps visuais / manuais (não-bloqueantes)

- Nenhum gap declarado para esta slice. Sem UX spec formal — segue padrão visual de `login.html` e `esqueci-senha.html`.

### Follow-ups (recomendações não-bloqueantes)

- Encapsular `peekOutcome` no `PasswordResetService` como método read-only (`inspect(UUID)`) para reduzir a dependência do `PasswordResetTokenRepository` na camada web. Candidato a REF futura.
- Harmonizar namespace de chaves i18n: US-008-c usa `password-reset.*`, US-008-d usa `password.reset.*`. Unificar para um único padrão. Candidato a chore.

## US-008-c · Fluxo "solicitar reset" (GET/POST /esqueci-senha) (#104)

**Tipo:** User Story (US-008-c)
**Issue:** [#104](https://github.com/dionialves/atrilha/issues/104)
**Branch:** feat/104-us-008-c-fluxo-solicitar-reset-get-post-esqueci-se
**Data de conclusão:** 2026-05-28

### O que foi feito

- Novo `PasswordResetRequestController` (package-private, padrão de `EmailVerificationController`): `GET /esqueci-senha` renderiza o formulário; `POST /esqueci-senha` valida o e-mail (Bean Validation `@Valid`/`@NotBlank`/`@Email`), faz lookup via `AccountReader.findByEmailIgnoreCase`, e — só quando a conta existe — emite o token (`PasswordResetService.issueToken`) e dispara o e-mail (`PasswordResetSender.sendReset`). Em qualquer caso redireciona para a mesma mensagem genérica (`?enviado=1`).
- **Anti-enumeration / LGPD:** resposta idêntica para e-mail cadastrado e não cadastrado — o usuário nunca descobre se o e-mail existe.
- Novo template `templates/auth/esqueci-senha.html` (padrão visual de `login.html`, `layout/public`), com `th:object`/`th:errors` para erros de validação.
- `SecurityConfig`: rota `/esqueci-senha` liberada (`permitAll`).
- `messages.properties`: textos i18n da tela (sem informação sensível).
- Erro de validação retorna a view com `200 OK`, consistente com os demais controllers de formulário do projeto (sem `response.setStatus(...)`).

### Decisões tomadas

- **Senders separados por `@Profile`** — existiam dois beans `PasswordResetSender` (`NoOpPasswordResetSender` e `JavaMailPasswordResetSender`), ambos `@Component` puros. Ao injetar o sender no novo controller, o contexto Spring quebrava com `NoUniqueBeanDefinitionException`. Resolvido com `@Profile`: `JavaMailPasswordResetSender` (`@Profile("!test")`) é o sender real em dev/prod; `NoOpPasswordResetSender` (`@Profile("test")`) fica restrito a testes. Garante exatamente um bean por contexto.
- **Envio fica no controller, não no service** — `PasswordResetService.issueToken` deixou de enviar e-mail internamente (evita envio duplicado); a orquestração (lookup → token → e-mail) é responsabilidade do controller.

### Impacto

- Arquivos novos: `PasswordResetRequestController.java`, `templates/auth/esqueci-senha.html`, `PasswordResetRequestControllerTest.java`.
- Arquivos editados: `SecurityConfig.java`, `messages.properties`, `NoOpPasswordResetSender.java`, `JavaMailPasswordResetSender.java`, `PasswordResetService.java`.
- `./mvnw test` verde para toda a suíte funcional (5 testes novos do controller). Os ITs que dependem de Postgres/testcontainers só rodam com Docker disponível.

### Refactors

- **ref-004 · remover-testes-cosmeticos-e-de-build-front** ([#84](https://github.com/dionialves/atrilha/issues/84)) — Remoção de 7 arquivos de teste cosméticos/build/frontend da suíte. `./mvnw test` verde: 159 testes, 0 falhas.

### Chores

## CHORE · Aplicar protótipos aprovados às telas públicas (#79)

**Tipo:** Chore (CHORE-016)
**Issue:** [#79](https://github.com/dionialves/atrilha/issues/79)
**Branch:** chore/79-aplicar-prototipos-telas-publicas
**Data de conclusão:** 2026-05-25

### O que foi feito

- Novo decorator `templates/layout/public.html`: shell enxuto para telas públicas (`<head>` com mesmos assets do `base.html`, `<body class="public-shell">` expondo apenas `layout:fragment="content"`). `layout/base.html` permanece **intocado** — telas internas continuam usando o shell completo (header com nav do app, email-verification-banner, footer).
- `templates/home.html` reescrito conforme protótipo `doc/UX/prototypes/home.html`: topbar com marca centrada, hero (overline + display-xl com palavra-acento + lead), SVG decorativo de trilha (chevrons sobre dashed path), CTA primário full-width "Começar" → `/comecar`, link secundário "Entrar" → `/login`, rodapé legal discreto. `aria-label="Bem-vindo à atrilha"` preserva o contrato com `HomeControllerTest`.
- `templates/comecar.html` reescrito conforme protótipo `comecar.html`: header com botão voltar (→ `/`) + marca, intro (overline + h1 + lead), dois cards `.card--interactive` de papel (adolescente / responsável) com ícone, faixa etária, seta animada e destinos `@{/cadastro/adolescente/escolher-metodo}` / `@{/cadastro/responsavel}`, callout informativo sobre vinculação, link "Entrar" no rodapé.
- `templates/auth/login.html` reescrito conforme protótipo `login.html` **preservando o contrato funcional intacto**: `<form th:action="@{/login}" method="post">`, CSRF hidden, `name="username"` / `name="password"`, banners server-side com `data-error="bad-credentials"`, `data-error="rate-limited"`, `data-state="logged-out"`, disable de campos em rate-limited, toggle "mostrar senha" via Alpine, botão Google `<button disabled aria-disabled="true" data-test="cta-google-disabled">` (consistente com REF-003 / PR #83).
- `frontend/css/app.css` ganhou bloco "Telas públicas (CHORE-016)" no final: `.public-shell`, `.home__*`, `.comecar__*`, `.login__*` — reaproveitando `.btn`, `.btn-primary`, `.btn-lg`, `.btn-ghost`, `.btn-google`, `.input-field`, `.input-group`, `.card`, `.card--interactive`, `.overline`, `.alert`, `.brand`, `.brand-mark`, `.brand-wordmark` do design system. Tokens vêm de `var(--token)`; **nenhum hex** redeclarado fora de `@theme`; `@theme` declarado uma única vez.
- Self-host das fontes da marca: 5 arquivos WOFF2 (subset latin) em `src/main/resources/static/fonts/` — `bricolage-grotesque-latin-600.woff2`, `bricolage-grotesque-latin-700.woff2`, `inter-latin-400.woff2`, `inter-latin-500.woff2`, `inter-latin-600.woff2` (~117KB total). `@font-face` declarados no topo de `app.css` com `font-display: swap`; tokens `--font-display` / `--font-sans` apontam para essas famílias com fallback `system-ui`. **Sem requisição a `fonts.googleapis.com` em runtime.**
- Regras de acessibilidade preservadas: `prefers-reduced-motion: reduce` aplicado a `.card--interactive` e a animação da seta dos role-cards; `:focus-visible` mantido em todos os botões/links de ação.

### Decisões tomadas

- **Botão Google permanece `disabled`** — a §5.6 da issue dizia "Google nunca disabled", mas foi escrita antes do REF-003 (PR #83, merge 2026-05-25) ter removido o Google OAuth. Os testes `LoginPageTest` e `OAuthRoutesRemovedIT` exigem `data-test="cta-google-disabled"` + `disabled` + `aria-disabled="true"` + sem `href`. Decisão do cliente: manter inerte por enquanto.
- **Orçamento de fontes** — total ~117KB, ligeiramente acima dos ~100KB sugeridos pela identidade §4.1. Margem pequena, aceitável; revisitar se for necessário subsetar mais agressivamente.
- **Decorator separado em vez de flags no `base.html`** — `public.html` enxuto isola a superfície pública do shell logado, evita flags de modelo e mantém o `base.html` intocado.

### Impacto

- Arquivos novos: `src/main/resources/templates/layout/public.html`, `src/main/resources/static/fonts/{bricolage-grotesque-latin-600,bricolage-grotesque-latin-700,inter-latin-400,inter-latin-500,inter-latin-600}.woff2`.
- Arquivos editados: `src/main/resources/templates/home.html`, `src/main/resources/templates/comecar.html`, `src/main/resources/templates/auth/login.html`, `src/main/frontend/css/app.css`.
- `layout/base.html` **não foi tocado** (diff vazio vs `main`).
- `./mvnw test` verde: **163 testes**, 0 falhas, 0 erros, 0 skipped. Contratos `LoginPageTest`, `HomeControllerTest`, `StaticAssetsCssIT`, `OAuthRoutesRemovedIT` e `CadastroELoginIT` continuam passando.

### Como testar

1. Subir o app em dev: `./mvnw spring-boot:run`.
2. Abrir `http://localhost:8084/` — confirmar layout da home (topbar com marca, hero com SVG, CTA "Começar" full-width, link "Entrar"); confere viewport 320–1280px sem scroll horizontal.
3. Abrir `http://localhost:8084/comecar` — confirmar header com botão voltar, dois cartões de papel com hover (sombra + seta translada), callout informativo.
4. Abrir `http://localhost:8084/login` — confirmar header com botão voltar, alerts (recarregar com `?error`, `?blocked`, `?logout` para cada banner), toggle "Mostrar/Esconder" senha, botão Google visível porém inerte (`disabled`, sem cursor `pointer`).
5. Submeter login com credenciais inválidas → banner `bad-credentials`; abusar de tentativas → banner `rate-limited` com campos desabilitados.
6. Verificar Network: zero requisição a `fonts.googleapis.com` / `fonts.gstatic.com`; fontes carregam de `/fonts/*.woff2`.

### Gaps visuais / manuais

- **Validação visual mobile↔desktop**: como esta é uma task de aplicação de protótipo, comparação pixel-a-pixel com `doc/UX/prototypes/{home,comecar,login}.html` deve ser feita manualmente no navegador (mobile real ou DevTools responsive). Não há teste automatizado de layout (proibido pelo workflow).

### Follow-ups (não bloqueantes deste PR)

- **Rotas `/termos`, `/privacidade`, `/esqueci-senha`** — `th:href` apontam para essas URLs mas elas ainda não têm controllers. Decisão de produto pendente; criar issues separadas (CHORE-???) quando definidas.
- **Botão Google** — revisitar quando o produto decidir remover o cartão visual ou substituir por outro provedor; hoje preservado por decisão explícita do cliente.
- **Testes adicionais sugeridos** — testes Jsoup leves de smoke (presença de `.public-shell` no body, ausência de `header.app-nav` nas três telas públicas, presença de `<link rel="stylesheet" href="/css/app.css">`) podem ser adicionados em task futura se houver risco de regressão de decorator.

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
