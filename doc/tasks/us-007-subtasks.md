# US-007 — Login recorrente (subtasks)

Decomposição da US-007 em tarefas independentes para execução paralela/sequencial.

> **Regra:** cada subtask = 1 GitHub Issue = 1 branch = 1 PR squash.
> Seguir `doc/workflow.md` para ciclo DISCOVER → CREATE → EXECUTE → COMPLETE → PUBLISH.
> US-007 NÃO cria migration Flyway — a próxima `V2__...` vem com US-001.

---

## Passo a passo — execução da subtask 007.01

> **Pré-requisito:** criar a GitHub Issue com o título `chore(007.01): adiciona-dependencias-security-oauth-e-config` e labels `chore`, `infra`, `us-007`. Criar branch `chore/??-us007-deps-e-config` a partir de `main`.

### Passo 1 — Adicionar dependências ao `pom.xml`

O `pom.xml` já possui `spring-boot-starter-security` e `spring-security-test`. **Só adicionar o que falta:**

1. Entre as dependências existentes, adicione `spring-boot-starter-oauth2-client` (infra de OAuth2 Google — compartilhada por US-002, US-004 e US-007). Colocar perto de `spring-boot-starter-security`.

2. Adicione `thymeleaf-extras-springsecurity6` (dialeto `sec:` nos templates). Colocar junto das dependências de Thymeleaf (perto do `thymeleaf-layout-dialect`).

**Verificação:** `mvn compile` passa sem errors nem warnings.

### Passo 2 — Adicionar blocos de configuração em `application.properties`

O arquivo atual tem apenas: nome da app, porta 8084, i18n messages, actuator/health e mail config. Adicionar os blocos abaixo **no final do arquivo**, respeitando a ordem:

**2a — Sessão (compartilhada por todos os perfis):**
```
spring.session.store-type=cookie
server.servlet.session.timeout=30d
server.servlet.session.cookie.max-age=30d
server.servlet.session.cookie.http-only=true
# same-site=lax e secure=false em dev/test; prod sobrescreve com secure=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.secure=false
```

**2b — OAuth Google (registrado uma vez, compartilhado por US-002/004/007):**
Adicionar um comentário no topo do bloco:
```
# OAuth2 Google — registrado uma vez aqui, reutilizado por US-002 (cadastro
# via Google), US-004 e US-007. Em dev usa credenciais fixas; em prod
# exige variaveis de ambiente GOOGlE_OAUTH_CLIENT_ID e _CLIENT_SECRET.
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_OAUTH_CLIENT_ID:dev-oauth-client-id}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET:dev-oauth-client-secret}
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://openidconnect.googleapis.com/v1/userinfo
spring.security.oauth2.client.provider.google.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs
```

> **Nota:** O fallback `${GOOGLE_OAUTH_CLIENT_ID:dev-oauth-client-id}` permite boot em dev/test sem env vars. Em prod, `application-prod.properties` sobrescreve com `${GOOGLE_OAUTH_CLIENT_ID}` (sem fallback) — a app falha fast se não estiver definida.

**2c — Rate-limit de login (valores padrão):**
```
atrilha.auth.login.max-attempts=5
atrilha.auth.login.attempt-window=15m
atrilha.auth.login.block-duration=15m
```

**2d — Seeds de teste (valores de desenvolvimento):**
```
atrilha.auth.seed.teen.email=teen@atrilha.dev
atrilha.auth.seed.teen.password=teen123
atrilha.auth.seed.guardian-linked.email=guardian@atrilha.dev
atrilha.auth.seed.guardian-linked.password=guard123
atrilha.auth.seed.guardian-linked.has-guardian-link=true
atrilha.auth.seed.guardian-unlinked.email=guardian-new@atrilha.dev
atrilha.auth.seed.guardian-unlinked.password=guard123
```

### Passo 3 — Atualizar `application-dev.properties`

No final do arquivo, adicionar OAuth com credenciais de fallback dev (garante boot sem env vars):
```
spring.security.oauth2.client.registration.google.client-id=dev-oauth-client-id
spring.security.oauth2.client.registration.google.client-secret=dev-oauth-client-secret
```

> **Importante:** NÃO alterar as config existentes de datasource, flyway, thymeleaf cache (`false`), resource chain (`enabled=false` — fix-001) ou mail (Mailpit). Só adicionar o bloco OAuth.

