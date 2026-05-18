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
