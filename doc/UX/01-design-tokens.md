# Design Tokens — atrilha

**Código:** chore-ux-002
**GitHub Issue:** #21
**Status:** Proposto
**Depende de:** `doc/UX/00-identidade-visual.md` (chore-ux-001, aprovada por Dioni em 2026-05-18)
**Implementação:** Tailwind v4 (`@theme` no CSS principal — não `tailwind.config.js` da v3)

Este documento traduz a identidade visual de `doc/UX/00-identidade-visual.md` em **tokens implementáveis**. Toda decisão aqui rastreia até uma seção daquele documento; **nada é inventado**. Tokens fora do escopo (componentes) estão listados na §11 e serão definidos na chore-ux-003.

---

## 1. Convenções de nomenclatura

| Camada | Convenção | Exemplo |
|---|---|---|
| CSS custom property (no `@theme`) | inglês, kebab-case, prefixada por domínio (`color-`, `font-`, `text-`, `space-`, `radius-`, `shadow-`, `duration-`, `ease-`, `z-`) | `--color-primary-500` |
| Classes Tailwind geradas | inglês, kebab-case (decorrência da v4) | `bg-primary-500`, `text-display`, `rounded-lg` |
| Escala numerada (primitivos) | sufixo `-NN` ou `-NNN` seguindo a convenção Tailwind (50, 100, 200, …, 900) | `--color-primary-700` |
| Tokens semânticos | sem número, descreve o **papel** | `--color-primary`, `--color-text-body`, `--color-focus-ring` |
| Tokens de estado (futuros, componentes) | composição com sufixo `-hover`, `-active`, `-disabled` | `--color-primary-hover` |

**Regras invioláveis:**

- Idioma sempre inglês — alinhamento com Tailwind/CSS standard. Documentação e copy seguem pt-BR; tokens não.
- Tokens semânticos **referenciam** primitivos via `var(--…)`; nunca duplicam hex.
- Tokens de componente (chore-ux-003) seguirão o padrão `--<componente>-<propriedade>-<modificador>` (ex.: `--button-primary-bg-hover`). Aqui ficam só registrados — não implementamos componentes nesta task.

---

## 2. Tokens de cor

Os primitivos das três escalas centrais (primary, secondary, neutral) derivam **literalmente** da paleta da chore-ux-001 (§2.1, §2.2, §2.5). Tons intermediários ausentes naquele documento foram interpolados preservando o matiz declarado (coral quente, lime fresco, cinza-quente puxado para coral).

### 2.1 Primary (coral — cor de marca)

Origem: `doc/UX/00-identidade-visual.md` §2.1. Contrastes já validados naquele documento.

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-primary-50` | `#FFF1F0` | Fundo de seção em destaque, hover muito leve |
| `--color-primary-100` | `#FFD9D6` | Badges sutis, pill de tag |
| `--color-primary-200` | `#FFBDB7` | Estado de borda em foco suave, ilustração |
| `--color-primary-300` | `#FF9A92` | Ilustrações, ícone decorativo |
| `--color-primary-400` | `#FA7B71` | Variante leve de CTA em fundo claro, ênfase ilustrativa |
| `--color-primary-500` | `#F25C54` | **Cor de marca.** Logo, CTA primário, dia ativo da trilha |
| `--color-primary-600` | `#D94A43` | Hover/pressed de CTA primário |
| `--color-primary-700` | `#A8362F` | Texto sobre `primary-50`, ícone em estado ativo |
| `--color-primary-800` | `#7C2823` | Texto crítico sobre fundo `primary-100` |
| `--color-primary-900` | `#501914` | Reservado — alto contraste em fundo coral muito claro |

### 2.2 Secondary (lime — cor de progresso)

Origem: `doc/UX/00-identidade-visual.md` §2.2. Interpolação preserva os tons declarados nas tabelas 50/300/500/700.

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-secondary-50` | `#F2FBE7` | Fundo de card "concluído" |
| `--color-secondary-100` | `#E2F4CB` | Highlight leve de sucesso |
| `--color-secondary-200` | `#D3EDAE` | Estado de hover de progresso |
| `--color-secondary-300` | `#BEEB6A` | Preenchimento de barra de progresso |
| `--color-secondary-400` | `#A1D848` | Estado intermediário de progresso |
| `--color-secondary-500` | `#7BC42F` | Selo de sessão completa, streak indicador (≥18px bold ou ≥24px gráfico — AA Large) |
| `--color-secondary-600` | `#5DA31F` | Hover de elemento gráfico de progresso |
| `--color-secondary-700` | `#3F7A0F` | Texto "Sessão concluída" sobre `secondary-50` |
| `--color-secondary-800` | `#2D5A0A` | Texto sobre fundo claro com alta hierarquia |
| `--color-secondary-900` | `#1E3D06` | Reservado — alto contraste |