### Passo 4 — Atualizar `application-prod.properties`

No final do arquivo, adicionar:

**4a — Cookie de sessão seguro (sobrescreve `secure=false` do base):**
```
server.servlet.session.cookie.secure=true
```

**4b — OAuth Google exigindo env vars (sem fallback):**
```
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_OAUTH_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_OAUTH_CLIENT_SECRET}
```

> **Importante:** Sem fallback `${...:default}` — a app deve fail-fast se as variáveis não estiverem setadas em produção. NÃO alterar config existentes de datasource, resource chain (fingerprinting fix-001) ou mail.

### Passo 5 — Atualizar `application-test.properties`

No final do arquivo, adicionar:

**5a — OAuth mock para testes:**
```
spring.security.oauth2.client.registration.google.client-id=test-client-id
spring.security.oauth2.client.registration.google.client-secret=test-client-secret
```

**5b — Seeds determinísticas para testes:**
```
atrilha.auth.seed.teen.email=teen@atrilha.test
atrilha.auth.seed.teen.password=test123
atrilha.auth.seed.guardian-linked.email=guardian@atrilha.test
atrilha.auth.seed.guardian-linked.password=test123
atrilha.auth.seed.guardian-linked.has-guardian-link=true
atrilha.auth.seed.guardian-unlinked.email=guardian-new@atrilha.test
atrilha.auth.seed.guardian-unlinked.password=test123
```

> **Importante:** NÃO alterar config existentes de H2, flyway desabilitado, media upload dir ou mail fake.

### Passo 6 — Adicionar `@ConfigurationPropertiesScan` ao `AtrilhaApplication.java`

O arquivo atual tem apenas `@SpringBootApplication`. Adicionar:
```java
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("dev.zayt.atrilha")
public class AtrilhaApplication {
```

> **Por que é necessário:** Sem esta anotação, os beans `LoginRateLimitProperties` (007.04) e o stub de seeds (007.03) com `@ConfigurationProperties` não são escaneados automaticamente, porque o package `dev.zayt.atrilha.auth` e `dev.zayt.atrilha.accounts` não são sub-packages do package da classe principal quando os beans estão em pacotes irmãos. O scan explícito garante que `@EnableConfigurationProperties` não seja necessário em cada bean.

### Passo 7 — Validar

1. `mvn compile` — passa sem errors nem warnings.
2. `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` — app sobe sem NPE de properties não-resolvidas.
3. `./mvnw spring-boot:run -Dspring-boot.run.profiles=test` — app sobe sem NPE.
4. `GOOGLE_OAUTH_CLIENT_ID=g1 GOOGLE_OAUTH_CLIENT_SECRET=s2 ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` — app tenta subir (vai falhar por database/mail, mas não por OAuth).
5. Sem as env vars em prod: `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` — app **deve** falhar no boot com binding error por `${GOOGLE_OAUTH_CLIENT_ID}` não resolvido.
6. `mvn test` — todos os testes existentes continuam verdes (regressão).

### Observações finais
- OAuth Google é registrado **uma única vez** (infra compartilhada por US-002, US-004, US-007). Documentar com comentário no topo do bloco em `application.properties`.
- As seeds de produção devem ser desabilitadas via `@Profile("!prod")` na implementação do stub (subtask 007.03).
- `secure=true` do cookie só em prod — sobrescrever em `application-prod.properties`.

### Critérios de aceitação
- `pom.xml` — adicionar:
  - `spring-boot-starter-security`
  - `spring-boot-starter-oauth2-client`
  - `thymeleaf-extras-springsecurity6` (dialeto `sec:` nos templates)
  - `spring-security-test` (scope `test`)
- `src/main/resources/application.properties` — adicionar blocos de:
  - Sessão (`server.servlet.session.*` com `timeout=30d`, `cookie.max-age=30d`, `http-only=true`, `same-site=lax`)
  - Rate-limit (`atrilha.auth.login.max-attempts`, `attempt-window`, `block-duration`)
  - Seeds de teste (`atrilha.auth.seed.teen.*`, `guardian-linked.*`, `guardian-unlinked.*`)
