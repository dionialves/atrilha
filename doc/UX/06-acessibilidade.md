# Acessibilidade — Checklist operacional WCAG 2.1 AA

**Código:** chore-ux-007
**GitHub Issue:** #26
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M1 — Identidade visual e DoD de acessibilidade definidos
**Status:** Proposto
**Depende de:** chore-ux-001 (contraste validado em `doc/UX/00-identidade-visual.md` §2 e §6.4), chore-ux-002 (tokens `--color-focus-ring`, `--shadow-focus`, `--space-11` em `doc/UX/01-design-tokens.md`), chore-ux-003 (componentes Modal com focus trap, Toast com `role="status"`/`role="alert"`, Input com `aria-describedby`/`aria-invalid`, Bottom-nav com `aria-current` em `doc/UX/02-componentes-base.md`)
**Bloqueia:** chore-ux-008
**Referências PRD:** §8.4 (RNF-A11Y-01..05), §8.5 (RNF-COMP-01..04), §17 (DoD)
**Escopo deste doc:** **checklist operacional** que cada agente (Arquiteto ao planejar US, Codificador antes do handoff, QA na cobertura, Revisor antes do APROVADO) consome. Cada item é **observável** — passa ou não passa em segundos com olho, teclado, DevTools ou Lighthouse. Nenhum item é princípio teórico. **Esta checklist evolui** — quando US futuras descobrirem cenário novo, abre-se Issue de patch ao doc; nada é editado direto.

---

## 1. Escopo e nível de conformidade

### 1.1 Alvo declarado

| Item | Valor | Fonte |
|---|---|---|
| Nível WCAG | **2.1 AA** (esforço razoável, **não certificação**) | PRD §8.4 RNF-A11Y-01 |
| Contraste corpo | **≥ 4.5:1** | PRD §8.4 RNF-A11Y-02 |
| Contraste display (≥ 18px regular ou ≥ 14px bold) | **≥ 3:1** | PRD §8.4 RNF-A11Y-02 |
| Navegação por teclado | **Todos os fluxos críticos** | PRD §8.4 RNF-A11Y-03 |
| ARIA em interativos | **Implementação razoável** | PRD §8.4 RNF-A11Y-04 |
| Touch target mínimo | **44×44px** (`--space-11`) | PRD §8.4 RNF-A11Y-05 |

### 1.2 Browsers e viewports alvo

PRD §8.5 (RNF-COMP-01..04). Toda verificação operacional roda em pelo menos um browser de cada coluna:

| Mobile | Desktop | Viewport mínima | Viewport máxima |
|---|---|---|---|
| Chrome Android ≥ 110 | Chrome / Edge / Firefox versões atuais −2 | **320px** | **1920px** |
| Safari iOS ≥ 16 | — | — | — |

### 1.3 Exceções aceitas (a v1 deliberadamente não cobre)

Estas três exceções são **as únicas** ressalvas declaradas à conformidade WCAG 2.1 AA do MVP. Toda outra divergência precisa virar Issue de patch a este doc — **não** ser tratada como improviso de US.

| # | Exceção | Mitigação na v1 | Justificativa | Origem |
|---|---|---|---|---|
| E1 | **US-031 Drag-and-drop versículo** — suporte completo a leitor de tela na mecânica de arrastar não é entregue. | Alternativa por **tap**: selecionar palavra do pool → tocar no slot vazio. Critério já está na US (US-031 critério 4). | Acessibilidade plena de drag para leitores de tela exige modelo `aria-grabbed`/`aria-dropeffect` deprecado ou padrão custom complexo, fora do escopo MVP. | PRD §8.4 nota; US-031 |
| E2 | **US-032 Caça-palavras 9×9** — suporte completo a leitor de tela na seleção por arrastar no grid não é entregue. | Lista textual lateral das palavras a encontrar com marcação manual por toque/clique nas palavras encontradas (fallback de progresso). | Mecânica de seleção contínua em grid 2D não tem padrão ARIA estável para leitores de tela. | PRD §8.4 nota; US-032 |
| E3 | **US-042 Onboarding do painel parental** — modal `critical` em que **`Esc` não fecha** enquanto Carlos não chega à tela 3. | Botão **"Pular tutorial"** (ghost) aparece **a partir da tela 3** como alternativa de saída via teclado. Atende WCAG 2.1.1 porque o usuário tem sim caminho de teclado para sair — só não é `Esc`. | Onboarding é bloqueante por design contratual (ADR-003 + US-042 critério 5). Permitir `Esc` quebraria o critério. Decisão registrada em `doc/UX/05-prototipo-painel-pais.md` §10.7. | chore-ux-006 §10.7; US-042 critério 1 e 5 |

Nenhuma das três isenta o componente que as cerca: a tela ao redor do drag, ao redor do caça-palavras e ao redor do onboarding ainda passa por **toda** a checklist abaixo.