### 2.3 Neutral (ink — cinza-quente puxado para coral)

Origem: `doc/UX/00-identidade-visual.md` §2.5. Renumerado para a convenção 0→1000 pedida pela Issue (chore-ux-001 usa 50/100/300/500/700/900; mantemos os hex exatos daqueles tons e adicionamos paradas interpoladas onde a chore-ux-001 era omissa, sem alterar viés cromático).

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-neutral-0` | `#FFFFFF` | Branco puro — fundo de card, campo de input |
| `--color-neutral-50` | `#F7F4F1` | **Fundo padrão da app** (chore-ux-001 §2.5, §6.3) |
| `--color-neutral-100` | `#EDE8E4` | Fundo de seção alternada, skeleton de loading |
| `--color-neutral-200` | `#DCD6D0` | Divisor sutil |
| `--color-neutral-300` | `#C9C2BD` | Borda de input em repouso, divisor |
| `--color-neutral-400` | `#A39A93` | Borda em hover, ícone secundário |
| `--color-neutral-500` | `#7A716B` | Texto secundário, metadado, placeholder (AA 4.62:1) |
| `--color-neutral-600` | `#5C544F` | Texto auxiliar com mais hierarquia |
| `--color-neutral-700` | `#3D3733` | Texto de corpo (AAA 10.8:1) |
| `--color-neutral-800` | `#2A2522` | Hierarquia entre corpo e display |
| `--color-neutral-900` | `#1A1614` | Título principal, alta hierarquia (AAA 16.4:1) |
| `--color-neutral-1000` | `#0E0B0A` | Quase-preto — não usar como `#000` puro (anti-acolhedor) |

### 2.4 Estados — Success / Warning / Danger / Info

Paleta deliberadamente enxuta: success reaproveita secondary; info reaproveita sky/terciária da chore-ux-001 §2.3; warning herda os hex declarados em §2.4; danger é a única cor própria de estado (justificativa em chore-ux-001 §2.4 e §6.4 — não pode colidir com CTA primário coral).

#### Success

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-success-100` | `#E2F4CB` | Fundo de toast/banner de sucesso |
| `--color-success-500` | `#7BC42F` | Ícone gráfico de sucesso |
| `--color-success-700` | `#3F7A0F` | Texto "Sessão salva", "Vinculação concluída" (AA 6.84:1) |

#### Warning

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-warning-100` | `#FFF5DC` | Fundo de aviso "sua vinculação expira em 2 dias" |
| `--color-warning-500` | `#D89B1A` | Ícone gráfico de atenção |
| `--color-warning-700` | `#9A5A00` | Texto sobre `warning-100` (AA 6.74:1) |

#### Danger

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-danger-100` | `#FDECEA` | Fundo de mensagem de erro (`danger-50` da chore-ux-001 renomeado) |
| `--color-danger-500` | `#E45A4F` | Ícone gráfico de erro (uso decorativo) |
| `--color-danger-700` | `#C8362B` | Texto de validação, `aria-invalid`, toast (AA 6.12:1) |

#### Info

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-info-100` | `#EAF7FB` | Fundo de callout neutro, dica de onboarding |
| `--color-info-500` | `#1FA8C9` | Link textual, ícone "info" (AA Large) |
| `--color-info-700` | `#0E6C82` | Texto de link sobre fundo claro (AAA 7.42:1) |

### 2.5 Heatmap (RF-E6-10, US-041)

PRD RF-E6-10 exige **célula vazia em cinza + 3 tons crescentes de laranja**. Derivamos os três tons a partir da escala primary (coral é a leitura "laranja" jovem da identidade — chore-ux-001 §1 e PRD §16 ADR-013 mantêm essa leitura sem retornar ao "laranja queimado escuro" rejeitado).