- `src/main/resources/application-dev.properties` — OAuth Google com fallbacks dev
- `src/main/resources/application-prod.properties` — OAuth Google via env vars + `cookie.secure=true`
- `src/test/resources/application-test.properties` — OAuth mock + seeds determinísticas para testes
- `AtrilhaApplication.java` — adicionar `@ConfigurationPropertiesScan("dev.zayt.atrilha")`

### Observações
- OAuth Google é registrado **uma única vez** (infra compartilhada por US-002, US-004, US-007). Documentar com comentário no topo do bloco em `application.properties`.
- As seeds de produção devem ser desabilitadas via `@Profile("!prod")` na implementação do stub (subtask 007.03).
- `secure=true` do cookie só em prod — sobrescrever em `application-prod.properties`.

### Critérios de aceitação
- `mvn compile` passa sem errors nem warnings.
- A app sobe em perfil `dev`, `prod` e `test` sem NPE de properties não-resolvidas.
- Perfil `prod` exige `${GOOGLE_OAUTH_CLIENT_ID}` e `${GOOGLE_OAUTH_CLIENT_SECRET}` como env vars.

### Branch
`chore/??-us007-deps-e-config`

---

## Subtask 007.02 — SPI: contrato de consulta e enums de destino

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.01

Define a interface `LoginAccountQuery` (SPI para persistência futura) e o enum `PostLoginDestination`. O `AccountRole` **já existe** em `dev.zayt.atrilha.auth` — reutilizar.

### Arquivos a criar
- `src/main/java/dev/zayt/atrilha/auth/login/LoginAccountQuery.java` — interface com:
  - `Optional<LoginAccount> findForLogin(String emailLowercase)`
  - Record interno `LoginAccount` com: `email`, `passwordHashBcrypt` (nullable), `role` (reusa `AccountRole` existente), `hasGuardianLink`, `displayName` (para o placeholder `/trilha`)
- `src/main/java/dev/zayt/atrilha/auth/login/PostLoginDestination.java` — enum com `TRILHA("/trilha")`, `PAINEL("/painel")`, `VINCULAR("/vincular")` + método `path()`

### Observações
- `AccountRole` (TEEN/GUARDIAN) já existe em `dev.zayt.atrilha.auth.AccountRole`. Importar, não duplicar.
- `displayName` no record: permite o "oi, {{apelido}} 👋" do placeholder `/trilha`. No stub (007.03), default = parte antes do `@` do e-mail.
- A interface é o contrato que US-001/002/003/004 vão implementar com JPA depois.

### Testes (unitários puros)
Nenhum teste unitário para interfaces/enums. A cobertura vem das subtasks de implementação (007.03, 007.05).

### Branch
`feat/??-us007-spi-login-account-query`

---

## Subtask 007.03 — Stub: contas-semente em memória (InMemoryLoginAccountQuery)

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.01, 007.02

Implementa a `InMemoryLoginAccountQuery` — bean concreto do SPI que carrega 3 contas-semente das properties e serve como mock de persistência até US-001 entregar JPA real.

### Arquivos a criar
- `src/main/java/dev/zayt/atrilha/auth/login/InMemoryLoginAccountQuery.java` — `@Component`, `@ConfigurationProperties(prefix = "atrilha.auth.seed")`, `@Profile("!prod")`
  - Lê seeds `teen`, `guardian-linked`, `guardian-unlinked` das properties
  - Cada seed: email, password (codificado para BCrypt no `@PostConstruct`), role, hasGuardianLink
  - `findForLogin(email)` busca em `Map<String, LoginAccount>` por email lowercase
  - `displayName` = parte antes do `@` do email (fallback para "oi 👋" sem nome)
  - Comentário obrigatório no topo: *"Stub do Sprint 3: contas-semente em memória para US-007 antes de US-001/002/003/004 persistirem usuários. Substituir por implementação JPA nas próximas USs."*

### Observações
- **NÃO usa Lombok** — setters explícitos ou record-like POJO com `@PostConstruct`.
- As senhas das properties podem estar em claro (dev/test) ou já BCrypt. Codificar no `@PostConstruct` via o `PasswordEncoder` do Spring.
- `@Profile("!prod")` garante que em produção o stub não carrega — a app deve fail-fast se nenhuma `LoginAccountQuery` estiver presente.

### Testes
- Verificar que as 3 seeds são carregadas corretamente (email lowercase, password BCrypt válido, role e hasGuardianLink corretos).
- `findForLogin` retorna `Optional.empty()` para e-mail inexistente.

