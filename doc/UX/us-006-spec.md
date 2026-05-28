# US-006 — Confirmar e-mail (OTP de 6 dígitos) · UX Spec

**Código:** US-006 (refinamento — abordagem OTP)
**GitHub Issue:** —
**Status:** Proposto (aguardando decisão de produto sobre OTP puro vs OTP + magic link convivendo)
**Depende de:** US-001 (cadastro adolescente) · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/06-acessibilidade.md`
**Referências PRD:** §13 (US-006) · §17 (DoD)
**Protótipo:** [`doc/UX/prototypes/verificar-email-otp.html`](prototypes/verificar-email-otp.html)
**Análise de abordagem:** [`us-006-comparativo.md`](us-006-comparativo.md) — discute OTP vs magic link e recomenda OTP
**Escopo deste doc:** **layout** da tela única `/verificar-email` com OTP de 6 dígitos. Cobre wireframe, tokens visuais, microcopy literal, estados, comportamento de entrada e acessibilidade. **Não cobre** controller, persistência de token, lockout no servidor ou template de e-mail — isso fica no spec de implementação. Microcopy em pt-BR (P5 — sem moralismo, P11 — vocabulário ASD jovem). Nenhum elemento depende só de cor.

---

## 1. Princípios

1. **Mobile-first sem trocar de contexto.** Júlia recebe um código de 6 dígitos no e-mail e digita aqui mesmo, sem sair do app. Em iOS/Android modernos o teclado nem precisa abrir o app de e-mail — o `autocomplete="one-time-code"` faz o teclado sugerir o código que chegou na notificação.
2. **Área autenticada, mas em pausa.** Júlia já tem conta (signup OK, `email_verified_at IS NULL`). Header **brand-only**, sem ícone de voltar. Saída só pelos CTAs (`Confirmar`, `Reenviar código`, `Continuar mesmo assim`).
3. **E-mail é credencial — não se troca.** O e-mail é credencial primária de login e **não pode ser alterado** após o cadastro. Nada nesta tela oferece "alterar e-mail", "atualizar no perfil" ou qualquer caminho de correção.
4. **6 caixas separadas, não 1 campo único.** Padrão visual reconhecido por adolescentes (Instagram, WhatsApp, banco). Cada caixa = 1 dígito. Auto-advance ao digitar, backspace volta, paste preenche tudo, auto-submit ao completar.
5. **Erro é convite a re-tentar, não a culpa.** Código errado: campos esvaziam, foco volta pro primeiro, mensagem inline diz "não bate" e oferece "pede um novo". Sem texto técnico, sem código de erro.
6. **Reenviar é a segunda ação, não a primeira.** Diferente do magic link (onde reenviar era primário), aqui a ação principal é **Confirmar**. Reenviar fica como link `text-link` abaixo dos botões — Júlia normalmente já tem o código na inbox, basta digitar.

---

## 2. Tela única — `/verificar-email`

**Rota:** `GET /verificar-email` → view `verificar-email`
**Acesso:** autenticado, com `email_verified_at IS NULL`. Usuário com e-mail já verificado é redirecionado para `/trilha` antes da renderização.
**Quando aparece:** logo após o POST do form de cadastro da US-001 (e-mail/senha). **Não aparece** após cadastro Google (US-002) — `email_verified_at` já vem preenchido pelo OAuth.

### 2.1 Wireframe (mobile 320px — desktop usa o mesmo card centralizado, `max-width: 28rem`)

```
┌────────────────────────────────────┐
│         [logo · atrilha]            │  ← header brand-only · 56px
├────────────────────────────────────┤
│                                    │
│   ┌──────────────────────────────┐ │
│   │                              │ │
│   │      ○ envelope coral ●      │ │  ← disco 96×96 + selo decorativo
│   │                              │ │
│   │       FALTA POUCO            │ │  ← overline
│   │   Confirme teu e-mail        │ │  ← H1 display
│   │  Enviamos um código de       │ │  ← lead com e-mail destacado
│   │  6 dígitos pra **e@mail**    │ │
│   │  Digita ele aqui embaixo.    │ │  ← helper muted
│   │  Vale por 10 minutos.        │ │
│   │                              │ │
│   │  [alert opcional — banner    │ │
│   │   success ou warning §2.6]   │ │
│   │                              │ │
│   │   ┌─┐ ┌─┐ ┌─┐ ┌─┐ ┌─┐ ┌─┐   │ │  ← 6 inputs 44×52
│   │   │ │ │ │ │ │ │ │ │ │ │ │   │ │     gap 8px
│   │   └─┘ └─┘ └─┘ └─┘ └─┘ └─┘   │ │
│   │                              │ │
│   │   [mensagem de erro inline]  │ │  ← reservado, hidden em normal
│   │                              │ │
│   │   [    Confirmar    ]  pill  │ │  ← primário (disabled até completar)
│   │   [ Continuar mesmo  ] ghost │ │
│   │     assim                    │ │
│   │                              │ │
│   │   Não chegou? Reenviar       │ │  ← link inline · text-sm
│   │                              │ │
│   └──────────────────────────────┘ │
│                                    │
└────────────────────────────────────┘
```

### 2.2 Estrutura HTML (referência, não obrigatória — Codificador decide o markup exato)

```html
<header>  brand-only (logo + "atrilha")  </header>