---

## 2. Cor e contraste (RNF-A11Y-02)

Referência primária: tabela validada em `doc/UX/00-identidade-visual.md` §2 (`primary-500` 4.62:1 sobre branco, `secondary-700` 6.84:1, `info-700` 7.42:1, `danger-700` 6.12:1, `neutral-700` 10.8:1 AAA, etc.). Não recalcular nesta task — **referenciar**.

### 2.1 Checklist observável

- [ ] **Texto de corpo (`--text-base` 16px, `--text-sm` 14px, `--text-xs` 12px) tem contraste ≥ 4.5:1 sobre seu fundo.** Verificar com DevTools → Inspect → Contrast.
- [ ] **Texto display (`--text-lg` 18px regular ou `--text-base` 16px bold) tem contraste ≥ 3:1.** WCAG AA Large.
- [ ] **Estado de erro nunca é comunicado apenas por cor.** Sempre há **ícone** (`✕` em `--color-danger-700`) + **texto** ("E-mail inválido") + **borda** (`--color-danger-700`, 2px) + `aria-invalid="true"`. Confere com `doc/UX/02-componentes-base.md` §2.4.
- [ ] **Anel de foco (`--color-focus-ring` = `--color-primary-500`) tem contraste ≥ 3:1 com o fundo adjacente.** Sobre `--color-bg` (`#F7F4F1`) ou `--color-surface` (`#FFFFFF`), o coral primário satisfaz (4.62:1 com branco, equivalente sobre off-white).
- [ ] **Estados ativo / concluído / bloqueado de nó na trilha não dependem só de cor.** Nó ativo combina **cor coral + escala 1.1 + label "HOJE" + `aria-current`** (chore-ux-001 §5.3). Nó concluído combina **cor lime + ícone de check + label visível**. Nó bloqueado combina **cor muted + ícone de cadeado + `aria-disabled="true"`**.
- [ ] **Heatmap binário do painel pai (US-043) não depende só de cor.** Cada célula tem **preenchimento sólido** (dia com sessão) **vs. apenas contorno** (dia sem sessão) — daltônicos diferenciam pela forma. Confere `doc/UX/05-prototipo-painel-pais.md` §9.
- [ ] **Selo de progresso / streak não depende só de cor.** Combina cor + ícone + número visível.
- [ ] **Link textual tem contraste ≥ 4.5:1 E é distinguível de texto comum por outro canal além da cor** (sublinhado, peso ou `:hover` revelando sublinhado).
- [ ] **Tokens semânticos da chore-ux-002 são reutilizados.** Não criar cor nova em US — se faltar token, abrir Issue de patch à chore-ux-002.

### 2.2 Não fazer

- Comunicar erro apenas pintando o input de vermelho (falta texto e ícone).
- Usar `--color-primary` e `--color-danger` lado a lado em CTAs (chore-ux-001 §6.4 — adolescente clica menos por ambiguidade).
- Sombras coloridas "glow" como única demarcação de foco (chore-ux-002 §6 veta).

---

## 3. Teclado (RNF-A11Y-03)

### 3.1 Checklist observável

- [ ] **Toda funcionalidade acessível por mouse/touch também é acessível por teclado.** Validar: completar o fluxo crítico (login → trilha → sessão → fim de sessão) usando **apenas teclado**, sem encostar no mouse.
- [ ] **Ordem de Tab respeita ordem visual de leitura.** Top-down, left-right. Sem `tabindex` positivo (≥ 1) — apenas `0` (focável) ou `-1` (programaticamente focável).
- [ ] **Foco visível sempre.** Anel `outline: 2px solid var(--color-focus-ring)` + `outline-offset: 2px` aplicado em `:focus-visible` (chore-ux-002 §2.6, chore-ux-003 §0.3). Quando `outline` não puder ser usado, fallback `box-shadow: var(--shadow-focus)`. **Vetado** `outline: none` sem substituto.
- [ ] **`Enter` e `Space` ativam botões.** Garantido se o elemento for `<button>` (semântico — chore-ux-003 §0.6). `<div role="button">` está vetado.
- [ ] **`Esc` fecha modais, sheets, dropdowns e popovers.** Implementado por `@keydown.escape.window` no componente Modal (chore-ux-003 §4.7). **Exceção E3** (Seção 1.3): onboarding parental US-042 — `Esc` desligado, mitigação por botão "Pular tutorial" na tela 3.
- [ ] **Modais implementam focus trap.** Tab cicla apenas entre elementos focáveis dentro do modal; Shift+Tab cicla na ordem reversa. Implementado por Alpine `x-trap.inert.noscroll` (chore-ux-003 §4.7). Vale **também** para o onboarding `critical` (US-042) — focus trap não é a exceção, apenas o `Esc`.
- [ ] **Ao fechar um modal, foco volta ao elemento que o abriu.** Implementado pelo Codificador no componente Modal (registrar `previousActiveElement` ao abrir, `.focus()` ao fechar).
- [ ] **Carrossel (US-023, núcleo da sessão) tem botões prev/next além de gesto.** Botões `icon-only` ghost com `aria-label="Card anterior"` / `aria-label="Próximo card"` (chore-ux-004 §4.5, microcopy 15 e 16). Tab navega: prev → card → next → dots. Setas ← / → opcionalmente mudam slide quando o card está focado.
- [ ] **Carrossel: dots clicáveis com `role="tab"` e `aria-selected`** (chore-ux-004 §12.3) — alternativa de teclado ao gesto.
- [ ] **Drag (US-031) tem alternativa por tap** (exceção E1 — Seção 1.3). Tap implementa o caminho de teclado equivalente (tap em palavra → tap em slot).
- [ ] **Caça-palavras (US-032) tem fallback navegável** (exceção E2 — Seção 1.3). Lista de palavras a encontrar marcável por foco + Enter.
- [ ] **`<input>` em formulário recebe `autocomplete`** apropriado (Seção 8) — permite preenchimento por teclado/keychain.
- [ ] **Bottom-nav navegável por Tab.** Cada item é `<a>` com `aria-current="page"` no ativo (chore-ux-003 §6.7).