### Branch
`feat/??-us007-inmemory-stub`

---

## Subtask 007.04 — Rate-limit: serviço de bloqueio por tentativas

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.01

Implementa o mecanismo de rate-limit in-memory para proteção contra brute-force no form login. Chave: IP + e-mail normalizado (lowercase).

### Arquivos a criar
- `src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptKey.java` — record com `emailNormalized`, `ip`; factory `of(email, ip)` normaliza email para lowercase/trimmed
- `src/main/java/dev/zayt/atrilha/auth/config/LoginRateLimitProperties.java` — `@ConfigurationProperties(prefix = "atrilha.auth.login")` com `maxAttempts`, `attemptWindow`, `blockDuration`
- `src/main/java/dev/zayt/atrilha/auth/login/LoginAttemptService.java` — `@Service`, usa `ConcurrentHashMap<LoginAttemptKey, AttemptState>` + `Clock` injetável
  - `isBlocked(key)` — verifica se chave está bloqueada (respeita expiração)
  - `registerFailure(key)` — incrementa contador; após N falhas na janela, bloqueia
  - `registerSuccess(key)` — zera estado da chave

### Observações
- O `Clock` é injetável para testes com tempo mockado. O bean `Clock.systemUTC()` pode ir no `SecurityConfig` (007.05) ou na aplicação — definir onde sem duplicar bean.
- Rate-limit **só se aplica ao form login** (OAuth Google tem proteção própria do provider).
- Limites padrão: 5 tentativas / janela 15 min / bloqueio 15 min (parametrizável).

### Testes
- `LoginAttemptServiceTest` — 22.04: incrementa contador, bloqueia após N falhas, sucesso zera, expira janela (Clock mock), normaliza email/IP (cases insensível).

### Branch
`feat/??-us007-rate-limit-service`

---

## Subtask 007.05 — Handlers e service de autenticação

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.02, 007.03, 007.04

Implementa o `AtrilhaUserDetails`, `LoginAccountUserDetailsService` (delegando para `LoginAccountQuery`), e os handlers de sucesso/falha do Spring Security com rate-limit integrado.

### Arquivos a criar
- `src/main/java/dev/zayt/atrilha/auth/login/AtrilhaUserDetails.java` — implementa `UserDetails`, expõe `getRole()` (`AccountRole`) e `hasGuardianLink()`
- `src/main/java/dev/zayt/atrilha/auth/login/LoginAccountUserDetailsService.java` — implementa `UserDetailsService`, delega para `LoginAccountQuery.findForLogin()` e retorna `AtrilhaUserDetails`
- `src/main/java/dev/zayt/atrilha/auth/login/RateLimitedAuthenticationFailureHandler.java` — `@Component`, implementa `AuthenticationFailureHandler`:
  - Lê `username` e `remoteAddr` do request → monta `LoginAttemptKey`
  - Chama `loginAttemptService.registerFailure(key)`
  - Se `isBlocked(key)` → redireciona para `/login?blocked`
  - Senão → redireciona para `/login?error`
  - **NÃO loga e-mail** — apenas `INFO auth.login.failure ip={hash do IP}` (PRD §11.8)
- `src/main/java/dev/zayt/atrilha/auth/login/RoleBasedAuthenticationSuccessHandler.java` — `@Component`, implementa `AuthenticationSuccessHandler`:
  - Se principal é `AtrilhaUserDetails` (form): resolve destino por role + hasGuardianLink
  - Se principal é `OAuth2User` (Google): busca em `LoginAccountQuery.findForLogin(email)`; se não encontrado → `/login?error`
  - Chama `loginAttemptService.registerSuccess(key)` antes de redirecionar

### Observações
- O `DaoAuthenticationProvider` com `preAuthenticationChecks` customizado (verifica bloqueio antes de comparar senha) deve ser configurado no `SecurityConfig` — mas o bean é criado na subtask 007.06 (dependência reversa). **Decisão:** o `preAuthenticationChecks` vai no SecurityConfig (007.06), e este handler trata `LockedException` → `/login?blocked`.
- **Nenhum log com dados sensíveis** (senha, e-mail completo, token).

