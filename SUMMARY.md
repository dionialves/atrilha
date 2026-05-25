# Resumo de execução — Issue #78

**Branch:** feat/78-fix-015-cadastro-novo-via-google-parou-de-funciona
**Estado:** working tree pronto para revisão (sem PR, sem push)
**Testes:** `./mvnw verify` BUILD SUCCESS — Surefire 206 / Failsafe 236 (total 442), Failures: 0, Errors: 0, Skipped: 0
**Warnings de compilação:** 0

## Arquivos alterados
```
SUMMARY.md
src/main/java/dev/zayt/atrilha/auth/GoogleOAuth2UserService.java
src/main/java/dev/zayt/atrilha/auth/OAuthSuccessHandler.java
src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java
src/main/java/dev/zayt/atrilha/auth/login/AtrilhaOAuth2User.java
src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java
src/main/java/dev/zayt/atrilha/auth/web/LoginController.java
src/main/resources/templates/cadastro/adolescente_escolher_metodo.html
src/test/java/dev/zayt/atrilha/auth/GoogleOAuth2UserServiceEdgeCasesTest.java
src/test/java/dev/zayt/atrilha/auth/GoogleOAuth2UserServiceTest.java
src/test/java/dev/zayt/atrilha/auth/OAuthHandlersIT.java
src/test/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandlerTest.java
src/test/java/dev/zayt/atrilha/auth/web/LoginPageTest.java
```

## Arquivos novos
```
src/main/java/dev/zayt/atrilha/auth/OAuthDispatcherSuccessHandler.java
src/test/java/dev/zayt/atrilha/auth/OAuth2GoogleSignupChainITStubs.java
src/test/java/dev/zayt/atrilha/auth/web/OAuth2GoogleSignupChainIT.java
```

## Diff (stat)
```
 SUMMARY.md                                         | 123 ++++++++-------------
 .../zayt/atrilha/auth/GoogleOAuth2UserService.java |  21 ++--
 .../dev/zayt/atrilha/auth/OAuthSuccessHandler.java |  18 ++-
 .../java/dev/zayt/atrilha/auth/SecurityConfig.java |   7 +-
 .../zayt/atrilha/auth/login/AtrilhaOAuth2User.java |  65 ++++++++++-
 .../RoleBasedAuthenticationSuccessHandler.java     |   8 ++
 .../dev/zayt/atrilha/auth/web/LoginController.java |  13 ++-
 .../cadastro/adolescente_escolher_metodo.html      |   3 +
 .../auth/GoogleOAuth2UserServiceEdgeCasesTest.java |  19 ++--
 .../atrilha/auth/GoogleOAuth2UserServiceTest.java  |  24 ++--
 .../dev/zayt/atrilha/auth/OAuthHandlersIT.java     |   4 +
 .../RoleBasedAuthenticationSuccessHandlerTest.java |  19 ++++
 .../dev/zayt/atrilha/auth/web/LoginPageTest.java   |  27 +++++
 13 files changed, 224 insertions(+), 127 deletions(-)
```

## O que foi feito

FIX-015 corrige a regressao do FIX-014/PR #77 que fez a US-002 (cadastro novo via Google) parar de funcionar — Julia clicava em "Continuar com Google" sem conta no banco e caia em tela em branco porque o `GoogleOAuth2UserService` passou a lancar `account_not_found`. A correcao foi a **Opcao A** descrita na issue:

- **`OAuthDispatcherSuccessHandler` (novo)** assume o `oauth2Login.successHandler(...)` no `SecurityConfig` e decide o destino: se o principal e `AtrilhaOAuth2User` em estado `PENDING_SIGNUP` delega para `OAuthSuccessHandler` (cadastro novo → grava `pendingGoogleSignup` na sessao e redireciona `/cadastro/adolescente/complementar`); caso contrario delega para `RoleBasedAuthenticationSuccessHandler` (login real → `/trilha`, `/painel`, `/vincular`).
- **`AtrilhaOAuth2User` ganha estado `PENDING_SIGNUP`** via fabrica `pendingSignup(email, attrs)` com `account = null` e authorities vazias; `role()`/`displayName()`/`hasGuardianLink()`/`getAccount()` lancam `IllegalStateException` para forcar o chamador a checar `isPendingSignup()`. Email normalizado para lowercase no construtor.
- **`GoogleOAuth2UserService` nao lanca mais `account_not_found`** — quando `loginAccountQuery.findForLogin(email)` devolve vazio, retorna `AtrilhaOAuth2User.pendingSignup(...)`. Validacao de `email_verified` intacta.
- **`OAuthSuccessHandler` aceita os dois tipos de principal** (`OAuth2AuthenticationToken` original ou `AtrilhaOAuth2User` pendente) extraindo atributos pelo caminho apropriado.
- **`LoginController` e `RoleBasedAuthenticationSuccessHandler`** ganham defesa em profundidade: `LoginController` reconhece `AuthenticatedPrincipal` generico e ignora `PENDING_SIGNUP`; handler logga e devolve `ERROR` se o dispatcher falhar.
- **Template `adolescente_escolher_metodo.html`** ganha branch `no_account` como defesa em profundidade.
- **`OAuth2GoogleSignupChainIT` (novo, em `auth/web/`)** exercita a chain real do Spring Security end-to-end, cobrindo CA1–CA8. O bootstrap **pre-popula a sessao MockMvc com um `OAuth2AuthorizationRequest`** cujo `state` casa com o parametro `state` da URL — isso e o que faltava no IT reprovado pela revisao anterior.