### 3.2 Verificação rápida

Em qualquer página: pressione `Tab` da URL bar repetidamente. Observe:

1. **Foco visível em cada parada** — o anel coral aparece e fica posicionado em torno do elemento.
2. **Nada é "pulado".** Cada elemento clicável é alcançável.
3. **Nada além de elementos clicáveis recebe foco.** Texto comum não é focável.
4. **Em modais abertos, Tab fica preso dentro do modal.**

---

## 4. Semântica HTML e ARIA (RNF-A11Y-04)

Princípio: **HTML semântico antes de ARIA** (chore-ux-003 §0.6). Cada `aria-*` adicional só entra quando o elemento nativo não cobre o caso.

### 4.1 Checklist observável

#### Elementos básicos

- [ ] **`<button>` para ações, `<a>` para navegação.** `<div onclick>` está vetado (chore-ux-003 §0.6). `<div role="button">` apenas como último recurso.
- [ ] **Cada página tem `<html lang="pt-BR">`.** Confere via DevTools → Elements → `<html>`.
- [ ] **`<main>` único por página.** Conteúdo principal envolto. Outros landmarks (`<header>`, `<nav>`, `<aside>`, `<footer>`) também presentes conforme aplicável.
- [ ] **Quando há mais de uma `<nav>`, cada uma tem `aria-label`.** Convenção em uso: `aria-label="Principal"` para bottom-nav (chore-ux-003 §6.7); `aria-label="Ações da página"` para barra de ações no header (chore-ux-003 §5.7); `aria-label="Notificações"` para toast region (chore-ux-003 §8.7).
- [ ] **`<h1>` único por tela.** Hierarquia descendente sem pular nível (h1 → h2 → h3; não h1 → h3) — chore-ux-001 §5.1.

#### Imagens e ícones

- [ ] **Toda `<img>` decorativa tem `alt=""`** (atributo presente, valor vazio — leitor de tela ignora).
- [ ] **Toda `<img>` com significado tem `alt="<descrição>"`** em pt-BR, descrevendo a função/conteúdo, **não** a aparência.
- [ ] **Ícone-botão sem texto visível tem `aria-label`** em pt-BR (chore-ux-003 §1.7, §5.7). Exemplos canônicos: `aria-label="Fechar"`, `aria-label="Voltar"`, `aria-label="Fechar sessão"`, `aria-label="Ajustes"`, `aria-label="Fechar notificação"`, `aria-label="Card anterior"`, `aria-label="Próximo card"`.
- [ ] **SVG decorativo (parte estética de outro componente) tem `aria-hidden="true"`** (chore-ux-003 §1.6, §6.8). Exemplo: o `<svg>` dentro do logo wrapper, que já tem `aria-label` no `<a>` pai.

#### Formulários (resumo — detalhamento na Seção 8)

- [ ] **Cada `<input>` / `<textarea>` / `<select>` tem `<label for="id">` associado** (chore-ux-003 §2.7). `aria-label` apenas como último recurso (campo de busca em header, onde label flutuante prejudica densidade).
- [ ] **Mensagens de erro têm `aria-describedby` apontando do input ao texto da mensagem.** Implementado pelo Input fragment (chore-ux-003 §2.8 example).
- [ ] **Campo com erro tem `aria-invalid="true"`** (chore-ux-003 §2.4 state).
- [ ] **Campo obrigatório tem `required` + `aria-required="true"`** (chore-ux-003 §2.7).
- [ ] **Placeholder não substitui label** (P15 — clareza; também WCAG 3.3.2).

