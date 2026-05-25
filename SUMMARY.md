# Resumo de execução — Issue #81 (REF-003)

**Branch:** `feat/81-ref-003-remover-google-oauth`
**Worktree:** `/Users/dionia.oliveira/sources/atrilha-worktrees/81-ref-003-remover-google-oauth`
**Estado:** working tree pronto para QA/revisão (sem commit, sem push, sem PR)
**Build:** `./mvnw verify` → BUILD SUCCESS
- Surefire (unit): **163 tests, 0 failures, 0 errors, 0 skipped**
- Failsafe (IT):   **168 tests, 0 failures, 0 errors, 0 skipped**
- Total: **331 tests**, 0 falhas. (A diferença para os 442 originais corresponde à
  remoção das 14 classes Google-only + 11 métodos Google-adjacentes; impacto é
  esperado pelo plano REF-003.)
- Boot logs limpos: nenhum WARN/ERROR mencionando OAuth ou Google.

## O que foi feito

Removeu-se por completo a integração Google OAuth, conforme plano REF-003
(Issue #81). A feature foi descontinuada pelo produto após três fixes
consecutivas falharem em produção (FIX-013/014/015). A camada removida cobre
banco, JPA, controllers, security, templates, properties, dependência Maven e
infra. O botão "Continuar com Google" permanece visível nas telas de login e
escolha de método de cadastro, mas se torna inerte (`<button type="button"
disabled aria-disabled="true">`), sem ação — exatamente o pedido do cliente.

### Migração de banco (NOVA)

- `src/main/resources/db/migration/V4__remove_google_oauth.sql`
  - Guarda inicial: `RAISE EXCEPTION` se houver `oauth_provider IS NOT NULL` em
    conta ativa (`deleted_at IS NULL`).
  - `DROP CONSTRAINT accounts_credential_chk` (XOR).
  - `DROP COLUMN oauth_provider`.
  - `ADD CONSTRAINT accounts_credential_chk CHECK (password_hash IS NOT NULL
    OR deleted_at IS NOT NULL)` — exige `password_hash` para contas ativas,
    preservando contas soft-deletadas históricas.

### Código de produção DELETADO

- `src/main/java/dev/zayt/atrilha/auth/GoogleOAuth2UserService.java`
- `src/main/java/dev/zayt/atrilha/auth/OAuthSuccessHandler.java`
- `src/main/java/dev/zayt/atrilha/auth/OAuthFailureHandler.java`
- `src/main/java/dev/zayt/atrilha/auth/PendingGoogleSignup.java`
- `src/main/java/dev/zayt/atrilha/auth/login/AtrilhaOAuth2User.java`
- `src/main/java/dev/zayt/atrilha/auth/AgeEligibilityResult.java` (confirmado
  sem consumidor em `src/main`)
- `src/main/java/dev/zayt/atrilha/accounts/AdolescentGoogleSignupController.java`
- `src/main/java/dev/zayt/atrilha/accounts/CompleteGoogleSignupForm.java`
- `src/main/java/dev/zayt/atrilha/accounts/CompleteGoogleSignupRequest.java`
- `src/main/resources/templates/cadastro/adolescente_complementar.html`

### Código de produção EDITADO

- `src/main/java/dev/zayt/atrilha/accounts/Account.java` — remove campo
  `oauthProvider`; Javadoc atualizado para refletir CHECK simplificado.
- `src/main/java/dev/zayt/atrilha/accounts/RegisterAdolescentService.java` —
  remove `registerFromGoogle`, `Outcome.GoogleRegistered` e imports.
- `src/main/java/dev/zayt/atrilha/accounts/AdolescentRegistrationController.java`
  — remove `case GoogleRegistered` do switch; **adicionado** handler
  `@GetMapping("/escolher-metodo")` (ver "Decisões não-óbvias" abaixo).
- `src/main/java/dev/zayt/atrilha/accounts/AccountReader.java` — remove
  `existsByEmailIgnoreCase`.
- `src/main/java/dev/zayt/atrilha/accounts/JpaAccountReader.java` — remove
  implementação de `existsByEmailIgnoreCase`.
- `src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java` — remove
  `.oauth2Login(...)`, remove parâmetros `googleOAuth2UserService` e
  `oauthFailureHandler`, remove `permitAll` para `/login/oauth2/code/**` e
  `/oauth2/authorization/**`, atualiza Javadoc.
- `src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java`
  — remove referências OAuth no Javadoc e no comentário interno.
- `src/main/java/dev/zayt/atrilha/auth/login/JpaLoginAccountQuery.java` —
  remove comentário sobre `passwordHash null` em contas OAuth.
- `src/main/java/dev/zayt/atrilha/auth/AuthenticatedPrincipal.java` — Javadoc
  atualizado.
- `src/main/java/dev/zayt/atrilha/auth/package-info.java` — descrição do módulo
  atualizada.
- `src/main/java/dev/zayt/atrilha/auth/AccountRegisteredEventListener.java` —
  comentário "caso futuro Google OAuth" removido (única menção textual
  remanescente fora do inventário do plano).
- `src/main/resources/templates/auth/login.html` — `<a th:href="@{/oauth2/...}">`
  → `<button type="button" disabled aria-disabled="true"
  data-test="cta-google-disabled" title="Indisponivel">` (SVG e texto
  preservados).
- `src/main/resources/templates/cadastro/adolescente_escolher_metodo.html` —
  mesma substituição do botão; bloco `<div th:if="${error}">...</div>` (banners
  OAuth) removido.
- `src/main/resources/application.properties` — removidas todas as linhas
  `spring.security.oauth2.client.*`.
- `src/main/resources/application-dev.properties` — removidas linhas OAuth.
- `src/main/resources/application-prod.properties` — removidas linhas OAuth.
- `src/test/resources/application-test.properties` — removidas linhas OAuth.
- `pom.xml` — removida dependência `spring-boot-starter-oauth2-client`.
- `infra/compose/docker-compose.prod.yml` — removidas variáveis
  `GOOGLE_OAUTH_CLIENT_ID/SECRET`.
- `infra/compose/.env.example` — removido bloco "OAuth Google".
- `.env.example` — removido bloco "OAuth Google".

### Suite de testes DELETADA (Google-only)

`src/test/java/dev/zayt/atrilha/auth/`: `GoogleOAuth2UserServiceTest.java`,
`GoogleOAuth2UserServiceEdgeCasesTest.java`, `PendingGoogleSignupTest.java`,
`OAuthFailureHandlerTest.java`, `OAuthHandlersIT.java`,
`OAuthHandlersEdgeCasesIT.java`, `SecurityConfigOAuth2IT.java`,
`AgeEligibilityResultTest.java`.

`src/test/java/dev/zayt/atrilha/accounts/`:
`AdolescentGoogleSignupControllerIT.java`,
`AdolescentGoogleSignupEdgeCasesIT.java`, `CompleteGoogleSignupFormTest.java`,
`RegisterAdolescentServiceGoogleIT.java`.

(`OAuth2GoogleSignupChainITStubs.java` e
`web/OAuth2GoogleSignupChainIT.java` não existiam — `rm -f` foi no-op.)

### Suite de testes EDITADA (Google-adjacente)

- `AccountsMigrationIT.java` — lista de colunas esperadas sem
  `oauth_provider`; testes XOR antigos substituídos por
  `credentialConstraintRequiresPasswordHash`.
- `JpaLoginAccountQueryIT.java` — removido teste Google-only; helper
  `insertAccount` simplificado.
- `LoginPageTest.java` — assertions `href="/oauth2/authorization/google"`
  substituídas por verificação do botão disabled (`data-test=
  "cta-google-disabled"`).
- `LoginRateLimitIT.java` — removido teste `bloqueioNoFormLoginNaoAfetaOAuthGoogle`;
  Javadoc da classe atualizado.
- `RoleBasedAuthenticationSuccessHandlerTest.java` — removidos 3 testes que
  usavam `AtrilhaOAuth2User`.
- `PostLoginRedirectTest.java` — removidos 3 testes finais com
  `AtrilhaOAuth2User`; import removido.
- `CadastroELoginIT.java` — removido teste Google E2E; imports + campo
  `JdbcTemplate` órfãos removidos.
- `RegressionUS001AndUS006IT.java` — removidos 3 testes Google
  (`contrastFluxoGoogleNaoDisparaEventoMasFluxoEmailDispara`,
  `contaCriadaPorGoogleEhLocalizavelPeloMesmoFinderDeEmailSenha`,
  `ca4UsCadastroGoogleComIdadeInvalidaNaoIncrementaContagemDeContas`); imports
  + campos órfãos (`accountRepository`, `clock`, `Clock`) removidos.

## ⚠️ Checagem LGPD (atrilha)

N/A para esta task no sentido positivo — REF-003 **remove** uma superfície de
dados pessoais em vez de adicionar. Contas Google deixam de ser criáveis e a
coluna `oauth_provider` é dropada. **Antes do deploy em prod**, o humano
precisa rodar (instrução já presente em "Riscos" da issue):

```sql
SELECT id, email, created_at
  FROM accounts
 WHERE oauth_provider IS NOT NULL
   AND deleted_at IS NULL;
```

Se houver linhas, a migration V4 **falha intencionalmente** com `RAISE
EXCEPTION`, e o time precisa decidir a estratégia (soft-delete + senha
temporária + reset por e-mail, exportar dados, etc.) antes de re-aplicar.
Nenhum dado de menor é manipulado por esta task — apenas estrutura é alterada.

## Pendências para o humano (PO / Designer)

Estritamente fora do escopo do papel Codificador:

- **PO** precisa atualizar `doc/Requisitos/UserStory.md` removendo US-002 e
  US-004 (cadastro/login Google) e ajustando US-007 para remover a referência
  ao botão funcional do Google. **Não fiz nesta worktree por restrição de
  papel** (`AGENTS.md` deixa explícito que `doc/Requisitos/` é território PO).
- **Designer** precisa atualizar `doc/UX/us-002-spec.md` (e qualquer outro
  arquivo `doc/UX/*-spec.md` que referencie o fluxo Google) refletindo que o
  botão é cosmético/desabilitado. **Não fiz nesta worktree** — `doc/UX/` é
  território do Designer.
- O **PR/Revisor** deverá atualizar `doc/changelog.md` e
  `doc/release_notes/unreleased.md` (papel exclusivo do Revisor, conforme
  protocolo).

## Decisões não-óbvias tomadas

1. **Handler GET `/cadastro/adolescente/escolher-metodo` salvaguardado.** O
   plano mandava deletar `AdolescentGoogleSignupController` e editar o
   template `adolescente_escolher_metodo.html`. Os dois pedidos em conjunto
   deixam a rota órfã (template existe, controller não). O teste
   `RegressionUS001AndUS006IT.getCadastroAdolescenteEscolherMetodoUS002ContinuaRenderizando`
   (não listado nos testes a remover/editar do plano) explicitamente exige
   que a rota retorne 200. Resolução: adicionei um `@GetMapping(
   "/escolher-metodo")` mínimo em `AdolescentRegistrationController.java`
   (mesmo `@RequestMapping("/cadastro/adolescente")` do US-001) — 5 linhas,
   sem lógica, só `return "cadastro/adolescente_escolher_metodo";`. Mantém
   bookmarks, satisfaz o contrato regredido e preserva o template editado
   conforme o plano.
2. **Não toquei na CSS `.btn-google` em `src/main/frontend/css/app.css`.** O
   plano explicitamente diz "Nao trocar a paleta/estilo do btn-google (CSS
   continua)". Os utilitários `.google-account-card*` em CSS (linhas 565+)
   também foram preservados pelo mesmo motivo — não tinham reuso fora do
   fluxo Google, mas removê-los está fora do escopo do plano e o impacto é
   neutro (apenas peso morto no CSS bundle, sem efeito de runtime).
