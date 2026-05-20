# US-002 — Cadastro de adolescente via Google · UX Spec

**Código:** US-002
**GitHub Issue:** #37
**Sprint:** Sprint 3 — Auth essencial (E1 parte 1)
**Status:** Proposto
**Depende de:** US-001 (#40, entregue) · US-005 (#36, entregue) · US-006 (#39, entregue) · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/06-acessibilidade.md`
**Referências PRD:** §6.2 (segmentos), §10.1 (modelo), §13 (US-002), §17 (DoD)
**Escopo deste doc:** as **quatro telas novas** introduzidas pela US-002 e os ajustes pontuais na tela existente `/cadastro/adolescente` (US-001). Não redesenha a US-001; reusa todos os tokens e componentes já catalogados. Microcopy em pt-BR (P5 — sem moralismo, P11 — vocabulário ASD jovem). Nenhum elemento depende só de cor.

---

## 1. Princípios da US-002

1. **Google é atalho, não substituto.** Júlia chega na escolha de método (Seção 3) e tem duas alternativas no mesmo nível visual: **Google** (botão branco oficial, ícone "G" colorido) e **E-mail** (botão ghost, leva ao form da US-001). Nada force-pusha Google.
2. **E-mail Google já vem verificado.** Diferente da US-001, **não** se mostra o banner "confirme teu e-mail" depois do cadastro Google. `email_verified_at` é gravado na criação (RF-E1-07).
3. **A faixa etária ainda é decidida pelo usuário.** Google não devolve idade. Júlia digita a data de nascimento na tela de complementação (Seção 4). Bloqueio reusa `adolescente_bloqueado.html` da US-005.
4. **Pré-preenchimento educado.** Nome (`given_name`) vira sugestão de apelido (editável); foto Google (`picture`) vira opção visível, mas **opcional** — Júlia pode tirar e ficar com a inicial do apelido.
5. **Falha do Google é falha de fluxo, não de Júlia.** Cancelar consentimento, erro OAuth, e-mail Google não verificado: todas voltam pra `/cadastro/adolescente` com toast de erro neutro (sem culpar Júlia).
6. **Branding Google respeitado.** Botão segue branding oficial (`https://developers.google.com/identity/branding-guidelines`): superfície branca, borda 1px `slate-200`, logo "G" colorido inalterado, texto "Continuar com Google" em Roboto/sistema. **Sem coral, sem variantes coloridas, sem alterar o logo.**

---

## 2. Tela 1 — Escolher papel (`/cadastro`)

**Rota:** `GET /cadastro` → view `cadastro/escolher-papel`
**Substitui:** a função atual da rota `/comecar` (que continua existindo como entrada legada). `/comecar` segue ativo e o link do `home.html` continua apontando para ele; a nova `/cadastro` é o canônico do épico E1 daqui pra frente.

### 2.1 Wireframe (mobile 320px, mesma estrutura em desktop)

```
┌────────────────────────────────────┐
│ [header com logo atrilha]          │
├────────────────────────────────────┤
│  overline · Começar                │
│  H1 — Qual caminho começa pra você?│
│  p   — atrilha é uma trilha de…    │
│                                    │
│  ┌──────────────────────────────┐ │
│  │ H2 · Sou adolescente         │ │
│  │ p  · 13 a 17 anos. Você faz… │ │
│  │                          →   │ │
│  └──────────────────────────────┘ │
│                                    │
│  ┌──────────────────────────────┐ │
│  │ H2 · Sou responsável         │ │
│  │ p  · Adulto que vai vincular │ │
│  │                          →   │ │
│  └──────────────────────────────┘ │
└────────────────────────────────────┘
```

### 2.2 Componentes / classes

- `<a class="card card--interactive">` × 2 — mesmo padrão da `comecar.html` existente. Reuso 1:1.
- Cards são `<a>` (não `<button>`) — navegação simples por GET.
- Touch target ≥ 44×44px garantido pelo padding interno do `card--interactive`.

### 2.3 Microcopy (pt-BR)

| Slot | Texto |
|---|---|
| Overline | Começar |
| H1 | Qual caminho começa pra você? |
| Lead | atrilha é uma trilha diária de 10 minutos pela Lição da Escola Sabatina Juvenil. Antes da gente conhecer você melhor, escolhe o caminho. |
| Card 1 título | Sou adolescente |
| Card 1 descrição | 13 a 17 anos. Você faz a trilha; um responsável seu autoriza a vinculação. |
| Card 2 título | Sou responsável |
| Card 2 descrição | Adulto que vai vincular uma adolescente sob sua responsabilidade. |

### 2.4 Comportamento

- Card "Sou adolescente" → `GET /cadastro/adolescente/escolher-metodo` (Tela 2).
- Card "Sou responsável" → `GET /cadastro/responsavel` (rota já existente, stub atual `responsavel_em_breve.html`).
- Sem JS. Sem `aria-current` (não é navegação). Foco visível em cada card.

---

## 3. Tela 2 — Escolher método (`/cadastro/adolescente/escolher-metodo`)

**Rota:** `GET /cadastro/adolescente/escolher-metodo` → view `cadastro/adolescente_escolher_metodo`
**Por quê não em `/cadastro/adolescente`?** Porque essa rota já existe (US-001) e renderiza o form de e-mail/senha. A US-002 introduz **uma tela nova antes** que oferece os dois caminhos. A US-001 form continua acessível, agora linkada a partir daqui.

### 3.1 Wireframe

```
┌────────────────────────────────────┐
│ [header]                           │
├────────────────────────────────────┤
│  overline · Cadastro · Adolescente │
│  H1 — Como você quer entrar?       │
│  p  — Você pode usar tua conta…    │
│                                    │
│  ┌──────────────────────────────┐ │
│  │ [G colorido]  Continuar com   │ │
│  │               Google          │ │
│  └──────────────────────────────┘ │
│                                    │
│  ─────────── ou ─────────────────  │
│                                    │
│  ┌──────────────────────────────┐ │
│  │ Cadastrar com e-mail e senha  │ │
│  └──────────────────────────────┘ │
│                                    │
│  link · Voltar pro começo          │
└────────────────────────────────────┘
```

### 3.2 Componentes

- **Botão Google** (`a` que faz GET para iniciar OAuth):
  - Marcação:
    ```html
    <a th:href="@{/oauth2/authorization/google}"
       class="btn-google"
       data-test="cta-google">
      <svg class="btn-google__logo" aria-hidden="true" focusable="false">…</svg>
      <span>Continuar com Google</span>
    </a>
    ```
  - **Estilo (Google branding obrigatório):** `background: #FFFFFF; color: #1F1F1F; border: 1px solid var(--color-slate-300); border-radius: var(--radius-md); min-height: var(--space-11); padding: 0 var(--space-4); display:inline-flex; align-items:center; gap: var(--space-3);`
  - Hover: `background: #F8F9FA; border-color: var(--color-slate-400);`
  - Focus: `outline: 2px solid var(--color-focus-ring); outline-offset: 2px;`
  - Disabled (durante navegação OAuth): opacidade 0.6, `aria-disabled="true"`.
- **Divisor "ou":** linha 1px `slate-200` à esquerda e direita do texto, texto em `slate-500 text-sm`.
- **Botão e-mail:** reusa `btn btn-secondary btn-lg` (já no design system) apontando para `/cadastro/adolescente` (form da US-001, intacto).
- **Link "Voltar pro começo":** texto-link, aponta para `/cadastro`.

### 3.3 Logo Google (SVG inline)

Salvar em `src/main/resources/static/img/google-g.svg` como SVG **oficial** (4 cores: `#4285F4` azul, `#34A853` verde, `#FBBC05` amarelo, `#EA4335` vermelho). Tamanho intrínseco 18×18px; renderizar a 20×20px no botão. **Vetado** monocromar, recolorir ou substituir por ícone genérico — Google guidelines exigem o logo inalterado em superfícies brancas.

Como o SVG é versionável, **inline no template** via `<svg>...</svg>` (não `<img>`) — evita request adicional e permite controle de `aria-hidden`/`focusable`.

### 3.4 Microcopy

| Slot | Texto |
|---|---|
| Overline | Cadastro · Adolescente |
| H1 | Como você quer entrar? |
| Lead | Você pode usar tua conta Google (mais rápido) ou criar uma com e-mail e senha. |
| Botão Google (label) | Continuar com Google |
| Divisor | ou |
| Botão e-mail | Cadastrar com e-mail e senha |
| Link voltar | Voltar pro começo |

### 3.5 Erros recebidos via querystring

Tela 2 também é o destino do `OAuthFailureHandler`. Renderizar **toast/alerta no topo** antes do H1 quando vier `?error=`:

| `error=` | Mensagem (pt-BR) | Severidade |
|---|---|---|
| `cancelled` | Tudo bem, você não precisou autorizar. Quando quiser, é só tentar de novo. | info |
| `email_unverified` | A conta Google que você usou ainda não tem e-mail confirmado. Confirma lá no Google e volta aqui. | warning |
| `account_exists` | Essa conta Google já tem cadastro no atrilha. Quer entrar? | info, com link para `/login` (placeholder até US-007 entregar) |
| `oauth` (genérico) | Não consegui falar com o Google agora. Tenta de novo em alguns segundos. | error |

Estrutura: `<div role="status">` para `cancelled`, `<div role="alert">` para os demais.

---

## 4. Tela 3 — Complementar perfil (`/cadastro/adolescente/complementar`)

**Rota:** `GET/POST /cadastro/adolescente/complementar` → view `cadastro/adolescente_complementar`
**Acesso:** só renderiza se houver `pendingGoogleSignup` na sessão. Senão → 302 para `/cadastro/adolescente/escolher-metodo`.

### 4.1 Wireframe

```
┌────────────────────────────────────┐
│ [header]                           │
├────────────────────────────────────┤
│  overline · Cadastro · Adolescente │
│  H1 — Quase lá, {given_name}       │
│  p  — Falta só apelido, data e    │
│      (se quiser) escolher a foto   │
│                                    │
│  ┌───────────────────────────────┐│
│  │ [foto Google ▓▓] ola@…        ││  ← cartão "conta Google"
│  └───────────────────────────────┘│
│                                    │
│  label · Como a gente te chama     │
│  [_____________________________]   │
│  helper · De 3 a 20 caracteres.    │
│                                    │
│  label · Data de nascimento        │
│  [____/____/____]                  │
│                                    │
│  label · Foto (opcional)           │
│  ( ) Usar foto do Google           │
│  ( ) Enviar outra foto             │
│  ( ) Ficar sem foto                │
│                                    │
│  [Concluir cadastro] (primary lg)  │
│  [Cancelar] (ghost)                │
└────────────────────────────────────┘
```

### 4.2 Componentes

- **Cartão "Conta Google" (read-only):**
  - Avatar circular 48×48 com `pending.picture()`; fallback inicial do `given_name` se faltar.
  - Texto: `pending.email()` em `text-sm slate-700`.
  - Borda `1px slate-200`, `rounded-md`, padding `space-3`.
- **Input apelido:** reusa o input-group da US-001 (`input-field`, `input-label`, `input-helper`, `input-error`). `th:field="*{nickname}"`, `value` pré-preenchido com `pending.given_name()` truncado para 20 chars.
- **Input data de nascimento:** `type="date"`, `th:field="*{birthDate}"`, mesmo padrão da US-001.
- **Escolha de foto** (radios verticais):
  - 3 opções: "Usar foto do Google" (default se `pending.picture()` existe), "Enviar outra foto", "Ficar sem foto".
  - Quando radio "Enviar outra foto" estiver selecionado, mostrar input file (`accept="image/jpeg,image/png,image/webp"`, max 5MB) abaixo dele via Alpine `x-show` (sem JS pesado).
  - Quando radio "Usar foto do Google" estiver selecionado, o form envia `photoSource=GOOGLE` e o service grava `pending.picture()` em `avatar_url`.
- **Botões:** `Concluir cadastro` (`btn btn-primary btn-lg`, `type="submit"`); `Cancelar` (`btn btn-ghost btn-md`, link para `/cadastro/adolescente/escolher-metodo` — limpa `pendingGoogleSignup` da sessão via `?cancel=1` no GET handler).

### 4.3 Microcopy

| Slot | Texto |
|---|---|
| Overline | Cadastro · Adolescente |
| H1 | Quase lá, {given_name} |
| Lead | Falta só apelido, data de nascimento e, se quiser, escolher a foto. |
| Cartão Google label | Sua conta Google |
| Apelido label | Como a gente te chama |
| Apelido helper | De 3 a 20 caracteres. Pode ser teu nome, apelido, qualquer coisa. |
| Data label | Data de nascimento |
| Foto label | Foto (opcional) |
| Radio 1 | Usar a foto da minha conta Google |
| Radio 2 | Enviar outra foto |
| Radio 3 | Ficar sem foto (vira a inicial do apelido) |
| Botão primário | Concluir cadastro |
| Botão secundário | Cancelar |

### 4.4 Validação visual

Mesmo padrão da US-001:

- `aria-invalid="true"` no input com erro.
- `aria-describedby` aponta para `<span class="input-error" role="alert">`.
- Mensagem inline em pt-BR; sem stack trace, sem código.

### 4.5 Bloqueio por idade (reuso US-005)

Se `birthDate` falhar no `@EligibleAge(role=TEEN)`, o controller redireciona para `cadastro/adolescente_bloqueado` (template já existe), passando `variant="under-13"` ou `variant="over-17"`. **Não duplicar** template. **Não revelar** as faixas numéricas (CA-4 da US-005). Antes do redirect, limpar `pendingGoogleSignup` da sessão.

### 4.6 Conta Google já cadastrada

Se na chegada ao `OAuthSuccessHandler` o e-mail já existir em `accounts`, **não** entrar nesta tela: redirecionar para `/cadastro/adolescente/escolher-metodo?error=account_exists` (toast Seção 3.5).

---

## 5. Tela 4 — Concluído (`/cadastro/concluido`)

**Rota:** `GET /cadastro/concluido` → view `cadastro/concluido`
**Status:** placeholder. CA-5 da US-002 explicita que o destino definitivo é a US-012 (geração do código de vinculação, sprint 5). Esta tela é a ponte.

### 5.1 Wireframe

```
┌────────────────────────────────────┐
│ [header]                           │
├────────────────────────────────────┤
│  ✓ (ícone verde lime)              │
│  H1 — Conta criada                 │
│  p — Pronto, {apelido}. Daqui pra  │
│      frente seguimos juntas.       │
│                                    │
│  card · próximo passo (placeholder)│
│    H3 — Em breve                   │
│    p  — A próxima etapa (vincular  │
│         tua responsável) entra no  │
│         próximo passo do atrilha.  │
│                                    │
│  [Ir para o início] (primary md)   │
└────────────────────────────────────┘
```

### 5.2 Componentes

- Ícone check em `secondary-500` (lime), 48×48px, SVG inline.
- `card card--flat` mesmo padrão do `responsavel_em_breve.html`.
- Link primário aponta para `/` (placeholder até US-012 fechar o destino real).

### 5.3 Microcopy

| Slot | Texto |
|---|---|
| H1 | Conta criada |
| Lead | Pronto, {apelido}. Daqui pra frente seguimos juntas. |
| Card título | Em breve |
| Card lead | A próxima etapa — vincular tua responsável — entra no próximo passo do atrilha. |
| Botão | Ir para o início |

### 5.4 Sessão / autenticação

Júlia chega autenticada (Spring Security `SecurityContext` setado via `SessionAuthenticator.authenticate(...)` no sucesso). O header mostra o estado logado (futuro — US-007 entrega o header autenticado; por ora não há mudança visual no header).

---

## 6. Reusos e proibições

### 6.1 Reusos obrigatórios

| Item | Origem | Não recriar |
|---|---|---|
| `layout/base.html` | repo | Toda tela nova herda. |
| `cadastro/adolescente_bloqueado.html` | US-001/US-005 | Mesma tela para bloqueio por idade no fluxo Google. |
| Tokens de cor, espaço, raio, sombra | `doc/UX/01-design-tokens.md` | Nenhum hex novo. |
| Padrão input-group, input-error, input-helper | `doc/UX/02-componentes-base.md` §2 | Sem variantes inéditas. |
| Botão `btn btn-primary btn-lg` | `doc/UX/02-componentes-base.md` §1 | Não criar variantes Google-coloridas. |
| Foco visível, touch target 44×44 | `doc/UX/06-acessibilidade.md` §2 e §3 | Sem exceções. |

### 6.2 Proibições

- **Não** usar coral no botão Google. Branding Google é branco com logo colorido.
- **Não** mostrar o banner "confirme teu e-mail" depois de cadastro Google — `email_verified_at` já vem preenchido, banner condicional do `EmailVerificationBannerAdvice` já cobre o caso (`emailVerifiedAt != null` → não renderiza).
- **Não** disparar `AccountRegisteredEvent` no fluxo Google — esse evento dispara a US-006 (verificação por link); para Google é redundante e geraria e-mail desnecessário.
- **Não** revelar texto técnico em microcopy (sem "OAuth", sem "401", sem "claim", sem "JWT").
- **Não** alterar `/cadastro/adolescente` (rota da US-001) além de **um detalhe**: adicionar link discreto "Já tem Google? Use sua conta Google" abaixo do form, apontando para `/cadastro/adolescente/escolher-metodo`. Esse link é **opcional** — se complicar o layout, deixar fora; a tela `escolher-metodo` é a porta canônica daqui pra frente.

---

## 7. Estados, validações e bordas

| Cenário | Comportamento esperado | Tela |
|---|---|---|
| Júlia clica Google → cancela na tela do Google | `OAuthFailureHandler` (access_denied) → redirect `escolher-metodo?error=cancelled` | Tela 2 + toast info |
| E-mail Google não verificado (`email_verified=false`) | `GoogleOAuth2UserService` lança OAuth2AuthException com código `email_unverified` → handler redireciona com `?error=email_unverified` | Tela 2 + toast warning |
| Token Google retornou, e-mail já existe em `accounts` | Sucesso OAuth chega, mas service detecta conflito → redirect `?error=account_exists` | Tela 2 + toast com link login |
| Sessão expira entre OAuth callback e POST de complementação | GET `/complementar` sem `pendingGoogleSignup` → redirect `escolher-metodo` (sem mensagem; cenário raro) | Tela 2 |
| Júlia preenche data inválida (futuro, malformada) | Erro inline `aria-invalid`, mantém valores preenchidos | Tela 3 |
| Júlia preenche idade < 13 ou ≥ 18 | Redirect para `adolescente_bloqueado.html` (template US-001/US-005) | Tela bloqueio |
| Upload de foto > 5MB ou tipo não suportado | `AvatarTooLargeException`/`AvatarUnsupportedTypeException` → erro inline no input file | Tela 3 |
| Sucesso completo | `SessionAuthenticator.authenticate(...)`, limpa `pendingGoogleSignup`, 302 → `/cadastro/concluido` | Tela 4 |

---

## 8. Acessibilidade (resumo — checklist completa em `doc/UX/06-acessibilidade.md`)

- Todo `<input>` tem `<label>` associado e `aria-describedby` quando há helper/error.
- Foco visível em todos os botões e links (Tab da URL bar até "Concluir cadastro" deve mostrar anel a cada parada).
- Botão Google: `aria-label="Continuar com Google"` redundante se o `<span>` já contém o texto — não duplicar. Logo SVG `aria-hidden="true" focusable="false"`.
- Toast/alert no topo da Tela 2: `role="alert"` para erro, `role="status"` para info. Foco programaticamente movido para o alert ao chegar com `?error=`.
- Modal/Sheet: **nenhum**. Todo fluxo é navegação por página (Spring MVC tradicional, sem HTMX nesta US).
- `Esc`: nenhuma tela tem modal; comportamento default do browser.

---

## 9. Eventos de produto (referência)

Pós-sucesso, o service emite log estruturado (PRD §13.1 — instrumentação real fica para US-069, sprint 17):

```
log.info("account_created type=ADOLESCENT oauth_provider=google age_bracket=13_17 account_id={}", accountId);
```

Sem PII (e-mail/foto fora do log).

---

## 10. Pendências de design

Nenhuma bloqueante. Pontos abertos para futuras Issues:

1. **Header autenticado** — visual do header com Júlia logada (avatar + nome) entra com US-007 (login por sessão). Esta US assume header atual (logado/deslogado idênticos).
2. **Tela de login Google** — login (não cadastro) via Google é US-008/US-009 (futuras). Esta US cobre apenas cadastro.
3. **Vinculação responsável** — destino real do CA-5 ("próximo passo") é US-012 (sprint 5). Tela 4 é placeholder e será substituída.