#### Modais e diálogos

- [ ] **Modal usa `role="dialog" aria-modal="true" aria-labelledby="<id-do-titulo>"`** (chore-ux-003 §4.7). `aria-describedby` opcional quando há instrução secundária.
- [ ] **Diálogos `critical` mantêm `role="dialog"` e `aria-modal="true"`** mesmo com `Esc` desligado (exceção E3 — Seção 1.3). A exceção atinge apenas teclas, não semântica ARIA.

#### Toasts e regiões dinâmicas

- [ ] **Toast `success` e `info` usam `role="status"` / `aria-live="polite"`** (chore-ux-003 §8.3).
- [ ] **Toast `error` e `warning` usam `role="alert"` (= `aria-live="assertive"`)** (chore-ux-003 §8.3).
- [ ] **Regiões com conteúdo que muda em runtime têm `aria-live` apropriado.** Exemplos do projeto: contador de caracteres no editor da sessão `aria-live="polite"` quando se aproxima do limite (chore-ux-004 §6); feedback do quiz `aria-live="polite"` (chore-ux-004 §5.4); toast de "Marcado como conversado" do painel parental `aria-live="polite"` (chore-ux-005 microcopy).

#### Estados específicos

- [ ] **Item ativo do bottom-nav tem `aria-current="page"`** (chore-ux-003 §6.4, §6.7).
- [ ] **Botão togglável tem `aria-pressed`** com valor `true`/`false` (chore-ux-005 §7 — "Conversamos sobre isso").
- [ ] **Disclosure (dropdown, accordion) tem `aria-expanded`** com valor `true`/`false` (chore-ux-003 §1.6).
- [ ] **Botão em loading tem `aria-busy="true"`** (chore-ux-003 §1.6).
- [ ] **Botão desabilitado usa atributo `disabled`** (não apenas `aria-disabled` solo) — `disabled` é o canal correto para botão (chore-ux-003 §1.7).
- [ ] **Barra de progresso tem `role="progressbar"` + `aria-valuenow` + `aria-valuemin` + `aria-valuemax` + `aria-label`** (chore-ux-004 §12.2).
- [ ] **Carrossel: container tem `role="region" aria-label="..." aria-roledescription="carrossel"`** e cada slide tem `role="group" aria-roledescription="slide" aria-labelledby="..."` (chore-ux-004 §12.1).

### 4.2 Ordem de leitura

- [ ] **Ordem do DOM = ordem visual de leitura.** Não usar `order` / `flex-direction: row-reverse` / `grid-template-areas` para reordenar conteúdo crítico — leitor de tela segue o DOM.
- [ ] **Trilha em zigue-zague (US-018): ordem DOM dos nós segue dia 1 → dia 7** mesmo quando o layout visual zigueza (chore-ux-003 §8.4 do protótipo trilha — registrado como armadilha conhecida).

---

## 5. Touch targets e mobile (RNF-A11Y-05, RNF-COMP-04)

### 5.1 Checklist observável

- [ ] **Todo elemento clicável tem área mínima 44×44px** (`min-height: var(--space-11)` E `min-width: var(--space-11)` quando aplicável). Vale para botões, links, checkboxes, ícones-botão, itens de bottom-nav, dots de carrossel (chore-ux-002 §4, chore-ux-003 §0.2). Botão `sm` (36px) só é permitido quando cercado por área clicável maior (cell inteira do card é alvo).
- [ ] **Não há sobreposição de áreas clicáveis adjacentes.** Dois botões lado a lado mantêm gap ≥ `--space-2` (8px) para evitar tap acidental.
- [ ] **Em viewport 320px nenhum elemento crítico é cortado.** Verificar com DevTools → Toggle device toolbar → 320px.
- [ ] **Em viewport 320px não há scroll horizontal.** PRD §8.5 RNF-COMP-04. Verificar arrastando a página lateralmente — só pode haver scroll horizontal **interno** a um componente declarado como tal (carrossel, scroller de tabs).
- [ ] **Botão fixo de rodapé respeita `env(safe-area-inset-bottom)`** (iOS notch). Implementado via `padding-bottom: env(safe-area-inset-bottom)` no container fixo (chore-ux-001 §5.5 implícito).
- [ ] **Drag (US-031) tem alternativa por tap** (exceção E1 — Seção 1.3, já listada).
- [ ] **CTA primário ocupa 100% da largura útil em mobile** (chore-ux-001 §5.2). Em desktop: `auto`, `min-width: 240px`.
- [ ] **Conteúdo de leitura tem `max-width: 38rem`** mesmo em desktop (chore-ux-001 §5.4) — conforto > preencher tela.

### 5.2 Verificação rápida

