# Protótipo da sessão diária — atrilha

**Task:** chore-ux-005 (Issue #24)
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M4 — Protótipo da sessão diária validado
**Status:** Proposto
**Depende de:** `doc/UX/00-identidade-visual.md` (chore-ux-001, aprovada) · `doc/UX/01-design-tokens.md` (chore-ux-002) · `doc/UX/02-componentes-base.md` (chore-ux-003) · `doc/UX/03-prototipo-trilha.md` (chore-ux-004)
**Bloqueia:** chore-ux-008 (smoke visual end-to-end)
**Antecipa:** US-023, US-024, US-025, US-026 (sprints 8–9)
**Referências:** PRD §4.3 (P1 — não-moralista, P8 — privado, P11 — sem FOMO, P12 — mobile-first) · PRD §6.1 (5 blocos da sessão, ARC bíblico — ADR-008) · PRD §8.4 (RNF-A11Y-01..05, RNF-COMP-04) · PRD §10 (RF-E4-01..12) · AGENTS.md (idioma pt-BR na UI, stack Thymeleaf + HTMX + Alpine + Lottie — ADR-011)

**Escopo deste doc:** especificar **em prosa** a sessão diária inteira — os 5 blocos sequenciais (gancho, núcleo, quiz, reflexão, fechamento), o layout estrutural comum, a retomada (US-025), os estados visuais, a microcopy e a acessibilidade. A **materialização navegável** vive em `doc/UX/prototypes/sessao-bloco.html` (bloco núcleo — referência mestra). Esta é arte conceitual: nada aqui vira código de produção em `src/**` — a implementação real entra nas US-023 a US-026 dos sprints 8–9.

**Declarações de escopo (essenciais antes de prosseguir):**

1. **Nomes próprios das sessões** (ex.: nome da sessão de quarta) e **nomes definitivos dos blocos** **não são fechados aqui** — virão de `doc/conteudo/fluxo-semana.md` (pré-requisito da US-023, critério 10). Usamos placeholders explícitos: `<<Título da sessão>>`, `<<Pergunta-gancho>>`, etc. Os nomes internos dos 5 blocos (gancho, núcleo, quiz, reflexão, fechamento) seguem PRD §6.1 e US-023 como **terminologia técnica do spec**, não como microcopy obrigatória.
2. **Mecânicas alternativas de quiz** (V/F, ordenar, drag-and-drop, completar frase, caça-palavras, escolha múltipla com imagens, etc.) **não são especificadas aqui**. O bloco "quiz" desta spec cobre **apenas** o quiz de múltipla escolha como representante genérico do MVP. As mecânicas alternativas serão specs próprios das US-027 a US-035 (sprints 10–12).

---

## 1. Visão geral da sessão diária

A sessão diária é o **núcleo de consumo** do produto (PRD §6.1, US-023): cinco blocos sequenciais que Júlia percorre em até **10 minutos**. É a tela onde a adolescente passa a maior parte do tempo dentro do app, e onde a marca atrilha precisa entregar simultaneamente leveza (P15), respeito ao espaço interno (P8) e clareza (P12) — sem cair em devocional adulto, sem cair em "joguinho educativo infantil", sem nunca cobrar (P11).

### 1.1 Os 5 blocos (nomes internos do spec)

| # | Bloco | Função | Duração-alvo | Interação principal |
|---|---|---|---|---|
| 1 | Gancho | Entrar no tema do dia, despertar curiosidade. | ~30s de leitura | Apenas avançar |
| 2 | Núcleo | Apresentar o conteúdo: contexto + texto bíblico ARC + aplicação. | ~3–4 min | Swipe horizontal entre 3–4 cards |
| 3 | Quiz | Consolidar entendimento com 2 perguntas curtas. | ~2 min | Múltipla escolha (MVP — outras mecânicas em US-027..035) |
| 4 | Reflexão | Espaço **privado** para Júlia escrever o que pensou (opcional). | ~2 min | Textarea ≤1000 chars |
| 5 | Fechamento | Celebrar a conclusão e abrir curiosidade pelo dia seguinte. | ~30s | Encerrar e voltar à trilha |

Soma das durações-alvo: ~8 minutos. Folga de ~2 minutos cabe nas transições entre blocos e em variações de ritmo de leitura.

### 1.2 Indicador "passo X de 5"

Visível em **todos** os blocos (US-023 critério 6). É a barra de progresso fina logo abaixo do header reduzido (ver §2). Avança em quintos:

| Bloco em curso | Preenchimento | Microcopy do leitor de tela |
|---|---|---|
| 1 — Gancho | 20% | "Passo 1 de 5" |
| 2 — Núcleo | 40% | "Passo 2 de 5" |
| 3 — Quiz | 60% | "Passo 3 de 5" |
| 4 — Reflexão | 80% | "Passo 4 de 5" |
| 5 — Fechamento | 100% | "Passo 5 de 5" |

O indicador é o **único** elemento de "gamificação visível" durante o consumo. Não há barra de tempo, contador regressivo nem qualquer pressão temporal (P11 — sem FOMO).

### 1.3 Botão sair (X) a qualquer momento

Presente no canto superior direito do header em **todos** os blocos. Toque salva o estado atual (servidor em produção via HTMX a cada mudança de bloco; aqui apenas descrevemos) e devolve Júlia à trilha. O nó da sessão na trilha passa ao estado **"em progresso"** (chore-ux-004 §3) e a próxima abertura retoma exatamente o bloco onde parou (US-025).

### 1.4 Sem bottom-nav durante a sessão

A sessão diária **oculta a bottom-nav** (chore-ux-003 §6). Decisão de foco: o usuário entra em modo de leitura/reflexão; outras áreas do app ficam fora do alcance. Para sair existe o botão X explícito, não a bottom-nav. Decisão conservadora — espelha o padrão de apps de leitura (Headspace, Calm).

---

## 2. Layout estrutural comum aos 5 blocos

Cada um dos 5 blocos compartilha a mesma estrutura geral em viewport mobile (320px de referência, conforme RNF-COMP-04 / chore-ux-001 §5). Variações específicas vivem dentro do `<main>`.

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │  Header compact (ux-003 §5) — título + close
└──────────────────────────────────────┘
│ ▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  Barra de progresso (passo X de 5) — fina, 4px
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│                                      │
│                                      │
│  ÁREA DE CONTEÚDO DO BLOCO           │  scrollable verticalmente quando necessário
│  (varia por bloco — §§3 a 7)         │
│                                      │
│                                      │
│                                      │
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│   [  Continuar / Encerrar  →  ]      │  Footer fixed-bottom, CTA primário lg full-width
└──────────────────────────────────────┘
           ▔▔▔▔▔ safe-area-inset-bottom
```

### 2.1 Header reduzido (compact)

- Variante `compact` do componente Header (chore-ux-003 §5.3), altura ~56px.
- **Leading:** título visível da sessão (`<<Título da sessão>>` — placeholder; nome próprio virá de `fluxo-semana.md`). Tipografia: `--font-display`, `--font-weight-semibold`, `--text-lg`. Trunca com `text-overflow: ellipsis` se exceder largura útil.
- **Trailing:** botão `icon-only` com X (24×24, SVG stroke), `aria-label="Fechar sessão"`. Min 44×44 (touch target).
- Sem botão "voltar" durante a sessão — a única saída é o X (consciente, não acidental).
- Sticky no topo (`position: sticky; top: 0; z-index: var(--z-sticky)`), com sombra `--shadow-sm` ao rolar (igual à trilha).

### 2.2 Barra de progresso fina

- Logo abaixo do header (gruda visualmente nele — sem gap).
- Altura: 4px. Fundo: `--color-neutral-100`. Preenchimento: `--color-primary` (coral).
- Implementada como `<progress>` nativo OU `<div role="progressbar" aria-valuenow="2" aria-valuemin="0" aria-valuemax="5">` — ver §12.
- **Animação:** transição de 240ms com `--ease-out-soft` quando avança de bloco (em motion-reduced, snap imediato).
- Texto associado oculto para leitor de tela (`<span class="sr-only">Passo 2 de 5</span>`) — sintetiza o estado quando o foco visita a barra.

### 2.3 Área de conteúdo

- Padding lateral `--space-4` (16px), padding-top `--space-6` (24px), padding-bottom `--space-12` (48px — espaço para o footer fixed).
- Largura máxima `38rem` (chore-ux-001 §5.4) — conforto de leitura em tablet/desktop.
- Scrollable verticalmente quando o conteúdo extrapola o viewport (sem scroll horizontal — vetado em todo o app).

### 2.4 Footer fixo no rodapé

- `position: fixed; bottom: 0`. Largura full. Fundo `--color-surface` opaco com borda superior `--color-divider` 1px.
- Padding `--space-4` (lateral) + `env(safe-area-inset-bottom)`.
- Contém **um único** CTA primário (Button variant `primary`, size `lg`, full-width — chore-ux-003 §1.3). Label varia por bloco ("Continuar" / "Próxima" / "Encerrar").
- Em mobile, fica na zona do polegar (chore-ux-001 §5.5 — densidade decresce do topo para a base, ação na base).
- **Não há bottom-nav durante a sessão** — esta faixa é exclusiva da CTA.

### 2.5 Touch targets ≥ 44×44

Aplicado em **todo** elemento interativo (CTA, botão X, dots de carrossel, opções de quiz, botões prev/next do carrossel). Padrão estabelecido por chore-ux-002 (`--space-11`) e chore-ux-003 §0.2.

---

## 3. Bloco 1 — Gancho (US-023 critério 1)

### 3.1 Objetivo

Entrar no tema do dia em **até 30 segundos de leitura**. É a primeira impressão da sessão: cria gancho de curiosidade, não exige decisão complexa, libera Júlia para avançar.

### 3.2 Estrutura

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │
├──────────────────────────────────────┤
│ ▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │  20% — passo 1 de 5
├──────────────────────────────────────┤
│                                      │
│       [ilustração editorial]         │  Slot reservado — placeholder
│                                      │
│   <<Título curto do gancho>>         │  h2 (display, semibold, text-xl)
│                                      │
│   <<Parágrafo de 2 a 4 linhas>>      │  body-lg (sans, regular, text-lg)
│   <<que apresenta o tema do dia.>>   │
│                                      │
├──────────────────────────────────────┤
│        [  Continuar  →  ]            │  CTA primary lg
└──────────────────────────────────────┘
```

### 3.3 Componentes consumidos

- **Header compact** (chore-ux-003 §5).
- **Barra de progresso** (componente inline — ver §9.1).
- **Button** variant `primary` size `lg` para CTA "Continuar" (chore-ux-003 §1.3).
- Tipografia: h2 em `--font-display` / `--text-xl`; corpo em `--font-sans` / `--text-lg` (`body-lg` da chore-ux-001 §4.2 — texto narrativo).

### 3.4 Slot de ilustração

Reservado para ilustração editorial coerente com a identidade (chore-ux-001 §5.8). **Não renderizamos placeholder genérico** — o espaço fica vazio com nota explícita até o estilo de ilustração estar definido (dúvida aberta em chore-ux-001 §7).

### 3.5 Sem interação obrigatória

Nenhum input, nenhuma escolha. Apenas o CTA "Continuar". Decisão deliberada: o gancho precisa fluir, não trancar.

---

## 4. Bloco 2 — Núcleo (US-023 critério 2)

### 4.1 Objetivo

Apresentar o conteúdo do dia em **3 a 4 cards swipáveis**: contexto, texto bíblico (ARC — ADR-008), explicação opcional, ponte para aplicação. É o coração da sessão e o bloco visualmente mais complexo — daí a escolha como **referência mestra** do HTML protótipo (`sessao-bloco.html`).

### 4.2 Estrutura

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │
├──────────────────────────────────────┤
│ ▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░  │  40% — passo 2 de 5
├──────────────────────────────────────┤
│                                      │
│   ◀  ┌────────────────────────┐  ▶   │
│      │                         │      │
│      │   Card N de 3           │      │   ← Card swipável + botões prev/next
│      │                         │      │
│      │   <<conteúdo do card>>  │      │
│      │                         │      │
│      └────────────────────────┘      │
│                                      │
│        ●  ○  ○                       │   ← Dot indicator (3 cards no MVP)
│                                      │
├──────────────────────────────────────┤
│        [  Continuar  →  ]            │   CTA habilitada após chegar ao último card
└──────────────────────────────────────┘
```

### 4.3 Os 3 cards do MVP (referência mestra)

Mínimo definido pela US-023 critério 2 (3 a 4 cards). O protótipo usa **3 cards** como caso-base. O quarto card (explicação) é opcional e fica para a decisão do conteudista por sessão.

| Card | Tipo | Conteúdo |
|---|---|---|
| 1 | **Contexto** | Parágrafo curto (~3–4 linhas) sobre o cenário histórico/narrativo da passagem. `--font-sans`, `--text-base`. |
| 2 | **Texto bíblico (ARC)** | Versículo(s) com **tratamento tipográfico distintivo** (ver §4.4) + referência bibliográfica (livro, capítulo, versículos) abaixo, em `--text-sm` muted. |
| 3 | **Aplicação** | Pergunta-gancho de 1–2 linhas que prepara o quiz. `--font-sans`, `--text-lg`, peso semibold. |

### 4.4 Tratamento tipográfico do card bíblico

O card de texto bíblico recebe **tipografia distinta** dos outros dois, sinalizando visualmente "isto é a Escritura, não comentário":

- **Família:** `--font-display` (Bricolage Grotesque) em vez de `--font-sans`. Personalidade editorial moderna sem cair em serifa "Bíblia antiga" (chore-ux-001 §4.3 — vetamos serifa decorativa).
- **Peso:** `--font-weight-semibold` (600).
- **Tamanho:** `--text-lg` (18px) — leitura confortável de versículo.
- **Line-height:** `1.5` (cabe em `--text-lg--line-height` da chore-ux-002).
- **Cor:** `--color-text-display` (`neutral-900`) — alta hierarquia.
- **Referência abaixo** (livro, cap., vv.): `--font-sans`, `--text-sm`, `--color-text-muted`, com prefixo `—` (travessão) ou apenas a referência centralizada.
- **Sem itálico decorativo, sem aspas curvas grandes, sem capitular** — todas leituras "devocional adulta sóbria" rejeitadas pela chore-ux-001 §1.

Esta é a única superfície da sessão que usa `--font-display` em corpo de texto — em todas as outras `--font-display` aparece apenas em headings.

### 4.5 Comportamento do carrossel

#### 4.5.1 Navegação

- **Swipe horizontal** (gesto touch) — `touchstart` + `touchend` com cálculo de delta X (limiar ~50px).
- **Botões prev/next** visíveis lateralmente (`◀` `▶`, SVG stroke 16×16, dentro de botões com `min 44×44`). Necessários para **acessibilidade** — não pode depender só de gesto (chore-ux-003 §0.2 + chore-ux-001 §5.3 — redundância de canal).
- **Dots clicáveis** (`●` ativo, `○` inativos) abaixo do card. Cada dot tem `aria-label="Card N de 3"` e tamanho clicável ≥ 24×24 (mínimo confortável para dots).
- **Foco do teclado:** Tab navega entre prev → card (focável com `tabindex="0"`) → next → dots. Setas ← / → no card mudam de slide (decisão conservadora — fica para a US-023 confirmar se implementa setas ou apenas tab+Enter no botão).

#### 4.5.2 Transição entre cards

- Slide horizontal de 240ms (`--duration-base`) com `--ease-out-soft`.
- Em `prefers-reduced-motion`, snap imediato (transição zerada pela regra global).

#### 4.5.3 CTA "Continuar"

Habilitada após Júlia chegar ao **último card** (estado: `current === cards.length - 1`). Decisão conservadora entre duas opções:

- **(escolhido)** "Trava suave" — CTA fica desabilitada visualmente (`opacity: 0.5`, `cursor: not-allowed`, `disabled` HTML) enquanto não há leitura completa. Mensagem `aria-describedby`: "Avance pelos cards para continuar."
- **(rejeitado)** "Apenas micro-feedback" — CTA sempre ativa, mas se clicada antes do último card, um toast aparece: "Você ainda tem mais cards aí." Rejeitado porque toast em sessão é interrupção; trava clara é menos custo cognitivo.

### 4.6 Componentes consumidos

- **Header compact**.
- **Barra de progresso** (componente inline — ver §9.1).
- **Button** variant `primary` size `lg` para CTA "Continuar".
- **Button** variant `ghost` `icon-only` para botões prev/next (chore-ux-003 §1.3).
- **Card** variant `raised` para os 3 cards do carrossel (chore-ux-003 §3.3) — destaque sutil sobre o fundo `--color-bg`.
- **Dot indicator** — **componente novo, não catalogado em chore-ux-003** (ver §9.2 — lacuna identificada).

---

## 5. Bloco 3 — Quiz (US-023 critério 3)

### 5.1 Objetivo

Consolidar entendimento do conteúdo do núcleo com **2 perguntas sequenciais**. Acerto/erro é informativo, **nunca punitivo** (P1, P11). Cada resposta abre uma explicação curta — independentemente de ter acertado ou errado (US-023 critério 3).

### 5.2 Escopo do MVP — apenas múltipla escolha

Este spec cobre exclusivamente o quiz de **múltipla escolha** (uma pergunta, 3–4 opções, uma correta). Outras mecânicas (V/F, ordenar, drag, caça-palavras, completar frase, escolha com imagens) virão em **specs próprios das US-027 a US-035** nos sprints 10–12.

### 5.3 Estrutura

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │
├──────────────────────────────────────┤
│ ▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░  │  60% — passo 3 de 5
├──────────────────────────────────────┤
│                                      │
│   Pergunta 1 de 2                    │  caption (text-xs, muted)
│                                      │
│   <<Enunciado da pergunta?>>         │  h2 (display, semibold, text-xl)
│                                      │
│   ┌────────────────────────────┐    │
│   │ A) <<Opção 1>>             │    │  Card variant interactive
│   └────────────────────────────┘    │
│   ┌────────────────────────────┐    │
│   │ B) <<Opção 2>>             │    │
│   └────────────────────────────┘    │
│   ┌────────────────────────────┐    │
│   │ C) <<Opção 3>>             │    │
│   └────────────────────────────┘    │
│   ┌────────────────────────────┐    │
│   │ D) <<Opção 4>>             │    │
│   └────────────────────────────┘    │
│                                      │
│   (Após seleção)                     │
│   ┌────────────────────────────┐    │
│   │ ✓ Isso mesmo!              │    │  Feedback (variant success ou info)
│   │ <<Explicação curta>>       │    │
│   └────────────────────────────┘    │
│                                      │
├──────────────────────────────────────┤
│       [  Próxima  →  ]               │  CTA habilitada após escolha
└──────────────────────────────────────┘
```

### 5.4 Comportamento

#### 5.4.1 Seleção

- Cada opção é um Card variant `interactive` (chore-ux-003 §3.3) com radio semântico oculto. Ao clicar:
  1. A opção escolhida ganha borda 2px na cor do feedback (acerto: `--color-secondary-500`; erro: `--color-danger-700`).
  2. A opção **correta** (se Júlia errou) ganha borda 2px `--color-secondary-500` para mostrar qual era.
  3. Demais opções recebem `opacity: 0.6` e ficam não-clicáveis.
  4. O bloco de feedback (§5.4.2) aparece logo abaixo das opções, com `aria-live="polite"` para anúncio por leitor de tela.
  5. A CTA "Próxima" habilita.

#### 5.4.2 Feedback com explicação (P11)

Imediato após a escolha. Variantes:

| Resultado | Variante visual | Microcopy do título | Microcopy da explicação |
|---|---|---|---|
| Acerto | `success` (`--color-success-100` / `--color-success-700`) com ícone `✓` | `Isso mesmo!` | `<<Por que esta é a melhor resposta — 2 a 3 linhas.>>` |
| Erro | `info` (`--color-info-100` / `--color-info-700`) com ícone `ⓘ` — **deliberadamente não-`danger`** | `Quase!` ou `Tente lembrar disto:` | `<<Explicação da resposta correta — 2 a 3 linhas.>>` |

**Por que erro usa `info` e não `danger`:** vermelho de erro tem peso afetivo de "você fez algo errado" — Júlia tem 14 anos e o app não está fiscalizando moral. A explicação é uma oportunidade de aprender, não uma reprimenda. P1 (não-moralista) + P11 (sem cobrança) na superfície visual.

#### 5.4.3 Microcopy proibida (P1, P11)

| ❌ Vetado | ✅ OK |
|---|---|
| `Você errou.` | `Quase!` |
| `Resposta incorreta.` | `Tente lembrar disto:` |
| `Você errou de novo.` | `Olha só:` |
| `-10 pontos.` | (Sem penalidade visível — quiz não gera XP negativo) |
| `Lembre-se de estudar mais.` | `<<Explicação direta da resposta correta.>>` |

A frase `Tente de novo` é OK **apenas** se houver retry (não há no MVP — cada pergunta é única). Decisão: sem retry. A explicação substitui o retry — Júlia avança aprendendo.

### 5.5 Componentes consumidos

- **Header compact**, **barra de progresso**.
- **Card** variant `interactive` (chore-ux-003 §3.3) para cada opção.
- **Badge** / **Toast inline** para o bloco de feedback — na prática um Card variant `flat` com borda esquerda colorida (padrão Toast da chore-ux-003 §8.3, mas embutido no fluxo, não flutuante).
- **Button** variant `primary` size `lg` para CTA "Próxima" / "Continuar".

### 5.6 Sequência das 2 perguntas

- **Pergunta 1 → escolha → feedback → CTA "Próxima"**: Júlia avança para a pergunta 2 dentro do mesmo bloco (sem trocar de bloco no indicador 3/5).
- **Pergunta 2 → escolha → feedback → CTA "Continuar"**: avança para o bloco 4 (Reflexão).
- O caption no topo (`Pergunta N de 2`) reflete o estado.

---

## 6. Bloco 4 — Reflexão (US-024)

### 6.1 Objetivo

Espaço **privado** para Júlia escrever o que pensou sobre o tema do dia. **Opcional** — Júlia pode avançar sem digitar nada (US-024 critério 2). Texto até 1000 caracteres (US-024 critério 5). Etiqueta "privado" visível enquanto o campo está em foco (US-024 critério 1).

### 6.2 Estrutura

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │
├──────────────────────────────────────┤
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░  │  80% — passo 4 de 5
├──────────────────────────────────────┤
│                                      │
│   <<Pergunta de reflexão em uma     │  h2 (display, semibold, text-xl)
│   linha ou duas.>>                   │
│                                      │
│                            ⓘ privado │  Badge (info, sm) — visível quando campo em foco
│   ┌────────────────────────────┐    │
│   │                             │    │
│   │   Escreva o que você        │    │  Textarea (chore-ux-003 §2.3)
│   │   pensou…                   │    │  placeholder muted
│   │                             │    │
│   │                             │    │
│   └────────────────────────────┘    │
│                       restam 1000    │  Contador (text-xs, muted) — aria-live polite
│                                      │
│   Sua reflexão é privada. Você       │  Helper (text-xs, muted, ~70 chars)
│   pode compartilhar com seu          │
│   responsável depois, item por       │
│   item, se quiser.                   │
│                                      │
├──────────────────────────────────────┤
│       [  Continuar  →  ]             │  CTA SEMPRE habilitada
└──────────────────────────────────────┘
```

### 6.3 Etiqueta "privado"

- Badge variant `info` size `sm` (chore-ux-003 §7.3), label `privado`, ícone `ⓘ` opcional.
- **Aparece quando o textarea está em foco** (US-024 critério 1) e **permanece visível** até o foco sair.
- Posicionada **acima do textarea**, alinhada à direita, com gap `--space-2`.
- Implementação Alpine: `x-data="{ focused: false }"` com `@focusin="focused = true"` e `@focusout="focused = false"` no contêiner. Badge usa `x-show="focused"`.

### 6.4 Contador "restam X"

- Variante do componente Input/textarea (chore-ux-003 §2.2).
- Posicionado abaixo do textarea, alinhado à direita.
- Texto: `restam <N>` onde `N = 1000 - value.length`.
- **`aria-live="polite"`** quando aproxima do limite (US-024 critério 5 + chore-ux-003 §0). Decisão: anunciar quando `N <= 100` (mudança a cada 10 chars consumidos a partir daí, para não saturar o leitor de tela).
- **Cor:** `--color-text-muted` por default; troca para `--color-danger-700` quando `N <= 50` (chore-ux-003 §2.2).
- **Trava no servidor (Jakarta `@Size(max=1000)`) + atributo `maxlength="1000"` no textarea** — não é o JS que protege o limite, é o input nativo.

### 6.5 CTA sempre habilitada

A CTA "Continuar" **nunca** fica desabilitada neste bloco — Júlia pode avançar com campo vazio (US-024 critério 2). Decisão deliberada contra dois padrões alternativos rejeitados:

| Padrão | Decisão |
|---|---|
| Botão secundário "Pular" + CTA "Continuar" habilitada só se texto preenchido | **Rejeitado.** Cria dois caminhos visuais; "pular" lê como "esta era a opção esperada e você está fugindo". P8 — campo é opcional, não é um teste a pular. |
| CTA única "Continuar" sempre habilitada, helper explica que campo é opcional | **Escolhido.** Uma decisão visual, sem hierarquia entre "escrever" e "não escrever". |

### 6.6 Helper text — privacidade explícita (P8)

Texto fixo abaixo do contador:

> Sua reflexão é privada. Você pode compartilhar com seu responsável depois, item por item, se quiser.

Esta frase carrega três decisões:

1. **"é privada"** — afirma o estado default (P8 + US-024 critério 4).
2. **"pode compartilhar"** — declara que o controle é da usuária (preview de US-047 — opt-in individual).
3. **"item por item"** — antecipa que não há compartilhamento "tudo ou nada", reduzindo medo.

Tipografia: `--font-sans`, `--text-xs`, `--color-text-muted`. Não ocupa espaço hierárquico — é nota de rodapé conceitual.

### 6.7 Preservação do texto digitado (US-024 critério 3, US-025)

Se Júlia sair da sessão no meio da reflexão (toque no X), o texto digitado é **preservado** para quando voltar. Em produção: HTMX POST a cada `blur` do textarea (debounce ~1s); o servidor persiste em `session_progress`. No protótipo, apenas documentamos — implementação concreta é responsabilidade da US-025.

### 6.8 Componentes consumidos

- **Header compact**, **barra de progresso**.
- **Input** variant `textarea` (chore-ux-003 §2.3) com contador.
- **Badge** variant `info` size `sm` para etiqueta "privado".
- **Button** variant `primary` size `lg` para CTA "Continuar".

---

## 7. Bloco 5 — Fechamento (US-026)

### 7.1 Objetivo

Celebrar a conclusão da sessão **sem cobrança** (P11), atualizar o estado motivacional (XP, streak) e abrir curiosidade pelo dia seguinte. Tom: "valeu a pena" + "vejo você amanhã" — **nunca** "se você não voltar amanhã perde tudo".

### 7.2 Estrutura

```
┌──────────────────────────────────────┐
│ <<Título da sessão>>          [✕]    │
├──────────────────────────────────────┤
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░  │  100% — passo 5 de 5
├──────────────────────────────────────┤
│                                      │
│         [Lottie sutil]               │  Microanimação 600–800ms — celebração curta
│                                      │
│             +15 XP                   │  display-lg (Bricolage, bold, text-3xl), tabular-nums
│                                      │
│         Total: 240 XP                │  body-sm (sans, regular, text-sm, muted)
│                                      │
│   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━     │  Divisor sutil (--color-divider)
│                                      │
│         Sequência                    │  caption (text-xs, semibold, uppercase, muted)
│            5 dias                    │  display-lg + ícone (pico ou onda, NÃO chama)
│                                      │
│   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━     │
│                                      │
│   Amanhã                             │  caption
│   ┌────────────────────────────┐    │
│   │ <<Título da próxima        │    │  Card variant flat
│   │   sessão.>>                │    │
│   │ <<Gancho de 1 linha.>>     │    │
│   └────────────────────────────┘    │
│                                      │
├──────────────────────────────────────┤
│        [  Encerrar  ]                │  CTA primary lg full-width
└──────────────────────────────────────┘
```

### 7.3 XP ganho na sessão (US-026 critério 1)

- **Número grande** em destaque: `+15 XP` (placeholder — valor real virá do backend).
- Tipografia: `--font-display`, `--font-weight-bold`, `--text-3xl` (32px). Cor `--color-text-display` ou `--color-primary` para celebração — decisão escolhida: `--color-text-display` para manter "cor de marca escassa" (chore-ux-001 §5.6); o coral fica apenas para a CTA.
- `font-variant-numeric: tabular-nums` para evitar "pular" quando o número for animado (chore-ux-002 §3.5).
- **Animação opcional** (Lottie sutil) — ver §7.6.

### 7.4 XP total atualizado (US-026 critério 1)

Linha menor abaixo do XP ganho: `Total: 240 XP`. Tipografia `--text-sm`, `--color-text-muted`. Sem destaque competindo com o número grande.

### 7.5 Streak — sem clichê de chama (US-026 critério 2)

- Caption "Sequência" (em vez de "Streak" — pt-BR; decisão de microcopy).
- Número de dias: `5 dias` em `--text-3xl` semibold + ícone à esquerda.
- **Ícone:** **não usar chama 🔥** (clichê de produtividade adulta + leitura de "queima" — moralista). Opções alternativas registradas para decisão futura:
  - **Pico de montanha** (SVG stroke triangular ascendente) — escolha provisória do spec, conversa com a metáfora "trilha".
  - Onda (SVG stroke curva).
  - Seta diagonal ascendente.
  - Pontos crescentes (3 dots em escala).

Decisão final fica para o conteudista/Designer no momento da US-026; aqui registramos o veto à chama e a escolha provisória do pico.

### 7.6 Onde caberia a Lottie (ADR-011)

- **Posição:** acima do XP ganho, em um quadrado de ~120×120px (mobile) / ~160×160px (desktop).
- **Conteúdo:** celebração curta — confete leve, brilho expandindo, ou marca-trilha pulsando. **Não obrigatório implementar no HTML do protótipo** (decisão da Issue #24 — pode ser placeholder estático).
- **Duração:** 600–800ms, autoplay uma única vez (sem loop). Decisão deliberada — loops criam ansiedade visual.
- **Fallback `prefers-reduced-motion`:** quando o sistema do usuário pede movimento reduzido, a Lottie é **substituída por uma ilustração estática** (SVG do estado final do quadro). Implementação: `<picture>` com `<source media="(prefers-reduced-motion: reduce)" srcset="static.svg">` ou Alpine `x-if` baseado em `window.matchMedia('(prefers-reduced-motion: reduce)').matches`.
- **No protótipo HTML:** placeholder estático `[Lottie sutil]` em texto, com nota de implementação em comentário HTML.

### 7.7 Prévia do próximo dia (US-026 critério 3)

- Caption "Amanhã".
- Card variant `flat` (chore-ux-003 §3.3) com:
  - **Título** da próxima sessão (placeholder — virá de `fluxo-semana.md`).
  - **Gancho de 1 linha** (~60–80 caracteres).
- **Sem revelar conteúdo crítico** (US-026 critério 3) — apenas um isqueiro de curiosidade.
- **Sem CTA "ir amanhã" ou "agendar lembrete"** — Júlia decide quando voltar; o app não faz pressão (P11).

### 7.8 CTA "Encerrar" (US-026 critério 5)

- Button variant `primary` size `lg` full-width — padrão do footer fixed.
- Label: `Encerrar` (não "Concluir", não "Finalizar" — soa cerimonioso; "Encerrar" é o mesmo verbo do PRD).
- Ação: marca a sessão como concluída no backend (HTMX POST), navega de volta à trilha. O nó da sessão na trilha passa ao estado **concluído** (chore-ux-004 §3).

### 7.9 Microcopy — tom "valeu a pena, vejo você amanhã" (P11)

| ❌ Vetado | ✅ OK |
|---|---|
| `Não perca o streak!` | (sem texto — número fala) |
| `Volte amanhã ou perde tudo.` | `Amanhã: <<título da próxima>>` |
| `Você ficou para trás.` | (não existe — sessão concluída é sucesso, ponto) |
| `🔥 Continue forte!` | (sem emoji decorativo) |
| `Última chance!` | (não existe — sem prazo final) |
| `Você foi melhor que 73% dos usuários!` | (sem comparação social — P11) |

### 7.10 Componentes consumidos

- **Header compact**, **barra de progresso** (100%).
- **Card** variant `flat` para prévia do amanhã.
- **Button** variant `primary` size `lg` para CTA "Encerrar".
- Tipografia display + tabular-nums em XP e streak.

---

## 8. Retomada (US-025)

### 8.1 Salvar a cada bloco

Em produção, cada transição entre blocos dispara um HTMX POST que atualiza `session_progress` no servidor com `(usuarioId, sessaoId, blocoAtual, respostasQuiz, textoReflexao)`. O toque no X também salva. Esta task **não implementa** o backend — apenas registra o contrato.

### 8.2 Sinal visual na trilha (chore-ux-004 §3)

Quando Júlia sai da sessão no meio de qualquer bloco e volta à trilha:

- O nó da sessão na trilha passa do estado `disponível ("hoje")` para `em progresso`.
- Visual definido em **chore-ux-004 §3** (linha "Em progresso"):
  - Card variant `raised` com borda `--color-secondary-300`.
  - Badge variant `success` com label "Continuar".
  - Microcopy: `Você parou em <X> de <Y>.` (X = bloco atual concluído, Y = 5 blocos).
  - Barra de progresso interna `--color-secondary-500` sobre `--color-neutral-100`.

### 8.3 Retomar exatamente onde parou (US-025 critério 2)

Ao tocar no nó "em progresso", Júlia entra direto no bloco onde estava:

- **Indicador "passo X de 5"** reflete o bloco atual (não volta para 1).
- **Respostas de quiz** já marcadas (com feedback exibido).
- **Texto de reflexão** já preenchido no textarea.
- **Carrossel do núcleo** posiciona-se no último card visualizado (decisão: `current = lastViewed` salvo no servidor).

### 8.4 Troca de dispositivo (US-025 critério 4)

Como o estado vive no servidor (não em `localStorage`), trocar de celular para tablet preserva tudo. **Nada a especificar visualmente** além do que já está em §8.2 e §8.3 — o protótipo só ilustra; a fonte da verdade é o backend.

### 8.5 Onde NÃO se aplica retomada

- Sessão **concluída** (bloco 5 / "Encerrar" tocado): nó passa a `concluído`, e tocar nele abre **modo revisão** (chore-ux-004 §3, US-019 critério 3) — sem XP, sem streak, sem CTA "Encerrar".

---

## 9. Componentes consumidos

Todos os componentes desta sessão estão (ou deveriam estar) catalogados em `doc/UX/02-componentes-base.md`. Esta seção mapeia cada slot ao componente correspondente e **identifica lacunas**.

| Slot | Componente catalogado? | Variante | Referência |
|---|---|---|---|
| Topo fixo (todos os blocos) | ✅ **Header** | `compact` | chore-ux-003 §5.3 |
| Botão X de fechar | ✅ **Button** | `ghost` + `icon-only` | chore-ux-003 §1.3 |
| CTA fixo do rodapé | ✅ **Button** | `primary` + size `lg` | chore-ux-003 §1.3 |
| Cards do núcleo | ✅ **Card** | `raised` | chore-ux-003 §3.3 |
| Opções de quiz | ✅ **Card** | `interactive` | chore-ux-003 §3.3 |
| Feedback de quiz (acerto/erro) | ✅ **Card** + estilos `--color-{state}-100`/`-700` (inline) | composto | herda chore-ux-003 §8.3 (visual de Toast embutido no fluxo) |
| Textarea de reflexão | ✅ **Input** | `textarea` | chore-ux-003 §2.3 |
| Etiqueta "privado" | ✅ **Badge** | `info` size `sm` | chore-ux-003 §7.3 |
| Contador "restam X" | ✅ Variante do **Input/textarea** | — | chore-ux-003 §2.2 |
| Card de "Amanhã" no fechamento | ✅ **Card** | `flat` | chore-ux-003 §3.3 |
| Barra de progresso "passo X de 5" | ❌ **Lacuna** — ver §9.1 | — | — |
| Dot indicator do carrossel | ❌ **Lacuna** — ver §9.2 | — | — |
| Botões prev/next do carrossel | ✅ **Button** | `ghost` `icon-only` | chore-ux-003 §1.3 (variante adicional) |

### 9.1 Lacuna identificada — Barra de progresso "passo X de 5"

**O que é:** indicador linear de progresso da sessão (5 segmentos, preenche 1/5 a cada bloco concluído).

**Onde aparece:** todos os 5 blocos da sessão, logo abaixo do header.

**Decisão sobre catalogação:**

| Opção | Decisão |
|---|---|
| (A) Adicionar componente "ProgressBar" à chore-ux-003 via patch | **Rejeitado nesta task.** chore-ux-003 está fechada e mergeada; abrir patch retrabalha algo aprovado por elemento muito específico desta tela. |
| (B) Especificar inline neste spec e materializar no HTML | **Escolhido.** Spec inline em §2.2; HTML usa `<div role="progressbar">` com CSS local. |
| (C) Promover à chore-ux-007 (auditoria WCAG + componentes finais) | **Backup** — se a barra de progresso aparecer em outras telas além da sessão, a auditoria final pode oficializá-la como componente. |

**Especificação inline (referência para o Codificador na US-023):**

```html
<div role="progressbar"
     aria-valuenow="2"
     aria-valuemin="0"
     aria-valuemax="5"
     aria-label="Progresso da sessão"
     class="session-progress">
  <div class="session-progress-fill" style="width: 40%;"></div>
</div>
<span class="sr-only">Passo 2 de 5.</span>
```

CSS:
- Altura 4px, fundo `--color-neutral-100`, raio `--radius-full` (pílula).
- Preenchimento `--color-primary` (coral) com transição `width var(--duration-base) var(--ease-out-soft)`.
- Em `prefers-reduced-motion`, transição zerada.

### 9.2 Lacuna identificada — Dot indicator

**O que é:** série de pontos circulares que mostram a posição atual em um carrossel (1 de N).

**Onde aparece:** bloco núcleo (carrossel de cards). Também usado em onboarding (US-002, sprint 1) e poderia aparecer em galerias futuras.

**Decisão sobre catalogação:**

| Opção | Decisão |
|---|---|
| (A) Adicionar componente "DotIndicator" à chore-ux-003 via patch | **Rejeitado nesta task** (mesmo motivo da §9.1). |
| (B) Especificar inline neste spec | **Escolhido para o MVP.** |
| (C) Promover à chore-ux-007 | **Backup** — se aparecer em 2+ telas, chore-ux-007 cataloga. |

**Especificação inline (referência para o Codificador na US-023):**

```html
<div role="tablist" aria-label="Indicador de cards" class="dot-indicator">
  <button type="button" role="tab"
          aria-selected="true" aria-label="Card 1 de 3"
          class="dot dot--active"></button>
  <button type="button" role="tab"
          aria-selected="false" aria-label="Card 2 de 3"
          class="dot"></button>
  <button type="button" role="tab"
          aria-selected="false" aria-label="Card 3 de 3"
          class="dot"></button>
</div>
```

CSS:
- Cada dot: 12×12 visual, mas botão clicável de 32×32 (padding generoso para touch target — `min-width/min-height: var(--space-8)` = 32px, ainda abaixo de 44 mas com área-pai estendida; **alternativa preferida**: dots em wrapper com `padding: var(--space-2)` somando ≥44 no eixo Y).
- Inativo: fundo `--color-neutral-300`, raio `--radius-full`.
- Ativo: fundo `--color-primary`, raio `--radius-full`. `aria-selected="true"`.
- Transição `background-color var(--duration-fast) var(--ease-out-soft)`.

**Decisão sobre `role="tablist"`:** alternativa seria `role="presentation"` + lista de `<button>` sem semântica de tabs. Escolhi `tablist` porque os dots **controlam** o card visível (Alpine `current = index`), o que se enquadra na semântica de tablist/tab. Reversível em revisão de a11y.

---

## 10. Tokens consumidos

Todos referenciados literalmente de `doc/UX/01-design-tokens.md` (chore-ux-002 §10 — bloco `@theme`). Nenhum valor é reinventado.

### 10.1 Cores

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da app durante toda a sessão |
| `--color-surface` | Header, footer fixed, cards do núcleo, opções de quiz, textarea, prévia do amanhã |
| `--color-surface-muted` | Fundo de skeleton de loading (se aplicável) |
| `--color-border` | Borda 1px do textarea em repouso |
| `--color-divider` | Borda inferior do header, borda superior do footer, divisor antes do bloco "Sequência" e "Amanhã" no fechamento |
| `--color-text-display` | Título da sessão no header, h2 dos blocos, XP grande no fechamento, texto bíblico no card 2 do núcleo |
| `--color-text-body` | Corpo dos parágrafos (gancho, contexto, aplicação), label das opções de quiz, valor digitado no textarea |
| `--color-text-muted` | Subtítulos, caption "Pergunta N de 2", helper text de privacidade, contador "restam X", caption "Amanhã" |
| `--color-primary` | Borda 2px da opção selecionada no quiz (acerto), preenchimento da barra de progresso, ícone "Sequência" (provisório), CTA primário, anel de foco |
| `--color-primary-hover` | Hover do CTA primário (desktop) |
| `--color-primary-100` | Fundo do badge "passo X de 5" se decidido sinalizar com cor (não usado no MVP) |
| `--color-primary-700` | Texto sobre fundos primary claros (não usado no MVP) |
| `--color-on-primary` | Texto do CTA primário (branco sobre coral) |
| `--color-secondary-500` | Borda 2px da opção correta no quiz, ícone de "acerto" |
| `--color-success-100` | Fundo do bloco de feedback "Isso mesmo!" no quiz |
| `--color-success-700` | Texto do bloco de feedback de acerto |
| `--color-info-100` | Fundo do bloco de feedback "Quase!" no quiz, fundo do badge "privado" |
| `--color-info-700` | Texto do bloco de feedback de erro, texto do badge "privado" |
| `--color-danger-700` | Cor do contador "restam X" quando `N ≤ 50` (alerta sem ser punitivo) |
| `--color-neutral-100` | Fundo da barra de progresso (vazio) |
| `--color-focus-ring` | Anel de foco visível em todos os elementos clicáveis |

### 10.2 Tipografia

| Token | Onde aparece |
|---|---|
| `--font-display` | Título da sessão no header (`--text-lg`), h2 dos blocos (`--text-xl`), **texto bíblico do card 2 do núcleo** (`--text-lg`, distintivo — §4.4), XP grande no fechamento (`--text-3xl`), número de dias da sequência |
| `--font-sans` | Tudo o mais — corpo dos parágrafos, opções de quiz, textarea, helper, caption, CTA |
| `--font-weight-regular` (400) | Corpo dos parágrafos, valor digitado no textarea |
| `--font-weight-medium` (500) | (não usado nesta tela) |
| `--font-weight-semibold` (600) | Headings, label do badge, labels do CTA, opções de quiz, texto bíblico |
| `--font-weight-bold` (700) | XP grande no fechamento (display) |
| `--text-xs` | Caption ("Pergunta N de 2", "Amanhã", "Sequência"), helper de privacidade, contador "restam X", `<span class="sr-only">` |
| `--text-sm` | Subtítulo da meta (se houver), referência bibliográfica do card 2, microcopy auxiliar |
| `--text-base` | Corpo padrão, label das opções de quiz, label do CTA |
| `--text-lg` | Texto bíblico no card 2 do núcleo, parágrafo do gancho (body-lg para conforto) |
| `--text-xl` | h2 dos blocos (gancho, quiz, reflexão) |
| `--text-3xl` | XP grande no fechamento, número de dias da sequência |
| `--tracking-overline` | Caption uppercase ("AMANHÃ", "SEQUÊNCIA") quando aplicável |
| `font-variant-numeric: tabular-nums` | XP no fechamento, contador "restam X" (não pula ao decrementar) |

### 10.3 Espaçamento

| Token | Onde aparece |
|---|---|
| `--space-1` (4px) | Gap interno de badge, gap entre ícone e label |
| `--space-2` (8px) | Padding interno de badge, padding do header lateral |
| `--space-3` (12px) | Gap entre opções de quiz, padding interno de pequenos componentes |
| `--space-4` (16px) | **Padding lateral da tela** (chore-ux-001 §5.2), padding interno do textarea, padding do footer |
| `--space-5` (20px) | Padding interno de cards `raised` do núcleo, padding interno da opção de quiz |
| `--space-6` (24px) | Gap entre seções dentro de um bloco, padding-top do `<main>` |
| `--space-8` (32px) | Gap entre o XP grande e o bloco "Sequência" no fechamento |
| `--space-10` (40px) | Gap entre o último elemento de conteúdo e o footer (se houver scroll) |
| `--space-11` (44px) | **Touch target mínimo** — CTA, X, prev/next, dots, opções de quiz |
| `--space-12` (48px) | Altura confortável do CTA primário `lg` |
| `--space-24` (96px) | `min-height` do textarea (chore-ux-003 §2.3) |

### 10.4 Raio, sombra, motion, z-index

| Token | Onde aparece |
|---|---|
| `--radius-md` (8px) | Anel de foco, raio do textarea, raio do feedback de quiz |
| `--radius-lg` (12px) | Cada card `raised` do núcleo, opções de quiz, card "Amanhã" no fechamento |
| `--radius-full` (9999px) | CTA primário (pílula), badges, dots, barra de progresso |
| `--shadow-sm` | Header quando `scrollY > 8` |
| `--shadow-md` | Cards `raised` do carrossel em repouso, opção de quiz selecionada |
| `--shadow-focus` | Anel de foco em `:focus-visible` |
| `--duration-fast` (120ms) | Hover/active do CTA, transição de cor das opções de quiz |
| `--duration-base` (240ms) | Transição de slide do carrossel, transição da barra de progresso, transição de sombra |
| `--ease-out-soft` | Easing de toda transição |
| `--z-sticky` (20) | Header e footer fixed |

---

## 11. Microcopy completa

Todo texto visível ao usuário em pt-BR, justificado contra P1 (não-moralista), P8 (privado) e P11 (sem cobrança/FOMO). **Placeholders** (`<<...>>`) virão de `doc/conteudo/fluxo-semana.md`.

| # | Slot | Texto exato | Bloco | Justificativa P1 / P8 / P11 |
|---|---|---|---|---|
| 1 | Título da sessão no header | `<<Título da sessão>>` (placeholder) | 1–5 | Nome próprio do `fluxo-semana.md`; veto a nomes da Lição oficial (ADR-013) |
| 2 | `aria-label` do botão X | `Fechar sessão` | 1–5 | Linguagem neutra; sem "sair", "cancelar", "abandonar" (P11 — sem peso afetivo) |
| 3 | `aria-label` da barra de progresso | `Progresso da sessão` | 1–5 | Descritivo, sem comparação |
| 4 | Texto SR-only da barra | `Passo 2 de 5.` | 1–5 | Fato; sem "ainda faltam 3" (sugere ansiedade) |
| 5 | Título do gancho (h2) | `<<Título curto do gancho>>` (placeholder) | 1 | Conteudista decide; tom da marca |
| 6 | Corpo do gancho | `<<Parágrafo de 2 a 4 linhas que apresenta o tema do dia.>>` | 1 | Tom convidativo, sem cobrança |
| 7 | CTA do gancho | `Continuar` (com seta `→` ícone-trailing) | 1 | Verbo de ação neutro |
| 8 | Card 1 do núcleo — título interno | `Contexto` (caption, text-xs uppercase) | 2 | Identifica o tipo do card; reduz ambiguidade |
| 9 | Card 1 — corpo | `<<Parágrafo de contexto histórico/narrativo.>>` | 2 | Conteudista |
| 10 | Card 2 — título interno | `Texto bíblico` (caption) | 2 | Distintivo + ARC (ADR-008) |
| 11 | Card 2 — versículo | `<<Versículo ARC, 2–4 linhas>>` | 2 | Texto bíblico da ARC (ADR-008, US-023 critério 8) |
| 12 | Card 2 — referência | `<<Livro Cap.X, vv.Y-Z>>` (ex.: `Daniel 1, vv.8-16`) | 2 | Forma curta padronizada |
| 13 | Card 3 — título interno | `Aplicação` (caption) | 2 | Identifica o ponto da reflexão |
| 14 | Card 3 — pergunta-gancho | `<<Pergunta de 1 linha que prepara o quiz.>>` | 2 | Convite |
| 15 | `aria-label` botão prev | `Card anterior` | 2 | Direção, sem "voltar" (que sugere desfazer) |
| 16 | `aria-label` botão next | `Próximo card` | 2 | Direção, neutro |
| 17 | `aria-label` de cada dot | `Card N de 3` (ex.: `Card 2 de 3`) | 2 | Fato indexado |
| 18 | CTA do núcleo | `Continuar` | 2 | Mesmo padrão |
| 19 | CTA desabilitada (helper invisível) | `Avance pelos cards para continuar.` (`aria-describedby`) | 2 | Instrução clara, sem cobrança |
| 20 | Caption do quiz | `Pergunta N de 2` (ex.: `Pergunta 1 de 2`) | 3 | Fato indexado |
| 21 | Enunciado da pergunta (h2) | `<<Enunciado da pergunta?>>` | 3 | Conteudista |
| 22 | Label de cada opção | `A) <<Opção 1>>` / `B) <<Opção 2>>` / etc. | 3 | Letras facilitam leitura por voz |
| 23 | Feedback acerto — título | `Isso mesmo!` | 3 | Confirmação positiva; sem "Parabéns" (afetação infantil — P15) |
| 24 | Feedback acerto — corpo | `<<Por que esta é a melhor resposta — 2 a 3 linhas.>>` | 3 | Aprofunda, não cobra (P1) |
| 25 | Feedback erro — título | `Quase!` | 3 | **NÃO** `Você errou` (P11 — sem punição); a explicação substitui o retry |
| 26 | Feedback erro — corpo | `<<Explicação da resposta correta — 2 a 3 linhas.>>` | 3 | Aprende com a explicação (P1) |
| 27 | CTA do quiz após pergunta 1 | `Próxima` | 3 | Avança para pergunta 2 |
| 28 | CTA do quiz após pergunta 2 | `Continuar` | 3 | Avança para reflexão |
| 29 | Pergunta da reflexão (h2) | `<<Pergunta de reflexão em 1 ou 2 linhas.>>` | 4 | Conteudista |
| 30 | Label do badge "privado" | `privado` (com ícone `ⓘ` opcional) | 4 | Minúscula deliberada — informativo, não "PRIVADO" em caixa alta (que soa institucional) |
| 31 | Placeholder do textarea | `Escreva o que você pensou…` | 4 | Convite gentil, sem "obrigatório", sem "responda" |
| 32 | Contador | `restam <N>` (ex.: `restam 934`) | 4 | Fato neutro; vira `--color-danger-700` quando `N ≤ 50` (alerta visual sem texto agressivo) |
| 33 | Helper de privacidade | `Sua reflexão é privada. Você pode compartilhar com seu responsável depois, item por item, se quiser.` | 4 | P8 explícito + preview de US-047; sem "deve compartilhar" (P1) |
| 34 | CTA da reflexão | `Continuar` | 4 | **Sempre habilitada**, mesmo com campo vazio (US-024 critério 2) |
| 35 | XP grande no fechamento | `+15 XP` (placeholder) | 5 | Número limpo |
| 36 | XP total | `Total: 240 XP` | 5 | Fato cumulativo, sem comparação social |
| 37 | Caption da sequência | `Sequência` | 5 | pt-BR, em vez de "Streak" (anglicismo); sem caps decorativo |
| 38 | Número da sequência | `5 dias` | 5 | Fato; sem "5 dias sem falhar" |
| 39 | Caption "Amanhã" | `Amanhã` | 5 | Convite, não promessa de cobrança |
| 40 | Card da prévia | `<<Título da próxima sessão.>>` + `<<Gancho de 1 linha.>>` | 5 | Curiosidade, sem revelar conteúdo crítico (US-026 critério 3) |
| 41 | CTA do fechamento | `Encerrar` | 5 | Verbo do PRD; "Finalizar" / "Concluir" são cerimoniosos |

### 11.1 Microcopy explicitamente vetada (P1, P8, P11)

| ❌ Vetado | Princípio | Por quê |
|---|---|---|
| `Não perca o streak!` | P11 | FOMO/pânico |
| `Volte amanhã ou perde tudo.` | P11 | Ameaça |
| `Você ficou para trás.` | P11 + P1 | Cobrança + comparação |
| `Você errou.` (no quiz) | P1 + P11 | Punição moralista |
| `Você foi melhor que 73% dos usuários!` | P11 | Comparação social |
| `Compartilhe com sua mãe!` (sem opt-in) | P8 | Quebra a privacidade default |
| `Lembre-se de orar antes de continuar.` | P1 | Moralismo prescritivo |
| `🔥 Em chamas!` (streak) | P11 + P15 | Clichê de produtividade adulta + infantilização do streak |
| `Última chance para fazer a sessão de ontem!` | P11 | Pânico |
| `Tá demorando, hein?` (em loading) | P1 | Ironia que soa cobrança |

---

## 12. Acessibilidade

### 12.1 Estrutura semântica

- `<header role="banner">` para o topo de cada bloco (papel implícito por `<header>` no nível raiz).
- `<main>` envolve o conteúdo do bloco. Cada bloco é renderizado em uma URL distinta em produção (HTMX swap do `<main>`); aqui descrevemos apenas o estado.
- `<footer>` para a faixa do CTA fixed-bottom (papel implícito).
- Carrossel do núcleo: container `<div role="region" aria-label="Bloco núcleo, cards" aria-roledescription="carrossel">`; cada card é `<article role="group" aria-roledescription="slide" aria-labelledby="card-N-title">`.
- Dots do carrossel: `<div role="tablist">` com cada dot como `<button role="tab" aria-selected aria-controls="card-N">`.
- Quiz: as opções são `<input type="radio" name="pergunta-N">` com `<label>` envolvente (chore-ux-003 §2.7) — leitor de tela anuncia "opção 1 de 4, selecionada". Decisão final entre `radio` ou `button` com `aria-pressed` fica para a US-027 (mecânica específica de quiz).
- Textarea: `<label for="reflexao">` explícito ligado a `<textarea id="reflexao">` (chore-ux-003 §2.7).

### 12.2 Barra de progresso — `progressbar`

```html
<div role="progressbar"
     aria-valuenow="2"
     aria-valuemin="0"
     aria-valuemax="5"
     aria-label="Progresso da sessão"
     class="session-progress">…</div>