3. **`AgeEligibilityResult.java` confirmado órfão e removido.** `grep` em
   `src/main` mostrou só auto-referência. Deletado conforme plano.
4. **`existsByEmailIgnoreCaseAndDeletedAtIsNull` em `AccountRepository`**
   (método Spring Data) **preservado**. O plano só pedia para remover o
   método `existsByEmailIgnoreCase` em `AccountReader`/`JpaAccountReader`
   (interface cross-module). O método do `AccountRepository` continua sendo
   usado em `RegisterAdolescentService.register(...)` (verificação de
   duplicata no fluxo e-mail/senha).
5. **Comentário em `AccountRegisteredEventListener.java`** ("caso futuro
   Google OAuth — US-002") foi removido — não estava no inventário do plano,
   mas era a única menção textual a Google fora do que o plano mandou
   limpar. Sem mudança de comportamento; só polimento de consistência.

## Itens do plano pendentes ou reinterpretados

Nenhum item técnico pendente. O único item que **não pude executar** é o
critério de aceitação "Commit unico no padrao `refactor(ref-003):
remove-integracao-google-oauth` (criado pelo Revisor)" — isso é, por
desenho, papel do Revisor (`AGENTS.md`); a worktree foi entregue com
working tree limpo (todas as alterações pendentes), sem commit.

## Comandos para o QA reproduzir

```bash
cd /Users/dionia.oliveira/sources/atrilha-worktrees/81-ref-003-remover-google-oauth
./mvnw verify          # full suite (Surefire + Failsafe) — esperado: BUILD SUCCESS
./mvnw spring-boot:run # smoke: confirmar que o app sobe sem warning OAuth
                       # abrir http://localhost:8084/login → botão Google disabled
                       # abrir http://localhost:8084/cadastro/adolescente/escolher-metodo → botão Google disabled
                       # tentar http://localhost:8084/oauth2/authorization/google → 404
                       # tentar http://localhost:8084/cadastro/adolescente/complementar → 404
```