DevTools → Toggle device toolbar → 320×568 (iPhone SE). Percorrer toda a tela:

1. Não há scroll horizontal global.
2. Cada botão visível tem área tocável de pelo menos 44×44.
3. Nenhum botão é cortado pela borda.

---

## 6. Motion e `prefers-reduced-motion`

### 6.1 Checklist observável

- [ ] **Toda animação > 200ms respeita `@media (prefers-reduced-motion: reduce)`.** Regra global (chore-ux-003 §0.5) zera `animation-duration` e `transition-duration`. Confere via DevTools → Rendering → Emulate CSS prefers-reduced-motion: reduce.
- [ ] **Animações Lottie têm fallback estático para `prefers-reduced-motion`** (chore-ux-004 §10.6 implícito; chore-ux-001 §5.8). Implementação: `<picture>` com `<source media="(prefers-reduced-motion: reduce)" srcset="static.svg">` OU Alpine `x-if` baseado em `window.matchMedia('(prefers-reduced-motion: reduce)').matches`.
- [ ] **Nenhum elemento pisca > 3 vezes por segundo** (WCAG 2.3.1). Nenhum padrão atual do projeto pisca; vale como guarda permanente para microinterações futuras.
- [ ] **Scroll automático na trilha (US-018) usa `behavior: 'instant'` por default** (chore-ux-003 §3.4 da trilha — decisão já tomada para evitar bifurcação `prefers-reduced-motion`). Caso uma US futura introduza `behavior: 'smooth'`, deve detectar `prefers-reduced-motion` e cair para `instant`.
- [ ] **Transições de hover/active em botões usam `transition: color/background-color var(--duration-fast) var(--ease-out-soft)`** (chore-ux-003 §1.6). Não transicionar `transform` para não conflitar com touch.
- [ ] **Spinner em botão loading respeita `prefers-reduced-motion`** — em reduced, troca por caractere estático ou texto "Carregando…" (chore-ux-003 §1.8).
- [ ] **Sombras (`--shadow-sm`, `--shadow-md`, `--shadow-lg`) não são animadas em hover.** Aparecem em repouso por elevação de estado (chore-ux-001 §6 / chore-ux-002 §6).

### 6.2 Verificação rápida

DevTools → Rendering → Emulate CSS prefers-reduced-motion: reduce. Recarregar a página. Disparar toda animação visível:

1. Modais abrem com fade instantâneo, sem slide/scale.
2. Toasts aparecem sem deslizar.
3. Spinner some ou vira estático.
4. Scroll automático não é animado.

---

## 7. Conteúdo e idioma

### 7.1 Checklist observável

- [ ] **`<html lang="pt-BR">`** em todas as páginas (já listado na Seção 4, repetido aqui por completude semântica).
- [ ] **Texto bíblico tem marcação semântica `<blockquote>` (com `<cite>` para a referência) ou `<q>`** (chore-ux-001 §4.2). Sem itálico decorativo serifado.
- [ ] **Microcopy nunca depende de visão para fazer sentido.** Vetar:
  - "Clique no botão verde" → usar o label do botão.
  - "Veja a seção destacada acima" → usar referência por nome ou link interno.
  - "Como mostra a figura" → descrever o conteúdo em palavras.
- [ ] **Sem texto em CAPS escritos.** Caps decorativos (overline "DIA 3") aplicados via `text-transform: uppercase` em CSS — DOM mantém capitalização normal. Leitor de tela lê palavra por palavra, não letra por letra.
- [ ] **Em viewports largos, linhas de corpo limitadas a ~75 caracteres** (chore-ux-001 §5.4 — `max-width: 38rem`). Boa prática editorial; não é critério WCAG mas afeta legibilidade real.
- [ ] **Mensagens vetadas pelo P11 (sem dark patterns) permanecem vetadas.** Não usar urgência artificial ("apenas hoje!"), cobrança ("Você ainda não fez!") ou guilt ("Sua filha não viu nada"). Cada US registra a microcopy permitida; este item garante que a checklist não permite a regressão.
- [ ] **Abreviações pouco óbvias têm `<abbr title="...">`** se necessário. Caso "atrilha" não conta — é nome próprio. "ARC" (Almeida Revista e Corrigida, PRD §12.4) **pode** ser anotado com `<abbr>` na primeira aparição da página.
- [ ] **Idioma diferente do `<html lang>` recebe `lang="..."` no elemento.** Caso atual: texto bíblico ARC é pt-BR; sem necessidade de override. Esta regra entra em jogo apenas se uma US futura introduzir conteúdo em outro idioma (ex.: nome próprio em hebraico transliterado).

---

## 8. Formulários (RNF-A11Y-04)

Detalhamento da Seção 4 aplicado a formulários. Componente Input ancorado em `doc/UX/02-componentes-base.md` §2.