<main class="main">
  <article class="card">
    <div class="art" aria-hidden="true">[svg envelope sobre disco coral]</div>

    <p class="overline">Falta pouco</p>
    <h1 class="title">Confirme teu e-mail</h1>
    <p class="lead">
      Enviamos um código de 6 dígitos pra <strong>{{email}}</strong>
    </p>
    <p class="helper">Digita ele aqui embaixo. Vale por 10 minutos.</p>

    <div role="status" aria-live="polite">
      <!-- alert--success (após reenvio OK) ou alert--warning (rate-limit) -->
    </div>

    <form method="post" action="/verificar-email/otp">
      [csrf]
      <div class="otp" role="group"
           aria-label="Código de 6 dígitos do e-mail"
           aria-describedby="otp-error">
        <input class="otp-digit" type="text" inputmode="numeric"
               pattern="[0-9]*" maxlength="1"
               autocomplete="one-time-code"
               aria-label="Dígito 1 de 6" />
        <!-- × 6 -->
      </div>

      <p id="otp-error" class="field-error" role="alert" hidden>
        <!-- mensagem inline de código errado/expirado -->
      </p>

      <div class="actions">
        <button type="submit" class="btn btn-primary" disabled>
          Confirmar
        </button>
        <a href="/trilha" class="btn btn-ghost">Continuar mesmo assim</a>
      </div>

      <p class="resend">
        Não chegou?
        <button type="button" class="resend-btn">Reenviar código</button>
      </p>
    </form>
  </article>
</main>
```

---

## 3. Especificações visuais

### 3.1 Layout do container

| Elemento | Token / valor |
|---|---|
| Fundo da página | `--color-bg` (`neutral-50` · `#F7F4F1`) |
| Padding do `<main>` | `--space-10` topo/baixo · `--space-4` laterais |
| `max-width` do card | `28rem` (448px) — caber 6 dígitos confortavelmente em mobile |
| Centralização | `margin: 0 auto` |

### 3.2 Header

| Propriedade | Valor |
|---|---|
| Posição | `sticky; top: 0; z-index: --z-sticky` |
| Fundo | `--color-surface` (`#FFFFFF`) |
| Borda inferior | `1px solid --color-divider` |
| Conteúdo | Logo (marca + texto "atrilha") **centralizado** horizontalmente — sem botão voltar, sem menu |
| Altura mínima | `56px` |
| Logo | Marca 26×26 com fundo coral (`--color-primary-500`), texto "atrilha" em `--text-base --font-weight-semibold --color-text-display` |

### 3.3 Card

