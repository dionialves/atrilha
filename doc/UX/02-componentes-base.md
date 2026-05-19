# Componentes-base — atrilha

**Task:** chore-ux-003 (Issue #22)
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M2 — Componentes-base catalogados
**Status:** Proposto
**Depende de:** `doc/UX/00-identidade-visual.md` (chore-ux-001, aprovada) · `doc/UX/01-design-tokens.md` (chore-ux-002)
**Referências:** PRD §4.3 (P12, P13, P14, P15) · PRD §8.4 (RNF-A11Y-01..05, RNF-COMP-04) · PRD §13 (US-024, US-041) · PRD §16 (ADR-011, ADR-013) · AGENTS.md §12
**Stack-alvo (ADR-011):** Thymeleaf + HTMX + Tailwind v4 + Alpine.js + Lottie

**Escopo deste doc:** catálogo de comportamento + visual dos 8 componentes-base reutilizáveis (button, input, card, modal, header, navegação, badge, toast) e tokens de componente que decorrem deles. **Implementação** dos fragments Thymeleaf (`src/main/resources/templates/components/*.html`) **não pertence a esta task** — fica para o Codificador na primeira US que consumir cada componente.

---

## 0. Princípios gerais

Aplicam-se a **todos** os componentes deste catálogo. Quando um componente parecer violar um princípio, é o componente que cede — não o princípio.

1. **Mobile-first absoluto.** Todo componente é desenhado primeiro para viewport de 320px de largura (chore-ux-001 §5; PRD §8.4 RNF-COMP-04). Refinamentos para tablet/desktop entram como utilitários `sm:`/`md:`/`lg:` (chore-ux-002 §7) — nunca como dependência funcional.

2. **Touch target ≥ 44×44px (PRD §8.4 RNF-A11Y-05).** Qualquer elemento interativo aplica `min-height: var(--space-11)` e, quando aplicável, `min-width: var(--space-11)`. Vale para botões, links, checkboxes, ícones-botão, itens de bottom-nav. Sem exceção.

3. **Foco visível sempre.** Anel de foco com `outline: 2px solid var(--color-focus-ring)` + `outline-offset: 2px` (ou `box-shadow: var(--shadow-focus)` quando `outline` não puder ser usado). Aplicado em `:focus-visible` — nunca esconder com `outline: none` sem substituir. WCAG 2.4.7.

4. **Loading explícito para HTMX.** Toda ação assíncrona disparada via HTMX precisa de feedback visual durante o request: spinner inline, skeleton ou estado "carregando" textual. Padrão: `hx-indicator` + classe `htmx-request` que o componente trata internamente. Sem flicker silencioso.

5. **Respeito a `prefers-reduced-motion`.** Componentes que usam `--duration-*` (chore-ux-002 §8) devem zerar duração e remover translates/scales quando o sistema do usuário pedir movimento reduzido:
   ```css
   @media (prefers-reduced-motion: reduce) {
     * { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
   }
   ```
   Esta regra é aplicada globalmente no CSS principal; componentes individuais não precisam repetir, mas **não podem** usar `animation` ou `transition` que sejam essenciais para a função (ex.: revelar conteúdo apenas após anim) — o conteúdo precisa funcionar com motion zerado.

6. **Idioma.** Texto visível ao usuário em **pt-BR**; nomes de classes, IDs, fragments e atributos `data-*` em **inglês**. Microcopy referenciada neste doc segue pt-BR (P5 — sem moralismo, P11 — vocabulário ASD jovem).

7. **Cor de marca é escassa (chore-ux-001 §5.6).** Componentes não devem aplicar `--color-primary` sem necessidade — coral é reservado para CTA primário, dia ativo da trilha, logo. Demais elementos usam neutros + secundários.

8. **Ilustração editorial > foto > ícone genérico (chore-ux-001 §5.8).** Componentes desta task se atêm a tipografia, formas e ícones SVG stroke (peso 1.5–2px). Ilustrações reais entram com cada US que precisar.

9. **Padrão de implementação Thymeleaf.** Cada componente vira **1 fragment** em `src/main/resources/templates/components/<nome>.html`, exposto com `th:fragment="<nome>(...parâmetros)"`. Parâmetros sempre com default via `${param ?: 'valorPadrao'}`. Variantes via classes Tailwind condicionais. **Sem JavaScript inline** — interatividade é Alpine `x-data` ou HTMX. Detalhes na §9.

10. **Decisão conservadora frente a ambiguidade.** Onde a Issue deixou margem (ex.: handle de arrastar no bottom sheet, posição do toast em desktop), este doc escolhe a opção que **menos custa retrabalho** se for revertida — registrada com justificativa no próprio componente.

---

## 1. Button (botão)

### 1.1 Propósito
Disparar uma ação — síncrona (navegação local) ou assíncrona (HTMX). É o componente mais reutilizado do app, e o que mais polui se mal especificado.

### 1.2 Anatomia
```
[ icon-leading? | label | icon-trailing? | spinner? ]
```
- **icon-leading** (opcional) — SVG stroke 16×16, à esquerda do label, gap `--space-2`.
- **label** — texto pt-BR, peso `--font-weight-semibold`, fonte `--font-sans`, tamanho `--text-base`.
- **icon-trailing** (opcional) — SVG stroke 16×16, à direita.
- **spinner** — renderizado apenas em estado `loading`, **substitui** `icon-leading` se houver, ou aparece à esquerda do label.

### 1.3 Variantes
Quatro variantes visuais × três tamanhos = matriz `4 × 3` (12 combinações). Variantes:

| Variante | Uso | Cor de fundo | Cor de texto | Borda |
|---|---|---|---|---|
| `primary` | Ação principal de uma tela (1 por tela) | `--color-primary` | `--color-on-primary` | nenhuma |
| `secondary` | Ação alternativa que não é destacada visualmente | `--color-surface` | `--color-text-body` | `1px solid var(--color-border-strong)` |
| `ghost` | Ação terciária, link-like, em barras de ação | transparente | `--color-text-body` | nenhuma |
| `destructive` | Ações irreversíveis ou perigosas (excluir, sair) | `--color-danger-100` | `--color-danger-700` | `1px solid var(--color-danger-700)` |

**Por que `destructive` usa danger-100 e não danger-500 como fundo:** mantém contraste de texto em AA sem virar "tela vermelha". Adolescente clica menos em vermelho-sangue (estresse visual). Decisão conservadora ancorada em chore-ux-001 §2.4.

Tamanhos (chore-ux-002 §4 — touch target):

| Size | Altura | Padding-x | `min-height` | Uso |
|---|---|---|---|---|
| `sm` | 36px | `--space-3` (12px) | 36px | **Uso restrito.** Inline em densidade alta (ex.: ação dentro de card pequeno). **Não atende touch target.** Permitido apenas quando o botão é cercado por área-alvo maior (cell clicável inteira ou aria-label garantindo alvo extendido). |
| `md` | **44px** | `--space-4` (16px) | `--space-11` | **Default.** Atende RNF-A11Y-05. |
| `lg` | 52px | `--space-6` (24px) | 52px | CTA primário mobile (chore-ux-001 §5.2 — full-width em mobile). |

**Variante adicional — `icon-only`:** botão quadrado mínimo 44×44 (`--space-11`), sem label visível, **obriga `aria-label`**. Usado em headers compactos e barras de ação. Trata-se de modificador ortogonal — `icon-only` combina com qualquer das 4 variantes visuais.

### 1.4 Estados

| Estado | Visual | Quando |
|---|---|---|
| `default` | Cor base da variante | Em repouso |
| `hover` | `primary` → `--color-primary-hover`. `secondary` → fundo `--color-surface-muted`. `ghost` → fundo `--color-surface-muted`. `destructive` → fundo `--color-danger-100` levemente mais escuro (deriva via Tailwind, sem token novo). | Mouse sobre (desktop) |
| `focus-visible` | Anel `outline: 2px solid var(--color-focus-ring)` com `outline-offset: 2px` | Foco via teclado |
| `active` (pressed) | `primary` → `--color-primary-active`. Demais: escurece marginalmente | Durante o toque/clique |
| `disabled` | Opacidade reduzida (`opacity: 0.5`), `cursor: not-allowed`, sem hover/active. `aria-disabled="true"` ou `disabled` atribuído. | Ação indisponível |
| `loading` | Spinner aparece, label permanece visível (não some — evita layout shift), clique fica bloqueado (`aria-busy="true"`, `disabled` aplicado). | Durante request HTMX (`htmx-request` aplicado automaticamente) |

**Por que label não some em loading:** previne CLS (Cumulative Layout Shift) e mantém comunicação do que está acontecendo. Padrão Stripe/Linear, contra padrão Bootstrap clássico.

### 1.5 Comportamento

- **HTMX (loading automático):**
  ```html
  <button hx-post="/api/sessions/complete" hx-indicator="this" class="btn btn-primary">
    <span class="btn-spinner" aria-hidden="true"></span>
    <span>Encerrar sessão</span>
  </button>
  ```
  - HTMX adiciona classe `htmx-request` no elemento durante a requisição.
  - CSS aplica `display: inline-block` ao `.btn-spinner` **apenas** quando `.htmx-request` está presente:
    ```css
    .btn-spinner { display: none; }
    .htmx-request .btn-spinner { display: inline-block; }
    .htmx-request { pointer-events: none; }
    ```
  - O atributo `aria-busy="true"` é aplicado via `hx-on::before-request` (configuração detalhada vira responsabilidade do Codificador).

- **Alpine (botão de toggle local, sem servidor):**
  ```html
  <button x-data="{ open: false }" @click="open = !open" :aria-expanded="open">…</button>
  ```

- **Transição:** `transition: background-color var(--duration-fast) var(--ease-out-soft), color var(--duration-fast) var(--ease-out-soft);`. **Não transicionar `transform`** — em motion-reduced o transform some, mas com motion ativo `:active` aplicaria um `scale(.98)` que conflitaria com touch (decisão conservadora: pular transform).

- **`prefers-reduced-motion`:** spinner usa `animation: spin 0.8s linear infinite`. Em motion-reduced, a regra global zera a animação — substituir o spinner por um **caractere estático** ou um label textual "Carregando…" é responsabilidade do componente (ver §1.8).

### 1.6 Tokens consumidos
Todos referenciados literalmente de `doc/UX/01-design-tokens.md`.

| Aspecto | Token consumido |
|---|---|
| Fundo `primary` | `--color-primary` |
| Fundo `primary:hover` | `--color-primary-hover` |
| Fundo `primary:active` | `--color-primary-active` |
| Texto `primary` | `--color-on-primary` |
| Fundo `secondary`/`ghost` (hover) | `--color-surface-muted` |
| Texto `secondary`/`ghost` | `--color-text-body` |
| Borda `secondary` | `--color-border-strong` |
| Fundo `destructive` | `--color-danger-100` |
| Texto/borda `destructive` | `--color-danger-700` |
| Raio (`primary`/`secondary`/`destructive`) | `--radius-full` (pill — visual jovem, chore-ux-002 §5) |
| Raio (`ghost` icon-only quadrado) | `--radius-md` |
| Padding-x `md` | `--space-4` |
| Padding-x `lg` | `--space-6` |
| Altura mínima `md` | `--space-11` (44px) |
| Tipografia | `--font-sans`, `--font-weight-semibold`, `--text-base` |
| Foco | `--color-focus-ring`, `--shadow-focus` |
| Duração transição | `--duration-fast` |
| Easing | `--ease-out-soft` |

Classes Tailwind v4 correspondentes (não exaustivas — Codificador valida na implementação): `bg-primary`, `text-on-primary`, `rounded-full`, `px-4`, `min-h-11`, `font-semibold`, `text-base`, `focus-visible:outline-2`.

### 1.7 Acessibilidade

- Sempre `<button type="button">` (ou `type="submit"` em formulários). Nunca `<div onclick>`.
- `icon-only` obriga `aria-label` em pt-BR.
- Estado `loading` aplica `aria-busy="true"`. Estado `disabled` aplica `disabled` (não `aria-disabled` solo — `disabled` é o canal correto para botão).
- Ordem de foco: a do DOM. Não usar `tabindex` positivo.
- Contraste de texto sobre fundo: validado na chore-ux-001 (`primary` 4.62:1 AA, `secondary` herda neutral-700 sobre branco 10.8:1 AAA, `destructive` 6.12:1 AA).

### 1.8 Ilustração e pseudocódigo

ASCII:
```
┌──────────────────────────────────────────┐
│              Encerrar sessão              │   ← primary, lg, full-width mobile
└──────────────────────────────────────────┘

┌────────────────────┐
│ ⟳  Carregando…    │   ← primary, md, htmx-request ativo (spinner + label visível)
└────────────────────┘

┌────────────────┐  ┌────────────────┐
│  Voltar         │  │ Próximo →     │   ← secondary + primary, md, lado-a-lado em desktop
└────────────────┘  └────────────────┘
```

Pseudocódigo Thymeleaf (fragment):
```html
<button th:fragment="button(variant, size, label, type, ariaLabel)"
        th:type="${type ?: 'button'}"
        th:classappend="|btn btn-${variant ?: 'primary'} btn-${size ?: 'md'}|"
        th:attr="aria-label=${ariaLabel}">
  <span class="btn-spinner" aria-hidden="true"></span>
  <span th:text="${label}"></span>
</button>
```

---

## 2. Input (campo de texto)

### 2.1 Propósito
Coletar texto do usuário — curto (login, busca) ou longo (reflexão US-024, até 1000 caracteres). Validação real é Jakarta no service; client-side é decoração.

### 2.2 Anatomia
```
┌─ label ─────────────────────────────┐
│                                       │
│  ┌───────────────────────────────┐   │
│  │ [icon-leading?] [valor]       │   │   ← field
│  │ [icon-trailing? | contador?]  │   │
│  └───────────────────────────────┘   │
│                                       │
│  helper text │ error text             │
└──────────────────────────────────────┘
```
- **label** — texto acima do campo, `--text-sm`, `--font-weight-semibold`. Sempre vinculada por `for`/`id`.
- **field** — o input/textarea em si.
- **icon-leading** / **icon-trailing** (opcional) — SVG stroke 20×20 dentro do field, com padding interno ajustado.
- **helper text** — texto orientativo `--text-xs`, `--color-text-muted`, abaixo do field.
- **error text** — substitui helper em estado de erro; `--color-danger-700`, vinculado por `aria-describedby`.
- **contador** (apenas em `textarea` com limite) — "restam 234", canto inferior direito, `--text-xs`, `--color-text-muted`. Vira `--color-danger-700` quando faltam ≤50 caracteres.

### 2.3 Variantes

| Variante | Tag HTML | Uso |
|---|---|---|
| `text` | `<input type="text">` | Campo padrão (nome, busca) |
| `email` | `<input type="email" inputmode="email">` | Login, vinculação responsável (US-046) |
| `password` | `<input type="password">` | Senha. Atributo `autocomplete` apropriado. |
| `textarea` | `<textarea>` | Multilinha. Usado por US-024 (reflexão, limite 1000 chars, contador obrigatório). |

Sem tamanhos múltiplos — input herda `min-height: var(--space-11)` (44px) sempre. Textarea tem `min-height: var(--space-24)` (96px) inicial, autoexpansível ou com scroll interno (decisão fica para a US-024).

### 2.4 Estados

| Estado | Visual |
|---|---|
| `default` | Fundo `--color-surface`, borda `--color-border` (1px), texto `--color-text-body`, placeholder `--color-text-muted` |
| `hover` (desktop) | Borda `--color-border-strong` |
| `focus` | Borda `--color-focus-ring` (2px) + anel de foco `--shadow-focus` |
| `filled` | Como default mas com texto digitado — sem mudança visual de borda |
| `disabled` | `opacity: 0.5`, fundo `--color-surface-muted`, cursor `not-allowed` |
| `readonly` | Fundo `--color-surface-muted`, sem borda de foco visível em hover |
| `error` | Borda `--color-danger-700`, ícone-trailing de alerta `--color-danger-700`, error text exibido. `aria-invalid="true"` |

### 2.5 Comportamento

- **Validação:** a fonte da verdade é o servidor (validação Jakarta no service). Erros vindos do servidor são exibidos via HTMX swap do bloco de erro:
  ```html
  <div id="email-error" hx-swap-oob="true">
    <span class="input-error" th:text="${erro}"></span>
  </div>
  ```
- **Validação client-side mínima:** apenas `required`, `maxlength`, `type="email"` (atributos HTML padrão). Não duplicar regras de negócio em JS.
- **Contador (textarea com limite):** Alpine controla:
  ```html
  <div x-data="{ value: '', max: 1000 }">
    <textarea x-model="value" :maxlength="max"></textarea>
    <span class="input-counter" :class="{ 'input-counter--warning': max - value.length <= 50 }">
      restam <span x-text="max - value.length"></span>
    </span>
  </div>
  ```
- **Transição:** `transition: border-color var(--duration-fast) var(--ease-out-soft)`. Aplicada apenas em borda — sem transform.

### 2.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo | `--color-surface` |
| Borda repouso | `--color-border` |
| Borda hover | `--color-border-strong` |
| Borda foco | `--color-focus-ring` |
| Borda erro | `--color-danger-700` |
| Anel foco | `--shadow-focus` |
| Texto valor | `--color-text-body` |
| Texto placeholder/helper | `--color-text-muted` |
| Texto erro | `--color-danger-700` |
| Raio | `--radius-md` |
| Padding-x | `--space-4` |
| Padding-y | `--space-3` |
| `min-height` | `--space-11` |
| Label tipografia | `--font-sans`, `--font-weight-semibold`, `--text-sm` |
| Helper/error tipografia | `--font-sans`, `--text-xs` |

### 2.7 Acessibilidade

- `<label for="id">` **sempre** explícito. `aria-label` apenas como último recurso (busca em header, por exemplo).
- `aria-describedby="id-helper"` aponta para helper text quando presente.
- Em erro: `aria-invalid="true"` no input + `aria-describedby="id-error"` apontando para mensagem.
- `aria-required="true"` quando obrigatório (complementa `required`).
- Placeholder **não** substitui label (P15 — clareza; também WCAG 3.3.2).
- Contraste do texto digitado: `--color-text-body` sobre `--color-surface` = AAA (chore-ux-001 §2.5).

### 2.8 Ilustração e pseudocódigo

ASCII (textarea com contador — caso US-024):
```
Sua reflexão                         ⓘ privado
┌─────────────────────────────────────────────┐
│ Hoje eu pensei sobre como…                  │
│                                             │
│                                             │
│                                             │
└─────────────────────────────────────────────┘
                                  restam 934
```

Pseudocódigo Thymeleaf (fragment):
```html
<div th:fragment="input(id, label, name, type, value, helper, error, required, maxLength)"
     th:classappend="|input-group|">
  <label th:for="${id}" class="input-label" th:text="${label}"></label>
  <input th:if="${type != 'textarea'}"
         th:id="${id}" th:name="${name}"
         th:type="${type ?: 'text'}"
         th:value="${value}"
         th:required="${required}"
         th:attr="aria-invalid=${error != null}, aria-describedby=${error != null ? id + '-error' : (helper != null ? id + '-helper' : null)}, maxlength=${maxLength}"
         class="input-field">
  <textarea th:if="${type == 'textarea'}"
            th:id="${id}" th:name="${name}"
            th:text="${value}"
            th:attr="aria-invalid=${error != null}, aria-describedby=${error != null ? id + '-error' : (helper != null ? id + '-helper' : null)}, maxlength=${maxLength}"
            class="input-field input-field--textarea"></textarea>
  <span th:if="${helper != null and error == null}"
        th:id="${id} + '-helper'" class="input-helper" th:text="${helper}"></span>
  <span th:if="${error != null}"
        th:id="${id} + '-error'" class="input-error" role="alert" th:text="${error}"></span>
</div>
```

---

## 3. Card

### 3.1 Propósito
Agrupar conteúdo relacionado em um bloco visualmente destacado. É a unidade básica de composição da trilha, dos blocos de sessão (US-023), do painel do responsável (US-043) e dos itens de listagem.

### 3.2 Anatomia
```
┌────────────────────────────────────────┐
│  header  : título + badge?/ícone?      │   (opcional)
├────────────────────────────────────────┤
│                                        │
│  body  : conteúdo livre                │
│                                        │
├────────────────────────────────────────┤
│  footer  : ações (botões/links)        │   (opcional)
└────────────────────────────────────────┘
```
- **header** — opcional. Título (`--text-lg`, `--font-weight-semibold`), com badge ou ícone à direita.
- **body** — qualquer conteúdo. Padding aplicado pelo card.
- **footer** — opcional. Ações alinhadas à direita (desktop) ou empilhadas (mobile).

### 3.3 Variantes

| Variante | Visual | Uso |
|---|---|---|
| `flat` | Fundo `--color-surface`, borda `1px solid var(--color-divider)`, sem sombra | Lista densa de itens, painel de pais |
| `raised` | Fundo `--color-surface`, sem borda, `--shadow-md` | Card que precisa "saltar" da página — nó da trilha ativo, bloco de sessão |
| `interactive` | Card inteiro vira clicável — renderiza como `<a>` ou `<button>`. Aplica hover (eleva `--shadow-md` → `--shadow-lg`) e foco visível. | Nó da trilha (clica para abrir sessão), item de lista clicável |

Padding interno padrão: `--space-5` (20px). Em densidade alta (lista de pais), pode reduzir para `--space-4` via modificador `--dense`.

### 3.4 Estados

| Estado | Aplicável a | Visual |
|---|---|---|
| `default` | todas | Variante base |
| `hover` | `interactive` | `--shadow-md` → `--shadow-lg`, opcional `transform: translateY(-1px)` (apenas se motion permitido) |
| `focus-visible` | `interactive` | Anel `--shadow-focus` |
| `active` | `interactive` | Sombra volta para `--shadow-md`, conteúdo levemente comprimido |
| `selected` | opcional (lista de seleção) | Borda `2px solid var(--color-primary)` |
| `loading` | opcional (skeleton) | Body substituído por blocos cinza `--color-neutral-100` com animação shimmer (respeitando reduced-motion — em motion-reduced, mantém estático) |

### 3.5 Comportamento

- **`interactive` via HTMX:** o card inteiro pode disparar uma requisição:
  ```html
  <a class="card card--interactive" hx-get="/sessions/today" hx-target="#main" hx-push-url="true">…</a>
  ```
- **Loading interno via HTMX:** quando o card carrega seu próprio conteúdo de forma assíncrona, mostra skeleton até o swap:
  ```html
  <div class="card" hx-get="/widgets/streak" hx-trigger="load" hx-swap="innerHTML">
    <div class="card-skeleton">…</div>
  </div>
  ```
- **Transição:** `transition: box-shadow var(--duration-base) var(--ease-out-soft), transform var(--duration-base) var(--ease-out-soft);`. Em motion-reduced, ambas zeram.

### 3.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo | `--color-surface` |
| Borda (`flat`) | `--color-divider` |
| Sombra (`raised`) | `--shadow-md` |
| Sombra (`raised:hover`) | `--shadow-lg` |
| Borda (`selected`) | `--color-primary` |
| Raio | `--radius-lg` (12px — chore-ux-002 §5 "card de sessão") |
| Padding | `--space-5` (default) / `--space-4` (dense) |
| Gap interno header→body→footer | `--space-4` |
| Tipografia título | `--font-sans`, `--font-weight-semibold`, `--text-lg` |
| Duração transição | `--duration-base` |

### 3.7 Acessibilidade

- `card--interactive` renderizado como `<a>` para navegação ou `<button>` para ação local. **Nunca** `<div role="button">` se houver alternativa semântica.
- Quando card contém múltiplos links/botões internos, o card **não** vira clicável inteiro (pega-um pega-todos confunde leitor de tela).
- Skeleton tem `aria-busy="true"` no container e `aria-live="polite"` no destino do swap.
- Contraste do título sobre `--color-surface`: AAA (`neutral-900` sobre branco).

### 3.8 Ilustração e pseudocódigo

ASCII (card raised, nó da trilha ativo):
```
┌──────────────────────────────────────────┐
│ Dia 3 — A escolha de Daniel    ● HOJE     │   header
│                                            │
│ Uma sessão de ~8 min sobre coragem        │   body
│ silenciosa.                                │
│                                            │
│                  ┌──────────────────────┐  │
│                  │  Começar agora →     │  │   footer
│                  └──────────────────────┘  │
└──────────────────────────────────────────┘
```

Pseudocódigo Thymeleaf (fragment):
```html
<div th:fragment="card(variant, dense)"
     th:classappend="|card card--${variant ?: 'flat'} ${dense ? 'card--dense' : ''}|">
  <header th:if="${headerSlot}" class="card-header" th:replace="${headerSlot}"></header>
  <div class="card-body" th:replace="${bodySlot}"></div>
  <footer th:if="${footerSlot}" class="card-footer" th:replace="${footerSlot}"></footer>
</div>
```
(Padrão final de "slots" via `th:fragment` admite ajustes — Codificador da primeira US que consumir define se usa slots, `th:insert` de fragments filhos, ou parâmetros texto.)

---

## 4. Modal / Sheet

### 4.1 Propósito
Sobrepor conteúdo focal sobre a tela atual exigindo decisão do usuário antes de continuar (confirmação, edição local, vinculação responsável US-046). Em mobile, vira **bottom sheet**; em tablet/desktop, modal centralizado.

### 4.2 Anatomia
```
mobile (bottom sheet):
       ┌────────────────────────────┐
       │ ▬▬                         │   handle (decisão conservadora: opcional, padrão OFF)
       │ Confirmar exclusão       × │   header (título + close)
       ├────────────────────────────┤
       │                            │
       │ Conteúdo                   │   body
       │                            │
       ├────────────────────────────┤
       │   [Cancelar]  [Excluir]    │   footer
       └────────────────────────────┘

desktop (modal centrado):
                ┌──────────────────────────────┐
                │ Confirmar exclusão         × │
                ├──────────────────────────────┤
                │ Conteúdo                     │
                ├──────────────────────────────┤
                │           [Cancelar][Excluir]│
                └──────────────────────────────┘
```
- **handle** — barra horizontal de "arrasto" no topo, decorativa. **Decisão conservadora: padrão OFF nesta versão.** Arrastar para fechar exige biblioteca de gesto que não está no stack (ADR-011 não inclui); reativável quando comportamento for validado.
- **header** — título (`--text-lg`, `--font-weight-semibold`) + botão close (`icon-only`, X de 24×24).
- **body** — conteúdo livre, com scroll interno se exceder altura máxima.
- **footer** — ações. **Primária à direita** (convenção desktop); **ações empilhadas em coluna em mobile** se houver mais de 2.
- **backdrop** — overlay semi-transparente atrás do modal.

### 4.3 Variantes

| Variante | Mobile | Tablet/Desktop |
|---|---|---|
| `default` | Bottom sheet, `max-height: 80vh`, sobe de baixo | Modal centrado, `max-width: 32rem`, fade-in com leve scale |
| `dismissible` | Pode fechar tocando no backdrop | Pode fechar clicando no backdrop |
| `critical` | **Não** fecha por backdrop (apenas via botão ou Esc) | Idem — força decisão consciente |

Sem tamanhos múltiplos. Para modais largos (ex.: heatmap em modal — não previsto no MVP), modificador `--wide` aumenta `max-width` para `48rem`.

### 4.4 Estados

| Estado | Visual |
|---|---|
| `closed` | DOM presente com `hidden` ou não renderizado (Alpine `x-show`) |
| `opening` | Animação 240ms — bottom sheet sobe `translateY(100%) → 0`; desktop modal escala `0.96 → 1` + fade |
| `open` | Backdrop opaco semi-transparente (`rgba(26, 22, 20, 0.6)` — `--color-neutral-900` translúcido), modal em posição |
| `closing` | Reverso de `opening`, 240ms |

### 4.5 Comportamento

- **Implementação Alpine:**
  ```html
  <div x-data="{ open: false }" x-cloak>
    <button @click="open = true">Abrir</button>
    <div x-show="open"
         x-trap.inert.noscroll="open"
         @keydown.escape.window="open = false"
         class="modal-overlay">
      <div role="dialog" aria-modal="true" aria-labelledby="modal-title" class="modal">
        <header class="modal-header">
          <h2 id="modal-title">Confirmar exclusão</h2>
          <button @click="open = false" aria-label="Fechar" class="modal-close">×</button>
        </header>
        …
      </div>
    </div>
  </div>
  ```
  - `x-trap` (Alpine Focus plugin) implementa **focus trap** automaticamente.
  - `.inert` aplica `inert` ao restante da página enquanto modal está aberto.
  - `.noscroll` trava scroll do body.

- **HTMX (conteúdo carregado sob demanda):**
  ```html
  <button hx-get="/modals/link-parent" hx-target="#modal-slot" hx-swap="innerHTML"
          hx-on::after-request="document.dispatchEvent(new CustomEvent('open-modal'))">
    Vincular responsável
  </button>
  <div id="modal-slot" x-data="{ open: false }" @open-modal.window="open = true">…</div>
  ```

- **Fechamento por backdrop:** apenas se `dismissible` (e nunca em `critical`). Implementado com `@click.self="open = false"` no overlay.

- **`prefers-reduced-motion`:** entrada vira fade simples sem translate/scale; duração mantida em 0.01ms via regra global.

### 4.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo do modal | `--color-surface` |
| Fundo do backdrop | `rgba(26, 22, 20, 0.6)` (derivado de `--color-neutral-900`) |
| Sombra | `--shadow-lg` |
| Raio | `--radius-xl` (20px — modal/sheet — chore-ux-002 §5) |
| Padding interno (header/body/footer) | `--space-5` |
| Tipografia título | `--font-display`, `--font-weight-semibold`, `--text-xl` |
| Z-index overlay | `--z-overlay` |
| Z-index modal | `--z-modal` |
| Duração animação | `--duration-slow` (400ms para entrada de modal — chore-ux-002 §8) |
| Easing | `--ease-out-soft` |

### 4.7 Acessibilidade — obrigatório

- `role="dialog"` no container principal do modal.
- `aria-modal="true"`.
- `aria-labelledby` apontando para o `id` do título.
- `aria-describedby` opcional, apontando para descrição/instrução.
- **Focus trap obrigatório** — Tab e Shift+Tab circulam apenas entre elementos focáveis dentro do modal (Alpine `x-trap`).
- **Foco inicial** entra no primeiro elemento focável do modal (ou no título com `tabindex="-1"` se for informacional).
- **Foco restaurado** ao botão que abriu o modal quando ele fecha.
- **Fecha com `Esc`** (sempre, inclusive em `critical` — Esc é canal universal de "cancelar").
- Botão close tem `aria-label="Fechar"`.
- Restante da página recebe `inert` enquanto modal está aberto (leitor de tela e tab navigation ficam confinados).

### 4.8 Ilustração e pseudocódigo

ASCII (bottom sheet mobile):
```
█████████████████████████████████  ← backdrop opaco
█  ┌──────────────────────────┐  █
█  │ Confirmar exclusão     × │  █
█  ├──────────────────────────┤  █
█  │ Esta ação não pode ser   │  █
█  │ desfeita.                │  █
█  ├──────────────────────────┤  █
█  │  Cancelar      Excluir   │  █
█  └──────────────────────────┘  █
```

Pseudocódigo Thymeleaf:
```html
<div th:fragment="modal(id, title, variant)"
     x-data="{ open: false }" x-cloak
     @open-modal.window="open = ($event.detail?.id === '${id}')">
  <div x-show="open"
       x-trap.inert.noscroll="open"
       @keydown.escape.window="open = false"
       th:classappend="|modal-overlay modal-overlay--${variant ?: 'default'}|"
       th:attr="data-dismissible=${variant != 'critical'}">
    <div role="dialog" aria-modal="true"
         th:attr="aria-labelledby=${id} + '-title'"
         class="modal">
      <header class="modal-header">
        <h2 th:id="${id} + '-title'" th:text="${title}"></h2>
        <button @click="open = false" aria-label="Fechar" class="modal-close">×</button>
      </header>
      <div class="modal-body" th:replace="${bodySlot}"></div>
      <footer class="modal-footer" th:if="${footerSlot}" th:replace="${footerSlot}"></footer>
    </div>
  </div>
</div>
```

---

## 5. Header (topbar)

### 5.1 Propósito
Fornecer ancoragem visual fixa no topo de cada tela: identidade (logo), contexto (título/breadcrumb) e ação contextual (avatar, notificações, menu).

### 5.2 Anatomia
```
compact (default):
┌──────────────────────────────────────────────┐
│ [logo]                            [ação]     │   altura ~56px
└──────────────────────────────────────────────┘

expanded (telas internas):
┌──────────────────────────────────────────────┐
│ [← voltar]   Esta semana          [ação]     │
│              Dia 3 de 7                       │
└──────────────────────────────────────────────┘
```
- **logo** — slot à esquerda. Em telas internas, substituído por botão "voltar" (icon-only).
- **título** (expanded) — `--text-lg`, `--font-weight-semibold`, fonte `--font-display`. Em compact, fica implícito no logo.
- **subtítulo/breadcrumb** (expanded) — `--text-sm`, `--color-text-muted`.
- **ação** — 1 a 3 elementos: avatar, ícone de notificação (com badge contador opcional), menu (`icon-only` button).

### 5.3 Variantes

| Variante | Altura | Conteúdo |
|---|---|---|
| `compact` | ~56px (`--space-14` — derivado de `--space-11` + padding) | Logo + ação. Default em todas as telas. |
| `expanded` | ~80px | Botão voltar + título + subtítulo + ação. Telas internas com hierarquia clara. |

### 5.4 Estados

| Estado | Visual |
|---|---|
| `default` | Fundo `--color-surface`, sem sombra |
| `scrolled` | `box-shadow: var(--shadow-sm)` aparece quando `scrollY > 8px` (Alpine listener) |
| `transparent` (opcional — landing) | Fundo `transparent`, herda do conteúdo abaixo. **Não usado no MVP** — registrado como hook futuro. |

### 5.5 Comportamento

- **Sticky:** `position: sticky; top: 0; z-index: var(--z-sticky)`. Permanece visível durante scroll.
- **Sombra dinâmica:**
  ```html
  <header x-data="{ scrolled: false }"
          @scroll.window="scrolled = window.scrollY > 8"
          :class="{ 'header--scrolled': scrolled }"
          class="header">…</header>
  ```
- **Transição da sombra:** `transition: box-shadow var(--duration-fast) var(--ease-out-soft)`.
- **Logo:** o arquivo final do logo é definido em chore-ux-001 §3 (`brand-mark` SVG + wordmark "atrilha"). Aqui o header apenas reserva o slot.
- **`prefers-reduced-motion`:** sombra ainda aparece (não é movimento), mas qualquer transição extra zera.

### 5.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo | `--color-surface` |
| Sombra (scrolled) | `--shadow-sm` |
| Z-index | `--z-sticky` |
| Padding-x | `--space-4` |
| Altura `compact` | `--space-14` (calculado: 56px) — usar `min-height` |
| Altura `expanded` | `--space-20` (calculado: 80px) — usar `min-height` |
| Tipografia título | `--font-display`, `--font-weight-semibold`, `--text-lg` |
| Tipografia subtítulo | `--font-sans`, `--text-sm`, `--color-text-muted` |
| Duração | `--duration-fast` |

**Nota sobre `--space-14`:** não está declarado em chore-ux-002 §4 (a escala pula de 12 para 16). Decisão conservadora: usar **`--space-12 + --space-2`** via Tailwind utilities (`h-14` = 56px no Tailwind padrão = 14 × 4px = 56) — Tailwind v4 gera escalas intermediárias automaticamente a partir de `--spacing`. Sem necessidade de criar token primitivo novo.

### 5.7 Acessibilidade

- Renderizado como `<header role="banner">` (papel ARIA implícito por `<header>` no nível raiz).
- Logo é `<a href="/" aria-label="atrilha — página inicial">` (chore-ux-001 §3.3).
- Botão voltar tem `aria-label="Voltar"`.
- Badge de notificação tem `aria-label` descritivo: `aria-label="3 novas notificações"`.
- Ordem de foco esquerda → direita.

### 5.8 Ilustração e pseudocódigo

ASCII:
```
compact:
┌──────────────────────────────────────────────┐
│ ▣ atrilha                       ⋯     ◉      │   logo · menu · avatar
└──────────────────────────────────────────────┘

expanded:
┌──────────────────────────────────────────────┐
│ ←   Esta semana                       ⋯       │
│     Dia 3 de 7 — A escolha de Daniel          │
└──────────────────────────────────────────────┘
```

Pseudocódigo Thymeleaf:
```html
<header th:fragment="header(variant, title, subtitle, showBack)"
        x-data="{ scrolled: false }"
        @scroll.window="scrolled = window.scrollY > 8"
        :class="{ 'header--scrolled': scrolled }"
        th:classappend="|header header--${variant ?: 'compact'}|">
  <div class="header-leading">
    <a th:if="${!showBack}" href="/" aria-label="atrilha — página inicial"
       th:replace="~{components/brand :: brand}"></a>
    <button th:if="${showBack}" aria-label="Voltar" class="btn btn-ghost btn-icon"
            hx-get="javascript:history.back()">←</button>
  </div>
  <div th:if="${variant == 'expanded'}" class="header-center">
    <h1 class="header-title" th:text="${title}"></h1>
    <p th:if="${subtitle != null}" class="header-subtitle" th:text="${subtitle}"></p>
  </div>
  <nav class="header-trailing" aria-label="Ações da página">
    <slot th:replace="${actionsSlot}"></slot>
  </nav>
</header>
```

---

## 6. Navigation (bottom-nav + nav-link)

### 6.1 Propósito
Permitir navegação entre as áreas principais do app diretamente do polegar do usuário (P12 — mobile-first, chore-ux-001 §5.5 — densidade decresce do topo para a base). É a navegação primária do app instalado.

### 6.2 Anatomia
```
mobile (bottom-nav):
┌────────────────────────────────────────────────┐
│                                                  │   ← área de conteúdo
│                                                  │
├────────────────────────────────────────────────┤
│  ◉      ⌂       👤      ⚙                       │   ← bottom-nav (altura ~64px)
│  Trilha  Hoje   Perfil  Ajustes                 │
└────────────────────────────────────────────────┘
              ↑
       safe-area iOS
```
- **bottom-nav** — container `<nav aria-label="Principal">` fixo no rodapé.
- **nav-link** — item individual: ícone (SVG stroke 24×24) + label (`--text-xs`, `--font-weight-medium`). Empilhamento vertical.
- **safe-area** — padding inferior dinâmico (`padding-bottom: env(safe-area-inset-bottom)`) para acomodar barra inferior de iOS.

### 6.3 Variantes

| Variante | Visual | Uso |
|---|---|---|
| `bottom-nav` | Fixo no rodapé, 3–5 itens, full-width | Default mobile |
| `top-nav-desktop` | Linha horizontal em header expandido (chore-ux-001 §5: "desktop é cortesia") | Override desktop opcional. Default desktop: **ocultar bottom-nav** e expor itens no header expanded. Decisão conservadora — uma única superfície de navegação por vez. |

**Itens MVP (informativo — não fechado nesta task):** Trilha · Hoje · Perfil · Ajustes (3–4 itens). Painel do responsável (US-043) vive em rota própria fora da bottom-nav, pois usuários têm perfil distinto. Decisões finais ficam nas US correspondentes.

### 6.4 Estados (por `nav-link`)

| Estado | Visual |
|---|---|
| `inactive` | Ícone `--color-text-muted`, label `--color-text-muted` |
| `active` | Ícone `--color-primary`, label `--color-primary`, label `--font-weight-semibold`. **`aria-current="page"`** |
| `focus-visible` | Anel `--shadow-focus` ao redor do item inteiro |
| `hover` (desktop) | Ícone e label `--color-text-body` |
| `pressed` (mobile) | Leve compressão visual (sem transform — apenas opacity 0.85 momentâneo) |

**Redundância de canal (chore-ux-001 §5.3):** estado ativo combina **cor + peso tipográfico + `aria-current`**. Daltônicos e leitores de tela recebem o sinal mesmo sem cor.

### 6.5 Comportamento

- Cada `nav-link` é `<a href="/rota">`. Navegação padrão de browser; HTMX opcional via `hx-boost="true"` no `<body>` ou no `<nav>` para transições sem reload.
- **`aria-current="page"`** aplicado server-side (Thymeleaf condicional baseado na rota atual).
- **Reload preserva foco:** ao trocar de aba via clique, o foco vai para o `<main>` da nova página (decisão de acessibilidade — ver chore-ux-007 ou US futura).
- **Sem animação de troca de aba neste catálogo** — transições entre páginas ficam para uma US dedicada se houver demanda. Decisão conservadora: simplicidade.
- **`prefers-reduced-motion`:** N/A — não há animação prevista aqui.

### 6.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo bottom-nav | `--color-surface` |
| Borda superior bottom-nav | `--color-divider` (1px solid) |
| Ícone/label inativo | `--color-text-muted` |
| Ícone/label ativo | `--color-primary` |
| Anel foco | `--shadow-focus` |
| Z-index | `--z-sticky` |
| Altura | 64px (Tailwind `h-16` = 4rem) + safe-area-inset-bottom |
| Padding-y de item | `--space-2` |
| `min-height` item | `--space-11` (touch target) |
| Tipografia label | `--font-sans`, `--text-xs`, `--font-weight-medium` (inativo) / `--font-weight-semibold` (ativo) |
| Gap ícone→label | `--space-1` |

### 6.7 Acessibilidade

- `<nav aria-label="Principal">` no container.
- Cada item é `<a>` com texto (label visível) — **nunca** apenas ícone sem label, mesmo em densidade alta. Itens com label cobrem leitor de tela e dão clareza.
- Item ativo: `aria-current="page"`.
- Touch target ≥ 44×44 (largura calculada pela divisão da bottom-nav entre 3–5 itens em viewport ≥ 320px sempre passa; altura garantida por `min-height: var(--space-11)` + padding).
- Em iOS, o `padding-bottom: env(safe-area-inset-bottom)` garante que os itens não fiquem sob o gesture bar (não-funcional → não-clicável é falha de acessibilidade).
- Ordem de foco esquerda → direita; primeiro item recebe foco quando entra com Tab.

### 6.8 Ilustração e pseudocódigo

ASCII (4 itens, "Hoje" ativo):
```
┌──────────────────────────────────────────────────┐
│   ◉           ⌂           👤           ⚙         │
│  Trilha       Hoje        Perfil      Ajustes    │
│              ───── coral, semibold, aria-current │
└──────────────────────────────────────────────────┘
                    ▔▔▔▔▔  safe-area
```

Pseudocódigo Thymeleaf (bottom-nav + nav-link como fragments separados):
```html
<!-- bottom-nav -->
<nav th:fragment="bottomNav(items, currentPath)"
     aria-label="Principal" class="bottom-nav">
  <ul class="bottom-nav-list">
    <li th:each="item : ${items}">
      <a th:replace="~{components/nav-link :: navLink(item=${item}, current=${item.path == currentPath})}"></a>
    </li>
  </ul>
</nav>

<!-- nav-link -->
<a th:fragment="navLink(item, current)"
   th:href="${item.path}"
   th:attr="aria-current=${current ? 'page' : null}"
   th:classappend="|nav-link ${current ? 'nav-link--active' : ''}|">
  <span class="nav-link-icon" aria-hidden="true" th:utext="${item.icon}"></span>
  <span class="nav-link-label" th:text="${item.label}"></span>
</a>
```

---

## 7. Badge

### 7.1 Propósito
Indicar estado, categoria ou contagem em um espaço visual mínimo. Usado para: estado de nó na trilha ("hoje", "concluído", "bloqueado"), marcador "privado" no campo de reflexão (US-024), contador em ícones (notificações no header).

### 7.2 Anatomia
```
[ icon? | label | count? ]
```
- **icon** (opcional) — SVG stroke 12×12, à esquerda.
- **label** — texto pt-BR curto (1–2 palavras), maiúsculas ou capitalizado, `--text-xs`, `--font-weight-semibold`.
- **count** — número (badge contador), substitui label em variantes numéricas.

### 7.3 Variantes

| Variante | Fundo | Texto | Borda | Uso |
|---|---|---|---|---|
| `neutral` | `--color-neutral-100` | `--color-neutral-700` | nenhuma | Categoria neutra, tag |
| `primary` | `--color-primary-100` | `--color-primary-700` | nenhuma | "HOJE" no dia ativo da trilha |
| `success` | `--color-success-100` | `--color-success-700` | nenhuma | "Concluído", "Salvo" |
| `warning` | `--color-warning-100` | `--color-warning-700` | nenhuma | "Expira em 2 dias" |
| `danger` | `--color-danger-100` | `--color-danger-700` | nenhuma | "Bloqueado", "Erro" |
| `info` | `--color-info-100` | `--color-info-700` | nenhuma | "Novo", "Beta" |

Tamanhos:

| Size | Altura | Padding-x | Uso |
|---|---|---|---|
| `sm` | 18px | `--space-2` | Inline em texto corrido, contador em ícone |
| `md` | 24px | `--space-3` | Default — chip de estado |

Variante adicional **`dot`**: ponto colorido de 8×8 sem texto (`primary`, `success`, `danger`), usado como indicador discreto em avatares ou ícones. **`aria-label` obrigatório** descrevendo significado.

### 7.4 Estados

Badge é estático — sem hover/focus/active próprios. Quando vira clicável (filtro removível, por exemplo), promove-se a botão (variante `chip` futura — não nesta task).

| Estado | Quando |
|---|---|
| `default` | Único estado |

### 7.5 Comportamento

- **Estático.** Não tem interação.
- **HTMX:** o servidor pode trocar a variante do badge re-renderizando o fragment (ex.: nó "em progresso" vira "concluído" após sessão completa).
- **`prefers-reduced-motion`:** N/A.

### 7.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo (varia por variante) | `--color-{variant}-100` |
| Texto (varia por variante) | `--color-{variant}-700` |
| Raio | `--radius-full` (pill — chore-ux-002 §5) |
| Padding-x `md` | `--space-3` |
| Padding-x `sm` | `--space-2` |
| Tipografia | `--font-sans`, `--font-weight-semibold`, `--text-xs` |
| Letter-spacing (uppercase) | `--tracking-overline` (opcional, se label for em CAPS) |
| Gap ícone→label | `--space-1` |

### 7.7 Acessibilidade

- Renderizado como `<span>` (inline) — não-interativo, sem necessidade de role.
- Quando o significado da badge for crítico para entender o conteúdo adjacente, garantir que o leitor de tela receba a informação: ou via label visível em pt-BR, ou via `aria-label` no elemento pai (ex.: nó da trilha tem `aria-label="Dia 3, em progresso"` e a badge "EM PROGRESSO" é visualmente redundante).
- Contraste validado: pares `100/700` de cada cor passam AA (chore-ux-001 §2 — contrastes pareados).
- Variante `dot` **obriga** `aria-label`.

### 7.8 Ilustração e pseudocódigo

ASCII:
```
[ HOJE ]            ← primary, md, label uppercase + tracking-overline
[ Concluído ]       ← success, md
[ ⚠ Expira ]        ← warning, md, com ícone
[ Privado ]         ← neutral, sm — usado no campo de reflexão (US-024)
[ 3 ]               ← primary, sm, contador no header
●                   ← dot variant, primary, aria-label="3 novas notificações"
```

Pseudocódigo Thymeleaf:
```html
<span th:fragment="badge(variant, size, label, icon, ariaLabel)"
      th:classappend="|badge badge--${variant ?: 'neutral'} badge--${size ?: 'md'}|"
      th:attr="aria-label=${ariaLabel}">
  <span th:if="${icon != null}" class="badge-icon" aria-hidden="true" th:utext="${icon}"></span>
  <span th:if="${label != null}" th:text="${label}"></span>
</span>
```

---

## 8. Toast (notificação flutuante)

### 8.1 Propósito
Comunicar resultado de uma ação (sucesso/erro/info/aviso) **sem interromper o fluxo** do usuário. Aparece momentaneamente, desaparece sozinho, não exige interação.

### 8.2 Anatomia
```
┌────────────────────────────────────────┐
│ ✓  Sessão salva                    ×   │
│    Volte amanhã para o dia 4.          │
└────────────────────────────────────────┘
  ▲          ▲              ▲       ▲
  icon       title          message close
```
- **icon** — SVG stroke 20×20, indicador semântico (✓ success, ✕ error, ⚠ warning, ⓘ info).
- **title** — texto curto, `--text-base`, `--font-weight-semibold`.
- **message** (opcional) — detalhe, `--text-sm`, `--color-text-muted`.
- **close** — botão `icon-only` X (opcional — toasts curtos podem dispensar). Sempre presente em variantes `error`/`warning` (usuário pode querer reter a mensagem).

### 8.3 Variantes

| Variante | Fundo | Borda esquerda | Ícone | ARIA |
|---|---|---|---|---|
| `success` | `--color-success-100` | `--color-success-700` (4px) | ✓ `--color-success-700` | `aria-live="polite"` |
| `info` | `--color-info-100` | `--color-info-700` (4px) | ⓘ `--color-info-700` | `aria-live="polite"` |
| `warning` | `--color-warning-100` | `--color-warning-700` (4px) | ⚠ `--color-warning-700` | `role="alert"` |
| `error` | `--color-danger-100` | `--color-danger-700` (4px) | ✕ `--color-danger-700` | `role="alert"` |

**Distinção `aria-live="polite"` vs `role="alert"`:**
- `polite`: leitor de tela anuncia quando estiver "livre" — não interrompe outro speech em andamento. Adequado para `success`/`info` — informativos, não urgentes.
- `alert`: equivale a `aria-live="assertive"` + `role="alert"` — interrompe e anuncia imediatamente. Adequado para `error`/`warning` — usuário precisa saber agora.

### 8.4 Estados

| Estado | Visual |
|---|---|
| `entering` | Slide-in lateral (do topo, 200px → 0) + fade. Duração `--duration-base`. |
| `visible` | Em repouso, no canto |
| `auto-dismissing` | Após `4s` (success/info) ou `6s` (warning/error), fade-out 200ms |
| `manually-dismissed` | Usuário clicou em close → fade-out imediato (120ms) |

Posicionamento:

| Viewport | Posição |
|---|---|
| Mobile | **Topo**, abaixo do header, com `top: calc(56px + var(--space-3))`. Não no rodapé — colidiria com bottom-nav e com o polegar (zona de toque acidental). |
| Tablet/Desktop | Canto superior direito, `top: var(--space-6); right: var(--space-6)`. |

**Decisão conservadora — topo em mobile:** Issue deixou em aberto ("topo ou rodapé acima do bottom-nav"). Topo escolhido porque (a) não compete com bottom-nav, (b) não fica sob o polegar do usuário, (c) é padrão em apps modernos que mais influenciam o público (Duolingo, Threads). Reversível sem mudar tokens.

### 8.5 Comportamento

- **HTMX (toast via servidor):** padrão `HX-Trigger` no response:
  ```http
  HTTP/1.1 200 OK
  HX-Trigger: {"toast": {"variant": "success", "title": "Sessão salva", "message": "Volte amanhã."}}
  ```
  Listener global Alpine (em `<body x-data="toastQueue()">`) captura o evento, adiciona à fila, renderiza:
  ```html
  <body x-data="{ toasts: [] }"
        @toast.window="toasts.push({...$event.detail, id: Date.now()})">
    <div class="toast-region" role="status">
      <template x-for="t in toasts" :key="t.id">
        <div :class="`toast toast--${t.variant}`"
             :role="['error','warning'].includes(t.variant) ? 'alert' : 'status'"
             :aria-live="['error','warning'].includes(t.variant) ? 'assertive' : 'polite'"
             x-init="setTimeout(() => toasts = toasts.filter(x => x.id !== t.id), ['error','warning'].includes(t.variant) ? 6000 : 4000)">…</div>
      </template>
    </div>
  </body>
  ```

- **Foco NÃO muda automaticamente para o toast** — decisão de acessibilidade: interromper o foco do usuário é mais invasivo do que o anúncio assertivo do leitor de tela. Usuário decide se quer interagir.

- **Múltiplos toasts:** empilham verticalmente, mais recente em cima. Máximo 3 visíveis ao mesmo tempo; excedentes ficam na fila.

- **`prefers-reduced-motion`:** entrada vira `opacity: 0 → 1` sem slide; saída idem.

### 8.6 Tokens consumidos

| Aspecto | Token consumido |
|---|---|
| Fundo (varia) | `--color-{variant}-100` |
| Borda esquerda (varia) | `--color-{variant}-700` (4px solid) |
| Texto título | `--color-text-display` |
| Texto mensagem | `--color-text-muted` |
| Sombra | `--shadow-md` |
| Raio | `--radius-lg` |
| Padding | `--space-4` |
| Gap interno (icon→texto, texto→close) | `--space-3` |
| Tipografia título | `--font-sans`, `--font-weight-semibold`, `--text-base` |
| Tipografia mensagem | `--font-sans`, `--text-sm` |
| Z-index | `--z-toast` |
| Duração entrada | `--duration-base` |
| Duração saída auto | 200ms (deriva — não cria token novo) |
| Easing | `--ease-out-soft` |
| `max-width` | `28rem` (não cria token — limite de leitura confortável) |

### 8.7 Acessibilidade

- Container do toast region renderizado com `aria-live` apropriado por variante (ver §8.3).
- Toasts não devem ser a **única** forma de comunicar erro crítico (P5/P8 — usuário pode ter perdido o toast). Erros que bloqueiam ação ficam também na UI persistente (mensagem inline, modal de confirmação).
- Botão close tem `aria-label="Fechar notificação"`.
- Toast `error`/`warning` não tem auto-dismiss tão curto que o usuário não consiga ler (mínimo 6s; pode-se aumentar conforme leitura).
- Contraste validado: pares `100/700` passam AA.

### 8.8 Ilustração e pseudocódigo

ASCII (toast success, mobile, topo):
```
═══════════════════════════════════════════════
│ HEADER (compact, 56px)                       │
═══════════════════════════════════════════════
                                                 ▲
   ┌──────────────────────────────────┐         │
   ┃ ✓  Sessão salva               ×  ┃         │  toast entra
   ┃    Volte amanhã para o dia 4.    ┃          ↓
   └──────────────────────────────────┘
```

Pseudocódigo Thymeleaf (apenas a região de toasts — disparo é via HX-Trigger):
```html
<div th:fragment="toastRegion"
     x-data="{ toasts: [] }"
     @toast.window="toasts.push({...$event.detail, id: Date.now()})"
     class="toast-region" aria-label="Notificações">
  <template x-for="t in toasts" :key="t.id">
    <div :class="`toast toast--${t.variant}`"
         :role="['error','warning'].includes(t.variant) ? 'alert' : 'status'"
         :aria-live="['error','warning'].includes(t.variant) ? 'assertive' : 'polite'"
         x-init="setTimeout(() => toasts = toasts.filter(x => x.id !== t.id), ['error','warning'].includes(t.variant) ? 6000 : 4000)">
      <span class="toast-icon" aria-hidden="true" x-html="icons[t.variant]"></span>
      <div class="toast-body">
        <p class="toast-title" x-text="t.title"></p>
        <p class="toast-message" x-show="t.message" x-text="t.message"></p>
      </div>
      <button class="toast-close" aria-label="Fechar notificação"
              @click="toasts = toasts.filter(x => x.id !== t.id)">×</button>
    </div>
  </template>
</div>
```

---

## 9. Tokens de componente

Tokens novos a serem **adicionados** ao `@theme` da chore-ux-002 (Seção 10 — bloco copiável). Cada token referencia primitivo/semântico já declarado; **nenhum hex novo** é introduzido aqui. Convenção: `--<componente>-<propriedade>[-<modificador>]` (chore-ux-002 §11.1).

| Token | Valor (referência) | Componente |
|---|---|---|
| `--button-primary-bg` | `var(--color-primary)` | Button (`primary`) |
| `--button-primary-bg-hover` | `var(--color-primary-hover)` | Button (`primary`) |
| `--button-primary-bg-active` | `var(--color-primary-active)` | Button (`primary`) |
| `--button-primary-text` | `var(--color-on-primary)` | Button (`primary`) |
| `--button-radius` | `var(--radius-full)` | Button (todas variantes não icon-only) |
| `--button-padding-x-md` | `var(--space-4)` | Button (`md`) |
| `--button-padding-x-lg` | `var(--space-6)` | Button (`lg`) |
| `--button-min-height-md` | `var(--space-11)` | Button (`md`) |
| `--input-bg` | `var(--color-surface)` | Input |
| `--input-border` | `var(--color-border)` | Input |
| `--input-border-focus` | `var(--color-focus-ring)` | Input |
| `--input-radius` | `var(--radius-md)` | Input |
| `--input-padding-x` | `var(--space-4)` | Input |
| `--input-min-height` | `var(--space-11)` | Input |
| `--card-bg` | `var(--color-surface)` | Card |
| `--card-radius` | `var(--radius-lg)` | Card |
| `--card-padding` | `var(--space-5)` | Card (default) |
| `--card-padding-dense` | `var(--space-4)` | Card (`--dense`) |
| `--card-shadow-raised` | `var(--shadow-md)` | Card (`raised`) |
| `--card-shadow-raised-hover` | `var(--shadow-lg)` | Card (`raised:hover`) |
| `--modal-bg` | `var(--color-surface)` | Modal |
| `--modal-radius` | `var(--radius-xl)` | Modal |
| `--modal-shadow` | `var(--shadow-lg)` | Modal |
| `--modal-backdrop` | `rgba(26, 22, 20, 0.6)` (deriva de `--color-neutral-900`) | Modal (backdrop) |
| `--modal-padding` | `var(--space-5)` | Modal |
| `--header-bg` | `var(--color-surface)` | Header |
| `--header-shadow-scrolled` | `var(--shadow-sm)` | Header (`scrolled`) |
| `--header-padding-x` | `var(--space-4)` | Header |
| `--bottom-nav-bg` | `var(--color-surface)` | Navigation (bottom-nav) |
| `--bottom-nav-border-top` | `var(--color-divider)` | Navigation (bottom-nav) |
| `--nav-link-color-inactive` | `var(--color-text-muted)` | Navigation (nav-link) |
| `--nav-link-color-active` | `var(--color-primary)` | Navigation (nav-link) |
| `--badge-radius` | `var(--radius-full)` | Badge |
| `--badge-padding-x-md` | `var(--space-3)` | Badge (`md`) |
| `--badge-padding-x-sm` | `var(--space-2)` | Badge (`sm`) |
| `--toast-radius` | `var(--radius-lg)` | Toast |
| `--toast-shadow` | `var(--shadow-md)` | Toast |
| `--toast-padding` | `var(--space-4)` | Toast |

**Total: 38 tokens de componente.** Excede o mínimo de 12 exigido pela Issue (§ critérios de aceitação). Todos consomem primitivos/semânticos declarados em `doc/UX/01-design-tokens.md`.

A escolha do **Codificador** é se aplica esses tokens como custom properties dentro do `@theme` (visíveis em DevTools) ou se traduz diretamente em classes Tailwind (sem variável intermediária). Recomendação: **aplicar como CSS custom properties no `@theme`** para manter rastreabilidade e permitir tema escuro futuro (chore-ux-002 §11.2) sem reescrever componentes.

---

## 10. Estilo de implementação Thymeleaf

Diretrizes para o Codificador. **Implementação real fica fora desta task** — esta seção define o **contrato**.

### 10.1 Localização

Cada componente vira **1 arquivo de template** em:
```
src/main/resources/templates/components/
├── button.html
├── input.html
├── card.html
├── modal.html
├── header.html
├── nav-link.html
├── bottom-nav.html
├── badge.html
├── toast-region.html
└── brand.html        ← logo (chore-ux-001 §3, fragment já implícito)
```

### 10.2 Estrutura do fragment

- Cada arquivo expõe **1 fragment principal** com o nome do componente:
  ```html
  <element th:fragment="<componente>(parametros...)" ...>…</element>
  ```
- Componentes complexos (modal, card com slots) podem usar `~{...}` para slots ou parâmetros adicionais.

### 10.3 Parâmetros

- Sempre **default explícito** via Elvis: `${variant ?: 'primary'}`. Nunca confiar em `null` chegar até o template sem fallback.
- Parâmetros opcionais documentados no início do fragment como comentário Thymeleaf:
  ```html
  <!--
    Fragment: button
    Parâmetros:
      - variant: 'primary' | 'secondary' | 'ghost' | 'destructive' (default: 'primary')
      - size: 'sm' | 'md' | 'lg' (default: 'md')
      - label: String — texto visível
      - type: 'button' | 'submit' (default: 'button')
      - ariaLabel: String? — obrigatório em icon-only
  -->
  ```

### 10.4 Variantes via classes

- Variantes aplicadas como **classes adicionais**, não como atributos `style`:
  ```html
  <button th:classappend="|btn btn--${variant} btn--${size}|">…</button>
  ```
- Tailwind v4 + tokens em `@theme` garantem que cada classe gere o CSS correto a partir dos tokens de componente da §9.

### 10.5 Interatividade

- **HTMX** para tudo que envolve servidor (loading de botão, swap de erro de input, toast via `HX-Trigger`, abertura de modal carregando conteúdo).
- **Alpine** para estado **puramente client** (toggle de modal, contador de caracteres em textarea, dismiss de toast manual).
- **JavaScript inline (`<script>` no template)** apenas se absolutamente necessário (ex.: registro de listener global de `toast` event). Preferir CSS + Alpine.
- **CSS** vai inteiramente para o stylesheet principal (consumindo tokens). Não usar `<style>` inline em fragments.

### 10.6 Reutilização

- Fragments compõem entre si via `th:replace="~{components/<arquivo> :: <fragment>(...)}"`.
- Composição típica:
  - Modal usa Button no footer.
  - Card pode conter Badge no header.
  - Header usa Button (`icon-only`) e brand.
  - Bottom-nav usa nav-link.

### 10.7 Validação inicial

Quando a primeira US consumir um componente, o Codificador:
1. Cria o arquivo do fragment.
2. Aplica os tokens de componente (§9) no `@theme`.
3. Adiciona test snapshot ou test de renderização do fragment isolado.
4. Documenta na PR qual US consumiu pela primeira vez (rastreabilidade).

**Catálogo de componentes (Storybook ou similar)** **não** está no escopo do MVP — fica como recomendação futura quando o time crescer.

---

## 11. Decisões implícitas registradas

Pontos onde a Issue deixou margem e este doc escolheu a opção conservadora. Cada decisão é reversível sem mexer em tokens.

| # | Ponto | Decisão | Justificativa |
|---|---|---|---|
| 1 | Handle de arrastar no bottom sheet | **OFF por padrão** | Arrastar para fechar exige biblioteca de gesto fora do stack (ADR-011). Reativável quando houver demanda. |
| 2 | Posição do toast em mobile | **Topo** (abaixo do header) | Não compete com bottom-nav, fora da zona de polegar, padrão em apps de referência (Duolingo, Threads). |
| 3 | Navegação em desktop | **Bottom-nav oculto; itens migram para header expanded** | Uma única superfície de navegação por vez. Desktop é cortesia (chore-ux-001 §5; P12). |
| 4 | Animação de troca de aba | **Não há** | Simplicidade. US futura pode adicionar se houver demanda explícita. |
| 5 | Foco automático para toast | **Não muda foco** | Interromper foco do usuário é mais invasivo que anúncio do leitor de tela. `role="alert"` cobre urgência. |
| 6 | `--space-14` (header compact = 56px) | **Não cria token primitivo novo** | Tailwind v4 gera `h-14` (= 14 × 4px = 56px) a partir do `--spacing` raiz. Sem necessidade de novo token. |
| 7 | Slots em Card / Modal (Thymeleaf) | **Padrão fica em aberto para o Codificador** | `th:replace="${slot}"` ou `th:fragment` filhos — ambos válidos em Thymeleaf. Primeira US que consumir escolhe e documenta. |
| 8 | Catálogo visual (Storybook) | **Fora do MVP** | Esforço alto, ROI baixo para time pequeno. Registrado como recomendação futura. |
| 9 | Toast botão close em success/info | **Opcional** (toasts curtos podem dispensar) | Auto-dismiss 4s cobre o caso. Botão close mandatório em error/warning (6s pode não bastar). |
| 10 | Dark theme em componentes | **Fora do escopo** | chore-ux-002 §11.2 já documenta o plano (futura `@theme` com `prefers-color-scheme: dark`). Componentes serão reaproveitados. |

---

## 12. Pendências e o que NÃO está aqui

- **Implementação dos fragments** (`templates/components/*.html`) — fica para o Codificador na primeira US que consumir cada componente. Esta task entrega só o catálogo.
- **CSS de produção** — Codificador escreve consumindo tokens da chore-ux-002 + tokens de componente da §9 deste doc.
- **Catálogo visual interativo (Storybook)** — fora do MVP (§11 decisão 8).
- **Ilustrações editoriais para estados vazios** — depende do estilo concreto (chore-ux-001 §7, dúvida aberta). Não bloqueia componentes-base.
- **Componentes adicionais que vão emergir**: progress bar (US-023), heatmap cell (US-041), chip removível (filtros), avatar com fallback iniciais, tooltip — nascem com cada US que precisar, seguindo a mesma convenção.
- **Microcopy final dos componentes** (mensagens, labels) — definida em cada US específica. Aqui só ilustramos exemplos (US-024 "privado", "Encerrar sessão" etc.).
- **Internacionalização** — copy em pt-BR direto nos templates por enquanto. i18n fica para v2.

---

## 13. Referências cruzadas

- **Identidade visual:** `doc/UX/00-identidade-visual.md` (chore-ux-001).
- **Tokens primitivos e semânticos:** `doc/UX/01-design-tokens.md` (chore-ux-002).
- **Acessibilidade:** PRD §8.4 — RNF-A11Y-01..05 (em especial RNF-A11Y-05 touch target 44×44).
- **Responsividade:** PRD §8.4 — RNF-COMP-04 (320px → 1920px).
- **US-024 (reflexão privada):** PRD §13 — input textarea com contador e badge "privado".
- **US-041 (heatmap):** PRD §13 — não cria componente novo aqui, mas usa Card + cor de heatmap (tokens em chore-ux-002 §2.5).
- **Stack:** ADR-011 (PRD §16) — Thymeleaf + HTMX + Tailwind v4 + Alpine + Lottie.
- **Direção visual:** ADR-013 (PRD §16) — claro, jovem, colorido. P14, P15 (PRD §4.3).
- **Workflow:** `doc/workflow.md` §3.10.2 — template UX spec.
- **Stack visual existente:** `AGENTS.md` §12 — animações fadeUp + scroll reveal já presentes no site institucional. Compatibilidade documentada em chore-ux-002 §8; componentes deste doc não dependem dessas animações.
