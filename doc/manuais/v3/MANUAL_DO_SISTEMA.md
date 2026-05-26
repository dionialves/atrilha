# Manual do Sistema — atrilha

> Status: sprints 01, 02 e 03 finalizados (autenticação Google/Apple adiada).
> Foco: documentar **cada endpoint da API hoje**, o fluxo de trabalho de cada um,
> as classes/métodos envolvidos e o porquê de terem sido construídos assim.
> Ao final, uma seção descreve os arquivos transversais que não pertencem a
> nenhum endpoint específico.

---

## Sumário

1. [Visão geral da arquitetura](#1-visão-geral-da-arquitetura)
2. [Inventário de endpoints](#2-inventário-de-endpoints)
3. Endpoints
   1. [`GET /`](#31-get--home)
   2. [`GET /comecar`](#32-get-comecar-legado-us-001)
   3. [`GET /cadastro`](#33-get-cadastro-porta-canônica)
   4. [`GET /cadastro/concluido`](#34-get-cadastroconcluido-placeholder)
   5. [`GET /cadastro/responsavel`](#35-get-cadastroresponsavel-stub)
   6. [`GET /cadastro/adolescente`](#36-get-cadastroadolescente-form)
   7. [`GET /cadastro/adolescente/escolher-metodo`](#37-get-cadastroadolescenteescolher-metodo)
   8. [`POST /cadastro/adolescente`](#38-post-cadastroadolescente-submissão-do-cadastro)
   9. [`GET /login`](#39-get-login)
   10. [`POST /login`](#310-post-login-spring-security-form-login)
   11. [`POST /logout`](#311-post-logout)
   12. [`GET /verificar-email`](#312-get-verificar-email)
   13. [`POST /verificar-email/reenviar`](#313-post-verificar-emailreenviar)
   14. [`GET /verify-email`](#314-get-verify-email)
   15. [`GET /trilha`](#315-get-trilha)
   16. [`GET /painel`](#316-get-painel-placeholder)
   17. [`GET /vincular`](#317-get-vincular-placeholder)
   18. [`GET /media/**`](#318-get-media--avatar-estático)
   19. [`GET /health`](#319-get-health-actuator)
4. [Arquivos transversais (fora dos endpoints)](#4-arquivos-transversais-fora-dos-endpoints)

---

## 1. Visão geral da arquitetura

O atrilha é uma aplicação **Spring Boot 3 / Java 21** server-side rendered, com
views Thymeleaf e autenticação por sessão HTTP. A organização do código segue
um modelo modular descrito em `doc/PRD.md` §9.3: cada pacote sob
`dev.zayt.atrilha.*` é tratado como um módulo de domínio, e a comunicação
entre módulos só acontece por interfaces públicas ou eventos do Spring
(`ApplicationEventPublisher`).

Módulos relevantes hoje:

- **`accounts`** — entidade `Account`, perfil do adolescente, cadastro,
  validação de idade, armazenamento de avatar.
- **`auth`** — login, sessão, verificação de e-mail, configuração de
  Spring Security, rate-limit.
- **`notifications`** — envio do e-mail de verificação via JavaMail +
  Thymeleaf.
- **`shared`** — utilitários horizontais (`HtmlSanitizer`).
- **`web`** — controllers transversais (home, entry point de cadastro,
  stubs).
- **`config`** — configs globais (atualmente só `MediaResourceConfig`).
- **`admin`**, **`content`**, **`progress`** — pacotes ainda vazios,
  reservados para sprints futuros (existe `package-info.java` em cada um
  para fixar a fronteira do módulo).

A entrada de tudo é a classe `AtrilhaApplication`
(`@SpringBootApplication` + `@ConfigurationPropertiesScan`).

Persistência: PostgreSQL com Flyway (`V1__baseline.sql` a
`V4__remove_google_oauth.sql`). O ORM é JPA via Spring Data.

Autenticação: Spring Security com form-login. Não há JWT, OAuth nem
autenticação por token — tudo passa pelo cookie `JSESSIONID`
(configurado como HttpOnly, SameSite=Lax, e Secure em produção).

---

## 2. Inventário de endpoints

| # | Método | Path | Controller / Handler | View / Resposta | Sprint |
|---|--------|------|----------------------|-----------------|--------|
| 1 | GET    | `/`                                  | `HomeController.home`                            | `home.html`                                    | 01 |
| 2 | GET    | `/comecar`                           | `StartFlowController.start`                      | `comecar.html`                                 | 01 (legado) |
| 3 | GET    | `/cadastro`                          | `SignupEntryController.entry`                    | `cadastro/escolher-papel.html`                 | 02 |
| 4 | GET    | `/cadastro/concluido`                | `SignupEntryController.concluido`                | `cadastro/concluido.html`                      | 02 |
| 5 | GET    | `/cadastro/responsavel`              | `GuardianRegistrationStubController.comingSoon`  | `cadastro/responsavel_em_breve.html`           | stub |
| 6 | GET    | `/cadastro/adolescente`              | `AdolescentRegistrationController.renderForm`    | `cadastro/adolescente.html`                    | 02 |
| 7 | GET    | `/cadastro/adolescente/escolher-metodo` | `AdolescentRegistrationController.renderEscolherMetodo` | `cadastro/adolescente_escolher_metodo.html` | 02 |
| 8 | POST   | `/cadastro/adolescente`              | `AdolescentRegistrationController.submit`        | redirect ou re-render                          | 02 |
| 9 | GET    | `/login`                             | `LoginController.renderLogin`                    | `auth/login.html` ou redirect                  | 03 |
| 10 | POST  | `/login`                             | Spring Security (`UsernamePasswordAuthenticationFilter`) | redirect via handlers                  | 03 |
| 11 | POST  | `/logout`                            | Spring Security (`LogoutFilter`)                 | redirect para `/login?logout`                  | 03 |
| 12 | GET   | `/verificar-email`                   | `EmailVerificationController.renderPending`      | `verificar-email.html` ou redirect             | 03 |
| 13 | POST  | `/verificar-email/reenviar`          | `EmailVerificationController.resend`             | redirect para `/verificar-email` com flash     | 03 |
| 14 | GET   | `/verify-email`                      | `EmailVerificationController.verify`             | `verify-email-resultado.html`                  | 03 |
| 15 | GET   | `/trilha`                            | `PostLoginRedirectController.trilha`             | `trilha/placeholder.html`                      | 03 (placeholder) |
| 16 | GET   | `/painel`                            | `PostLoginRedirectController.painel`             | `painel/placeholder.html`                      | 03 (placeholder) |
| 17 | GET   | `/vincular`                          | `PostLoginRedirectController.vincular`           | `vinculacao/inserir-codigo-placeholder.html`   | 03 (placeholder) |
| 18 | GET   | `/media/**`                          | `MediaResourceConfig` (ResourceHandler)          | arquivo estático em `${app.media.upload-dir}`  | 02 |
| 19 | GET   | `/health`                            | Spring Boot Actuator                             | JSON `{"status":"UP"}`                         | infra |

Endpoints declarados `permitAll` no `SecurityConfig` (públicos): `/`,
`/health`, `/login`, `/error`, `/css/**`, `/img/**`, `/js/**`,
`/cadastro/**`, `/comecar`, `/verificar-email`, `/verify-email` e — por
catch-all — quaisquer outras rotas não listadas como `hasRole(...)`.

Endpoints protegidos:

- `/trilha/**` exige `ROLE_TEEN`.
- `/painel/**` exige `ROLE_GUARDIAN`.
- `/vincular/**` exige `ROLE_GUARDIAN`.
- `/verificar-email/reenviar` exige sessão autenticada (qualquer papel).

---

## 3. Endpoints

### 3.1 `GET /` — Home

**Arquivo:** `web/HomeController.java`

**View:** `templates/home.html`

#### Fluxo

1. Spring Security marca a rota como `permitAll`.
2. `HomeController` (uma única classe package-private com um único método)
   responde devolvendo o nome lógico de view `home`.
3. O resolver Thymeleaf renderiza `templates/home.html`, que apresenta o
   call-to-action principal apontando para `/comecar` (rota legada) e/ou
   `/cadastro`.

#### Classes e métodos

- **`HomeController`** — controller anêmico. Único método `home()` retorna a
  string `"home"`. Não recebe parâmetros e não toca em nenhum service. Existe
  apenas para reservar a porta canônica `/` e satisfazer o teste
  `HealthEndpointIT` / smoke tests sem expor a view via redirect.

#### Por que assim

- Não há nada de domínio acontecendo na home — é uma página de marketing
  estática hoje. Controller minimalista evita criação de service vazio.
- A classe é `package-private` (sem `public`) seguindo a convenção do
  projeto: o Spring registra via stereotype, e ninguém fora do package
  precisa referenciar a classe.

#### Considerações / alternativas

- **Alternativa A — usar `WebMvcConfigurer#addViewControllers`**: serviria a
  view sem precisar de classe. Mais enxuto, mas perde o ponto de extensão
  caso a home eventualmente precise carregar dados (banner promocional,
  destaque de trilha, etc.). O custo de manter o controller já existente é
  praticamente zero, então o trade-off favorece deixar como está.
- **Alternativa B — renderizar redirect para `/cadastro`**: tornaria a home
  uma porta de entrada direta, mas elimina a possibilidade de uma landing
  específica e contraria o desenho da US-001 que prevê material de
  apresentação.

---

### 3.2 `GET /comecar` — Legado (US-001)

**Arquivo:** `web/StartFlowController.java`

**View:** `templates/comecar.html`

#### Fluxo

Idêntico ao `/`: o controller devolve a string `"comecar"` e o Thymeleaf
renderiza a view com dois cards — "Sou adolescente" e "Sou responsável".

#### Classes e métodos

- **`StartFlowController.start()`** — handler único, sem parâmetros.

#### Por que assim

Esta rota é o entry point original da US-001 CA-1 ("Apresenta os dois
caminhos antes do e-mail/senha"). A US-002 introduziu uma versão mais
elaborada da mesma tela em `/cadastro`. O Javadoc de `SignupEntryController`
diz explicitamente que `/comecar` é mantida porque a `home.html` ainda
aponta para ela.

#### Considerações / alternativas

- **Alternativa A — apagar `/comecar` e fazer redirect 301 para `/cadastro`**.
  Eliminaria duplicidade de manutenção (dois templates muito parecidos).
  Trade-off: links externos antigos (e bookmarks) continuariam funcionando
  por causa do redirect, mas qualquer divergência visual desejada entre as
  duas telas seria perdida. Recomendado fazer essa unificação assim que a
  `home.html` for atualizada para apontar direto para `/cadastro`.
- **Alternativa B — manter as duas rotas como pontos de teste A/B**. Se o
  produto quiser experimentar variações de CTA, ter duas portas é
  conveniente. Não há infraestrutura de A/B no projeto hoje, então a
  vantagem é apenas teórica.

---

### 3.3 `GET /cadastro` — Porta canônica

**Arquivo:** `web/SignupEntryController.java`

**View:** `templates/cadastro/escolher-papel.html`

#### Fluxo

1. Rota pública (`/cadastro/**` é `permitAll` no `SecurityConfig`).
2. Controller devolve o nome de view `cadastro/escolher-papel`.
3. Thymeleaf renderiza dois cards: "Sou adolescente" (link →
   `/cadastro/adolescente/escolher-metodo`) e "Sou responsável" (link →
   `/cadastro/responsavel`).

#### Classes e métodos

- **`SignupEntryController.entry()`** — retorna `"cadastro/escolher-papel"`.
  Convive com `/comecar` deliberadamente: o Javadoc da classe documenta que
  esta é a "porta canonica daqui pra frente" enquanto `/comecar` é
  mantida por compatibilidade.

#### Por que assim

- A US-002 reorganizou a entrada do cadastro para um caminho mais coerente
  com a estrutura de URLs (`/cadastro/...`). Como `home.html` ainda
  referencia `/comecar`, as duas rotas continuam ativas até a próxima
  refatoração de links.

#### Considerações / alternativas

- Mesmo trade-off do `/comecar`: o controller poderia ser substituído por
  uma simples view controller (`addViewControllers`) — mais enxuto, menos
  flexível.
- **Alternativa interessante**: unificar `entry()` e `concluido()` em um
  controller só agrupando todas as rotas de "fluxo de cadastro
  transversal" (já é o caso hoje, pois ambas vivem em `SignupEntryController`).

---

### 3.4 `GET /cadastro/concluido` — Placeholder

**Arquivo:** `web/SignupEntryController.java`

**View:** `templates/cadastro/concluido.html`

#### Fluxo

Retorna o nome `cadastro/concluido`. A view é hoje um placeholder do passo
"O que vem agora?" — a US-012 (vinculação responsável → adolescente) vai
substituir o conteúdo.

#### Classes e métodos

- **`SignupEntryController.concluido()`** — handler único.

#### Por que assim

A rota foi criada para que o cadastro tenha uma página de aterrissagem
**conhecida** mesmo enquanto a próxima feature (vinculação) ainda não existe.
Permite que testes de fluxo navegacional já passem.

#### Considerações / alternativas

- **Alternativa A — não criar a rota até a US-012 estar pronta**. Vantagem:
  menos código mortinho. Desvantagem: links no fluxo precisariam ser
  alterados em duas etapas (criar a rota agora, atualizar links agora;
  depois implementar a feature real). A escolha foi privilegiar links
  estáveis.
- **Alternativa B — usar redirect direto para `/`**. Seria mais transparente,
  mas perderia a oportunidade de mostrar mensagem "Cadastro concluído ✓"
  antes de mandar o usuário embora.

---

### 3.5 `GET /cadastro/responsavel` — Stub

**Arquivo:** `web/GuardianRegistrationStubController.java`

**View:** `templates/cadastro/responsavel_em_breve.html`

#### Fluxo

Retorna `cadastro/responsavel_em_breve`, uma view estática com a mensagem
"Em breve — a tela do responsável chega no Sprint 4".

#### Classes e métodos

- **`GuardianRegistrationStubController.comingSoon()`** — handler único.
  O Javadoc da classe avisa que o controller será removido ou substituído
  quando a US-003 for implementada.

#### Por que assim

O card "Sou responsável" no `/cadastro` precisa de um destino para evitar
um 404 frustrante. O stub mantém o fluxo navegacional íntegro até a
US-003 entregar o cadastro real do responsável.

#### Considerações / alternativas

- **Alternativa A — desabilitar o card no front até US-003 chegar**: mais
  honesto sobre o estado, mas perde sinal de funil (não dá pra medir quem
  tentou se cadastrar como responsável). Manter o stub permite rastrear
  intenção.
- **Alternativa B — coletar e-mail nesse stub para notificar quando a
  feature for lançada**: agrega valor de produto, mas adiciona escopo
  (precisaria de service, repositório e fila de notificação) que não
  estava previsto. Decisão atual privilegiou minimalismo.

---

### 3.6 `GET /cadastro/adolescente` — Form

**Arquivo:** `accounts/web/AdolescentRegistrationController.java`

**View:** `templates/cadastro/adolescente.html`

#### Fluxo

1. Rota pública.
2. `renderForm(Model model)` é chamado.
3. Se `model` ainda **não** contém um atributo `"form"` (caso de acesso
   direto), o controller adiciona um `new RegisterAdolescentForm()` vazio
   para o Thymeleaf poder fazer binding sem NPE.
4. Quando o controller é chamado a partir do `POST` falho (re-render), o
   atributo `"form"` já está populado com os valores submetidos —
   preservando o que o usuário digitou.
5. View renderiza inputs de email, senha, apelido, data de nascimento e
   upload de foto, todos com `th:field="*{...}"` ligados ao bean.

#### Classes e métodos

- **`AdolescentRegistrationController.renderForm`** — handler do GET; pura
  preparação do model. Não toca em service nem repositório.
- **`RegisterAdolescentForm`** — bean mutável (precisa de getters/setters
  para o data-binder do Spring), separado do record imutável
  `RegisterAdolescentRequest`. O Javadoc da classe explica essa decisão:
  records têm setters virtuais que falham silenciosamente em algumas
  combinações de binder; uma classe POJO é mais robusta para o caminho
  HTTP.

#### Por que assim

- Renderizar form vazio vs. com flash attribute é o padrão Spring MVC para
  fluxos POST-redirect-GET. Aqui o re-render acontece **na mesma request**
  (sem redirect), então o `BindingResult` injeta o atributo diretamente —
  por isso o `if (!model.containsAttribute("form"))` evita sobrescrever.

#### Considerações / alternativas

- **Alternativa A — separar o GET inicial do GET pós-erro em rotas
  distintas** (ex.: `/cadastro/adolescente/novo` e `/cadastro/adolescente/erro`).
  Mais explícito, mas quebra a convenção `MVC POST → re-render`. O custo
  ergonômico não compensa.
- **Alternativa B — usar JSR-303 com `@RestController` + frontend JS** para
  validar inline sem reload. Mais moderno, mas exige adoção de JS framework
  e mantenedor com conhecimento de SPA. Não está no escopo do MVP.

---

### 3.7 `GET /cadastro/adolescente/escolher-metodo`

**Arquivo:** `accounts/web/AdolescentRegistrationController.java`

**View:** `templates/cadastro/adolescente_escolher_metodo.html`

#### Fluxo

Após o card "Sou adolescente" no `/cadastro`, o usuário cai nesta tela que
oferece duas opções: continuar com e-mail/senha (link → `/cadastro/adolescente`)
ou usar Google.

O botão "Google" **existe na UI mas está inerte** após a REF-003 ter
removido o `AdolescentGoogleSignupController`. O Javadoc do método deixa
isso explícito: a rota é mantida para preservar bookmarks e contratos de
teste de regressão da US-002.

#### Classes e métodos

- **`AdolescentRegistrationController.renderEscolherMetodo()`** — handler
  único, sem parâmetros.

#### Por que assim

A separação em "tela de escolha → tela do formulário" foi pedida pela
US-002 (Tela 2). Permite preparar o terreno para múltiplos métodos
(Google/Apple) sem inflar o `AdolescentRegistrationController` com lógica
de roteamento condicional.

#### Considerações / alternativas

- **Alternativa A — fundir essa tela com `/cadastro/adolescente`** colocando
  os botões "Google/Apple" no topo do form. Reduz uma rota e remove uma
  hop de navegação. Trade-off: a tela atual está alinhada com o desenho de
  UX da Sprint 02 que prefere passos pequenos.
- **Alternativa B — esconder o botão Google até a feature estar pronta**:
  evita a sensação de "botão quebrado". Foi adiado por decisão de produto
  (queriam medir interesse pelo método antes de implementar).

**Pendência conhecida**: enquanto a auth Google/Apple não voltar, esse
botão é dead code do ponto de vista funcional. Vale colocar um tooltip
"em breve" ou desabilitá-lo via `disabled` no template.

---

### 3.8 `POST /cadastro/adolescente` — Submissão do cadastro

**Arquivo:** `accounts/web/AdolescentRegistrationController.java`

**Comportamento de saída:**
- Sucesso → `redirect:/verificar-email` (após autenticar sessão).
- Erro de idade → renderiza `cadastro/adolescente_bloqueado` com `variant`
  (`under-13` ou `over-17`).
- Outros erros de validação → re-renderiza `cadastro/adolescente`.
- E-mail duplicado → re-renderiza `cadastro/adolescente` com erro inline
  no campo `email`.

#### Fluxo detalhado

1. Spring binda o `RegisterAdolescentForm` a partir do `multipart/form-data`
   (necessário por causa do upload de foto).
2. Jakarta Validation executa: `@NotBlank`/`@Email` no email,
   `@Size(8..72)` na senha, `@NotBlank`/`@Size(3..20)` no apelido,
   `@NotNull`/`@EligibleAge(role=TEEN)` na data de nascimento.
3. `BindingResult` recebe os erros.
4. **Detecção de bloqueio etário exclusivo** (`detectAgeBlockVariant`): o
   controller examina se o BindingResult contém **exclusivamente** uma
   violação de `@EligibleAge` no campo `birthDate`. Se sim, calcula a
   idade real e devolve a variante `under-13` (nascido há menos de 13 anos)
   ou `over-17` (18+). Caso contrário, devolve `null`.
5. Se a variante foi detectada → renderiza
   `cadastro/adolescente_bloqueado` com `model.variant = ...`.
6. Se há outros erros (compostos, ou não relacionados a idade) →
   re-renderiza o form (`cadastro/adolescente`), preservando valores.
7. Sem erros → chama `RegisterAdolescentService.register(form.toRequest(),
   photo)`:
   - Normaliza e-mail (`trim().toLowerCase(Locale.ROOT)`).
   - Checa duplicidade (`existsByEmailIgnoreCaseAndDeletedAtIsNull`).
   - Se duplicado → retorna `Outcome.EmailConflict`.
   - Senão: gera `UUID.randomUUID()` para `accountId`, cria `Account`
     (`type="ADOLESCENT"`), `passwordHash = BCrypt(senha)`,
     `createdAt = OffsetDateTime.now()`. Persiste com `saveAndFlush`
     (garante que o INSERT vai antes do FK do perfil).
   - Cria `AdolescentProfile`: `nickname` passado pelo `HtmlSanitizer`,
     `birthDate`, `timezone="America/Sao_Paulo"`.
   - Se `photo` veio preenchida: chama `AvatarStorage.store(accountId, photo)`
     e seta `avatarUrl`.
   - Persiste o profile com `saveAndFlush`.
   - Publica `AccountRegisteredEvent(accountId)` no `ApplicationEventPublisher`.
   - Retorna `Outcome.Registered(accountId)`.
8. Controller faz pattern matching no `Outcome`:
   - `Registered`: chama `SessionAuthenticator.authenticate(request,
     response, accountId, AccountRole.TEEN)` e devolve
     `redirect:/verificar-email`.
   - `EmailConflict`: faz `bindingResult.rejectValue("email",
     "email.duplicate", "Esse e-mail já tem conta. Quer entrar?")` e
     re-renderiza o form.
9. Após o commit da transação do `register`, o
   `AccountRegisteredEventListener.onAccountRegistered` (fase
   `AFTER_COMMIT`, propagation `REQUIRES_NEW`) dispara:
   - Busca a conta.
   - Se já estiver verificada (não é o caso de cadastro novo) — sai.
   - `EmailVerificationService.issueToken(account)` cria
     `EmailVerificationToken` (UUID v4, TTL 24h) no banco e invalida
     tokens pendentes anteriores.
   - Busca o nickname via `AccountProfileLookup`.
   - Chama `EmailVerificationSender.sendVerification(email, nickname,
     token)` (JavaMail + Thymeleaf renderiza HTML + texto-plano).
   - Falha de SMTP é capturada e logada (sem token e sem corpo do e-mail);
     o usuário pode pedir reenvio na tela `/verificar-email`.
10. O browser segue o `redirect:/verificar-email` e cai em
    `EmailVerificationController.renderPending`.

#### Classes e métodos

- **`AdolescentRegistrationController.submit`** — orquestra o POST.
- **`AdolescentRegistrationController.detectAgeBlockVariant`** — método
  privado, contém a regra de negócio "só bloqueia idade se for o único
  problema". Lê `BindingResult.getCodes()` procurando algum começando com
  `"EligibleAge"`. Resolve a variante pela idade real
  (`Period.between(birthDate, LocalDate.now(clock)).getYears()`), não pela
  string traduzida — assim a regra funciona em qualquer locale.
- **`RegisterAdolescentForm`** — bean mutável (vide §3.6).
- **`RegisterAdolescentRequest`** — record imutável com as validações
  centralizadas; é o que o service consome.
- **`RegisterAdolescentService`** — orquestrador do cadastro. Anotado
  `@Transactional`. Decisão consciente de **não** validar idade aqui — o
  contrato vive nas anotações Jakarta no DTO; o service assume input
  válido.
- **`RegisterAdolescentService.Outcome`** — sealed interface com
  `Registered(UUID accountId)` e `EmailConflict()`. O Javadoc explica que
  é sealed "para forçar pattern matching exaustivo no controller". Java 21
  + switch expression exhaustiveness check pega o erro em compile-time se
  alguém adicionar um terceiro caso sem tratá-lo.
- **`AccountRepository`** — JpaRepository com derived queries
  `findByEmailIgnoreCaseAndDeletedAtIsNull` e o `exists...` (soft-delete-aware).
- **`AdolescentProfileRepository`** — JpaRepository com
  `findByAccountId(UUID)`.
- **`HtmlSanitizer`** — wrapper sobre Jsoup com
  `Safelist.none()` (zero tags, zero atributos). Limpa o apelido antes de
  persistir.
- **`FilesystemAvatarStorage`** — implementação default de `AvatarStorage`.
  Aceita JPG/PNG/WEBP até 5 MB; grava em
  `${app.media.upload-dir}/avatars/{accountId}.{ext}`. Devolve URL
  relativa `/media/avatars/{accountId}.{ext}` (consumido pelo
  `MediaResourceConfig`).
- **`SessionAuthenticator`** — encapsula `SecurityContextHolder` +
  `HttpSessionSecurityContextRepository.saveContext` para garantir que a
  sessão fica gravada *no momento* do POST. Cria
  `UsernamePasswordAuthenticationToken.authenticated` com principal
  `AuthenticatedAccount(id, role)` e authority `ROLE_<role>`.
- **`AccountRegisteredEventListener`** — listener
  `@TransactionalEventListener(phase = AFTER_COMMIT)`,
  `@Transactional(propagation = REQUIRES_NEW)`. O Javadoc explica que
  AFTER_COMMIT garante que e-mail só sai se a transação persistir; e
  REQUIRES_NEW garante uma transação ativa para o INSERT do token.
- **`AccountRegisteredEvent`** — record `(UUID accountId)`. Decisão de
  carregar só o id no evento — não a entidade — para evitar serialização
  cross-module e LazyInitializationException em listeners.
- **`AccountReader`** / **`JpaAccountReader`** — interface SPI exposta
  `accounts` → consumidores. `findById`, `markEmailVerifiedAt` e
  `findByEmailIgnoreCase`. Restringe quais operações cruzam a fronteira do
  módulo `accounts`, deixando `AccountRepository` package-private.
- **`AccountProfileLookup`** / **`JpaAccountProfileLookup`** — mesmo
  princípio, exposto só o `findNickname(UUID)`.
- **`EligibleAge`** + **`EligibleAgeValidator`** + **`AgeEligibilityChecker`**
  + **`AgeEligibilityViolation`** + **`AgeEligibilityConfig`** — conjunto
  de validação por idade. A constraint anotada delega ao
  `EligibleAgeValidator` (que recebe `AgeEligibilityChecker` via
  `SpringConstraintValidatorFactory`); o checker é puro (sem persistência,
  sem PII, recebe `Clock` injetado). O `AgeEligibilityConfig` define o
  bean `Clock` fixado em `America/Sao_Paulo` para que "hoje" tenha
  significado determinístico independente da timezone do container.
- **`Account`** — entidade JPA. Coluna `type` discrimina ADOLESCENT vs.
  GUARDIAN. CHECK constraint no banco
  (`accounts_credential_chk`) aceita `password_hash NULL` apenas se
  `deleted_at` estiver preenchido.
- **`AdolescentProfile`** — entidade JPA com PK compartilhada
  (`@MapsId`) garantindo 1:1 com Account no nível do banco.

#### Por que assim

- **Sealed `Outcome`** evita códigos de erro mágicos (strings, ints) e dá
  exaustividade verificada pelo compilador. Quando aparecer um novo caso
  (ex.: `Throttled` se houver rate-limit no cadastro), o `switch` no
  controller deixa de compilar até ser tratado.
- **`saveAndFlush`** explícito antes de tentar inserir o profile evita
  `DataIntegrityViolationException` quando o JPA decidir fazer flush
  somente no commit.
- **`HtmlSanitizer` com `Safelist.none()`** elimina XSS armazenado mesmo
  que o template fosse renderizar sem `th:text` (defesa em profundidade).
- **`AccountRegisteredEvent` com AFTER_COMMIT** desacopla o envio de e-mail
  do caminho crítico do cadastro. Se o SMTP estiver lento ou caído, o
  usuário ainda termina o cadastro (o e-mail pode ser reenviado).
- **`SessionAuthenticator` próprio** em vez de `SecurityContextHolder`
  cru: precisamos persistir o contexto na sessão HTTP **dentro** do POST
  para que o redirect já chegue como autenticado. O método encapsula essa
  pegadinha (que costuma esquecer o `saveContext`).
- **`AccountReader`/`AccountProfileLookup`** como interfaces SPI: cumprem
  a regra de fronteira de módulo descrita em `package-info.java` — o
  módulo `auth` não importa `AccountRepository` diretamente.

#### Considerações / alternativas

- **Alternativa A — coletar idade em vez de data de nascimento**: form
  mais simples (1 input numérico), mas perde-se exatidão (idade muda no
  aniversário) e o cálculo de elegibilidade fica menos auditável. Trade-off
  ruim — manter `LocalDate` é a escolha correta.
- **Alternativa B — usar `@RestController` + retornar JSON 422 com lista
  de erros**: melhor para integrar com SPA, pior para o fluxo
  server-rendered atual (que precisa repopular form). A escolha
  Thymeleaf-friendly é coerente com o restante do stack.
- **Alternativa C — fazer a validação de idade dentro do service em vez
  da anotação**: centraliza a regra no domínio, mas perde o ponto de
  intercepção pré-binding (não dá pra renderizar a tela de bloqueio sem
  passar pelo service). O modelo atual permite o "atalho" da tela
  `adolescente_bloqueado` sem fazer um SELECT no banco.
- **Alternativa D — salvar foto em S3 desde o MVP**: melhor para scale,
  mas exige credenciais, custo extra e fluxo de pre-signed URL. O design
  já prevê isso ao isolar `AvatarStorage` como interface — basta plugar
  outro bean.
- **Alternativa E — enviar e-mail no caminho síncrono (não via evento)**:
  ergonômico em código, péssimo em latência e robustez (SMTP down derruba
  o cadastro). O modelo via evento é o padrão correto.
- **Riscos atuais**:
  - O sanitizer aplica `Safelist.none()` antes do `@Size` ser validado.
    Como a constraint roda **antes** do service, isso não é problema na
    prática — mas vale lembrar que se algum dia o sanitizer entrar **antes**
    da validação, um apelido com tags HTML poderia "encolher" e passar pela
    constraint de tamanho mínimo errada. Hoje o fluxo está seguro porque
    o `@Size` valida o input cru.
  - `FilesystemAvatarStorage` não escala para múltiplas instâncias do
    container (cada uma teria seu próprio diretório). Aceitável para MVP,
    mas é dívida explícita.

---

### 3.9 `GET /login`

**Arquivo:** `auth/web/LoginController.java`

**View:** `templates/auth/login.html` ou redirect.

#### Fluxo

1. Rota pública.
2. Se o usuário **já está autenticado** com um `AtrilhaUserDetails` no
   `SecurityContext`, o controller resolve o destino "pós-login" e faz
   `redirect:`:
   - `TEEN` → `/trilha`.
   - `GUARDIAN` com vínculo → `/painel`.
   - `GUARDIAN` sem vínculo → `/vincular`.
3. Senão, o controller lê o `parameterMap` da request:
   - Contém chave `blocked` (sem `=`) → `model.errorState = "rate-limited"`.
   - Contém chave `error` → `model.errorState = "bad-credentials"`.
   - Contém chave `logout` → `model.infoState = "logged-out"`.
4. Renderiza `auth/login.html`.

#### Classes e métodos

- **`LoginController.renderLogin(HttpServletRequest, Model)`** — handler.
- **`LoginController.resolveDestination(AtrilhaUserDetails)`** — helper
  privado que devolve `PostLoginDestination` (enum com `path()`).
- **`PostLoginDestination`** — enum com 4 valores
  (`TRILHA`, `PAINEL`, `VINCULAR`, `ERROR`) carregando cada um o path
  associado.
- **`AtrilhaUserDetails`** — `UserDetails` do Spring Security adaptando
  `LoginAccountQuery.LoginAccount`. Implementa também
  `AuthenticatedPrincipal` (interface mais genérica).

#### Por que assim

- **Detecção de "já autenticado"** evita renderizar a tela de login para
  quem já está logado (UX spec) — em vez disso, manda direto para o
  destino apropriado.
- **`parameterMap` em vez de `@RequestParam`**: o comentário no código
  explica que o Spring Security envia `?error` e `?logout` sem `=`. O
  Tomcat materializa isso como string vazia, mas o MockMvc usado nos
  testes devolve `null`. Olhar para `containsKey` funciona nos dois
  ambientes.
- **Estado da tela** é exposto como duas strings (`errorState`,
  `infoState`) em vez de booleans separados para que o template possa
  fazer `th:switch` limpo.

#### Considerações / alternativas

- **Alternativa A — usar `@RequestParam(required=false)` com default
  null** e checar `!= null`. Funciona no Tomcat mas falha no MockMvc.
  Como há testes de integração extensos (`LoginPageTest`,
  `PostLoginRedirectTest` etc.), a opção `parameterMap` paga o pequeno
  custo de verbosidade.
- **Alternativa B — extrair o roteamento pós-login (TEEN→trilha, etc.)
  para um único componente compartilhado entre `LoginController` e
  `RoleBasedAuthenticationSuccessHandler`**. Hoje a regra está duplicada
  (em `LoginController.resolveDestination` e
  `RoleBasedAuthenticationSuccessHandler.resolveForRole`). Bom alvo de
  refactor — extrair para `PostLoginDestinationResolver` e injetar nos
  dois lugares.

---

### 3.10 `POST /login` — Spring Security form login

**Não há controller customizado.** O endpoint é processado pelo
`UsernamePasswordAuthenticationFilter` configurado em
`SecurityConfig.filterChain`.

#### Fluxo

1. Filter chain do Spring Security recebe o POST.
2. `DaoAuthenticationProvider` (bean definido em `SecurityConfig`):
   - Chama `LoginAccountUserDetailsService.loadUserByUsername(email)`.
   - O service delega para `LoginAccountQuery.findForLogin(emailLower)`.
   - Em produção, o bean ativo é **`JpaLoginAccountQuery`** (consulta o
     banco via `AccountReader.findByEmailIgnoreCase`).
   - Em dev/test com `atrilha.auth.seed.enabled=true`, o bean ativo é
     **`InMemoryLoginAccountQuery`** (mapa em memória populado por
     `@ConfigurationProperties` `atrilha.auth.seed.*`).
   - Resultado vira `AtrilhaUserDetails`.
3. **Pre-authentication checks** (customizado no provider):
   - Extrai o IP via `RequestContextHolder`.
   - Cria `LoginAttemptKey.of(username, ip)`.
   - Se `LoginAttemptService.isBlocked(key)` for `true` → lança
     `LockedException` com mensagem "Conta temporariamente bloqueada...".
4. `BCryptPasswordEncoder` compara a senha submetida com `getPassword()`
   do `AtrilhaUserDetails` (que é o hash do banco).
5. **Sucesso** → `RoleBasedAuthenticationSuccessHandler.onAuthenticationSuccess`:
   - Resolve destino (TEEN→TRILHA, GUARDIAN com vínculo→PAINEL, sem→VINCULAR).
   - `LoginAttemptService.registerSuccess(key)` limpa contadores de
     tentativas para a chave IP+email.
   - Loga `auth.login.success destination=... ip=<hash>`.
   - `response.sendRedirect(destination.path())`.
6. **Falha** → `RateLimitedAuthenticationFailureHandler.onAuthenticationFailure`:
   - Se a exception é `LockedException` (vindo do pre-check) → redirect
     direto `/login?blocked`.
   - Senão extrai username + IP, chama
     `LoginAttemptService.registerFailure(key)`, verifica
     `isBlocked` e redireciona `/login?blocked` ou `/login?error`.

#### Classes e métodos

- **`SecurityConfig.filterChain`** — declara `formLogin` com
  `loginPage("/login")`, `loginProcessingUrl("/login")`,
  `usernameParameter("username")`, `passwordParameter("password")`,
  success handler e failure handler customizados.
- **`SecurityConfig.daoAuthenticationProvider`** — bean
  `DaoAuthenticationProvider` com `setPreAuthenticationChecks(...)`
  customizado para consultar o rate-limit.
- **`SecurityConfig.passwordEncoder`** — `BCryptPasswordEncoder(12)`.
- **`LoginAccountUserDetailsService`** — `UserDetailsService` do Spring
  Security; delega para `LoginAccountQuery`.
- **`LoginAccountQuery`** — interface SPI com `findForLogin(String) →
  Optional<LoginAccount>` e o record interno
  `LoginAccount(email, passwordHashBcrypt, role, hasGuardianLink,
  displayName)`.
- **`JpaLoginAccountQuery`** — ativo por default
  (`@ConditionalOnProperty(name="atrilha.auth.seed.enabled",
  havingValue="false", matchIfMissing=true)`). Consulta `AccountReader`,
  resolve `role` a partir de `account.type`, resolve `displayName` via
  `AccountProfileLookup.findNickname` (com fallback para parte antes do
  `@`). O Javadoc avisa que `hasGuardianLink` retorna sempre `false`
  hoje — a tabela de vínculo só chega na US-014.
- **`InMemoryLoginAccountQuery`** — alternativa só ativada com
  `atrilha.auth.seed.enabled=true`. Carrega 3 contas-semente
  (`teen`, `guardian-linked`, `guardian-unlinked`) com senhas
  hasheadas em `@PostConstruct`.
- **`AtrilhaUserDetails`** — `UserDetails` + `AuthenticatedPrincipal`,
  expõe `getRole()`, `hasGuardianLink()`, `getAccount()`.
- **`LoginAttemptService`** — serviço in-memory de rate-limit. Usa
  `ConcurrentHashMap<LoginAttemptKey, AttemptState>`. Janela de 15 min,
  bloqueio de 15 min, máximo 5 tentativas por janela (configurável via
  `LoginRateLimitProperties`).
- **`LoginAttemptKey`** — record com `emailNormalized` (trim+lowercase)
  + `ip`. Factory `of(email, ip)` aplica a normalização.
- **`LoginRateLimitProperties`** — record `@ConfigurationProperties`
  prefixo `atrilha.auth.login`. Constrói defaults se valores chegarem
  zerados.
- **`RateLimitedAuthenticationFailureHandler`** — implementa
  `AuthenticationFailureHandler`. Mapeia `LockedException` →
  `/login?blocked`. Caso contrário, registra falha e redireciona.
  Loga somente IP **hasheado** (SHA-256, primeiros 8 hex). Nunca loga
  email em claro nem senha.
- **`RoleBasedAuthenticationSuccessHandler`** — implementa
  `AuthenticationSuccessHandler`. Resolve destino por papel + vínculo,
  limpa rate-limit, redireciona.

#### Por que assim

- **Pre-authentication check** em vez de filtro próprio para rate-limit:
  reusa o pipeline padrão do Spring Security (mensagens, eventos,
  `RememberMe` etc.). O `DaoAuthenticationProvider` chama o check **antes**
  de validar a senha, então tentativas com a senha certa também ficam
  bloqueadas durante o período de penalidade — comportamento exigido pela
  política anti-brute-force.
- **`LockedException` como sinal**: o Spring Security já tem o conceito;
  reusar evita criar `AuthenticationException` própria.
- **Rate-limit in-memory**: limitação consciente (documentada no Javadoc
  do `LoginAttemptService`). Não funciona em múltiplas instâncias —
  dívida pós-MVP.
- **Logs sem PII**: requisito de privacidade do PRD §11.8. O IP é
  hasheado, o email aparece como hash decimal só em
  `LoginAccountUserDetailsService` para correlação de logs sem expor
  o valor.
- **`InMemoryLoginAccountQuery`** existe para dev local sem precisar de
  popular o banco. Em prod, o `@ConditionalOnProperty` garante que ele
  fica fora do contexto.

#### Considerações / alternativas

- **Alternativa A — usar Redis para rate-limit** (Bucket4j, Resilience4j,
  Spring `RateLimiter`): resolve o problema de single-instance. Custo:
  adicionar Redis ao infra (já não existe). Vale para fase
  pós-MVP.
- **Alternativa B — proteger por IP só (sem email)**: mais simples, mas
  permite que um atacante chute senhas de várias contas a partir do mesmo
  IP. A chave composta é mais defensiva.
- **Alternativa C — usar Spring Security `@PreAuthorize` em métodos** ao
  invés de matchers no filter chain: mais granular, menos legível. O
  modelo atual com matchers concentra a configuração de autorização num
  único arquivo, o que é útil para auditar.
- **Alternativa D — JWT em vez de sessão de cookie**: deslocaria
  complexidade para o cliente e exigiria infraestrutura de refresh
  tokens. Para um produto server-rendered com fluxo POST-redirect-GET,
  cookie de sessão é o caminho certo.
- **Alternativa E — registrar o `RoleBasedAuthenticationSuccessHandler`
  no fluxo de cadastro** (em vez do `SessionAuthenticator` próprio):
  faria sentido se o usuário precisasse passar pelo mesmo redirect — mas
  o cadastro força `/verificar-email` em vez de `/trilha`. Caminhos
  diferentes justificam handlers diferentes.

**Refactor sugerido**: extrair a regra de roteamento por papel para um
`PostLoginDestinationResolver`. Hoje a mesma lógica aparece em três
lugares: `LoginController.resolveDestination`,
`RoleBasedAuthenticationSuccessHandler.resolveForRole` e (parcialmente)
`PostLoginRedirectController.vincular`. Centralizar reduz risco de
divergência.

---

### 3.11 `POST /logout`

**Não há controller customizado.** Configurado em `SecurityConfig`:

```
logout
  .logoutUrl("/logout")
  .logoutSuccessUrl("/login?logout")
  .invalidateHttpSession(true)
  .clearAuthentication(true)
```

#### Fluxo

1. POST `/logout` chega ao `LogoutFilter`.
2. Spring valida o CSRF token.
3. Invalida a `HttpSession` e limpa o `SecurityContext`.
4. Redireciona para `/login?logout`.
5. `LoginController.renderLogin` detecta o parâmetro `logout` na URL e
   adiciona `infoState=logged-out` no model — a view mostra "Você saiu
   da sua conta.".

#### Por que assim

- POST (e não GET) protege contra logout-via-CSRF (link em página
  externa não consegue submeter POST com token válido).
- `invalidateHttpSession(true)` é a única forma confiável de impedir que
  o cookie residual permita acesso pós-logout.

#### Considerações / alternativas

- **Alternativa A — logout sem CSRF (GET)**: simples mas perigoso. Não
  recomendado.
- **Alternativa B — redirecionar para `/` em vez de `/login?logout`**:
  perde a confirmação visual ("Você saiu"). Decisão de UX foi mostrar a
  confirmação.

---

### 3.12 `GET /verificar-email`

**Arquivo:** `auth/web/EmailVerificationController.java`

**View:** `templates/verificar-email.html` ou redirect.

#### Fluxo

1. Spring Security marca a rota como `permitAll`, mas o controller exige
   sessão real internamente (chama `currentAccount()`).
2. `currentAccount()` pega o `Authentication` do `SecurityContextHolder`,
   verifica que o principal é um `AuthenticatedAccount`, e busca a
   conta via `AccountReader.findById(principal.id())`.
3. Se não há sessão → `redirect:/` (defensivo, não deveria acontecer
   porque o cadastro acabou de autenticar).
4. Se a conta já tem `emailVerifiedAt != null` → `redirect:/` (não
   precisa mostrar a tela).
5. Caso contrário, popula `model.email = current.getEmail()` e renderiza
   `verificar-email.html` (mostra o e-mail mascarado e o botão de
   reenvio).

#### Classes e métodos

- **`EmailVerificationController.renderPending(Model)`** — handler.
- **`EmailVerificationController.currentAccount()`** — helper privado.
  Extrai o principal e busca a conta. Visibilidade isolada para não vazar
  pra outros controllers (que devem usar `AccountReader` direto).
- **`AccountReader.findById(UUID)`** — interface SPI já documentada.
- **`AuthenticatedAccount`** — record `(UUID id, AccountRole role)` que
  é o principal gravado pelo `SessionAuthenticator` no cadastro.

#### Por que assim

- A rota está em `permitAll` pra evitar loop infinito (se exigisse
  autenticação, um usuário não logado seria redirecionado para `/login`,
  o que confunde). O enforcement de "tem sessão?" fica no próprio
  controller, com fallback amigável.
- Re-buscar a conta a cada GET (em vez de confiar no `emailVerifiedAt`
  guardado em sessão) garante que se outra aba do mesmo usuário fizer a
  verificação, esta aba também passa a refletir o novo estado.

#### Considerações / alternativas

- **Alternativa A — guardar `emailVerifiedAt` no principal da sessão e
  evitar o SELECT**: mais rápido (uma query a menos), mas dessincroniza
  abas. O custo de uma SELECT por GET é desprezível.
- **Alternativa B — fazer a rota `authenticated()` em vez de `permitAll`**:
  redirecionaria para `/login` quem não tem sessão. Pior UX no caso de
  cookie perdido durante o cadastro.

---

### 3.13 `POST /verificar-email/reenviar`

**Arquivo:** `auth/web/EmailVerificationController.java`

**Resposta:** redirect 302 para `/verificar-email` com flash attribute.

#### Fluxo

1. `SecurityConfig` exige `authenticated()` para esta rota (CSRF também
   é validado).
2. `EmailVerificationController.resend(RedirectAttributes)` é chamado.
3. Repete a checagem de `currentAccount()` (sessão + conta existe + não
   já verificada).
4. Chama `EmailVerificationService.resend(account)`:
   - Conta tokens criados na última 1h
     (`countByAccountIdAndCreatedAtAfter`). Se `>= 5` → lança
     `EmailResendRateLimitedException` com tempo restante até o token
     mais antigo da janela "expirar" (sair da hora).
   - Senão, verifica o último token: se foi criado há menos de 60s
     (`Duration.between(createdAt, now) < RESEND_COOLDOWN`) → lança
     a mesma exceção com `retryAfter = 60 - elapsed` (mínimo 1s).
   - Caso passe nos dois filtros: chama `issueToken(account)` (que
     invalida tokens pendentes anteriores e gera um novo UUID v4 com TTL
     24h), busca o nickname, e chama
     `EmailVerificationSender.sendVerification(email, nickname, token)`.
5. **Sucesso** → flash attribute `resendStatus=success`.
6. **`EmailResendRateLimitedException`** → flash attributes
   `resendStatus=rate_limited` + `resendRetryAfter=<segundos>`.
7. Redirect para `/verificar-email`, onde o template lê os flash
   attributes para mostrar a mensagem apropriada.

#### Classes e métodos

- **`EmailVerificationController.resend(RedirectAttributes)`** — handler.
- **`EmailVerificationService.resend(Account)`** — orquestrador. Anotado
  `@Transactional`.
- **`EmailVerificationService.issueToken(Account)`** — invalida pendentes,
  cria novo `EmailVerificationToken`, persiste, retorna UUID.
- **`EmailVerificationService.invalidatePendingTokens(UUID, Instant)`** —
  busca `findByAccountIdAndUsedAtIsNull`, seta `usedAt = now` em cada,
  faz `saveAll`.
- **`EmailVerificationToken`** — entidade JPA mapeada para
  `email_verification_token`. Colunas: `id`, `account_id`, `token`,
  `expires_at`, `used_at`, `created_at`. **Não tem `toString()` de Lombok**
  porque o `token` é segredo e não deve aparecer em logs (PRD §11.8).
- **`EmailVerificationTokenRepository`** — JpaRepository com derived
  queries:
  - `findByToken(UUID)` — leitura simples.
  - `findByTokenForUpdate(UUID)` — JPQL com
    `@Lock(LockModeType.PESSIMISTIC_WRITE)`, usado no `verify`.
  - `findByAccountIdAndUsedAtIsNull(UUID)`.
  - `countByAccountIdAndCreatedAtAfter(UUID, Instant)`.
  - `findFirstByAccountIdOrderByCreatedAtDesc(UUID)`.
- **`EmailResendRateLimitedException`** — `RuntimeException` carregando
  `retryAfterSeconds`.
- **`EmailVerificationSender`** / **`JavaMailEmailVerificationSender`** —
  interface + implementação JavaMail. A implementação constrói um
  `TemplateEngine` Thymeleaf **interno** (não usa o engine do Spring MVC)
  para renderizar `email/verify-email.html` e
  `email/verify-email-plain.txt`. O Javadoc explica por que: o auto-config
  do Spring Boot registra o `SpringTemplateEngine` com
  `@ConditionalOnMissingBean`, então registrar outro bean do mesmo tipo
  desativaria o engine das views.

#### Por que assim

- **`@Transactional` na service** garante atomicidade entre invalidar
  tokens antigos e criar o novo.
- **Token UUID v4 persistido (não JWT)**: permite revogação imediata e
  auditoria via SQL. JWT exigiria blacklist.
- **TTL 24h** vs. recuperação de senha (1h): padrão de uso real
  (verificação de e-mail é menos sensível, tolera link mais "vivo").
- **Rate-limit em SQL (`created_at`)** sobrevive a restart, multi-instance,
  e é auditável. In-memory perderia estado.
- **Limite por hora verificado **antes** do cooldown**: o limite por hora
  vaza menos informação ao chamador (não revela qual foi a hora exata da
  última tentativa).
- **Não revelar o limite de 5/h ao usuário**: decisão UX 3.3. Apenas o
  cooldown é exibido. O `retryAfterSeconds` devolvido no rate-limit é o
  tempo até "sobrar 1 vaga" (intencionalmente impreciso sobre a regra).
- **Template engine interno** evita conflito com o bean Spring; ao mesmo
  tempo é o caminho oficial recomendado pelo Thymeleaf quando você
  precisa de um pipeline separado de views.

#### Considerações / alternativas

- **Alternativa A — usar JWT como token de verificação**: stateless, mas
  sem revogação (uma vez emitido, vale até expirar). Mau trade-off para
  fluxo onde o usuário pode pedir vários reenvios.
- **Alternativa B — guardar contadores de reenvio in-memory** (em vez de
  contar `created_at`): mais rápido, mas perde estado em restart e não
  funciona em múltiplas instâncias.
- **Alternativa C — enviar e-mail síncrono no controller** (sem service):
  mistura responsabilidade web ↔ domínio. O service centraliza a regra
  e permite reaproveitar em outros fluxos (ex.: futuro endpoint admin).
- **Alternativa D — usar uma fila (RabbitMQ, SQS) para o envio**: melhor
  para volume e robustez. Custo de infra extra. Apropriado pós-MVP.

---

### 3.14 `GET /verify-email`

**Arquivo:** `auth/web/EmailVerificationController.java`

**View:** `templates/verify-email-resultado.html` (com `model.outcome`).

#### Fluxo

1. Rota pública (`permitAll`). É o link que chega no e-mail.
2. Controller pega o parâmetro `token` como string opcional.
3. `parseUuid(tokenParam)` tenta converter; em qualquer falha retorna
   `null`.
4. `EmailVerificationService.verify(uuidOrNull)`:
   - `null` → `VerificationResult.EXPIRED_OR_INVALID`.
   - Faz `tokenRepository.findByTokenForUpdate(uuid)` (SELECT ... FOR
     UPDATE — lock pessimista).
   - Não encontrado → `EXPIRED_OR_INVALID`.
   - `usedAt != null` → `ALREADY_USED`.
   - `expiresAt < now` → `EXPIRED_OR_INVALID`.
   - `account` não existe mais → `EXPIRED_OR_INVALID`.
   - Marca `token.usedAt = now` (independentemente do estado da conta,
     defesa em profundidade).
   - Se a conta já estava verificada → `ALREADY_USED` (preserva
     timestamp original).
   - Senão chama `AccountReader.markEmailVerifiedAt(accountId, OffsetDateTime.ofInstant(now, UTC))`
     e retorna `SUCCESS`.
5. Controller adiciona `model.outcome = outcome.name()` e renderiza
   `verify-email-resultado.html`.

#### Classes e métodos

- **`EmailVerificationController.verify(String, Model)`** — handler.
- **`EmailVerificationController.parseUuid(String)`** — static. Engole
  exceção e devolve `null` para evitar 400 com stacktrace para o usuário.
- **`EmailVerificationService.verify(UUID)`** — orquestra a state machine
  do token. Anotado `@Transactional`.
- **`EmailVerificationTokenRepository.findByTokenForUpdate(UUID)`** —
  query JPQL com `PESSIMISTIC_WRITE`. Serializa duas verificações
  concorrentes do mesmo token: a segunda só lê depois do commit da
  primeira (que já gravou `used_at`).
- **`AccountReader.markEmailVerifiedAt(UUID, OffsetDateTime)`** —
  marca **somente se** ainda for `NULL` (preserva timestamp).
  Implementação em `JpaAccountReader` é `@Transactional` e usa
  `findById` + `setEmailVerifiedAt` + `saveAndFlush`.
- **`VerificationResult`** — enum `SUCCESS`, `ALREADY_USED`,
  `EXPIRED_OR_INVALID`. O Javadoc explica que `EXPIRED_OR_INVALID`
  unifica "não-existe" e "expirado" propositadamente (UX spec §5.3:
  mesma tela, mesmo nível de informação para privacidade).

#### Por que assim

- **Lock pessimista**: a única forma simples de impedir double-spend do
  token sob concorrência (atacante abrindo o link em duas abas em
  paralelo, por exemplo).
- **Marcar `usedAt` antes de verificar `emailVerifiedAt`**: defesa em
  profundidade — mesmo se a marca de verificação falhar por outra razão,
  o token nunca pode ser usado outra vez.
- **`SUCCESS` vs. `ALREADY_USED`**: telas diferentes na UX. "Já usado" é
  positiva ("seu e-mail está confirmado, prossiga"), `SUCCESS` é
  celebrativa. `EXPIRED_OR_INVALID` é a tela com botão "Pedir novo link".
- **Engolir parse errors**: evitar vazar se o token era malformado vs.
  ausente vs. nunca existiu — UI mostra a mesma tela.

#### Considerações / alternativas

- **Alternativa A — lock otimista (versão de linha)**: menos contenção
  mas exige retry. O lock pessimista é mais simples para um caminho de
  baixa frequência.
- **Alternativa B — token assinado (HMAC) sem ler banco**: stateless,
  mas perde a possibilidade de invalidar tokens pendentes ao pedir
  reenvio. Optar pela tabela paga o custo de uma SELECT em troca de
  controle total.
- **Alternativa C — separar `EXPIRED` de `INVALID` na resposta**: dá mais
  diagnóstico ao usuário ("o link expirou — peça um novo" vs. "esse link
  não é válido"), mas vaza se o token existiu. UX preferiu a versão
  unificada.

---

### 3.15 `GET /trilha`

**Arquivo:** `auth/web/PostLoginRedirectController.java`

**View:** `templates/trilha/placeholder.html` (com `model.displayName`).

#### Fluxo

1. `SecurityConfig` exige `hasRole("TEEN")`.
2. Controller recebe `Authentication` injetado pelo Spring Security.
3. Se `authentication == null` (caminho defensivo, raro) → `displayName
   = "Amigo"` e renderiza placeholder.
4. Inspeciona o principal:
   - `AuthenticatedAccount` (cadastro via `SessionAuthenticator`): busca
     o perfil via `AdolescentProfileRepository.findByAccountId` e usa
     `AdolescentProfile.getNickname()`. Fallback: primeiros 8 chars do
     UUID.
   - `AuthenticatedPrincipal` (login via form, principal é
     `AtrilhaUserDetails`): usa `principal.displayName()`.
   - Outro tipo → `"Amigo"` (defensivo, não deveria acontecer).
5. Renderiza `trilha/placeholder.html` com `displayName`.

#### Classes e métodos

- **`PostLoginRedirectController.trilha(Authentication, Model)`** — handler.
- **`AdolescentProfileRepository.findByAccountId(UUID)`** — derived query.
- **`AdolescentProfile.getNickname()`** — Lombok getter.

#### Por que assim

- Existem **dois caminhos** que chegam aqui com tipos de principal
  diferentes (cadastro recém-feito vs. login). O `if/else if` é o mínimo
  necessário para suportar ambos sem mudar contrato de evento.
- O fallback "primeiros 8 chars do UUID" é uma escolha pragmática:
  apresenta algo que **não** é nulo nem vazio quando o perfil ainda não
  foi carregado.

#### Considerações / alternativas

- **Alternativa A — unificar todos os principals atrás de uma interface
  comum** (já existe — `AuthenticatedPrincipal`) e fazer cadastro também
  gerar um `AtrilhaUserDetails`: eliminaria o branch. Trade-off: o
  cadastro teria que fazer um SELECT a mais para popular `LoginAccount`
  (com `passwordHashBcrypt`, `displayName` etc.). Vale aprimorar quando
  a vinculação responsável-adolescente entrar (US-012) — o branch já fica
  mais complexo.
- **Alternativa B — carregar o perfil uma vez na sessão**: cache de
  request reduz o SELECT por hop, mas adiciona invalidation logic
  quando o nickname mudar. Não compensa hoje.
- **Alternativa C — usar `@AuthenticationPrincipal` para injetar
  diretamente o principal**: deixaria o branch mais explícito.
  Recomendado para refactor futuro.

---

### 3.16 `GET /painel` — Placeholder

**Arquivo:** `auth/web/PostLoginRedirectController.java`

**View:** `templates/painel/placeholder.html`

#### Fluxo

`SecurityConfig` exige `hasRole("GUARDIAN")`. Controller devolve o nome
de view direto — nenhuma lógica.

#### Por que assim

A tela real do painel do responsável ainda não existe (US-013/014/015).
Stub serve para o `RoleBasedAuthenticationSuccessHandler` ter um destino
válido para `GUARDIAN com vínculo`.

#### Considerações / alternativas

- **Alternativa A — retornar 503 ou redirect para uma página "em
  construção"**: mais honesto, mas exige tratamento extra no handler.
  A solução atual mantém o pipeline simples.

---

### 3.17 `GET /vincular` — Placeholder

**Arquivo:** `auth/web/PostLoginRedirectController.java`

**View:** `templates/vinculacao/inserir-codigo-placeholder.html`

#### Fluxo

1. `SecurityConfig` exige `hasRole("GUARDIAN")`.
2. `@AuthenticationPrincipal AuthenticatedPrincipal principal` recebe o
   principal.
3. Se `principal.hasGuardianLink()` for `true` → `redirect:/painel`
   (ele já tem filho vinculado; não tem motivo para estar nessa tela).
4. Senão, renderiza o placeholder de "inserir código de vinculação".

#### Classes e métodos

- **`PostLoginRedirectController.vincular(AuthenticatedPrincipal)`**.
- **`AuthenticatedPrincipal.hasGuardianLink()`** — método da interface
  comum a `AtrilhaUserDetails`.

#### Por que assim

- A injeção via `@AuthenticationPrincipal AuthenticatedPrincipal`
  (interface, não classe concreta) abstrai a origem da autenticação. O
  comentário `// TODO: mesmo problema para GUARDIAN via cadastro` indica
  que o autor sabe que o cadastro de GUARDIAN (US-003) ainda não emite
  esse tipo de principal — quando US-003 chegar, será preciso garantir
  que cadastro de responsável também gere um principal compatível.

#### Considerações / alternativas

- **Alternativa A — fazer a verificação `hasGuardianLink()` no
  `RoleBasedAuthenticationSuccessHandler` e nunca chegar aqui se tiver
  vínculo**: já é o caso para login (o handler manda para `/painel` ou
  `/vincular` conforme estado). A dupla verificação aqui defende contra
  o caso de o usuário digitar `/vincular` na URL diretamente.

---

### 3.18 `GET /media/**` — Avatar estático

**Arquivo:** `config/MediaResourceConfig.java`

**Resposta:** arquivo estático lido de `${app.media.upload-dir}`.

#### Fluxo

1. Spring MVC pega a request por matching de `ResourceHandlerRegistry`.
2. `MediaResourceConfig.addResourceHandlers` registra `/media/**` →
   `file:${app.media.upload-dir}/`.
3. O `ResourceHttpRequestHandler` serve o arquivo direto do disco.

Acesso público sem autenticação. O nome do arquivo é o `accountId` UUID,
ou seja, não enumerável.

#### Por que assim

- Avatar não é dado sensível; deixar aberto evita complexidade.
- UUID v4 funciona como token de acesso por obscuridade — não dá pra
  iterar (`/media/avatars/1.jpg`, `2.jpg`...) e encontrar avatars.
- Tornar a config trocável: quando migrar para S3/CDN, basta substituir
  esta configuração por outra.

#### Considerações / alternativas

- **Alternativa A — proteger com autenticação**: garantiria que avatars
  só sejam vistos por quem pode ver o perfil. Aumenta a complexidade.
  Decisão consciente foi não proteger no MVP.
- **Alternativa B — servir via CDN com URLs assinadas**: padrão da
  indústria para escala, mas exige integração com S3 + assinaturas.
  Será preciso quando o volume aumentar.
- **Riscos**: path traversal — coberto pelos testes
  `MediaResourcePathTraversalIT`. O Spring `ResourceHttpRequestHandler`
  já normaliza paths antes de servir, e o `accountId` é gerado pelo
  servidor (não vem do usuário).

---

### 3.19 `GET /health` — Actuator

**Não há controller customizado.** Configurado em
`application.properties`:

```
management.endpoints.web.base-path=/
management.endpoints.web.path-mapping.health=health
management.endpoint.health.enabled=true
management.endpoint.health.show-details=never
```

Resposta JSON: `{"status":"UP"}` (sem detalhes — o `show-details=never`
evita vazar informação de infraestrutura).

#### Por que assim

- Endpoint de healthcheck é canônico para deploy/orquestração
  (Kubernetes liveness/readiness probe, Docker healthcheck, load
  balancer).
- `enabled-by-default=false` + only `health` exposto restringe a
  superfície do Actuator (sem `/env`, `/heapdump`, `/loggers`).

#### Considerações / alternativas

- **Alternativa A — usar `show-details=when-authorized`** com role
  específica: dá detalhes (DB up, mail server up) para operadores.
  Trade-off: mais auditoria, mais setup. Aceitável para fase pós-MVP.

---

## 4. Arquivos transversais (fora dos endpoints)

Esta seção descreve arquivos que **não pertencem a nenhum endpoint
diretamente** mas são essenciais para que o sistema funcione. Estão
agrupados por responsabilidade.

### 4.1 Bootstrapping e configuração global

- **`AtrilhaApplication.java`** — main class. `@SpringBootApplication`
  + `@ConfigurationPropertiesScan("dev.zayt.atrilha")` (necessário porque
  alguns `@ConfigurationProperties` — como `LoginRateLimitProperties` e
  `InMemoryLoginAccountQuery` — vivem em subpacotes específicos).

- **`config/MediaResourceConfig.java`** — já documentado em
  [§3.18](#318-get-media--avatar-estático).

- **`accounts/validation/AgeEligibilityConfig.java`** — bean `Clock`
  fixado em `America/Sao_Paulo`. Mantém o cálculo de idade
  determinístico e testável (testes substituem por `Clock.fixed`).

### 4.2 Segurança transversal

- **`auth/config/SecurityConfig.java`** — `SecurityFilterChain` + matchers
  + `formLogin` + `logout` + `passwordEncoder` (BCrypt cost 12) +
  `DaoAuthenticationProvider` com pre-authentication check de rate-limit.
  Pontos sutis:
  - `enableSessionUrlRewriting(true)` (linha 81): habilita
    `session-url-rewriting`. O comentário in-line explica que isso evita
    que o `DisableEncodeUrlFilter` do Spring Security seja adicionado ao
    chain, o que neutralizaria o `ResourceUrlEncodingFilter` do Spring
    Web e impediria o fingerprint de CSS configurado em
    `application-prod.properties`. A app não usa `JSESSIONID` em URL
    (sessão 100% cookie HttpOnly), então liberar a flag é seguro.

- **`auth/config/LoginRateLimitProperties.java`** — record
  `@ConfigurationProperties("atrilha.auth.login")` com `maxAttempts`,
  `attemptWindow`, `blockDuration` e defaults conservadores (5/15min/15min).

- **`auth/login/LoginAttemptService.java`** — rate-limit in-memory
  (`ConcurrentHashMap<LoginAttemptKey, AttemptState>`). API:
  `isBlocked`, `registerFailure`, `registerSuccess`. Janela e bloqueio
  configuráveis. Dívida pós-MVP: não funciona em múltiplas instâncias.

- **`auth/login/LoginAttemptKey.java`** — record imutável (email
  normalizado + IP).

- **`auth/login/RateLimitedAuthenticationFailureHandler.java`** —
  failure handler que registra falha, decide redirect (`?blocked` ou
  `?error`) e nunca loga email/senha em claro. IP é hasheado SHA-256
  (primeiros 8 hex) para logs auditáveis sem PII.

- **`auth/login/RoleBasedAuthenticationSuccessHandler.java`** —
  success handler que resolve `PostLoginDestination` por papel +
  vínculo, limpa rate-limit, redireciona.

- **`auth/login/PostLoginDestination.java`** — enum com paths.

- **`auth/login/LoginAccountQuery.java`** — interface SPI + record
  `LoginAccount(email, passwordHashBcrypt, role, hasGuardianLink,
  displayName)`.

- **`auth/login/LoginAccountUserDetailsService.java`** —
  `UserDetailsService` do Spring Security. Apenas adapta o
  `LoginAccountQuery`.

- **`auth/login/JpaLoginAccountQuery.java`** — bean default,
  `@ConditionalOnProperty(name="atrilha.auth.seed.enabled",
  havingValue="false", matchIfMissing=true)`. Consulta
  `AccountReader.findByEmailIgnoreCase`, deriva role, resolve nickname.
  Nota: `hasGuardianLink = false` enquanto a US-014 não chega (tabela
  de vínculo ainda não existe).

- **`auth/login/InMemoryLoginAccountQuery.java`** — bean alternativo
  ativado por `atrilha.auth.seed.enabled=true`. Carrega 3 contas seed
  via `@ConfigurationProperties("atrilha.auth.seed")`. Útil para dev
  local sem banco populado e testes que precisam de seeds determinísticas.

- **`auth/login/AtrilhaUserDetails.java`** — `UserDetails`
  + `AuthenticatedPrincipal`. Expõe `getRole()`, `hasGuardianLink()`,
  `getAccount()`.

- **`auth/domain/AuthenticatedPrincipal.java`** — interface comum aos
  principals (hoje só `AtrilhaUserDetails`). Útil para o Spring
  resolver via `@AuthenticationPrincipal AuthenticatedPrincipal
  principal` sem se importar com o tipo concreto.

- **`auth/domain/AuthenticatedAccount.java`** — record `(UUID id,
  AccountRole role)`. Usado **somente** pelo fluxo de cadastro
  (não implementa `AuthenticatedPrincipal` — ainda).

- **`auth/session/SessionAuthenticator.java`** — encapsula
  `SecurityContextHolder` + `HttpSessionSecurityContextRepository.saveContext`
  para que o cadastro autentique a sessão imediatamente após o INSERT.

### 4.3 Verificação de e-mail (US-006)

- **`auth/verification/EmailVerificationService.java`** — orquestrador.
  Já detalhado em [§3.13](#313-post-verificar-emailreenviar) e
  [§3.14](#314-get-verify-email).

- **`auth/verification/EmailVerificationToken.java`** — entidade JPA.

- **`auth/verification/EmailVerificationTokenRepository.java`** — Spring
  Data JPA repository com `findByTokenForUpdate` (lock pessimista).

- **`auth/verification/AccountRegisteredEventListener.java`** —
  `@TransactionalEventListener(AFTER_COMMIT)`,
  `@Transactional(REQUIRES_NEW)`. Dispara o envio do e-mail de
  verificação após o cadastro persistir.

- **`auth/verification/RequiresVerifiedEmail.java`** — anotação para
  marcar endpoints que **exigem** e-mail verificado.

- **`auth/verification/RequiresVerifiedEmailInterceptor.java`** —
  `HandlerInterceptor` que faz o enforcement: se a conta tem
  `emailVerifiedAt == null` e o método está anotado, redireciona GETs
  para `/verificar-email` ou devolve 403 em POSTs.

- **`auth/web/AuthWebMvcConfig.java`** — registra o interceptor acima
  no pipeline MVC.

- **`auth/web/EmailVerificationBannerAdvice.java`** — `@ControllerAdvice`
  que injeta os atributos `unverifiedEmail` e `showEmailVerificationBanner`
  no modelo de **toda** view Thymeleaf. O fragment
  `layout/fragments/email-verification-banner.html` consome esses
  atributos para mostrar o banner amarelo "Confirma teu e-mail" em
  páginas pós-login. O `showEmailVerificationBanner` é mais restrito —
  não aparece dentro do próprio fluxo de verificação
  (`/verificar-email`, `/verify-email`).

- **`auth/exception/EmailResendRateLimitedException.java`** —
  `RuntimeException` com `retryAfterSeconds`.

- **`auth/domain/VerificationResult.java`** — enum `SUCCESS`,
  `ALREADY_USED`, `EXPIRED_OR_INVALID`.

> Observação importante: a anotação `@RequiresVerifiedEmail` **não está
> aplicada em nenhum endpoint real** hoje (apenas plantamos o ponto de
> extensão). Quando rotas como `/trilha/**` precisarem do gate, basta
> anotar a classe ou métodos. O comentário no Javadoc da anotação deixa
> isso explícito.

### 4.4 Cadastro / Conta / Perfil

- **`accounts/service/RegisterAdolescentService.java`** — orquestrador
  do cadastro. Já detalhado em [§3.8](#38-post-cadastroadolescente-submissão-do-cadastro).

- **`accounts/domain/Account.java`** — entidade JPA (`accounts`).
  Coluna `type` discrimina TEEN vs. GUARDIAN. `password_hash` pode ser
  NULL apenas se `deleted_at` estiver preenchido (CHECK no banco).

- **`accounts/domain/AdolescentProfile.java`** — entidade JPA
  (`adolescent_profiles`), 1:1 com `Account` via `@MapsId`.

- **`accounts/domain/AccountRole.java`** — enum `TEEN`/`GUARDIAN`.

- **`accounts/domain/AccountRegisteredEvent.java`** — record `(UUID
  accountId)`. Evento publicado pelo `RegisterAdolescentService` e
  consumido pelo `AccountRegisteredEventListener`.

- **`accounts/domain/RegisterAdolescentRequest.java`** — record DTO
  imutável com validações Jakarta. Consumido pelo service.

- **`accounts/web/RegisterAdolescentForm.java`** — bean mutável que
  o data-binder do Spring usa para repopular o form em caso de erro.

- **`accounts/repository/AccountRepository.java`** — JpaRepository
  (package-private). Derived queries soft-delete-aware e
  case-insensitive.

- **`accounts/repository/AccountReader.java`** + **`JpaAccountReader.java`**
  — interface SPI exposta + implementação. Restringe as operações que
  cruzam a fronteira do módulo `accounts`.

- **`accounts/repository/AccountProfileLookup.java`** +
  **`JpaAccountProfileLookup.java`** — interface SPI para buscar
  apelido sem expor a entidade.

- **`accounts/repository/AdolescentProfileRepository.java`** —
  JpaRepository com `findByAccountId`.

- **`accounts/avatar/AvatarStorage.java`** + **`FilesystemAvatarStorage.java`**
  — interface + implementação. Validação por MIME (JPG/PNG/WEBP), limite
  5 MB, escrita em `${app.media.upload-dir}/avatars/`.

- **`accounts/avatar/AvatarTooLargeException.java`** + **`AvatarUnsupportedTypeException.java`**
  — exceções de domínio do storage.

### 4.5 Validação por idade (US-005)

- **`accounts/validation/AgeEligibilityChecker.java`** — service puro.
  Recebe `LocalDate birthDate` + `AccountRole role` → `Optional<AgeEligibilityViolation>`.

- **`accounts/validation/AgeEligibilityViolation.java`** — enum com
  três violações (`TEEN_TOO_YOUNG`, `TEEN_TOO_OLD`,
  `GUARDIAN_TOO_YOUNG`), cada uma com sua chave de
  `messages.properties`.

- **`accounts/validation/EligibleAge.java`** — anotação Jakarta
  Validation com parâmetro `AccountRole role()`.

- **`accounts/validation/EligibleAgeValidator.java`** —
  `ConstraintValidator` que delega ao `AgeEligibilityChecker` e
  resolve a chave de mensagem.

- **`accounts/validation/AgeEligibilityConfig.java`** — bean `Clock`
  fixado em `America/Sao_Paulo` (já mencionado em §4.1).

### 4.6 Notificações

- **`notifications/EmailVerificationSender.java`** — interface pública
  consumida pelo módulo `auth`.

- **`notifications/JavaMailEmailVerificationSender.java`** —
  implementação. Renderiza dois templates Thymeleaf
  (`email/verify-email.html` e `email/verify-email-plain.txt`) com um
  `TemplateEngine` **interno** para não conflitar com o engine do
  Spring MVC. Logs nunca contêm token nem corpo do e-mail.

### 4.7 Shared

- **`shared/HtmlSanitizer.java`** — wrapper sobre Jsoup com
  `Safelist.none()`. Usado no nickname antes de persistir; pode ser
  reutilizado em qualquer campo de texto livre.

### 4.8 Stubs e placeholders (a remover quando as USs entregarem)

Os arquivos abaixo existem deliberadamente como **dead code temporário**
para manter o fluxo navegacional consistente enquanto features não
foram implementadas:

- **`web/GuardianRegistrationStubController.java`** — stub do cadastro
  de responsável (US-003). Substituído pelo controller real em Sprint 04.
- **Métodos `painel()` e `vincular()` em `PostLoginRedirectController`**
  — apontam para placeholders. Substituídos por controllers reais quando
  a US-012/013/014/015 entregar.
- **`AdolescentRegistrationController.renderEscolherMetodo()`** — rota
  com botão Google "inerte" enquanto a auth OAuth não voltar (REF-003).

### 4.9 Pacotes reservados (vazios hoje)

`admin`, `content` e `progress` contêm apenas `package-info.java`
fixando a fronteira do módulo. São pontos de extensão para sprints
futuros conforme `doc/PRD.md` §9.3.

### 4.10 Recursos e infraestrutura

Embora fora do escopo de código Java, vale citar:

- **`src/main/resources/db/migration/V1__baseline.sql`** ... **`V4__remove_google_oauth.sql`**
  — migrações Flyway. V2 cria `accounts` e `adolescent_profiles`. V3
  cria `email_verification_token`. V4 remove resíduos do OAuth Google.
- **`src/main/resources/messages.properties`** — i18n. Chaves
  `validation.age.*` consumidas pelo `EligibleAgeValidator`.
- **`src/main/resources/application*.properties`** —
  perfis dev/prod/test. O `application.properties` raiz define defaults
  e perfil ativo `dev`. Em prod, sobrescreve cookie `Secure=true`,
  fingerprint de CSS etc.
- **`src/main/resources/templates/**`** — views Thymeleaf. O `layout/base.html`
  é a master page; `layout/fragments/*` são reaproveitados (header,
  footer, banner de verificação). `error/{403,404,5xx}.html` são as
  páginas de erro padrão. Cada controller que devolve uma string `"foo/bar"`
  está apontando para `templates/foo/bar.html`.

---

## Pontos a discutir / dívidas conhecidas

Resumo das pendências que apareceram durante o levantamento:

1. **Botão Google "inerte"** em `/cadastro/adolescente/escolher-metodo`.
   Avaliar mostrar `disabled` ou tooltip "em breve".
2. **Duplicação da regra de roteamento pós-login** em três lugares
   (`LoginController`, `RoleBasedAuthenticationSuccessHandler`,
   `PostLoginRedirectController.vincular`). Extrair para um único
   `PostLoginDestinationResolver`.
3. **`/comecar` vs. `/cadastro`** — duas portas com mesma intenção;
   unificar quando a `home.html` for atualizada.
4. **Cadastro e login emitem principals de tipos diferentes**
   (`AuthenticatedAccount` vs. `AtrilhaUserDetails`). Unificar para
   simplificar o branch em `PostLoginRedirectController.trilha`.
5. **Rate-limit in-memory** (`LoginAttemptService`) — não escala para
   múltiplas instâncias. Pós-MVP migrar para Redis ou similar.
6. **`FilesystemAvatarStorage`** — não escala horizontalmente. Migração
   para S3/CDN já está prevista (interface `AvatarStorage` permite
   troca).
7. **`hasGuardianLink` hardcoded `false`** em `JpaLoginAccountQuery` —
   atualizar quando a tabela de vínculo chegar na US-014.
8. **`@RequiresVerifiedEmail` plantado mas não aplicado** — quando
   `/trilha/**` precisar do gate de verificação, basta anotar.
9. **`/painel` e `/vincular`** ainda são placeholders puros — Sprint 04+
   substitui pelos controllers reais.

---

> Fim do manual. À medida que novas USs entregarem (Sprint 04 — US-003,
> US-012 etc.), este documento deve ser atualizado seguindo o mesmo
> molde: para cada novo endpoint, descrever fluxo, classes/métodos, e
> alternativas técnicas consideradas.