| Propriedade | Valor |
|---|---|
| Fundo | `--color-surface` |
| Borda | `1px solid --color-divider` |
| Raio | `--radius-xl` (20px) |
| Sombra | `--shadow-md` |
| Padding | `--space-8` topo · `--space-6` laterais · `--space-6` baixo |
| Alinhamento de texto | `center` (banners e mensagens de erro internas voltam a `left`) |

### 3.4 Ilustração

| Propriedade | Valor |
|---|---|
| Dimensões | `96 × 96px` |
| Forma | `--radius-full` (círculo) |
| Margem | `0 auto --space-6` (centro + gap pro overline) |
| Fundo | `radial-gradient(circle at 30% 30%, --color-primary-200 0%, --color-primary-100 60%, --color-primary-100 100%)` |
| Glyph | SVG envelope `44 × 44`, `stroke: --color-primary-700`, `stroke-width: 1.6` |
| Selo decorativo | Pseudo-elemento `::after`, 14×14 círculo `--color-primary-500`, posição `right: 14px; bottom: 14px`, anel branco via `box-shadow: 0 0 0 3px --color-surface` |

### 3.5 Tipografia textual do card

| Slot | Token / valor |
|---|---|
| Overline ("FALTA POUCO") | `--text-xs` · `text-transform: uppercase` · `--tracking-overline` (0.05em) · `--font-weight-semibold` · `--color-text-muted` · `margin-bottom: --space-2` |
| H1 ("Confirme teu e-mail") | `--font-display` (Bricolage Grotesque) · `--text-2xl` (24px) · `line-height: 1.15` · `--font-weight-semibold` · `--color-text-display` · `margin-bottom: --space-3` |
| Lead | `--text-base` · `--color-text-body` · `margin-bottom: --space-2` |
| Lead `<strong>` (e-mail) | `--color-text-display` · `--font-weight-semibold` · `word-break: break-all` (e-mails longos não estouram) |
| Helper | `--text-sm` · `--color-text-muted` · `margin-bottom: --space-6` |

### 3.6 Inputs OTP (o coração da tela)

**Container `.otp`:**

| Propriedade | Valor |
|---|---|
| Display | `flex; justify-content: center` |
| Gap entre inputs | `--space-2` (8px) |
| Margem inferior | `--space-3` |
| `data-error` | Atributo dinâmico — quando `true` (código inválido/expirado), todos os inputs viram borda `--color-danger-700` |
| Role | `role="group"` |
| `aria-label` | `"Código de 6 dígitos do e-mail"` |
| `aria-describedby` | `"otp-error"` (aponta para o `<p>` de erro abaixo) |

**Cada `.otp-digit` (input):**

| Propriedade | Valor |
|---|---|
| Largura | `2.75rem` (44px — touch target mínimo da RNF-A11Y-05) |
| Altura | `3.25rem` (52px — confortável em mobile) |
| Texto | `text-align: center`, `--font-display`, `--text-2xl` (24px), `--font-weight-semibold`, `--color-text-display` |
| Fundo | `--color-surface` |
| Borda | `1.5px solid --color-border` em repouso |
| Raio | `--radius-md` (8px) |
| Caret | `caret-color: --color-primary` (cursor coral, micro-detalhe de brand) |
| Hover | `border-color: --color-border-strong` |
| Focus | `outline: none; border-color: --color-focus-ring; box-shadow: --shadow-focus` |
| Erro (`data-error="true"` no `.otp`) | `border-color: --color-danger-700` |
| Foco em erro | `box-shadow: --shadow-focus-danger` (anel coral substituído por anel vermelho translúcido) |
| Atributos | `type="text" inputmode="numeric" pattern="[0-9]*" maxlength="1" autocomplete="one-time-code"` |
| `aria-label` (por input) | `"Dígito {n} de 6"` |
| `aria-invalid` | Dinâmico, espelha `data-error` do container |

**Spin buttons:** ocultar via `::-webkit-outer-spin-button { -webkit-appearance: none }` e `[type=number] { -moz-appearance: textfield }` (defensivo, caso a implementação use `type="number"` — o protótipo usa `type="text"` por aceitar paste melhor).

