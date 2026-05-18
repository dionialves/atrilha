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