| Token CSS | Hex | Uso pretendido |
|---|---|---|
| `--color-heatmap-empty` | `#EDE8E4` | Célula sem atividade (alias visual de `neutral-100`) |
| `--color-heatmap-1` | `#FFD9D6` | 1 dia da semana com atividade (alias de `primary-100`) |
| `--color-heatmap-2` | `#FA7B71` | 2–4 dias com atividade (alias de `primary-400`) |
| `--color-heatmap-3` | `#D94A43` | 5+ dias com atividade / streak forte (alias de `primary-600`) |

### 2.6 Tokens semânticos de cor (≥14)

Tokens que **toda US a partir do Sprint 3** vai consumir. Não usam número; descrevem papel. Cada um aponta para um primitivo via `var(--…)`.

| Token semântico | Consome | Uso pretendido |
|---|---|---|
| `--color-bg` | `var(--color-neutral-50)` | Fundo padrão da app (`#F7F4F1`) |
| `--color-surface` | `var(--color-neutral-0)` | Fundo de card, campo de input (branco) |
| `--color-surface-raised` | `var(--color-neutral-0)` | Fundo de card elevado (combinado com `--shadow-md`) |
| `--color-surface-muted` | `var(--color-neutral-100)` | Fundo de seção alternada |
| `--color-border` | `var(--color-neutral-300)` | Borda de input em repouso |
| `--color-border-strong` | `var(--color-neutral-400)` | Borda em hover/foco visual |
| `--color-divider` | `var(--color-neutral-200)` | Divisor sutil entre blocos |
| `--color-text-display` | `var(--color-neutral-900)` | Título principal, display |
| `--color-text-body` | `var(--color-neutral-700)` | Texto de corpo |
| `--color-text-muted` | `var(--color-neutral-500)` | Texto secundário, placeholder, metadado |
| `--color-text-inverted` | `var(--color-neutral-0)` | Texto sobre fundo coral/escuro |
| `--color-text-link` | `var(--color-info-700)` | Link textual padrão |
| `--color-primary` | `var(--color-primary-500)` | CTA primário, marca |
| `--color-primary-hover` | `var(--color-primary-600)` | Hover de CTA primário |
| `--color-primary-active` | `var(--color-primary-700)` | Pressed/active de CTA primário |
| `--color-on-primary` | `var(--color-neutral-0)` | Texto/ícone sobre `--color-primary` |
| `--color-focus-ring` | `var(--color-primary-500)` | Anel de foco visível (WCAG 2.4.7), `outline 2px solid var(--color-focus-ring)` com `outline-offset: 2px` |

**Total: 17 tokens semânticos.** Atende ao mínimo de 14 exigido pela Issue.

---

## 3. Tipografia

Origem: `doc/UX/00-identidade-visual.md` §4.

### 3.1 Famílias

| Token CSS | Stack completa | Uso |
|---|---|---|
| `--font-display` | `"Bricolage Grotesque", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` | Display, headings (h1, h2) |
| `--font-sans` | `"Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif` | Corpo, h3, body, button |
| `--font-mono` | `ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace` | Reservado para metadados técnicos/datas tabulares (`font-variant-numeric: tabular-nums` cobre o caso de streak — preferir antes de carregar mono). **Não importar fonte mono** — usar stack do sistema. |

**Carregamento (referência para chore-ux-003):** Bricolage Grotesque variable (pesos 600, 700) e Inter variable (pesos 400, 500, 600) servidas via self-host com `font-display: swap`. Total aproximado ≤100KB (chore-ux-001 §4.1).

### 3.2 Pesos

Declarar **apenas** os pesos efetivamente usados para conter bundle (chore-ux-001 §4.3).

| Token CSS | Valor | Uso |
|---|---|---|
| `--font-weight-regular` | `400` | Inter body, body-lg, body-sm |
| `--font-weight-medium` | `500` | Inter caption, ênfase leve em corpo |
| `--font-weight-semibold` | `600` | Inter h3, button, overline; Bricolage h1, h2 |
| `--font-weight-bold` | `700` | Bricolage display-lg, display-xl |

Pesos 100/200/300/800/900 **não são importados** — economia de rede em Brasil real (P13).

### 3.3 Escala de tamanho (`font-size` + `line-height` pareados)