**Por que `type="text"` + `inputmode="numeric"`** em vez de `type="number"`:

- Em iOS/Android o `inputmode="numeric"` já abre teclado numérico.
- `type="number"` quebra `maxlength` em alguns mobiles (aceita "12345" mesmo com `maxlength="1"`).
- `type="text"` aceita paste de string limpa; o tratamento de "só dígitos" é JS (já está no protótipo: `value.replace(/\D/g, '')`).

### 3.7 Mensagem de erro inline

| Propriedade | Valor |
|---|---|
| Elemento | `<p id="otp-error" class="field-error" role="alert">` |
| `--text-sm` | tamanho |
| Cor | `--color-danger-700` |
| `margin-bottom` | `--space-3` |
| `min-height` | `1.4em` — **reserva o espaço** mesmo quando vazio, evita layout shift quando o erro aparece |
| Atributo `hidden` | Aplicado em estado `normal` (CSS `visibility: hidden` mantém o espaço; não usar `display: none` para não pular o layout) |

### 3.8 Botões — área `.actions`

| Propriedade | Valor |
|---|---|
| Display | `flex; flex-direction: column; gap: --space-3` |
| Margem inferior | `--space-5` (separa do bloco "Reenviar") |

**`.btn` (base de ambos):**

| Propriedade | Valor |
|---|---|
| Display | `inline-flex; align-items: center; justify-content: center; gap: --space-2` |
| Largura | `100%` |
| Altura mínima | `--space-12` (48px — confortável) |
| Padding | `--space-3 --space-6` |
| Peso | `--font-weight-semibold` |
| Tamanho | `--text-base` |
| Raio | `--radius-full` (pill) |
| Transição | `background-color --duration-fast --ease-out-soft` |

**`.btn-primary` ("Confirmar"):**

| Estado | Estilo |
|---|---|
| Default | `background: --color-primary; color: --color-on-primary` |
| Hover | `background: --color-primary-hover` (`primary-600`) |
| Active | `background: --color-primary-active` (`primary-700`) |
| Disabled | `opacity: 0.55; cursor: not-allowed` — ativo enquanto < 6 dígitos preenchidos ou enquanto loading |
| Loading | Conteúdo trocado por spinner (18×18, `border: 2px solid currentColor; border-right-color: transparent; animation: spin 0.7s linear infinite`) + texto "Validando…" |

**`.btn-ghost` ("Continuar mesmo assim"):**

| Estado | Estilo |
|---|---|
| Default | `background: transparent; color: --color-text-body` |
| Hover | `background: --color-surface-muted` (`neutral-100`) |
| Active | Mesmo do hover |

### 3.9 Link de reenviar

| Propriedade | Valor |
|---|---|
| Container `.resend` | `<p>` com `--text-sm` · `--color-text-muted` · `text-align: center` |
| Texto introdutório | "Não chegou?" (some quando `cooldown > 0`) |
| Botão `.resend-btn` | `<button type="button">` (não `<a>` — é POST submetido via JS, e/ou form interno) |
| Cor | `--color-primary` · `--font-weight-semibold` |
| Padding | `--space-1 --space-2` |
| Altura mínima | `--space-11` (44px touch target) |
| Raio | `--radius-md` |
| Hover | `background: --color-surface-muted` |
| Disabled (durante cooldown) | `color: --color-text-muted; cursor: not-allowed` — texto vira `"Reenviar em {N}s"` |

---

## 4. Microcopy (pt-BR — literal, copiar como está)

| Slot | Texto |
|---|---|
| `<title>` | Confirme teu e-mail |
| Overline | Falta pouco |
| H1 | Confirme teu e-mail |
| Lead | Enviamos um código de 6 dígitos pra **{email}** |
| Helper | Digita ele aqui embaixo. Vale por 10 minutos. |
| CTA primário (default) | Confirmar |
| CTA primário (loading) | Validando… (com spinner) |
| CTA secundário | Continuar mesmo assim |
| Reenviar (introdução) | Não chegou? |
| Reenviar (CTA default) | Reenviar código |
| Reenviar (cooldown) | Reenviar em {N}s |
| Banner success (após reenvio OK) | Reenviamos. Confere a tua caixa de entrada (e o spam). |
| Banner warning (rate-limited) | Ei, calma — espera alguns minutos antes de tentar de novo. |
| Erro inline — código inválido | Esse código não bate. Confere os dígitos ou pede um novo. |
| Erro inline — código expirado | Esse código expirou. Pede um novo aí em baixo. |