### Testes
- `RoleBasedAuthenticationSuccessHandlerTest` — unitário com mocks: resolve `/trilha` para TEEN, `/painel` para GUARDIAN vinculado, `/vincular` para GUARDIAN sem vínculo.
- `RateLimitedAuthenticationFailureHandlerTest` — unitário: falha → `/login?error`, bloqueado → `/login?blocked`.

### Branch
`feat/??-us007-auth-handlers`

---

## Subtask 007.06 — Configuração de segurança (SecurityConfig update)

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.01, 007.05

Atualiza o `SecurityConfig` existente para habilitar form login + OAuth2 Google, integrando com os handlers e o rate-limit.

### Arquivo a editar
- `src/main/java/dev/zayt/atrilha/auth/SecurityConfig.java` — reescrever o `filterChain`:
  - **Rotas públicas:** `/`, `/health`, `/login`, `/css/**`, `/img/**`, `/js/**`, `/error/**` (preservar rotas de cadastro existentes: `/cadastro/**`, `/comecar`, `/verificar-email`, etc.)
  - **Protegidas:** `/trilha/**` → `hasRole("TEEN")`, `/painel/**` → `hasRole("GUARDIAN")`, `/vincular/**` → `hasRole("GUARDIAN")`
  - **Form login:** `.loginPage("/login")`, `.loginProcessingUrl("/login")`, success/failure handlers customizados
  - **OAuth2 login:** `.loginPage("/login")`, `successHandler` compartilhado
  - **Logout:** `/logout` → `/login?logout`
  - **Bean `DaoAuthenticationProvider`:** com `preAuthenticationChecks` que verifica bloqueio via `LoginAttemptService` (lança `LockedException` se bloqueado)
  - **Bean `Clock`:** `systemUTC()` (se não existir ainda na aplicação)

### Observações
- O SecurityConfig **já existe** — é um UPDATE, não criação. Preservar a flag `enableSessionUrlRewriting(true)` (fix-001) e o `PasswordEncoder` existente.
- `.formLogin(AbstractHttpConfigurer::disable)` do config atual é REMOVIDO — substituído pelos handlers customizados.
- `.httpBasic(AbstractHttpConfigurer::disable)` é MANTIDO.

### Testes
Cobertura via `LoginPageTest` e `LoginFlowTest` (subtask 007.07).

### Branch
`feat/??-us007-security-config`

---

## Subtask 007.07 — Controllers e templates de login

**Tipo:** `feat` | **Prioridade:** alta | **Depende de:** 007.06

Implementa os controllers (`LoginController`, `PostLoginRedirectController`) e todos os templates Thymeleaf (tela de login + 3 placeholders pós-login).

### Arquivos a criar
- `src/main/java/dev/zayt/atrilha/auth/web/LoginController.java` — `@GetMapping("/login")`:
  - Se já autenticado → redirect para destino do papel (via `RoleBasedAuthenticationSuccessHandler` logic ou direto)
  - Parâmetros `?error`, `?blocked`, `?logout` → atributos de modelo `errorState`/`infoState`
  - Renderiza `auth/login.html`

- `src/main/java/dev/zayt/atrilha/auth/web/PostLoginRedirectController.java`:
  - `GET /trilha` → `trilha/placeholder.html` (acessível por TEEN)
  - `GET /painel` → `painel/placeholder.html` (acessível por GUARDIAN vinculado)
  - `GET /vincular` → `vinculacao/inserir-codigo-placeholder.html` (GUARDIAN sem vínculo); se GUARDIAN vinculado acessa direto → 302 `/painel`

- `src/main/resources/templates/auth/login.html`:
  - Segue **literalmente** `doc/UX/us-007-spec.md` §2.1–2.3, §5
  - Banners de erro com `data-error="bad-credentials"` / `data-error="rate-limited"`
  - Form com CSRF token, campos disabled quando rate-limited
  - Botão "Continuar com Google" → link `/oauth2/authorization/google` (SEMPRE habilitado)
  - Toggle "mostrar senha" via Alpine.js

- `src/main/resources/templates/trilha/placeholder.html` — UX spec §5.1: "oi, {{apelido}} 👋" + link Sair (form POST `/logout` com CSRF)
- `src/main/resources/templates/painel/placeholder.html` — UX spec §5.2
- `src/main/resources/templates/vinculacao/inserir-codigo-placeholder.html` — UX spec §5.3: 6 inputs decorativos (disabled), botão com `alert('Em breve.')`