Mobile-first. Os valores abaixo correspondem à coluna mobile da chore-ux-001 §4.2. Overrides para desktop (≥1024px) entram na chore-ux-003 via utilitários responsivos (`md:` / `lg:`), não como tokens primitivos.

| Token CSS | font-size | line-height | Família alvo | Rótulo da chore-ux-001 | Uso |
|---|---|---|---|---|---|
| `--text-xs` | `0.75rem` (12px) | `1.4` | sans | `caption` | Metadado, timestamp, label de gráfico. Nunca texto narrativo. |
| `--text-sm` | `0.875rem` (14px) | `1.5` | sans | `body-sm` | Texto secundário em card, descrição de selo |
| `--text-base` | `1rem` (16px) | `1.5` | sans | `body` / `button` | Corpo padrão. Mínimo absoluto para texto contínuo em mobile. |
| `--text-lg` | `1.125rem` (18px) | `1.5` | sans | `body-lg` / `h3` | Texto narrativo da sessão, título de card |
| `--text-xl` | `1.25rem` (20px) | `1.2` | display | `h2` | Título de seção dentro da tela |
| `--text-2xl` | `1.5rem` (24px) | `1.15` | display | `h1` | Título de tela (um por tela) |
| `--text-3xl` | `2rem` (32px) | `1.1` | display | `display-lg` | Tela cheia de conquista |
| `--text-display` | `2.5rem` (40px) | `1.05` | display | `display-xl` | Hero de landing, tela vazia ilustrada |

**Total: 8 níveis.** Atende ao mínimo da Issue. Cada nível tem line-height **pareado** ao tamanho.

### 3.4 Letter-spacing (overline)

| Token CSS | Valor | Uso |
|---|---|---|
| `--tracking-overline` | `0.05em` | `overline` da chore-ux-001 §4.2 — uppercase ("DIA 3") sobre h1/h2 |
| `--tracking-tight` | `-0.01em` | Refinamento opcional em `--text-3xl`/`--text-display` (ajuste perceptivo em display grande) |

### 3.5 Numérico

Não é um token primitivo, mas registramos a convenção: **streak e contadores incrementais** devem usar `font-variant-numeric: tabular-nums` para não "pular" ao incrementar (chore-ux-001 §4.2). Implementação fica em componente (chore-ux-003).

---

## 4. Espaçamento

Escala em múltiplos de **4px**, base padrão Tailwind. Mobile-first (chore-ux-001 §5). Tokens primitivos numerados; tokens semânticos (gap entre blocos, padding de container) ficam para chore-ux-003.

| Token CSS | Valor | px | Uso pretendido |
|---|---|---|---|
| `--space-0` | `0` | 0 | Reset |
| `--space-1` | `0.25rem` | 4 | Gap mínimo entre ícone e texto |
| `--space-2` | `0.5rem` | 8 | Padding interno de chip/badge |
| `--space-3` | `0.75rem` | 12 | Gap entre campos compactos |
| `--space-4` | `1rem` | 16 | **Padding lateral padrão de tela mobile** (chore-ux-001 §5.2) |
| `--space-5` | `1.25rem` | 20 | Padding interno de card |
| `--space-6` | `1.5rem` | 24 | **Gap entre blocos de sessão** (chore-ux-001 §5.7 — `space-y-6` mínimo) |
| `--space-8` | `2rem` | 32 | Margem entre seções secundárias |
| `--space-10` | `2.5rem` | 40 | **Gap entre seções de tela** (chore-ux-001 §5.7 — `space-y-10`) |
| `--space-11` | `2.75rem` | **44** | **Touch target mínimo (RNF-A11Y-05).** Aplicar em `min-height`/`min-width` de todo interativo |
| `--space-12` | `3rem` | 48 | Touch target confortável (recomendado para CTA primário) |
| `--space-16` | `4rem` | 64 | Margens superiores de hero |
| `--space-20` | `5rem` | 80 | Reservado — espaçamento generoso desktop |
| `--space-24` | `6rem` | 96 | Reservado — hero desktop |

**Total: 14 paradas.** Atende ao mínimo de 12.

**Touch target (RNF-A11Y-05 / PRD §8.4):** todo elemento interativo deve ter `min-height: var(--space-11)` E `min-width: var(--space-11)`. O Codificador implementa essa regra como utilitário padrão dos componentes em chore-ux-003.