**Vetado:**

- "Atualizar no perfil", "Trocar e-mail", "Editar e-mail" — e-mail é credencial imutável.
- Termos técnicos: "token", "OTP", "verificação por código", "validação", "ativar conta", "JWT", "rate-limit", "429", "TTL". Usar "código" e "confirmar".
- "Tentativas restantes (2/5)" — não expor contagem interna do lockout.

---

## 5. Estados visuais

Cinco estados alternáveis (contrato testável via `data-state` no container ou variáveis Alpine):

| Estado | Quando | UI |
|---|---|---|
| `normal` (default) | GET inicial, ou usuário começou a digitar sem submeter | Sem banner, inputs em repouso, botão Confirmar `disabled` até 6 dígitos preenchidos |
| `loading` | POST de validação em voo | Botão Confirmar com spinner + "Validando…", `aria-busy="true"`, inputs ainda visíveis mas o submit já não dispara |
| `resent` (sucesso de reenvio) | POST `/verificar-email/reenviar` retornou OK | Banner `alert--success` no topo do card · botão "Reenviar" entra em cooldown 30s · foco volta para o 1º input |
| `invalid` (código errado) | POST de validação retornou "código incorreto" | Container `.otp` recebe `data-error="true"` (borda vermelha em todos) · `.field-error` mostra mensagem · inputs são **esvaziados** · foco volta para o 1º input |
| `expired` (código velho) | POST de validação retornou "expirado" | Mesma UI de `invalid`, mas mensagem é "Esse código expirou. Pede um novo aí em baixo." A diferença textual leva Júlia direto pro botão Reenviar |
| `rate-limited` | POST retornou 429 (servidor) | Banner `alert--warning` no topo do card · botão Reenviar fica `disabled` por curto período (cooldown client de 30s — o servidor é a autoridade) |

**Limpar erro ao re-digitar:** assim que Júlia digita qualquer tecla em qualquer input estando em `invalid`/`expired`, o estado volta para `normal` (mensagem some, borda volta). Confirmação textual no protótipo: trecho `if (this.state === 'invalid' || this.state === 'expired') { this.state = 'normal'; }` no handler `onInput`.

---

## 6. Comportamento de entrada

### 6.1 Sequência típica

1. Página carrega → foco automático no **1º input** (`d0`).
2. Júlia digita "1" → input aceita, dispatcha foco no **2º input**.
3. Repete até o 6º. Ao preencher o 6º, **auto-submit** dispara (sem precisar clicar Confirmar).
4. Loading → resposta → ou redireciona (sucesso) ou mostra erro (falha).

### 6.2 Tabela de interações

| Interação | Comportamento esperado |
|---|---|
| Digitar dígito num input vazio | Caractere aceito; foco vai pro próximo input |
| Digitar caractere não-numérico | Filtro JS remove (`replace(/\D/g, '')`); input fica vazio |
| Digitar dígito num input já preenchido (selecionado) | Substitui (`@focus="$event.target.select()"` faz o select antes da digitação) |
| Backspace num input vazio | Foco volta pro anterior; conteúdo do anterior é apagado |
| Backspace num input com dígito | Apaga só o dígito desse input, foco fica |
| Seta ← / → | Move foco entre inputs |
| `Paste` (Ctrl+V / Cmd+V) em qualquer input | Cleaned (só dígitos), distribuído nos inputs a partir do índice em foco; foco vai para o próximo vazio (ou o último); se completar os 6, auto-submit |
| Autofill `one-time-code` (iOS) | O OS injeta o código completo no 1º input (`value="123456"`); o handler detecta `value.length > 1` e trata como paste |
| Tab | Pula para o próximo elemento focável fora do `.otp` (não pula entre os 6 inputs — quem faz isso é o auto-advance) |
| Submit do form (botão Confirmar) | Só dispara se `isComplete()` for true e `loading` for false |
| Submit ao completar 6º | Dispara automaticamente via `$nextTick(() => this.submit())` no handler de input |