### 8.1 Checklist observável

- [ ] **`<form>` semântico com `method` e `action`** explícitos. Mesmo formulários processados via HTMX preservam `<form action="...">` como fallback noscript (chore-ux-003 §0.4 — progressive enhancement).
- [ ] **`<label for="id">` visível para cada `<input>`** (chore-ux-003 §2.7). `aria-label` apenas como último recurso (campo de busca em header).
- [ ] **Botão primário do `<form>` tem `type="submit"`** — permite envio por `Enter` no campo.
- [ ] **Botões secundários no formulário têm `type="button"`** explícito (evita submit acidental).
- [ ] **Validação client-side é decorativa.** A fonte da verdade é Jakarta Validation no servidor (AGENTS.md §12). Client-side antecipa feedback mas não substitui — toda mensagem precisa ter origem server-side equivalente.
- [ ] **Mensagens de erro descrevem o problema E a correção.** Padrão: `"E-mail inválido. Use o formato nome@dominio.com"`. Não usar apenas `"Campo inválido"` ou `"Erro"`.
- [ ] **`aria-describedby` aponta do input ao helper text ou error text** (chore-ux-003 §2.4, §2.8).
- [ ] **`aria-invalid="true"` em campo com erro; ausente ou `"false"` quando válido** (chore-ux-003 §2.4 state).
- [ ] **`required` HTML + `aria-required="true"`** em campos obrigatórios (chore-ux-003 §2.7).
- [ ] **`autocomplete` apropriado** preenchido em cada campo. Tabela canônica:

  | Caso | `autocomplete` |
  |---|---|
  | E-mail de login | `email` |
  | Senha em login | `current-password` |
  | Senha em cadastro / reset | `new-password` |
  | Nome do usuário | `name` (ou `given-name` / `family-name` quando separados) |
  | Telefone | `tel` |
  | Data de nascimento | `bday` |
  | Confirmação de e-mail | `email` |

- [ ] **Campos sensíveis (senha) usam `type="password"`** com botão de revelar opcional. Botão "revelar senha" tem `aria-label="Mostrar senha"` / `"Ocultar senha"` e `aria-pressed`.
- [ ] **Foco volta ao primeiro campo com erro após submit inválido.** Implementado no componente Form pelo Codificador via JavaScript / HTMX afterRequest. WCAG 3.3.1.
- [ ] **Mensagem de sucesso após submit usa toast `success`** (chore-ux-003 §8.3) — `role="status"` / `aria-live="polite"`.

### 8.2 Não fazer

- Usar placeholder como label (P15; WCAG 3.3.2).
- Limpar todos os campos ao falha de validação (frustrante e WCAG 3.3.1 problemático).
- Mensagem de erro apenas em cor (já coberto na Seção 2).

---

## 9. Mecânicas interativas com fallback (E5)

PRD §8.4 nota: "acessibilidade plena para leitores de tela em mecânicas drag/caça-palavras é difícil; v1 entrega versão alternativa textual (fallback) para essas mecânicas."

Estas duas mecânicas estão registradas em §1.3 como **exceções aceitas** (E1 e E2). Esta seção detalha o que **precisa estar presente** para que a exceção seja válida — ou seja, o fallback obrigatório.

### 9.1 US-031 — Drag-and-drop versículo (exceção E1)

- [ ] **Modo tap funciona em paralelo ao drag.** Tocar palavra no pool → palavra fica "selecionada" (visual + `aria-pressed="true"`) → tocar slot → palavra move ao slot. US-031 critério 4.
- [ ] **Reverter funciona por tap.** Tocar palavra em slot → volta ao pool.
- [ ] **Estado focado das palavras é distinguível.** Foco visível (`--shadow-focus`) na palavra atualmente focada por teclado.
- [ ] **Botão "Confirmar versículo" tem `aria-describedby`** explicando o estado quando incompleto (`"Preencha todos os slots antes de confirmar."`).
- [ ] **Microcopy de feedback após confirmar é textual + visual.** Não apenas verde/vermelho.

Esta US **não exige** suporte completo a leitor de tela na ação de arrastar — o tap cobre o caminho alternativo. O Codificador / QA / Revisor da US-031 validam que o tap funciona; **não** validam que `aria-grabbed` foi implementado (deprecado).

### 9.2 US-032 — Caça-palavras 9×9 (exceção E2)

- [ ] **Lista lateral das palavras a encontrar está navegável por teclado.** Tab focando cada palavra; estado "encontrada" indicado por ícone + cor + `aria-label` complementar (ex.: `aria-label="encontrada"`).
- [ ] **Cada palavra encontrada é anunciada por `aria-live="polite"`** na região da lista lateral.
- [ ] **Quando todas as palavras são achadas, próximo bloco é liberado** com anúncio em `aria-live="polite"` ("Bloco liberado. Continue para o próximo card.").
- [ ] **Botão "Dica" tem `aria-label`** descritivo e confirmação prévia (US-032 critério 6) — toast `warning` ou modal pequeno com botão de cancelar.