---

## 5. Raios (border radius)

Escala generosa — a personalidade "clara, jovem, acolhedora" da chore-ux-001 §1 pede curvas suaves. Comparativo: apps adultos sérios (LinkedIn, GitHub) usam 4–6px; apps jovens (Duolingo, Headspace) usam 12–16px em cards e 20–24px em CTAs. Posicionamos atrilha mais perto do segundo grupo, sem cair em "infantilização" (que exigiria pill `9999px` em tudo).

| Token CSS | Valor | Uso pretendido | Justificativa |
|---|---|---|---|
| `--radius-none` | `0` | Reset, separadores | — |
| `--radius-sm` | `0.25rem` (4px) | Tag inline, divisor com pontas | Curva mínima — não cai em "pontiagudo" |
| `--radius-md` | `0.5rem` (8px) | Input, badge, marca gráfica da logo (alinhamento com chore-ux-001 §3.1) | Padrão para elementos pequenos |
| `--radius-lg` | `0.75rem` (12px) | Card de sessão, callout | Suave o suficiente para "acolhedor" |
| `--radius-xl` | `1.25rem` (20px) | Card de destaque, modal | "Editorial moderno" sem virar pill |
| `--radius-2xl` | `1.5rem` (24px) | Hero card, tela de conquista | Reservado para superfícies grandes |
| `--radius-full` | `9999px` | Pill button secundário, avatar, indicador de progresso circular | — |

**Total: 7 tokens.** Excede o mínimo de 5.

---

## 6. Sombras

Sombras discretas, mobile-friendly. **Vetados explicitamente** (Issue §8):

- Sombras coloridas "glow" (ex.: `0 0 20px rgba(242, 92, 84, 0.6)`).
- Elevação skeumórfica (sombras duplas, gradientes embutidos, simulação 3D).

A única sombra "interna" permitida é o baixo-relevo da marca gráfica da logo (`inset 0 -3px 0 rgba(0,0,0,.08)` — chore-ux-001 §3.3), que **não vira token genérico** — fica como CSS local do componente de logo.

| Token CSS | Valor | Uso pretendido |
|---|---|---|
| `--shadow-none` | `none` | Reset |
| `--shadow-sm` | `0 1px 2px 0 rgba(26, 22, 20, 0.05)` | Elementos em repouso (input, card plano) |
| `--shadow-md` | `0 4px 6px -1px rgba(26, 22, 20, 0.08), 0 2px 4px -2px rgba(26, 22, 20, 0.06)` | Cards elevados, dropdown |
| `--shadow-lg` | `0 10px 15px -3px rgba(26, 22, 20, 0.10), 0 4px 6px -4px rgba(26, 22, 20, 0.06)` | Modal, sheet, popover |
| `--shadow-focus` | `0 0 0 3px rgba(242, 92, 84, 0.35)` | **Anel de foco** acessível (acompanha `--color-focus-ring`). Usar via `box-shadow` quando `outline` não puder ser aplicado. |

**Total: 5 tokens.** Excede o mínimo de 4.

Cor base das sombras: `rgba(26, 22, 20, …)` = `--color-neutral-900` translúcido. Mantém a paleta "uma família" (chore-ux-001 §2.5) — sombras herdam o viés quente do neutro, em vez de virar cinza-azulado.

---

## 7. Breakpoints

Defaults do Tailwind v4 (sem customização — Issue §9).

| Token CSS | Valor | Alvo |
|---|---|---|
| `--breakpoint-sm` | `40rem` (640px) | Smartphones grandes / phablets |
| `--breakpoint-md` | `48rem` (768px) | Tablets |
| `--breakpoint-lg` | `64rem` (1024px) | Desktop |
| `--breakpoint-xl` | `80rem` (1280px) | Desktop largo |

**Mobile-first. Breakpoints são overrides "up".** Nenhum estilo de produção pode **depender** de breakpoint para funcionar — a tela deve estar usável em viewport de 320px sem qualquer media query disparada. Os utilitários `sm:`, `md:`, `lg:`, `xl:` aplicam **acréscimos** (refino de layout, aumento de fonte, troca de coluna para grid) sobre uma base mobile já válida.

**Restrição PRD RNF-COMP-04:** layout responsivo de 320px → 1920px. Implica:

- Faixa de 320–639px: base mobile, sem nenhum prefixo aplicado.
- Faixa de 1280–1920px: usar `xl:` para limitar `max-width` de conteúdo (ver chore-ux-001 §5.4 — conteúdo de leitura tem `max-width: 38rem`).

---

## 8. Motion (duração + easing)

Origem: AGENTS.md §12 menciona "animações fadeUp + scroll reveal" como ativos já existentes do site institucional. Esta task **mantém compatibilidade** declarando durações/easings genéricos; a chore-ux-003 (componentes) decide se reaproveita as classes existentes ou as substitui por utilitários Tailwind v4. **Sem decisão prematura aqui** — registramos só os tokens.

| Token CSS | Valor | Uso pretendido |
|---|---|---|
| `--duration-fast` | `120ms` | Hover/active de botão, toggle de chip |
| `--duration-base` | `240ms` | Transição padrão (cor, fundo, transform) |
| `--duration-slow` | `400ms` | Entrada de modal, fadeUp inicial |
| `--ease-out-soft` | `cubic-bezier(0.16, 1, 0.3, 1)` | Suavização padrão — desacelera no fim (sensação "jovem", não corporativa) |
| `--ease-in-out` | `cubic-bezier(0.4, 0, 0.2, 1)` | Reservado — transições que precisam acelerar e desacelerar |

**Respeito a `prefers-reduced-motion`:** componentes que usarem `--duration-*` devem aplicar `@media (prefers-reduced-motion: reduce)` para zerar duração (regra a documentar em chore-ux-003; aqui apenas registramos a obrigação).

---

## 9. Z-index

Tokens nomeados por camada, evitando "guerra de números" (`z-index: 9999`).

| Token CSS | Valor | Uso |
|---|---|---|
| `--z-base` | `0` | Conteúdo de fluxo normal |
| `--z-dropdown` | `10` | Menus suspensos, autocomplete |
| `--z-sticky` | `20` | Header fixo, barra de navegação inferior |
| `--z-overlay` | `30` | Backdrop de modal, drawer |
| `--z-modal` | `40` | Conteúdo de modal, sheet |
| `--z-toast` | `50` | Toast, notificação flutuante (sempre acima de tudo) |

---

## 10. Mapeamento Tailwind v4 — bloco `@theme` (contrato copiável)

O bloco abaixo é **o contrato** com o Codificador. A próxima task que consumir estes tokens (provavelmente chore-ux-003) deve colar este bloco **literalmente** no CSS principal do projeto (sintaxe Tailwind v4).

Tokens semânticos referenciam primitivos via `var(--…)` — funciona dentro do `@theme` do Tailwind v4 desde que a referência seja para outra custom property declarada no mesmo bloco.