### Decisoes de implementacao nao-obvias

1. **Scopes do `OAuth2AuthorizationRequest` no IT = `Set.of("email", "profile")` (sem `openid`).** A issue sugeria `Set.of("openid","email","profile")` para fidelidade ao production, mas com `openid` a chain roteia via `OidcAuthorizationCodeAuthenticationProvider` — que exige `id_token` JWT valido, JWK validation e nonce hashing. Sem `openid`, o `OAuth2LoginAuthenticationProvider` (nao-OIDC) e quem processa, e ele chama exatamente o `OAuth2UserService<OAuth2UserRequest, OAuth2User>` que estamos stubando. A logica do success/failure handler validada e identica nos dois caminhos. Decisao documentada no Javadoc do IT.

2. **Stub do `GoogleOAuth2UserService` mora em arquivo separado (`OAuth2GoogleSignupChainITStubs.java`) no pacote `dev.zayt.atrilha.auth`** porque `GoogleOAuth2UserService` e package-private. O IT em si vive em `dev.zayt.atrilha.auth.web` (conforme a issue determinou) e importa o stub. Marquei a fabrica do stub com `@Primary` para que o Spring DI injete o stub no `SecurityConfig.filterChain(...)` no lugar do bean real.

3. **Stub do `OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>` registrado como `@Bean` simples** — o `OAuth2LoginConfigurer.getAccessTokenResponseClient()` auto-resolve esse bean por `ResolvableType.forClassWithGenerics(...)`, sem precisar mexer no `SecurityConfig`. Devolve token falso BEARER 3600s — suficiente para a chain progredir ate o `userInfoEndpoint`.

4. **Testcontainers Postgres em vez de H2** no IT — os cenarios de login (CA2, CA3) precisam que `Account` persistido via `EntityManager.persist(...)` seja visivel para `JpaLoginAccountQuery`. H2 em modo PostgreSQL nao roda as migrations Flyway (sintaxe especifica). Mesmo padrao do `OAuthHandlersIT`.

5. **`atrilha.auth.seed.enabled=false`** no `@SpringBootTest.properties` do IT para forcar o `JpaLoginAccountQuery` ativo no lugar do `InMemoryLoginAccountQuery`. Sem isso, `findForLogin(email)` consultaria o stub em memoria e nao acharia a conta persistida via JPA.

### Autoavaliacao dos CAs (todos OK)

- CA1 ✓ `cadastroNovoViaGoogleRedirecionaParaComplementarComPendingSession`
- CA2 ✓ `loginGoogleComContaTeenRedirecionaParaTrilha`
- CA3 ✓ `loginGoogleComContaGuardianSemVinculoRedirecionaParaVincular` + `loginGoogleComContaGuardianComVinculoRedirecionaParaPainel`
- CA4 ✓ `emailGoogleNaoVerificadoRedirecionaComEmailUnverified`
- CA5 ✓ `cancelamentoNoGoogleRedirecionaComCancelled`
- CA6 ✓ template `adolescente_escolher_metodo.html` ganhou branch `no_account`
- CA7 ✓ `OAuth2GoogleSignupChainIT` exercita a chain real (filter → provider → userService → dispatcher)
- CA8 ✓ `OAuthHandlersIT` ganhou Javadoc explicando que e teste unitario isolado, complementado pelo novo IT
- CA9 ✓ `LoginController` ja reconhece `AtrilhaOAuth2User` (`AuthenticatedPrincipal` generico)
- CA10 ✓ testes do `GoogleOAuth2UserServiceTest`/`EdgeCasesTest` foram ajustados para validar `isPendingSignup() == true` em vez de esperar excecao `account_not_found`

### Nota de execucao

`./mvnw verify` rodado localmente — BUILD SUCCESS, Tests run: 442 (Surefire 206 + Failsafe 236), Failures: 0, Errors: 0, Skipped: 0.

## Checagem LGPD (atrilha)

N/A — sem superficie de dados pessoais nova. O FIX nao toca consentimento, compartilhamento, nem novos campos de dados de menor: e correcao de roteamento entre handlers OAuth ja existentes. O `pendingGoogleSignup` ja era gravado em sessao pre-FIX-015 (US-002 original) e continua com o mesmo escopo. ADRs 005/006/007 nao sao afetadas.