### 6.3 Cooldown de reenvio

- Após `resend()` voltar OK, botão Reenviar entra em `cooldown = 30` e contador regressivo decrementa 1/s.
- Cooldown é **client-side** — refresh perde o contador (aceitável; o servidor mantém o rate-limit como fonte da verdade).
- Cooldown **não persiste em sessão** — simplicidade > correção marginal.
- Quando `cooldown` chega a 0, botão volta a "Reenviar código" habilitado.

### 6.4 Auto-focus inicial

`init()` chama `$nextTick(() => this.$refs.d0?.focus())` no `x-init`. Aceitável em telas onde o input é claramente a ação principal (RNF-A11Y permite quando a tela é dedicada à entrada).

---

## 7. Acessibilidade

- `<header>` com `role="banner"`.
- `<main>` único na página.
- H1 único; overline é `<p>` (não heading) — preserva hierarquia.
- Container `.otp`: `role="group"`, `aria-label="Código de 6 dígitos do e-mail"`, `aria-describedby="otp-error"`.
- Cada input: `aria-label="Dígito {n} de 6"`, `aria-invalid={hasError()}`.
- Mensagem de erro: `<p id="otp-error" role="alert">` — leitor de tela anuncia quando aparece. `min-height` reserva o espaço pra não dar layout shift.
- Banner `success`: `<div role="status" aria-live="polite">`. Banner `warning`: `<div role="alert">`.
- Botão "Confirmar" em loading: `aria-busy="true"` (idealmente também `aria-label="Validando código"` se o texto for "Validando…").
- Botão "Reenviar" em cooldown: `disabled` + texto visível "Reenviar em Ns".
- Ilustração: `aria-hidden="true"`.
- Contraste calculado:
  - `text-display` (`#1A1614`) sobre `surface` (`#FFFFFF`) → **16.4:1** (AAA).
  - `text-body` (`#3D3733`) sobre `surface` → **10.8:1** (AAA).
  - `text-muted` (`#7A716B`) sobre `surface` → **4.62:1** (AA).
  - Dígito digitado (`text-display`) em input → **16.4:1** (AAA).
  - CTA primário coral 500 (`#F25C54`) com texto branco → ≥ **4.5:1** (chore-ux-001 §2.1).
  - Borda em erro `danger-700` (`#C8362B`) sobre `surface` → suficiente como indicador secundário (cor + ícone de mensagem + texto inline).
- `prefers-reduced-motion: reduce`: spinner para de girar; contador textual segue funcionando.

---

## 8. Reusos e proibições

### 8.1 Reusos obrigatórios

| Item | Origem | Não recriar |
|---|---|---|
| Tokens de cor, espaço, raio, sombra | `doc/UX/01-design-tokens.md` | Nenhum hex novo. |
| `btn btn-primary` (pill coral lg) | `doc/UX/02-componentes-base.md` §1 | Sem variantes inéditas. |
| `btn btn-ghost` | `doc/UX/02-componentes-base.md` §1 | Sem variantes inéditas. |
| `alert--success`, `alert--warning` | `doc/UX/02-componentes-base.md` §2.4 | Mesmo padrão dos demais banners (login, verify-email-resultado). |
| Header brand-only | `layout/fragments/public-header :: public-header-brand` | Já existe — reuso direto. |
| Layout `layout/public` | repo | Decorator herda. |
| Foco visível, touch target 44×44 | `doc/UX/06-acessibilidade.md` | Sem exceções. |

### 8.2 Proibições

