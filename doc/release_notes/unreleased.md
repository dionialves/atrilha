# Release Notes — Unreleased

## CHORE · Identidade visual — paleta, tipografia e princípios de hierarquia (#20)

**Tipo:** Chore (UX, Sprint 2, marco M1)
**Issue:** [#20](https://github.com/dionialves/atrilha/issues/20)
**Branch:** chore/20-identidade-visual
**Data de conclusão:** 2026-05-18

### O que foi feito

- Criado `doc/UX/00-identidade-visual.md` com paleta, logo, tipografia e princípios de hierarquia.
- Paleta: coral `#F25C54` (marca), lime `#7BC42F` (progresso), sky `#1FA8C9` (descoberta), danger `#C8362B`, neutros `ink-50`..`ink-900` em cinza-quente. Fundo padrão `ink-50` (`#F7F4F1`), não branco puro. Contraste WCAG declarado para cada cor de texto, com restrição explícita de uso para cores que só passam AA Large.
- Tipografia: Bricolage Grotesque (display, pesos 600/700) + Inter (texto, pesos 400/500/600), ambas variable, total ~100KB WOFF2 — cabe no budget de 200KB do DoD §5 com folga. Escala mobile-first com 11 rótulos (`display-xl` a `overline`), texto corrido nunca abaixo de 16px em mobile.
- 8 princípios de hierarquia mobile-first (viewport 320px+, touch target 44×44px): um H1 por tela, CTA primário full-width em mobile, estado ativo com redundância de canal (cor + tamanho + texto), conteúdo de leitura limitado a ~65 caracteres, densidade decrescente do topo para a base, cor de marca escassa (máx. 2 elementos `primary-500` por viewport), espaço em branco como elemento de hierarquia, ilustração editorial > foto > ícone genérico.
- Logo definida: marca gráfica (quadrado coral 28×28 com chevron branco + ponto, SVG inline) + wordmark "atrilha" minúscula em Inter, com regras de uso e área de respiro.
- Seção §6 com decisões registradas e alternativas descartadas (azul-céu como marca, Space Grotesk display, branco puro, reaproveitar `primary-600` como erro).
- Seção §7 com histórico de aprovação datado em 2026-05-18 pelo humano (Dioni).

### Decisões aprovadas pelo humano (Dioni) em 2026-05-18

- Cor de marca coral `#F25C54`
- Display font Bricolage Grotesque
- Fundo `ink-50` `#F7F4F1` (não branco puro)
- Modo escuro fora do MVP (ADR-013 reafirmado)
- Logo: chevron + ponto em quadrado coral

### Impacto

- Arquivos novos: `doc/UX/00-identidade-visual.md`.
- Arquivos editados: `doc/changelog.md`, `doc/release_notes/unreleased.md` (este).
- Nenhuma alteração em código Java, migrations, templates, `static`, `properties` ou `pom.xml`.
- Destrava `ux-002` (tokens Tailwind), `ux-003` (componentes base), `ux-004..006` (protótipos HTML) e `ux-007` (acessibilidade) do Sprint 2.

### Como testar

1. Abrir `doc/UX/00-identidade-visual.md`.
2. Conferir presença das 4 seções de entrega: paleta (§2), logo (§3), tipografia (§4), princípios de hierarquia (§5).
3. Verificar que cada cor de texto declara contraste WCAG (colunas das tabelas §2.1–2.5); validar pelo menos um valor numérico em ferramenta externa (ex: contrast-ratio.com).
4. Confirmar histórico de aprovação datado em §7.

### Dúvidas abertas registradas (não bloqueiam)

- Terminologia visível ao usuário para "sessão de sábado" e blocos internos — afeta microcopy futura.
- Pipeline de produção de ilustrações editoriais — afeta `ux-004..006` e telas de estado vazio/conquista.

---

## CHORE-UX-002 · Design tokens Tailwind — cores, espaçamento, raios, sombras, tipografia (#21)

**Tipo:** Chore (UX, Sprint 2, marco M1)
**Issue:** [#21](https://github.com/dionialves/atrilha/issues/21)
**Branch:** chore/21-design-tokens
**Data de conclusão:** 2026-05-18

### O que foi feito

- Criado `doc/UX/01-design-tokens.md` (≈589 linhas, 11 seções + Referências cruzadas) traduzindo `doc/UX/00-identidade-visual.md` em tokens implementáveis no Tailwind v4 (sintaxe `@theme` em CSS — não `tailwind.config.js` da v3).
- **Cores primitivas (escalas numeradas):** `primary` (coral) 50→900 com hex da chore-ux-001 §2.1 (50/100/300/500/600/700) e paradas 200/400/800/900 interpoladas; `secondary` (lime) 50→900 com hex da chore-ux-001 §2.2 (50/300/500/700) e paradas 100/200/400/600/800/900 interpoladas; `neutral` (ink, cinza-quente puxado para coral) 0→1000 com hex da chore-ux-001 §2.5 (50/100/300/500/700/900) e paradas 0/200/400/600/800/1000 interpoladas; estados `success`/`warning`/`danger`/`info` com 3 paradas cada (100/500/700).
- **Heatmap (RF-E6-10 / US-041):** `--color-heatmap-empty` + 3 tons crescentes (`heatmap-1/2/3`) alias de `primary-100/400/600`.
- **17 tokens semânticos de cor:** `--color-bg`, `--color-surface`, `--color-surface-raised`, `--color-surface-muted`, `--color-border`, `--color-border-strong`, `--color-divider`, `--color-text-display`, `--color-text-body`, `--color-text-muted`, `--color-text-inverted`, `--color-text-link`, `--color-primary`, `--color-primary-hover`, `--color-primary-active`, `--color-on-primary`, `--color-focus-ring`. Todos referenciam primitivos via `var(--…)`.
- **Tipografia:** stacks `--font-display` (Bricolage Grotesque), `--font-sans` (Inter), `--font-mono` (stack do sistema, sem importar fonte); 4 tokens de peso (400/500/600/700); 8 níveis de tamanho (`--text-xs` 12px → `--text-display` 40px) com `--text-*--line-height` pareado; `--tracking-overline` (0.05em) e `--tracking-tight` (-0.01em).
- **Espaçamento:** 14 paradas em múltiplos de 4px de `--space-0` a `--space-24`, com `--space-11` = 44px destacado como touch target mínimo RNF-A11Y-05.
- **Raios:** 7 tokens (`--radius-none` a `--radius-2xl` 24px + `--radius-full` 9999px) calibrados para a personalidade "clara, jovem, acolhedora" (chore-ux-001 §1).
- **Sombras:** 5 tokens com base `rgba(26, 22, 20, …)` (= `--color-neutral-900` translúcido, mantém o viés quente da paleta) + `--shadow-focus` para anel de foco acessível. Vetos explícitos a glow colorido e skeumórfico.
- **Breakpoints:** 4 tokens Tailwind default (`--breakpoint-sm` 640px → `--breakpoint-xl` 1280px) com nota explícita de mobile-first ("breakpoints são overrides up", nenhum estilo pode depender deles para funcionar a 320px).
- **Motion:** 3 durações (`--duration-fast` 120ms / `--duration-base` 240ms / `--duration-slow` 400ms) + 2 easings (`--ease-out-soft`, `--ease-in-out`); nota de obrigação de respeitar `prefers-reduced-motion` na chore-ux-003.
- **Z-index:** 6 camadas nomeadas (`--z-base` 0 a `--z-toast` 50).
- **Seção 10 — contrato copiável:** bloco `@theme { … }` literal contendo todos os primitivos, semânticos e tokens não-cromáticos, pronto para o Codificador da próxima task colar no CSS principal.
- **Seção 11 — fora do escopo:** convenção de nomenclatura para tokens de componente (`--<componente>-<propriedade>-<estado>`) com exemplos antecipados para botão/card/input; dark theme, tokens de ilustração e microinterações Lottie ficam para tasks futuras.

### Impacto

- Arquivos novos: `doc/UX/01-design-tokens.md`.
- Arquivos editados: `doc/changelog.md`, `doc/release_notes/unreleased.md` (este).
- Nenhuma alteração em código Java, migrations, templates, `static`, `properties` ou `pom.xml`.
- **Destrava:** chore-ux-003 (componentes consomem estes tokens), chore-ux-004/005/006 (protótipos), chore-ux-008.

### Como testar

1. Abrir `doc/UX/01-design-tokens.md` e conferir presença das 11 seções + Referências cruzadas.
2. Cruzar amostralmente hex declarados com `doc/UX/00-identidade-visual.md`: `primary-500` = `#F25C54` (§2.1), `secondary-500` = `#7BC42F` (§2.2), `neutral-50` = `#F7F4F1` (§2.5), `info-700` = `#0E6C82` (§2.3), `danger-700` = `#C8362B` (§2.4).
3. Confirmar bloco `@theme` da §10 copiável — todos os tokens das §§2–9 aparecem como custom properties; semânticos referenciam primitivos via `var(--…)`.
4. Validar contagens mínimas: ≥14 tokens semânticos de cor (17 entregues), ≥8 níveis tipográficos (8), ≥12 paradas de espaçamento (14), ≥5 raios (7), ≥4 sombras (5).
5. Conferir mapeamento explícito `--space-11` = 44px → RNF-A11Y-05 e nota mobile-first nos breakpoints.

### Decisões registradas

- Neutral renumerada para escala 0→1000 (Issue exige), preservando os hex declarados na chore-ux-001 §2.5 nas paradas correspondentes (50/100/300/500/700/900) e interpolando 0/200/400/600/800/1000.
- `--font-mono` definida apenas com stack do sistema (`ui-monospace, SFMono-Regular, …`) — nenhuma fonte mono é importada, preservando o budget de ~100KB tipográfico declarado na chore-ux-001 §4.1. `font-variant-numeric: tabular-nums` cobre o caso de streak sem precisar de fonte mono.
- Tokens de componente (`--button-primary-bg`, `--card-radius`, etc.) deliberadamente **fora** desta task — nascem na chore-ux-003 junto com cada componente, conforme passo 13 da Issue.

---

## CHORE-UX-003 · Componentes base — botão, input, card, modal, header, navegação, badge, toast (#22)

**Tipo:** Chore (UX, Sprint 2, marco M2)
**Issue:** [#22](https://github.com/dionialves/atrilha/issues/22)
**Branch:** chore/22-componentes-base
**Data de conclusão:** 2026-05-18

### O que foi feito

- Criado `doc/UX/02-componentes-base.md` (1228 linhas, 13 seções) com o **catálogo dos 8 componentes-base reutilizáveis** que vão sustentar todas as telas do MVP: **button, input, card, modal/sheet, header, navigation (bottom-nav + nav-link), badge e toast** — cada um com as 8 subseções padronizadas exigidas pela Issue (propósito, anatomia, variantes, estados, comportamento, tokens consumidos, acessibilidade, ilustração ASCII + pseudocódigo Thymeleaf).
- **Seção 0 — princípios gerais** (mobile-first 320px, touch target ≥44×44 via `--space-11`, foco visível com `--color-focus-ring`, loading HTMX explícito via `htmx-request`, respeito a `prefers-reduced-motion`, pt-BR para usuário e inglês para classes/IDs/fragments).
- **Button**: 4 variantes (`primary`, `secondary`, `ghost`, `destructive`) × 3 sizes (`sm` 36px restrito, `md` 44px default RNF-A11Y-05, `lg` 52px CTA mobile) + modificador `icon-only` 44×44 com `aria-label` obrigatório; estado `loading` preserva label (anti-CLS) e bloqueia clique via `aria-busy="true"`.
- **Input**: 4 variantes (`text`, `email`, `password`, `textarea`), label vinculada por `for`/`id` sempre, `aria-invalid`/`aria-describedby` para erro, contador Alpine para textarea US-024 (limite 1000 chars, vira danger quando ≤50 restantes), validação real no servidor via HTMX swap.
- **Card**: 3 variantes (`flat`, `raised`, `interactive`), padding `--space-5` default ou `--space-4` em densidade alta; `card--interactive` renderiza como `<a>` ou `<button>` (nunca `<div role=button>`), com hover/focus dedicados e skeleton via `aria-busy`/`aria-live="polite"`.
- **Modal / Sheet**: bottom sheet em mobile (`max-height: 80vh`), modal centrado em tablet/desktop (`max-width: 32rem`); variantes `default`/`dismissible`/`critical`; **acessibilidade obrigatória**: `role="dialog"` + `aria-modal="true"` + `aria-labelledby`, focus trap via Alpine `x-trap.inert.noscroll`, `Esc` sempre fecha (inclusive em `critical`), foco restaurado ao botão de origem; backdrop `rgba(26, 22, 20, 0.6)` derivado de `--color-neutral-900`.
- **Header (topbar)**: variantes `compact` (~56px) e `expanded` (~80px com botão voltar + título + subtítulo); sticky com sombra dinâmica via Alpine listener (`scrollY > 8px`); logo é `<a aria-label="atrilha — página inicial">` referenciando chore-ux-001 §3.
- **Navigation**: `bottom-nav` mobile com 3–5 itens (`<nav aria-label="Principal">`), cada `nav-link` com ícone SVG stroke 24×24 + label `--text-xs`, **estado ativo com redundância de canal** (cor + peso tipográfico + `aria-current="page"`), `padding-bottom: env(safe-area-inset-bottom)` para iOS; desktop por default oculta o bottom-nav e migra itens para o header expanded (decisão conservadora: uma superfície de navegação por vez).
- **Badge**: 6 variantes (`neutral`, `primary`, `success`, `warning`, `danger`, `info`) × 2 sizes (`sm` 18px, `md` 24px) + variante `dot` 8×8 com `aria-label` obrigatório; consumidos por estado de nó da trilha ("HOJE", "Concluído", "Bloqueado"), marcador "Privado" do campo de reflexão (US-024) e contador de notificações no header.
- **Toast**: 4 variantes com **distinção ARIA correta** — `aria-live="polite"` para `success`/`info` (não interrompe leitor) vs `role="alert"` para `warning`/`error` (interrompe e anuncia imediatamente); auto-dismiss 4s para success/info, 6s para warning/error (botão close obrigatório nessas duas), foco **não muda** automaticamente; pattern HTMX via header `HX-Trigger: {"toast": {...}}` consumido por listener global Alpine em `toast-region` (`<body @toast.window=…>`); **posição mobile: topo abaixo do header** (decisão conservadora — fora da zona do polegar, não compete com bottom-nav, padrão Duolingo/Threads).
- **Seção 9 — Tokens de componente**: **38 tokens novos** no padrão `--<componente>-<propriedade>[-<modificador>]` (excede o mínimo de 12 da Issue), **todos referenciando primitivos/semânticos** já declarados em `doc/UX/01-design-tokens.md` — `--button-primary-bg = var(--color-primary)`, `--card-radius = var(--radius-lg)`, `--modal-backdrop = rgba(26,22,20,0.6)`, etc. **Nenhum hex novo** foi introduzido nesta task.
- **Seção 10 — Estilo de implementação Thymeleaf**: contrato para o Codificador (1 fragment por componente em `src/main/resources/templates/components/<nome>.html`, parâmetros com default via Elvis `${param ?: 'valor'}`, variantes via classes Tailwind `th:classappend`, HTMX para servidor + Alpine para estado client, sem JS inline em fragments, CSS no stylesheet principal). **Implementação dos fragments é trabalho da primeira US que consumir cada componente, não desta task.**
- **Seção 11 — Decisões implícitas registradas**: 10 pontos onde a Issue deixou margem (handle de arrastar OFF, toast no topo, bottom-nav oculto em desktop, sem animação de troca de aba, sem foco automático em toast, sem token novo para 56px do header, slots em aberto, Storybook fora do MVP, close opcional em success/info, dark theme fora do escopo). Cada decisão é reversível sem mexer em tokens.

### Impacto

- Arquivos novos: `doc/UX/02-componentes-base.md`.
- Arquivos editados: `doc/changelog.md`, `doc/release_notes/unreleased.md` (este).
- Nenhuma alteração em código Java, migrations, templates, `src/**`, `static`, `properties` ou `pom.xml`. Os fragments Thymeleaf em `src/main/resources/templates/components/*.html` serão criados pelo Codificador na primeira US que consumir cada componente.
- **Destrava:** chore-ux-004 (trilha — usa card, badge, header), chore-ux-005 (sessão — usa card, button, input), chore-ux-006 (painel pais — usa card, badge, header) e chore-ux-008.

### Como testar

1. Abrir `doc/UX/02-componentes-base.md` e confirmar presença dos 8 componentes numerados 1..8 + Seção 0 (princípios gerais) + Seções 9..13 (tokens de componente, estilo Thymeleaf, decisões implícitas, pendências, referências cruzadas).
2. Para cada componente, verificar as 8 subseções padronizadas (propósito, anatomia, variantes, estados, comportamento, tokens consumidos, acessibilidade, ilustração + pseudocódigo).
3. Auditar critérios da Issue #22: Button cobre 4 variantes × 3 sizes com touch ≥44×44 em md/lg (§1.3); Modal documenta focus trap + `Esc` + `role="dialog"` + `aria-modal="true"` (§4.7); Toast diferencia `aria-live="polite"` (success/info) vs `role="alert"` (warning/error) (§8.3); Bottom-nav documenta `aria-current="page"` e `<nav aria-label="Principal">` (§6.5, §6.7); tabela de tokens de componente (§9) lista ≥12 tokens (entregues **38**) referenciando primitivos/semânticos da chore-ux-002.
4. Cruzar tokens citados (`--color-primary-hover`, `--color-focus-ring`, `--shadow-focus`, `--radius-xl`, `--space-11`, `--duration-fast`, `--z-modal`, `--z-toast`, `--tracking-overline`, pares `*-100`/`*-700` de success/warning/danger/info) com `doc/UX/01-design-tokens.md` — todos existem.
5. Confirmar consistência com `doc/UX/00-identidade-visual.md`: redundância de canal no estado ativo (§5.3 da identidade → §6.4 dos componentes), CTA primário full-width em mobile (§5.2 → button `lg`), cor de marca escassa (§5.6 → §0 princípio 7).
6. Verificar que o documento declara explicitamente, em §0.9, §10 e §12, que **implementação dos fragments Thymeleaf não é desta task** — fica para a primeira US que consumir cada componente.

### Decisões registradas (Seção 11 do doc)

- **Handle de arrastar no bottom sheet:** OFF por padrão (exigiria biblioteca de gesto fora do ADR-011).
- **Posição do toast em mobile:** topo abaixo do header (não compete com bottom-nav, fora da zona do polegar).
- **Navegação em desktop:** bottom-nav oculto; itens migram para header expanded (uma superfície de navegação por vez).
- **`--space-14` (header compact 56px):** sem token primitivo novo — Tailwind v4 gera `h-14` a partir do `--spacing` raiz.
- **Slots em Card/Modal:** padrão Thymeleaf fica em aberto para a primeira US que consumir (slots via `~{...}` ou `th:fragment` filhos — ambos válidos).
- **Catálogo visual (Storybook):** fora do MVP — registrado como recomendação futura.
- **Dark theme em componentes:** fora do escopo desta task; plano já documentado em chore-ux-002 §11.2.

### Pendências e o que NÃO está aqui (Seção 12 do doc)

- **Implementação dos fragments Thymeleaf** (`templates/components/*.html`) — fica para o Codificador na primeira US que consumir cada componente.
- **CSS de produção** — Codificador escreve consumindo tokens da chore-ux-002 + tokens de componente da §9 deste doc.
- **Componentes adicionais que vão emergir**: progress bar (US-023), heatmap cell (US-041), chip removível (filtros), avatar com fallback iniciais, tooltip — nascem com cada US que precisar.
- **Microcopy final** dos componentes (labels, mensagens) — definida em cada US específica.
- **Internacionalização** — copy em pt-BR direto nos templates por enquanto; i18n fica para v2.

---

## CHORE-UX-004 · Protótipo da trilha — spec UX + HTML estático da trilha vazia (#23)

**Tipo:** Chore (UX, Sprint 2, marco M3 — protótipo da trilha validado)
**Issue:** [#23](https://github.com/dionialves/atrilha/issues/23)
**Branch:** chore/23-prototipo-trilha
**Data de conclusão:** 2026-05-18

### O que foi feito

- Criado `doc/UX/03-prototipo-trilha.md` (≈451 linhas, 9 seções obrigatórias + §10 Pendências + §11 Histórico) com a especificação textual da tela inicial da adolescente (US-018) — estrutura, hierarquia, comportamentos e microcopy ancorados em US-018/019/020/021. Spec deliberadamente descreve a tela **em prosa** e amarra cada slot a tokens da chore-ux-002 e componentes da chore-ux-003 — nenhum hex novo, nenhum componente novo.
- **§1 Objetivo:** tela inicial pós-login, ponto de chegada do "voltar para o começo", quatro funções simultâneas (contexto temporal + caminho da semana + destaque do hoje + status de cada dia).
- **§2 Wireframe textual em 320px:** header compact → cabeçalho da semana (overline `SEMANA 3` + h1 + subtítulo + link "Trimestre") → lista vertical de 7 nós com linha conectora vertical entre eles (sólida nos concluídos, gradiente no "em progresso → hoje", tracejada no futuro, respiro extra antes do sábado) → bottom-nav com "Hoje" ativo. Decisão registrada de **lista vertical** em vez de mapa SVG sinuoso (custo de largura útil em 320px).
- **§3 Estados visuais dos 4 status de nó (US-019):** tabela contrato com variante de card, borda, ícone (SVG stroke 20×20), cor de ícone, badge, microcopy auxiliar e comportamento ao toque para `Concluído`/`Em progresso`/`Disponível (hoje)`/`Bloqueado`. Caso especial **sábado** (US-020) com badge `info` + badge `neutral` empilhados e microcopy "Termine mais N sessões para abrir o sábado". Hook documentado para microanimação Lottie de desbloqueio (ADR-011) — **não implementada nesta task**, apenas o lugar.
- **§4 Ancoragem ao hoje (US-018 critério 3):** decisão pelo Alpine `x-init` + `scrollIntoView({block:'center', behavior:'instant'})` — comparada com CSS-only (não posiciona inicialmente) e scroll-snap (não posiciona inicialmente). `behavior: 'instant'` evita duplicar caminho de código para `prefers-reduced-motion` e respeita adolescente que abre o app sabendo o que quer.
- **§5 Componentes consumidos:** Header `compact` + Card `interactive`/`flat` + Card "hoje" com modificador local (borda 2px coral + `shadow-md`) + Badge 4 variantes (`primary`/`success`/`neutral`/`info`) + Button `primary lg full-width` (CTA "Começar agora") + Button `ghost md` (link "Trimestre") + Bottom-nav. **Sem componente novo.** Modal/Sheet, Input e Toast explicitamente fora do escopo desta tela vazia.
- **§6 Tokens consumidos:** 4 tabelas amarrando cada token da chore-ux-002 a uma posição visual concreta — cores (`--color-primary`, `--color-secondary-300`, `--color-divider`, `--color-success-100/700`, `--color-info-100/700`, `--color-focus-ring`), tipografia (`--font-display`, `--font-sans`, pesos, `--text-xs/sm/base/2xl`, `--tracking-overline`), espaçamento (com `--space-11`=44px marcado como touch target RNF-A11Y-05 e `--space-6` como gap entre nós) e raio/sombra/motion/z-index.
- **§7 Estados e microcopy:** microcopy exata em pt-BR para cada slot, **explicitamente sem cobrança/FOMO** (P11) — §7.4 lista o que foi vetado ("Não perca o streak", "Você ficou para trás", emojis funcionais de pressão) vs. o que está aceito ("Você parou em 3 de 5.", "Liberada em quinta, 21/05.", "Termine mais 3 sessões para abrir o sábado."). Inclui estados de borda (semana sem dados, erro de carregamento, toque em nó bloqueado).
- **§8 Acessibilidade:** `<ol>` semântico (ordem é significativa); cada nó é `<a>` (estado ≠ bloqueado) ou `<button aria-disabled="true">` (bloqueado), nunca `<div onclick>`; padrão de `aria-label` por estado; ordem natural de Tab sem `tabindex` positivo; nó "hoje" com `tabindex="-1"` para foco programático futuro; tabela de contraste WCAG ancorada na chore-ux-001 §2; `prefers-reduced-motion` respeitado (scrollIntoView `instant` + transições zeradas globalmente).
- **§9 Decisões e alternativas descartadas:** 5 decisões registradas — 9.1 lista vertical vs. mapa sinuoso SVG, 9.2 badge como reforço vs. cor pura, 9.3 header compact vs. expanded (trilha é raiz, sem botão voltar), 9.4 sem ilustração editorial placeholder, 9.5 `behavior: 'instant'` vs. `'smooth'`. Cada decisão registra também **quando reabriríamos**.
- **§10 Pendências:** nomes próprios das sessões virão de `doc/conteudo/fluxo-semana.md`, ilustração de cabeçalho depende do estilo editorial (chore-ux-001 §7), microanimação Lottie entra com a US-020, skip-link entra na chore-ux-007, este HTML será consumido pela chore-ux-008 (smoke visual end-to-end).
- Criado `doc/UX/prototypes/trilha.html` (≈835 linhas, ~32 KB) — **arquivo único autocontido**: CSS inline em `<style>` com subset literal dos tokens da chore-ux-002 (cores, tipografia, espaçamento, raios, sombras, motion, z-index), Alpine.js via CDN apenas para o `x-init` do `scrollIntoView` e para o popover de motivo dos nós bloqueados. Sem fetch, sem service worker, sem manifest. Abre direto no navegador / Live Server. Mock de 7 nós inline: 2 concluídos + 1 em progresso (com barra de progresso 60%) + 1 hoje (com CTA "Começar agora →") + 2 bloqueados por data + 1 sábado bloqueado por critério.
- **`prefers-reduced-motion` global** no protótipo: bloco `@media` zera animações e transições, ancoragem usa `behavior:'instant'` independentemente — sem dois caminhos de código.
- **`:has()` é progressive enhancement** — o respiro extra antes do nó de sábado é garantido pelo `.trail-node--saturday::before` (sempre aplicado); o `li:has(.trail-node--saturday){margin-top:--space-2}` apenas reforça em browsers modernos. Função não quebra em browser sem `:has()`.
- Fontes **não importadas** no protótipo (decisão consciente): mantém o arquivo offline/autocontido e usa o fallback stack `Bricolage Grotesque, Inter, system-ui, …` — fontes reais entram quando o pipeline de produção for ligado em US futura.

### Impacto

- Arquivos novos: `doc/UX/03-prototipo-trilha.md`, `doc/UX/prototypes/trilha.html`.
- Arquivos editados: `doc/changelog.md`, `doc/release_notes/unreleased.md` (este).
- Nenhuma alteração em código Java, migrations, templates `src/**`, `static`, `properties` ou `pom.xml`.
- **Destrava:** chore-ux-008 (smoke visual end-to-end consome este HTML como uma das telas-âncora).
- **Antecipa:** US-018, US-019, US-020, US-021 (Sprint 7) — quando essas US entrarem, a implementação real consumirá este contrato visual + microcopy.

### Como testar

1. Abrir `doc/UX/03-prototipo-trilha.md` e confirmar presença das 9 seções obrigatórias (§1..§9) + §10 Pendências + §11 Histórico.
2. Conferir que §5 Componentes e §6 Tokens citam **nominalmente** referências de `doc/UX/02-componentes-base.md` e `doc/UX/01-design-tokens.md` — sem reinventar.
3. Confirmar que §9 lista **pelo menos 3 decisões** registradas (entregues 5).
4. Verificar que toda microcopy de §7 é em pt-BR e que §7.4 lista o vetado vs. o aceito (P11).
5. Confirmar que §3 trata o caso especial sábado (US-020) com microcopy "Termine mais N sessões para abrir o sábado".
6. Abrir `doc/UX/prototypes/trilha.html` direto no navegador (Live Server, `file://`, ou qualquer servidor estático). Verificar:
   - Carrega sem erro de console, sem requisição externa além do CDN do Alpine.
   - Header sticky no topo, bottom-nav sticky no rodapé com "Hoje" destacado.
   - Cabeçalho da semana com overline `SEMANA 3`, h1 placeholder, subtítulo, link "Trimestre →".
   - 7 nós renderizados: 2 concluídos (badge "Concluído"), 1 em progresso (badge "Continuar" + barra 60%), 1 hoje (borda 2px coral + badge "HOJE" + CTA "Começar agora →"), 2 bloqueados (badge "Bloqueado", popover ao tocar com "Esta sessão abre em..."), 1 sábado (dois badges empilhados "Sábado" + "Bloqueado", microcopy "Termine mais 3 sessões para abrir o sábado").
   - Ao carregar, a página rola automaticamente para deixar o nó "hoje" centrado no viewport.
   - Em DevTools mobile (iPhone SE 375×667 ou viewport forçado 320×568): zero scroll horizontal, nenhum elemento cortado, todos os touch targets ≥ 44×44.
7. Cruzar tokens consumidos no `<style>` com `doc/UX/01-design-tokens.md` — hex de `--color-primary-500` (#F25C54), `--color-secondary-500` (#7BC42F), `--color-neutral-50` (#F7F4F1) batem com a identidade.

### Gaps visuais e manuais declarados

- **Validação visual em 320px exato** (DevTools forçado): pendente de validação manual do humano. O CSS aplica `max-width: 38rem` no main, padding lateral `--space-4`, `min-height: var(--space-11)` em todos os interativos — estrutura é correta, mas medição pixel-a-pixel cabe ao humano.
- **Renderização real com fontes Bricolage Grotesque e Inter**: o protótipo usa o fallback stack para manter o arquivo offline. Quando o pipeline de produção carregar as fontes, a hierarquia tipográfica pode ganhar mais personalidade no h1.
- **Microanimação Lottie de desbloqueio do sábado**: documentada em §3, **não implementada** — entra com a US-020.
- **Ilustração editorial no cabeçalho**: §9.4 explica por que o slot está reservado mas vazio — depende do estilo de ilustração ainda não fechado (chore-ux-001 §7).

### Decisões registradas

- Lista vertical em vez de mapa sinuoso SVG (custo de largura útil em 320px, custo de acessibilidade do `aria-flowto`, dependência de ilustrações ainda não fechadas).
- Badge textual como reforço de estado em vez de cor pura (redundância de canal — WCAG / chore-ux-001 §5.3).
- Header `compact` (não `expanded`) — trilha é raiz, sem botão voltar; o h1 do `<main>` carrega o título da semana.
- Sem ilustração editorial placeholder — slot marcado em comentário HTML, virá quando o estilo for definido.
- `scrollIntoView({behavior: 'instant'})` em vez de `'smooth'` — evita duplicar caminho de código para `prefers-reduced-motion` e respeita adolescente que abre o app sabendo o que quer.