<span class="sr-only">Passo 2 de 5.</span>
```

- `aria-valuenow` atualiza a cada transição de bloco.
- `aria-label` fixo: "Progresso da sessão" (não mudamos por bloco — o número conta).
- `<progress>` nativo seria alternativa, mas estilização cross-browser é dolorosa; `role="progressbar"` em `<div>` permite controle total do CSS.

### 12.3 Carrossel — `region` + labels

```html
<div role="region"
     aria-label="Bloco núcleo, cards"
     aria-roledescription="carrossel">
  <article role="group" aria-roledescription="slide"
           aria-labelledby="card-1-title" id="card-1">
    <h3 id="card-1-title">Contexto</h3>
    …
  </article>
  …
</div>
```

- `aria-live="polite"` no container quando muda de slide para anunciar "Card 2 de 3" (decisão alternativa: somente o dot ativo muda `aria-selected`, deixando o leitor de tela acompanhar). Para o MVP escolho a opção mais conservadora: **`aria-live="polite"` no container do dot indicator**, anuncia "Card N de 3" quando muda.
- Botões prev/next com `aria-label="Card anterior"` / `aria-label="Próximo card"`.
- Setas do teclado (← / →) controlam o slide quando o foco está dentro do `region` — implementação no Alpine via `@keydown.left` / `@keydown.right`.

### 12.4 Contador "restam X" — `aria-live`

- `aria-live="polite"` ativada apenas quando `N ≤ 100` (decisão de não saturar o leitor de tela).
- Anuncia a cada 10 chars consumidos (não a cada keypress).
- Em `N ≤ 50`, vira `aria-live="polite"` constante + cor `--color-danger-700` (sinal redundante: voz + cor).

### 12.5 Foco e ordem de tabulação

| Bloco | Ordem de Tab |
|---|---|
| 1 — Gancho | X → CTA "Continuar" |
| 2 — Núcleo | X → botão prev → card (focável com `tabindex="0"` apenas se carrossel implementar setas) → botão next → dots → CTA "Continuar" |
| 3 — Quiz | X → opção A → B → C → D → (após escolha) feedback (não-focável) → CTA "Próxima/Continuar" |
| 4 — Reflexão | X → textarea (com `aria-describedby` apontando para helper) → CTA "Continuar" |
| 5 — Fechamento | X → CTA "Encerrar" |

- Foco visível: anel `--shadow-focus` em `:focus-visible` em **todos** os elementos clicáveis (chore-ux-002 §2.6 + chore-ux-003 §0.3).
- **Sem `tabindex` positivo** — ordem natural do DOM.
- **Skip-link "Pular para conteúdo"** — registrado como dívida (chore-ux-007).
- **Ao trocar de bloco** (via CTA "Continuar"), foco vai para o `<main>` da nova URL — decisão de acessibilidade idêntica à chore-ux-003 §6.5.

### 12.6 Contraste (WCAG AA mínimo)

Todas as combinações já estão validadas em `doc/UX/00-identidade-visual.md` §2:

| Combinação | Razão | Nível |
|---|---|---|
| Título da sessão (`neutral-900`) sobre branco | 16.4:1 | AAA |
| Corpo de gancho/núcleo (`neutral-700`) sobre branco | 10.8:1 | AAA |
| Texto bíblico (`neutral-900`) sobre branco | 16.4:1 | AAA |
| Caption "Pergunta 1 de 2" (`neutral-500`) sobre branco | 4.62:1 | AA |
| Badge "privado" (`info-700` sobre `info-100`) | 7.42:1 | AAA |
| Feedback acerto (`success-700` sobre `success-100`) | 6.84:1 | AA |
| Feedback erro/info (`info-700` sobre `info-100`) | 7.42:1 | AAA |
| Contador alerta (`danger-700` sobre branco) | 6.12:1 | AA |
| CTA "Continuar" (branco sobre `primary-500`) | 4.62:1 | AA |
| Barra de progresso preenchida (`primary-500` sobre `neutral-100`) | (elemento gráfico ≥ 4px, não-texto) | — |

### 12.7 Redundância de canal (chore-ux-001 §5.3)

- **Quiz — acerto vs. erro:** combina **cor** (verde/azul-info), **ícone** (✓ vs ⓘ — nunca ✕ vermelho) e **texto** ("Isso mesmo!" / "Quase!"). Daltônicos lêem por ícone + texto.
- **Carrossel — card atual:** combina **posição visual** (slide centralizado), **dot ativo** (cor + `aria-selected`) e **anúncio do leitor de tela** ("Card N de 3").
- **Reflexão — privado:** combina **badge visual** (info), **texto** ("privado") e **helper de privacidade** abaixo do campo.
- **Fechamento — sequência:** combina **número** (5 dias), **ícone** (pico, não chama) e **caption** ("Sequência").

### 12.8 `prefers-reduced-motion`

- **Barra de progresso:** transição `width 240ms` zerada para `0.01ms` pela regra global (chore-ux-003 §0.5). Conforme.
- **Carrossel:** slide de 240ms zerado. O card simplesmente troca sem animação. Conforme.
- **Feedback de quiz:** entrada com fade de 240ms zerada. Aparece estático. Conforme.
- **Lottie do fechamento:** **fallback obrigatório** — substituída por SVG estático do estado final (§7.6). Implementação: `window.matchMedia('(prefers-reduced-motion: reduce)').matches` decide qual elemento renderiza.
- **Anel de foco e estados de hover:** transições zeradas; foco aparece imediatamente.

---

## 13. Decisões e alternativas descartadas

### 13.1 5 blocos em URLs distintas (escolhido) vs. SPA-like com Alpine controlando tudo (rejeitado)

Considerei usar uma única URL `/sessao/<id>` e um único `<main x-data="{ block: 1 }">` controlando os 5 blocos via `x-show`. Rejeitei por **três custos**:

1. **HTMX é o stack default** (ADR-011) — cada transição de bloco já é um POST natural (`/sessao/<id>/bloco/<N>/avancar`) que persiste o estado. SPA-with-Alpine duplica esforço.
2. **Retomada (US-025)** funciona melhor com URL por bloco — abrir `/sessao/<id>/bloco/4` direto é mais simples que reconstruir estado no Alpine.
3. **Acessibilidade:** novo `<main>` por bloco facilita anúncio "carregando" via `aria-live` e foco programático no novo título.

**Quando reabriríamos:** se medirmos latência inaceitável de HTMX swap em conexões 3G reais (PRD §8.4). Mitigação: pré-carregar próximo bloco via `hx-preload`.

### 13.2 Carrossel com swipe + botões prev/next (escolhido) vs. scroll horizontal nativo (rejeitado)

Considerei usar `overflow-x: scroll` + `scroll-snap-type: x mandatory` para os cards do núcleo — minimalista, sem JavaScript. Rejeitei porque:

1. **Acessibilidade:** scroll horizontal nativo é navegável por teclado apenas com setas após focar o container; usuários de leitor de tela perdem o controle de "qual card está visível agora". `role="region"` + dots clicáveis dão controle redundante.
2. **Dot indicator:** scroll nativo não tem como sincronizar dots ativos sem JS — o JS volta no fim.
3. **Botões prev/next:** necessários por chore-ux-003 §0.2 (não depender de gesto). Scroll nativo + botões pedem `scrollTo({left, behavior})` — quase a mesma quantidade de JS que controlar `current` via Alpine.

**Quando reabriríamos:** se Alpine virar gargalo de bundle (P13). Mitigação: a alternativa scroll + JS mínimo sempre cabe.

### 13.3 Feedback de erro do quiz como `info` (escolhido) vs. `danger` (rejeitado)

Considerei dar ao feedback "Quase!" a paleta `danger-100`/`danger-700` (vermelho) — leitura imediata de "errado". Rejeitei porque:

1. **P1 (não-moralista):** vermelho carrega "você fez algo errado/perigoso". Em quiz educativo de fé, não é uma falha — é uma oportunidade.
2. **P11 (sem cobrança):** vermelho cobra. Azul-info informa.
3. **Daltonismo:** acerto verde + erro vermelho colidem em deuteranopia/protanopia (chore-ux-001 §6.4). Acerto verde + erro azul-info distinguem por matiz mesmo em daltonismo.

**Quando reabriríamos:** se em teste de usabilidade adolescentes reportarem que "não entenderam que erraram". Mitigação: mantém info, mas troca o título de "Quase!" para "Quase! A resposta correta era B:" — explicita o estado.

### 13.4 Streak como "Sequência + pico de montanha" (escolhido) vs. "Streak + chama" (rejeitado)

Considerei o padrão dominante de apps de hábito (Duolingo, Snapchat): número + 🔥 chama. Rejeitei porque:

1. **P11 + chore-ux-001 §1:** "queima" lê como urgência e pressão; o pico de montanha conversa com a metáfora "trilha" do nome do produto.
2. **pt-BR:** "Streak" é anglicismo; "Sequência" é uma palavra que adolescente brasileiro usa e entende.
3. **Tom da marca:** chama é a estética de "produtividade adulta competitiva" rejeitada explicitamente em chore-ux-001 §1.

**Quando reabriríamos:** se em pesquisa com público real "Sequência" soar burocrático. Mitigação: testar com "Você está em 5 dias" como microcopy alternativa.

### 13.5 CTA "Continuar" travada até último card do núcleo (escolhido) vs. micro-feedback toast (rejeitado)

Já discutido em §4.5.3. Trava clara > toast de interrupção.

### 13.6 Sem bottom-nav durante sessão (escolhido) vs. bottom-nav presente (rejeitado)

Considerei manter a bottom-nav visível na sessão para coerência. Rejeitei porque:

1. **Foco:** sessão é modo "imersão" — Júlia está lendo/refletindo. Bottom-nav cria 4 saídas competindo com o conteúdo.
2. **Espaço vertical:** em 320×568, cada pixel conta. Remover bottom-nav devolve 64px para conteúdo.
3. **Saída clara:** o X explícito no header é uma saída consciente; toques acidentais na bottom-nav (zona do polegar) tirariam Júlia da sessão sem querer.

**Quando reabriríamos:** se em teste de usabilidade adolescentes reportarem "não consegui sair". Mitigação: aumentar tamanho do X ou adicionar `Esc` no teclado físico.

---

## 14. Pendências e ganchos para tasks futuras

- **Nomes próprios das sessões e dos blocos** — virão de `doc/conteudo/fluxo-semana.md` (US-023 critério 10). Placeholders deste spec e do HTML serão substituídos nas US-023 a US-026.
- **Mecânicas alternativas de quiz** — V/F, ordenar, drag-and-drop, completar frase, caça-palavras, escolha com imagens. Specs próprios em US-027 a US-035 (sprints 10–12).
- **Estilo concreto da Lottie de fechamento** — registrado em §7.6, dependente da definição de ilustração editorial (chore-ux-001 §7).
- **Ícone definitivo da sequência** — registrado em §7.5; veto a chama é firme; pico de montanha é provisório.
- **ProgressBar e DotIndicator** — lacunas catalogadas em §9. Decisão de promover a componentes oficiais (chore-ux-007) fica para a auditoria final.
- **Backend de retomada (US-025)** — contrato HTMX a cada transição de bloco; persistência em `session_progress`. Detalhes técnicos com Arquiteto na US-025.
- **Skip-link "Pular para conteúdo"** — entra na chore-ux-007 (auditoria WCAG completa).
- **Smoke visual end-to-end** — chore-ux-008 consumirá este HTML como uma das telas-âncora.

---

## 15. Histórico de aprovação

| Data       | Item                              | Decisão                |
|------------|-----------------------------------|------------------------|
| 2026-05-18 | Estrutura da sessão (13 seções)   | Proposto pelo Designer |
| —          | Aprovação pelo humano (Dioni)     | Pendente               |