- **Não** oferecer UI de "alterar e-mail", "atualizar e-mail", "trocar e-mail", "perfil" nesta tela. O e-mail é credencial imutável.
- **Não** colocar botão "voltar" no header — área autenticada, fluxo de saída é só pelos CTAs ou pela conclusão.
- **Não** usar coral no botão ghost — coral é reservado ao CTA primário e à marca.
- **Não** disparar reenvio automático no `onload` — Júlia precisa pedir explicitamente.
- **Não** mostrar contador de "tentativas restantes" — expõe lockout interno.
- **Não** mostrar o código nem fragmentos do e-mail completo em log/console do client.
- **Não** usar termos técnicos no microcopy.
- **Não** substituir o banner inline por toast flutuante — banner dentro do card é o padrão das demais telas de auth.
- **Não** transformar os 6 inputs em 1 campo único — perde o reconhecimento visual de "código" e degrada a experiência de paste e autofill em mobile.
- **Não** usar `type="number"` nos inputs — quebra `maxlength="1"` em Android Chrome.

---

## 9. Estados, validações e bordas

| Cenário | Comportamento esperado |
|---|---|
| Júlia chega logo após signup e-mail/senha (US-001) | Estado `normal`, foco no 1º input |
| Júlia chega após signup Google (US-002) | **Não chega** — Google entrega `email_verified_at != null`, o controller redireciona para `/trilha` antes da renderização |
| Júlia digita os 6 dígitos rapidamente | Auto-submit dispara ao completar o 6º |
| Júlia cola o código completo (paste) no 1º input | Os 6 inputs são preenchidos de uma vez, auto-submit dispara |
| Júlia cola o código colando 6 dígitos com espaços ou hífens (`"123 456"` ou `"123-456"`) | Filtro `replace(/\D/g, '')` remove e preenche normalmente |
| iOS sugere o código no teclado e Júlia toca a sugestão | Mesmo que paste — handler detecta `value.length > 1` e distribui |
| Júlia digita "12345" e clica Confirmar | Botão segue `disabled` — `isComplete()` retorna false. Sem mensagem de erro (ainda) |
| Júlia digita os 6, servidor responde "código errado" | Estado `invalid` → inputs esvaziam → foco volta pro 1º → mensagem inline aparece |
| Júlia volta a digitar após erro | Estado volta para `normal` no 1º keystroke |
| Júlia digita os 6, servidor responde "expirado" | Estado `expired` → mensagem específica que aponta para o botão Reenviar |
| Júlia clica Reenviar | Loading 600–800ms → `resent` (banner success) → cooldown 30s → foco volta pro 1º input |
| Júlia clica Reenviar repetidamente (mesmo durante cooldown) | Botão `disabled` ignora — cooldown protege a UX antes do servidor barrar |
| Servidor barrou Reenviar (429) | Estado `rate-limited`, banner warning. Cliente **não** dispara cooldown adicional |
| Júlia clica "Continuar mesmo assim" | Redirect `/trilha`. Banner global de "confirme teu e-mail" segue aparecendo nas demais telas (fora deste doc) |
| Tela carregada com viewport < 320px | Card encolhe; os 6 inputs continuam cabendo com gap `--space-2` (6×44 + 5×8 = 304px, cabe em 320px com padding `--space-4`) |
| `prefers-reduced-motion: reduce` | Spinner sem animação; cooldown segue textual |
| Sessão expira durante a tela aberta e Júlia clica Confirmar | POST retorna 401/302 para `/login` — comportamento padrão Spring Security |

---