Esta US **não exige** que o leitor de tela navegue o grid 9×9 letra por letra — a lista lateral cobre o progresso. O QA / Revisor da US-032 validam a navegação pela lista.

### 9.3 Toda outra mecânica do MVP segue a checklist plena

Quiz (US-027), trilha (US-018), retomada (US-025), fechamento (US-026), painel parental (US-043, US-045): **passam por toda a checklist** sem exceção. Nenhuma outra mecânica do MVP foi declarada como exceção.

---

## 10. Como verificar — procedimento operacional

Para Codificador / QA / Revisor antes de marcar uma US como pronta. Tempo estimado por tela: **5–10 minutos**.

### 10.1 Procedimento mínimo

1. **Lighthouse → Accessibility** na página alterada. Score esperado: **≥ 90**.
   - DevTools → Lighthouse → Categories: Accessibility → Generate report.
   - Score < 90: tratar como bug; corrigir antes do handoff.
2. **Navegação por teclado** do início ao fim do fluxo crítico, sem mouse.
   - Confere Seção 3 (Teclado) — em particular foco visível e ordem de Tab.
3. **`prefers-reduced-motion`** ativo.
   - DevTools → Rendering → Emulate CSS prefers-reduced-motion: reduce.
   - Recarregar; disparar animações; confirmar que duraram instantes.
4. **Viewport 320px**.
   - DevTools → Toggle device toolbar → 320×568.
   - Sem scroll horizontal global; touch targets ≥ 44×44.
5. **Contraste pontual** dos textos novos.
   - DevTools → Inspect → Contrast (do painel Computed) confirma ≥ 4.5:1 para corpo e ≥ 3:1 para display.
6. **Leitor de tela rápido** (1–2 minutos) — apenas para US com fluxo crítico de formulário/modal.
   - macOS: `Cmd+F5` (VoiceOver).
   - Windows: NVDA (gratuito).
   - Verificar: labels lidos, erros anunciados, foco navega coerentemente.

### 10.2 Ferramentas recomendadas

| Ferramenta | Uso | Quando |
|---|---|---|
| **Lighthouse** (Chrome DevTools) | Score Accessibility ≥ 90; lista de issues | Sempre, antes do handoff |
| **axe DevTools** (extensão Chrome / Firefox) | Lint de WCAG mais detalhado que Lighthouse; encontra issues que Lighthouse não pega | Sempre, complementar |
| **WAVE** (extensão / web app) | Visualização sobreposta dos issues no DOM | Quando o issue do axe não está claro no DOM |
| **Stark** (plugin Figma + browser) | Conferir contraste e simulação de daltonismo no design e ao vivo | Designer (em revisão de spec); útil para Codificador validar simulação visual |
| **VoiceOver** (macOS) / **NVDA** (Windows) | Leitor de tela | Fluxos críticos com formulário ou modal |

### 10.3 Quando o procedimento falha

- **Lighthouse < 90:** Codificador corrige até passar; QA não aceita score menor.
- **Issue do axe não cobre o critério desta checklist:** prevalece esta checklist (axe é heurística; checklist é contrato).
- **Item da checklist não se aplica à US:** documentar na PR ("US sem UI nova — itens X, Y, Z não aplicam"). Revisor confirma.

---

## 11. O que está fora desta checklist (v1)

A checklist é **mínima viável**, não exaustiva. Os itens abaixo **não** são exigidos no MVP v1 e **não** entram em DoD de US.

| Item fora do escopo v1 | Razão | Origem |
|---|---|---|
| **WCAG AAA** | PRD §8.4 declara AA, não AAA. | PRD §8.4 RNF-A11Y-01 |
| **Tradução para Libras** | Fora do MVP. | PRD §6.2 (escopo) |
| **Áudio descrições / TTS de leitura da sessão** | Fora do MVP. | PRD §6.2 |
| **Modo alto contraste customizado** | Paleta única clara; modo escuro fora do MVP. | ADR-013, chore-ux-001 §6.3 |
| **Dark theme** | Fora do MVP. Reaproveitamento dos primitivos quando vier. | chore-ux-002 §11.2 |
| **Suporte completo a leitor de tela em drag (US-031)** | Exceção E1 com fallback tap. | PRD §8.4 nota |
| **Suporte completo a leitor de tela em caça-palavras (US-032)** | Exceção E2 com fallback lista lateral. | PRD §8.4 nota |
| **Esc para fechar onboarding parental (US-042)** | Exceção E3 com mitigação "Pular tutorial". | chore-ux-006 §10.7 |
| **Skip-link "Pular para conteúdo principal"** | Não exigido na v1; pode entrar em chore-ux-008 ou patch desta. Padrão `<main>` único + bottom-nav cobre boa parte. | chore-ux-005 §9.7 registra como decisão consciente desta task |
| **Lighthouse Accessibility 100** | Pisos é ≥ 90. Lighthouse 100 não garante WCAG AA; é meta cosmética. | Issue #26 passo 12 |