### Arquivos a editar
- `src/main/resources/templates/home.html` — adicionar CTA "Já tenho conta" → link para `/login`. **Única modificação** — não redesenhar a home.

### Observações
- Seguir microcopy **literal** da `doc/UX/us-007-spec.md` §5.
- Nos placeholders, usar dialeto `sec:` do Thymeleaf Spring Security para condicionar conteúdo por papel.
- **Sem scroll horizontal em 320px** — validar manualmente.

### Testes (integração com MockMvc)
- `LoginPageTest`: GET `/login` retorna 200 + elementos do form; quando autenticado redireciona.
- `LoginFlowTest`: credenciais corretas (3 cenários de destino), senha errada, e-mail inexistente (mesmo erro genérico), rate-limit bloqueia (5 falhas + 6ª), expiração do bloqueio, OAuth não afetado por bloqueio, CSRF obrigatório (403), sessão persiste.
- `PostLoginRedirectTest`: `/trilha` protegido por auth, `/painel` restrito a GUARDIAN vinculado (403 para teen), `/vincular` restrito a GUARDIAN sem vínculo.

### Branch
`feat/??-us007-login-controller-e-templates`

---

## Subtask 007.08 — Asset Google G e validação final

**Tipo:** `chore` | **Prioridade:** média | **Depende de:** 007.07

Adiciona o asset SVG oficial do Google G e roda a bateria completa de testes.

### Arquivos a criar
- `src/main/resources/static/img/google-g.svg` — asset oficial do Google Brand Resource Center (G colorido, sem repintar)

### Validações
- `mvn test` — **todos os 22 testes** da ordem TDD passam (verde, zero warnings).
- Testes de regressão: `GET /health` 200 público, `GET /` 200 público, `/css/app.css` 200, páginas de erro 404/5xx sem auth.
- Validar manualmente: viewport 320px sem scroll horizontal em `/login`.

### Observações
- Esta é a última subtask. Após passar tudo, o codificador entrega ao QA para cenários adicionais (caminhos infelizes, bordas, segurança, configurabilidade — ver seção "Estratégia de testes do QA" da US-007 original).

### Branch
`chore/??-us007-google-svg-e-validacao`

---

## Mapa de dependências

```
007.01 (deps/config)
  ├── 007.02 (SPI: LoginAccountQuery + PostLoginDestination)
  │     └── 007.03 (InMemory stub) ───────────────┐
  ├── 007.04 (Rate-limit service) ────────────────┤
  └────────────────────────────────────────────────┼── 007.05 (Handlers + UserDetailsService)
                                                   │        └── 007.06 (SecurityConfig update)
                                                   
007.02 ────────────────────────────────────────────┘                    │
                                                                         ▼
                                                        007.07 (Controllers + templates)
                                                             └── 007.08 (Asset + validação final)
```

**Perfis de execução paralela:**
- Faixa A (infra): 007.01 → serial com tudo que vem depois
- Faixa B (core): 007.02 + 007.04 podem ir em paralelo (ambos dependem só de 007.01)
- Faixa C (integração): 007.03, 007.05 serial depois de B
- Faixa D (UI): 007.06, 007.07 serial depois de C
- Faixa E (final): 007.08 último

---

## Riscos compartilhados

1. **`AccountRole` já existe** em `dev.zayt.atrilha.auth`. A US-007 original sugere criá-lo em `auth/login/AccountRole.java` — **NÃO duplicar**. Importar o existente.
2. **`SecurityConfig` já existe** com formLogin desabilitado e todas rotas públicas. A subtask 007.06 faz UPDATE, não criação. Preservar `enableSessionUrlRewriting(true)` (fix-001).
3. **`@ConfigurationPropertiesScan`** precisa ser adicionado ao `AtrilhaApplication` (007.01) — sem ele, `LoginRateLimitProperties` e seeds não carregam.
4. **OAuth Google registrado uma vez.** US-002 e US-004 herdam a config — não duplicar em outras tasks.
5. **Prod sem stub.** A app em produção sem `LoginAccountQuery` real (antes de US-001) deve fail-fast no startup. Registrar em "Riscos" do PR que Sprint 3 não deploya login em prod.