```css
@theme {
  /* =====================================================================
     CORES — PRIMITIVOS
     ===================================================================== */

  /* Primary (coral — cor de marca) */
  --color-primary-50:  #FFF1F0;
  --color-primary-100: #FFD9D6;
  --color-primary-200: #FFBDB7;
  --color-primary-300: #FF9A92;
  --color-primary-400: #FA7B71;
  --color-primary-500: #F25C54;
  --color-primary-600: #D94A43;
  --color-primary-700: #A8362F;
  --color-primary-800: #7C2823;
  --color-primary-900: #501914;

  /* Secondary (lime — cor de progresso) */
  --color-secondary-50:  #F2FBE7;
  --color-secondary-100: #E2F4CB;
  --color-secondary-200: #D3EDAE;
  --color-secondary-300: #BEEB6A;
  --color-secondary-400: #A1D848;
  --color-secondary-500: #7BC42F;
  --color-secondary-600: #5DA31F;
  --color-secondary-700: #3F7A0F;
  --color-secondary-800: #2D5A0A;
  --color-secondary-900: #1E3D06;

  /* Neutral (ink — cinza-quente puxado para coral) */
  --color-neutral-0:    #FFFFFF;
  --color-neutral-50:   #F7F4F1;
  --color-neutral-100:  #EDE8E4;
  --color-neutral-200:  #DCD6D0;
  --color-neutral-300:  #C9C2BD;
  --color-neutral-400:  #A39A93;
  --color-neutral-500:  #7A716B;
  --color-neutral-600:  #5C544F;
  --color-neutral-700:  #3D3733;
  --color-neutral-800:  #2A2522;
  --color-neutral-900:  #1A1614;
  --color-neutral-1000: #0E0B0A;

  /* Estados */
  --color-success-100: #E2F4CB;
  --color-success-500: #7BC42F;
  --color-success-700: #3F7A0F;

  --color-warning-100: #FFF5DC;
  --color-warning-500: #D89B1A;
  --color-warning-700: #9A5A00;

  --color-danger-100:  #FDECEA;
  --color-danger-500:  #E45A4F;
  --color-danger-700:  #C8362B;

  --color-info-100: #EAF7FB;
  --color-info-500: #1FA8C9;
  --color-info-700: #0E6C82;

  /* Heatmap (RF-E6-10) */
  --color-heatmap-empty: #EDE8E4;
  --color-heatmap-1:     #FFD9D6;
  --color-heatmap-2:     #FA7B71;
  --color-heatmap-3:     #D94A43;

  /* =====================================================================
     CORES — SEMÂNTICOS
     ===================================================================== */

  --color-bg:              var(--color-neutral-50);
  --color-surface:         var(--color-neutral-0);
  --color-surface-raised:  var(--color-neutral-0);
  --color-surface-muted:   var(--color-neutral-100);
  --color-border:          var(--color-neutral-300);
  --color-border-strong:   var(--color-neutral-400);
  --color-divider:         var(--color-neutral-200);
  --color-text-display:    var(--color-neutral-900);
  --color-text-body:       var(--color-neutral-700);
  --color-text-muted:      var(--color-neutral-500);
  --color-text-inverted:   var(--color-neutral-0);
  --color-text-link:       var(--color-info-700);
  --color-primary:         var(--color-primary-500);
  --color-primary-hover:   var(--color-primary-600);
  --color-primary-active:  var(--color-primary-700);
  --color-on-primary:      var(--color-neutral-0);
  --color-focus-ring:      var(--color-primary-500);

  /* =====================================================================
     TIPOGRAFIA
     ===================================================================== */

  --font-display: "Bricolage Grotesque", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-sans:    "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-mono:    ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;

  --font-weight-regular:  400;
  --font-weight-medium:   500;
  --font-weight-semibold: 600;
  --font-weight-bold:     700;

  --text-xs:      0.75rem;     --text-xs--line-height:      1.4;
  --text-sm:      0.875rem;    --text-sm--line-height:      1.5;
  --text-base:    1rem;        --text-base--line-height:    1.5;
  --text-lg:      1.125rem;    --text-lg--line-height:      1.5;
  --text-xl:      1.25rem;     --text-xl--line-height:      1.2;
  --text-2xl:     1.5rem;      --text-2xl--line-height:     1.15;
  --text-3xl:     2rem;        --text-3xl--line-height:     1.1;
  --text-display: 2.5rem;      --text-display--line-height: 1.05;

  --tracking-overline: 0.05em;
  --tracking-tight:    -0.01em;

  /* =====================================================================
     ESPAÇAMENTO (escala 4px)
     ===================================================================== */

  --space-0:  0;
  --space-1:  0.25rem;   /* 4  */
  --space-2:  0.5rem;    /* 8  */
  --space-3:  0.75rem;   /* 12 */
  --space-4:  1rem;      /* 16 */
  --space-5:  1.25rem;   /* 20 */
  --space-6:  1.5rem;    /* 24 */
  --space-8:  2rem;      /* 32 */
  --space-10: 2.5rem;    /* 40 */
  --space-11: 2.75rem;   /* 44 — touch target mínimo (RNF-A11Y-05) */
  --space-12: 3rem;      /* 48 */
  --space-16: 4rem;      /* 64 */
  --space-20: 5rem;      /* 80 */
  --space-24: 6rem;      /* 96 */

  /* =====================================================================
     RAIOS
     ===================================================================== */

  --radius-none: 0;
  --radius-sm:   0.25rem;   /* 4  */
  --radius-md:   0.5rem;    /* 8  */
  --radius-lg:   0.75rem;   /* 12 */
  --radius-xl:   1.25rem;   /* 20 */
  --radius-2xl:  1.5rem;    /* 24 */
  --radius-full: 9999px;

  /* =====================================================================
     SOMBRAS
     ===================================================================== */

  --shadow-none:  none;
  --shadow-sm:    0 1px 2px 0 rgba(26, 22, 20, 0.05);
  --shadow-md:    0 4px 6px -1px rgba(26, 22, 20, 0.08), 0 2px 4px -2px rgba(26, 22, 20, 0.06);
  --shadow-lg:    0 10px 15px -3px rgba(26, 22, 20, 0.10), 0 4px 6px -4px rgba(26, 22, 20, 0.06);
  --shadow-focus: 0 0 0 3px rgba(242, 92, 84, 0.35);

  /* =====================================================================
     BREAKPOINTS (mobile-first — overrides "up")
     ===================================================================== */

  --breakpoint-sm: 40rem;   /* 640px  */
  --breakpoint-md: 48rem;   /* 768px  */
  --breakpoint-lg: 64rem;   /* 1024px */
  --breakpoint-xl: 80rem;   /* 1280px */

  /* =====================================================================
     MOTION
     ===================================================================== */

  --duration-fast: 120ms;
  --duration-base: 240ms;
  --duration-slow: 400ms;
  --ease-out-soft: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-in-out:   cubic-bezier(0.4, 0, 0.2, 1);

  /* =====================================================================
     Z-INDEX
     ===================================================================== */

  --z-base:     0;
  --z-dropdown: 10;
  --z-sticky:   20;
  --z-overlay:  30;
  --z-modal:    40;
  --z-toast:    50;
}
```