Itens fora deste documento **não** precisam ser verificados nas PRs do MVP. Quando entrarem (v1.5 / v2), abrir Issue de patch a este doc.

---

## 12. Como aplicar em cada Issue futura — diretriz para o Arquiteto

Esta checklist serve **três momentos** do ciclo de uma US:

### 12.1 Ao planejar a US (Arquiteto)

Quando o Arquiteto criar a Issue da US, ele **copia os itens relevantes** desta checklist na seção "Critérios de Aceitação WCAG 2.1 AA" da Issue. Regra de seleção:

- **Issue com UI nova:** copiar **toda seção 2 (Cor), 3 (Teclado), 4 (Semântica), 5 (Touch), 6 (Motion), 7 (Conteúdo) que se aplica**. Geralmente isso são 10–20 checkboxes. Não precisa copiar item que não aplica (ex.: US sem formulário não copia Seção 8).
- **Issue com formulário novo:** adicionalmente copiar **Seção 8 (Formulários)** inteira.
- **Issue com mecânica E5 (US-031, US-032):** copiar Seção 9 da mecânica correspondente. Confirmar exceção E1/E2 no escopo da Issue.
- **Issue com modal/sheet:** copiar **focus trap, `Esc` fecha, `role="dialog"`, `aria-modal`, `aria-labelledby`, foco volta ao caller**.
- **Issue de refactor backend / chore técnica sem UI:** marcar explicitamente na Issue "Sem UI nova — checklist WCAG não se aplica." Revisor confirma na revisão.

### 12.2 No handoff (Codificador)

Antes de passar a US para QA, o Codificador roda o procedimento da Seção 10 e marca cada checkbox copiado da checklist como concluído na PR. Itens em aberto ficam visíveis para QA.

### 12.3 Na revisão (Revisor)

Revisor confere que:

1. Todos os itens marcados na Issue estão de fato cobertos (revisão visual + Lighthouse + teclado).
2. Não há itens "ignorados sem justificativa" — qualquer pulo precisa de comentário na PR.
3. Nenhuma exceção nova foi introduzida sem virar Issue de patch a este doc.

### 12.4 Evolução desta checklist

Quando uma US descobrir cenário novo (ex.: caça-palavras revela necessidade de modo de teclado específico não previsto aqui), **não editar este doc na PR da US**. Em vez disso:

1. Abrir Issue de patch (`chore-ux-007.<n>`) descrevendo o cenário.
2. Designer atualiza a checklist; Arquiteto incorpora na Issue da US.
3. PR original aguarda o patch antes do APROVADO.

Essa disciplina evita que cada Codificador interprete "acessibilidade básica" do próprio jeito.

---

## Referências cruzadas

- **PRD:** `doc/PRD.md` §8.4 (RNF-A11Y-01..05), §8.5 (RNF-COMP-01..04), §17 (DoD).
- **Identidade visual:** `doc/UX/00-identidade-visual.md` §2 (tabela de contraste), §5.3 (redundância de canal), §6.4 (danger separado de primary).
- **Tokens:** `doc/UX/01-design-tokens.md` §2.6 (`--color-focus-ring`), §4 (`--space-11` touch target), §6 (`--shadow-focus`), §8 (motion + `prefers-reduced-motion`).
- **Componentes:** `doc/UX/02-componentes-base.md` §0 (princípios), §1 (Button), §2 (Input), §4 (Modal + focus trap), §6 (Bottom-nav + `aria-current`), §8 (Toast + `role` polite/assertive).
- **Protótipos:** `doc/UX/03-prototipo-trilha.md` §8 (semântica e foco da trilha), `doc/UX/04-prototipo-sessao.md` §12 (progressbar e dot-indicator), `doc/UX/05-prototipo-painel-pais.md` §9 (semântica do painel e heatmap), §10.7 (exceção do onboarding parental).
- **User Stories:** `doc/Requisitos/UserStory.md` — US-018, US-023, US-025, US-026, US-031, US-032, US-042, US-043.
- **Convenções:** AGENTS.md (pt-BR na UI, identificadores em inglês, Jakarta Validation no servidor).

---

## Histórico

| Data | Item | Decisão |
|---|---|---|
| 2026-05-19 | Estrutura das 12 seções + 3 exceções aceitas (E1 drag, E2 caça-palavras, E3 onboarding sem `Esc`) | Proposto pelo Designer |