## 10. Tabela completa de tokens consumidos

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da página |
| `--color-surface` | Header, card, inputs OTP, ilustração (anel branco do selo) |
| `--color-surface-muted` | Hover de botão ghost e do link reenviar |
| `--color-divider` | Borda inferior do header, borda do card |
| `--color-border` | Borda dos inputs OTP em repouso |
| `--color-border-strong` | Borda dos inputs OTP em hover |
| `--color-text-display` | H1, lead `<strong>`, dígito dentro do input |
| `--color-text-body` | Lead, botão ghost |
| `--color-text-muted` | Overline, helper, "Não chegou?", cooldown text |
| `--color-primary` (`primary-500`) | Marca, fundo do botão Confirmar, caret-color dos inputs, "Reenviar código" texto |
| `--color-primary-100` / `200` / `700` | Ilustração (gradiente do disco) e glyph |
| `--color-primary-hover` (`600`) | Hover do botão Confirmar |
| `--color-primary-active` (`700`) | Active do botão Confirmar |
| `--color-on-primary` | Texto do botão Confirmar |
| `--color-focus-ring` | Foco em todos os elementos focáveis |
| `--color-success-100` / `700` | Banner "Reenviamos…" |
| `--color-warning-100` / `700` | Banner rate-limit |
| `--color-danger-700` | Borda do `.otp[data-error]`, texto da `.field-error` |
| `--font-display` | H1, dígitos dos inputs OTP |
| `--font-sans` | Restante do texto |
| `--text-xs` | Overline |
| `--text-sm` | Helper, banners, link reenviar, erro inline |
| `--text-base` | Lead, botões, "Não chegou?" |
| `--text-2xl` | H1 e dígitos OTP |
| `--space-2` / `3` / `5` / `6` / `8` / `10` / `11` / `12` | Espaçamentos verticais e padding |
| `--radius-md` | Inputs OTP, botão ghost de reenviar |
| `--radius-lg` | Banners |
| `--radius-xl` | Card |
| `--radius-full` | Ilustração (disco e selo), botões pill |
| `--shadow-md` | Card |
| `--shadow-focus` | Anel de foco padrão |
| `--shadow-focus-danger` (`0 0 0 3px rgba(200,54,43,0.30)`) | Anel de foco em estado de erro — **token novo proposto**, registrar em `doc/UX/01-design-tokens.md` quando esta US for implementada |
| `--duration-fast` · `--ease-out-soft` | Transições de hover e estado |

---

## 11. Pendências de design

Nenhuma bloqueante. Pontos abertos:

1. **Token `--shadow-focus-danger`** — derivado em `0 0 0 3px rgba(200, 54, 43, 0.30)`. Quando a US for implementada, registrar formalmente em `doc/UX/01-design-tokens.md` §6.
2. **Persistência do cooldown** — client-side puro (perde no refresh). Se métricas mostrarem que vira problema, considerar persistir `lastResendAt` em sessão e calcular cooldown remanescente no GET.
3. **Ilustração editorial** — disco coral + envelope é a solução do design system. Se a produção de ilustrações editoriais (`doc/UX/00-identidade-visual.md` §7) avançar, esta tela é candidata a uma ilustração custom.
4. **Banner global pós "Continuar mesmo assim"** — comportamento do `EmailVerificationBannerAdvice` fica fora deste doc, mas conecta. Eventual redesign desse banner deve manter coerência (mesmo tom de microcopy, mesma cor `--color-warning-100`).
5. **Decisão de produto pendente** (de [`us-006-comparativo.md`](us-006-comparativo.md) §4):
   - OTP puro **ou** OTP + magic link convivendo no mesmo e-mail?
   - TTL de 10 minutos — aceitar?
   - Lockout: 5 erros em 15 min — aceitar?

---

## Referências cruzadas

- Protótipo executável: [`doc/UX/prototypes/verificar-email-otp.html`](prototypes/verificar-email-otp.html)
- Análise de abordagem (OTP vs magic link): [`doc/UX/us-006-comparativo.md`](us-006-comparativo.md)
- Tokens visuais: `doc/UX/01-design-tokens.md`
- Componentes base (botão, alert, card): `doc/UX/02-componentes-base.md`
- Acessibilidade: `doc/UX/06-acessibilidade.md`
- Fluxo OAuth que **não** passa por esta tela: `doc/UX/us-002-spec.md` §1.2
- Implementação atual (magic link, a ser substituída): `src/main/resources/templates/verificar-email.html`
- Memória do projeto: [e-mail é credencial imutável](../../.claude/projects/-Users-dionia-oliveira-sources-atrilha/memory/email-imutavel-apos-cadastro.md)