---

## 11. Tokens fora do escopo desta task

Os tokens abaixo **não** são declarados aqui. Cada um nasce junto com seu componente na **chore-ux-003** (Sprint 2, próxima task após esta), consumindo os primitivos e semânticos definidos acima.

### 11.1 Convenção de nomenclatura para tokens de componente

```
--<componente>-<propriedade>[-<estado>]
```

Onde:

- `<componente>` é o nome do componente em inglês kebab-case (`button-primary`, `card`, `input`, `badge`, `chip`, `toast`, `modal`, `sheet`).
- `<propriedade>` é um aspecto visual (`bg`, `text`, `border`, `radius`, `padding-x`, `padding-y`, `shadow`).
- `<estado>` (opcional) é `hover`, `active`, `focus`, `disabled`, `selected`.

**Exemplos antecipados (não declarar agora):**

```
--button-primary-bg              → var(--color-primary)
--button-primary-bg-hover        → var(--color-primary-hover)
--button-primary-bg-active       → var(--color-primary-active)
--button-primary-text            → var(--color-on-primary)
--button-primary-radius          → var(--radius-full)
--button-primary-padding-x       → var(--space-6)
--button-primary-padding-y       → var(--space-3)
--button-primary-min-height      → var(--space-11)

--card-bg                        → var(--color-surface)
--card-radius                    → var(--radius-lg)
--card-padding                   → var(--space-5)
--card-shadow                    → var(--shadow-sm)

--input-bg                       → var(--color-surface)
--input-border                   → var(--color-border)
--input-border-focus             → var(--color-focus-ring)
--input-radius                   → var(--radius-md)
--input-padding-x                → var(--space-4)
--input-min-height               → var(--space-11)
```

### 11.2 Outras questões fora desta task

- **Dark theme** — explicitamente fora do MVP (chore-ux-001 §6.3 / ADR-013). Quando vier, será uma `@theme` alternativa (provavelmente `prefers-color-scheme: dark`), reaproveitando os primitivos e redefinindo apenas os semânticos.
- **Tokens de ilustração** (paleta editorial para SVGs decorativos) — dependem do estilo concreto a definir (chore-ux-001 §7, dúvida aberta "produção de ilustrações editoriais").
- **Microinterações Lottie / animações ricas** — fora desta task. Esta camada define apenas duração/easing de transições CSS.

---

## Referências cruzadas

- Fonte primária das decisões: `doc/UX/00-identidade-visual.md` (chore-ux-001).
- Acessibilidade: PRD `doc/PRD.md` §8.4 — RNF-A11Y-01..05 (em especial RNF-A11Y-05, touch target 44×44px → `--space-11`).
- Responsividade: PRD §8.4 RNF-COMP-04 — 320px → 1920px.
- Heatmap: PRD §10 RF-E6-10 — empty + 3 tons crescentes.
- Stack visual: AGENTS.md §12 e ADR-011 (PRD §16).
