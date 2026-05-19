# Protótipo da trilha — atrilha

**Task:** chore-ux-004 (Issue #23)
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M3 — Protótipo da trilha validado
**Status:** Proposto
**Depende de:** `doc/UX/00-identidade-visual.md` (chore-ux-001, aprovada) · `doc/UX/01-design-tokens.md` (chore-ux-002) · `doc/UX/02-componentes-base.md` (chore-ux-003)
**Bloqueia:** chore-ux-008 (smoke visual end-to-end)
**Antecipa:** US-018, US-019, US-020, US-021 (Sprint 7)
**Referências:** PRD §4.3 (P11 — sem cobrança/FOMO; P12 — mobile-first; P15 — sem infantilização) · PRD §8.4 (RNF-A11Y-01..05, RNF-COMP-04) · PRD §10 (RF-E3-01..10) · AGENTS.md (idioma pt-BR na UI)

**Escopo deste doc:** especificar **em prosa** a tela da trilha vazia (sem dados de backend) — estrutura, hierarquia, comportamentos, microcopy. A **materialização navegável** vive em `doc/UX/prototypes/trilha.html`. Esta é arte conceitual: nada aqui vira código de produção em `src/**` — a implementação real entra nas US-018/019/020/021 do Sprint 7.

**Nomes próprios das sessões da semana** (ex.: "A escolha de Daniel", "Coragem silenciosa") **não são fechados aqui** — virão do doc complementar `doc/conteudo/fluxo-semana.md` (referenciado em PRD §10 como em refinamento antes do Sprint 2 conteudístico). Usamos placeholders explícitos do tipo `Sessão de domingo`, `Sessão de segunda`, etc.

---

## 1. Objetivo da tela

A trilha da semana atual é a **tela inicial** do adolescente logada (US-018, critério 1) — a primeira impressão do app em uso real e o ponto de chegada de toda navegação "voltar para o começo". Ao abrir o app, Júlia precisa, em um único toque, identificar visualmente qual é a sessão de hoje e começá-la. A tela cumpre quatro funções simultâneas: (a) dar contexto temporal ("é a semana 3 do trimestre, e hoje é quarta"), (b) mostrar o caminho inteiro da semana — passado, presente e futuro — em uma única peça (os 7 nós), (c) destacar o nó do dia para que a ação principal seja óbvia, e (d) sinalizar o status de cada outro dia (concluído, em progresso, bloqueado) com clareza suficiente para Júlia decidir, sem dúvida, onde tocar.

---

## 2. Wireframe textual da tela (mobile, 320px)

A leitura é vertical, de cima para baixo, do topo do viewport até o `safe-area-inset-bottom`. Toda a coluna útil tem `--space-4` (16px) de padding lateral; nenhum elemento ultrapassa essa borda.

```
┌──────────────────────────────────────┐
│ ▣ atrilha                ⚙           │  Header compact (ux-003 §5) — logo + ícone-botão "Ajustes"
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│  SEMANA 3                            │  overline (text-xs, tracking +0.05em, uppercase, text-muted)
│                                      │
│  <<TÍTULO DA SEMANA>>                │  h1 (display, semibold, text-2xl) — placeholder
│                                      │
│  <<Meta da semana em 1 linha.>>      │  subtítulo (sans, text-sm, text-muted)
│                                      │
│  ┌────────────────────────────────┐ │
│  │ Trimestre  →                   │ │  link/botão ghost, label "Trimestre" (US-021/US-022)
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘

  (linha conectora vertical "trilha" — pontilhada onde há nó futuro,
   sólida onde há nó concluído; coral apenas no segmento adjacente ao "hoje")

  ┌──────────────────────────────────┐
  │ ✓  Sessão de domingo  [Concluído]│   ← nó 1 — concluído (card flat, neutral)
  │    Gancho curto de 1 linha.      │
  └──────────────────────────────────┘
              │ (linha sólida secondary-300)
  ┌──────────────────────────────────┐
  │ ✓  Sessão de segunda  [Concluído]│   ← nó 2 — concluído
  │    Gancho curto de 1 linha.      │
  └──────────────────────────────────┘
              │ (linha sólida secondary-300, transitando para coral)
  ┌──────────────────────────────────┐
  │ ▶  Sessão de terça    [Continuar]│   ← nó 3 — em progresso (card raised, faixa secondary)
  │    Você parou em 3 de 5.         │
  │    ▓▓▓▓▓▓░░░░ 60%                │   ← indicador de progresso
  └──────────────────────────────────┘
              │ (linha sólida coral até o nó "hoje")
  ┌══════════════════════════════════┐
  ║ ●  Sessão de quarta    [HOJE]    ║   ← nó 4 — disponível ("hoje"), card raised + borda coral
  ║    <<Gancho do tema do dia.>>    ║      escala visual +, ancoragem de foco
  ║                                  ║
  ║   ┌────────────────────────────┐ ║
  ║   │  Começar agora →           │ ║   ← CTA primary, lg, full-width
  ║   └────────────────────────────┘ ║
  └══════════════════════════════════┘
              │ (linha tracejada neutral-300, futuro)
  ┌──────────────────────────────────┐
  │ 🔒 Sessão de quinta   [Bloqueado]│   ← nó 5 — bloqueado (futuro)
  │    Liberada em quinta, 21/05.    │
  └──────────────────────────────────┘
              │ (linha tracejada neutral-300)
  ┌──────────────────────────────────┐
  │ 🔒 Sessão de sexta    [Bloqueado]│   ← nó 6 — bloqueado (futuro)
  │    Liberada em sexta, 22/05.     │
  └──────────────────────────────────┘
              │ (linha tracejada neutral-300, diferenciando o ramo "sábado")
  ┌──────────────────────────────────┐
  │ 🔒 Sessão de sábado   [Sábado]   │   ← nó 7 — sábado bloqueado (US-020)
  │    Termine mais 3 sessões        │       Badge variant info ("Sábado") +
  │    para abrir o sábado.          │       badge variant danger ("Bloqueado") empilhados
  └──────────────────────────────────┘

┌──────────────────────────────────────┐
│  Trilha    Hoje    Perfil   Ajustes  │  Bottom-nav (ux-003 §6) — "Hoje" ativo
└──────────────────────────────────────┘
            ▔▔▔▔▔ safe-area-inset-bottom
```

### Por que lista vertical e não mapa SVG sinuoso

A primeira escolha intuitiva para uma "trilha" é um caminho sinuoso estilo Duolingo — nós em zigue-zague conectados por curvas. Rejeitamos para o MVP (decisão registrada em §9.1): em viewport de 320px, qualquer zigue-zague rouba largura útil do card de cada nó (sobram ~140px por coluna), deixando o nome da sessão e o gancho em duas linhas espremidas. A lista vertical entrega ~288px de largura útil ao card (320 − 2×16 padding) e mantém a leitura natural top-down do polegar — adolescente em ônibus rola para cima/baixo, não para os lados.

### Linha conectora vertical (o "fio da trilha")

Entre cada par de nós aparece uma **linha vertical de 2px**, posicionada à esquerda dos cards, a 20px da borda do viewport (alinhada visualmente ao ícone de estado dentro do card). Variações de estilo:

| Segmento entre… | Estilo | Cor |
|---|---|---|
| Dois nós **concluídos** | sólida | `--color-secondary-300` (lime, leitura "trilha pisada") |
| Concluído → **em progresso** | sólida, com gradiente sutil | de `--color-secondary-300` para `--color-primary-300` |
| Em progresso → **hoje** | sólida | `--color-primary-300` |
| **Hoje** → bloqueado futuro | tracejada (`dasharray: 4 4`) | `--color-neutral-300` |
| Entre dois **bloqueados futuros** | tracejada | `--color-neutral-300` |
| Bloqueado sexta → sábado | tracejada **+ pequeno espaço extra** (`margin-top: --space-2`) | `--color-neutral-300` |

O pequeno respiro vertical antes do nó de sábado é a única licença gráfica para sinalizar "este nó tem regra diferente" — sem precisar de um separador horizontal explícito.

### Ancoragem visual ao "hoje"

O nó do dia recebe **quatro reforços de canal** sobrepostos (chore-ux-001 §5.3 — redundância de canal):
1. **Borda 2px coral** (`--color-primary`) em vez do `1px solid var(--color-divider)` dos demais.
2. **Card variante `raised`** com `--shadow-md` (os demais ficam em `flat`/repouso).
3. **Badge "HOJE"** (variant `primary`, uppercase, ux-003 §7).
4. **CTA "Começar agora →"** embutido no próprio nó (button variant `primary`, size `lg`, full-width — ux-003 §1).

Sem esses quatro canais o estado "hoje" leria apenas como uma cor diferente — daltônicos e leitura por contexto perdem o sinal.

---

## 3. Estados visuais dos 4 status de nó (US-019)

A tabela abaixo é o **contrato visual** que o Codificador implementa quando a US-019 sair do backlog. Cada linha é um estado mutuamente exclusivo — um nó está em **exatamente um** dos quatro estados a qualquer momento.

| Estado | Variante de card (ux-003 §3) | Borda | Ícone (SVG stroke 20×20) | Cor de ícone | Badge (ux-003 §7) | Microcopy auxiliar | Comportamento ao toque |
|---|---|---|---|---|---|---|---|
| **Concluído** | `flat` | `1px solid var(--color-secondary-300)` | check (`✓`) | `--color-secondary-700` | variant `success`, label "Concluído" | Sem texto extra — o badge basta | Abre a sessão em modo **revisão** (US-019 critério 3): sem alterar XP nem streak; cabeçalho da sessão exibe etiqueta "Revisão". |
| **Em progresso** | `raised` | `1px solid var(--color-secondary-400)` | seta-em-círculo (`▶`) | `--color-secondary-700` | variant `success` (tom mais leve via `bg-secondary-100`), label "Continuar" | "Você parou em 3 de 5." + barra de progresso `--color-secondary-500` sobre faixa `--color-secondary-100` | Retoma a sessão exatamente onde parou (preparar US-025). |
| **Disponível ("hoje")** | `raised` | `2px solid var(--color-primary)` | círculo preenchido (`●`) | `--color-primary` | variant `primary`, label "HOJE" (uppercase) | Gancho de 1 linha + CTA embutido "Começar agora →" | Abre a sessão do dia (US-018 critério 4). |
| **Bloqueado** | `flat` | `1px solid var(--color-divider)` | cadeado (`🔒`, SVG genérico) | `--color-text-muted` | variant `neutral`, label "Bloqueado" | "Liberada em \<dia-da-semana, DD/MM\>." | Toque exibe **tooltip/sheet** com a data de liberação; **não permite avanço** (US-019 critério 2). Em mobile, vira bottom sheet de 1 linha apresentando o motivo + botão "Entendi". |

**Caso especial — sábado (US-020).** Quando a sessão de sábado está bloqueada por critério de progresso semanal (e não apenas por data), o nó usa a variante `bloqueado` acima **com duas mudanças**:

1. O badge `neutral` "Bloqueado" é **acompanhado** por um segundo badge variant `info`, label "Sábado", empilhado verticalmente acima dele. A leitura semântica: "este nó tem regra própria, e ainda não atingiu o critério".
2. A microcopy auxiliar troca o texto de data por: **"Termine mais N sessões para abrir o sábado."** Onde `N = 5 − sessoesConcluidas`. Quando `N=0` mas ainda não chegou sábado, a microcopy passa a "Abre no sábado, 24/05" — voltando ao formato padrão. Quando o critério é atingido E é sábado, o nó **transiciona** para o estado **Disponível ("hoje")** sem reload (US-020 critério 2).

**Onde caberia uma microanimação Lottie (ADR-011):** no **momento exato do desbloqueio do sábado** (`N` cai de 1 para 0 enquanto Júlia conclui a 5ª sessão e volta à trilha). Uma Lottie curta (~800ms) de "cadeado se abrindo" no ícone do nó, sincronizada com a troca de classe via Alpine `x-show` + `x-transition`, daria celebração proporcional à conquista sem virar fogos de artifício. **Não implementamos nesta task** — apenas registramos o lugar.

---

## 4. Ancoragem ao "hoje" (US-018 critério 3)

### Comportamento esperado

Ao abrir a trilha, o nó do dia precisa estar **visível e centrado verticalmente** no viewport sem que Júlia precise rolar manualmente. Em uma semana média (3 dias concluídos + 1 hoje + 3 futuros), o nó "hoje" fica na metade inferior da lista — em viewport de 320×568 (iPhone SE pré-2022), isso é fora da dobra inicial.

### Implementação no protótipo (e contrato para a US-018)

JavaScript via Alpine `x-init` é a abordagem escolhida. Comparação:

| Abordagem | Funciona? | Por quê |
|---|---|---|
| **CSS-only** com `scroll-margin-top` + âncora `#hoje` | Não | A página não é aberta com hash; o navegador não rola automaticamente. |
| `scroll-snap-type` no container da lista | Parcial | Snap-to-element funciona durante o scroll do usuário, mas não posiciona inicialmente. |
| **Alpine `x-init` + `scrollIntoView`** | **Sim** | Roda imediatamente após o DOM montar, é declarativo, e respeita `prefers-reduced-motion` se o `behavior` for `instant`. |

**Pseudo-implementação** (referência para o Codificador na US-018; **idêntica** à do protótipo HTML):

```html
<main x-data
      x-init="$nextTick(() => {
        const hoje = document.getElementById('no-hoje');
        if (hoje) hoje.scrollIntoView({ block: 'center', behavior: 'instant' });
      })">
  …
  <li id="no-hoje" class="trilha-no trilha-no--hoje">…</li>
  …
</main>
```

**Detalhes não-óbvios:**

- `behavior: 'instant'` (não `'smooth'`) — não rolagem animada porque (a) `prefers-reduced-motion` exigiria desligar a animação, gerando dois caminhos de código; e (b) animação inicial em tela de carregamento incomoda — adolescente acha que "o app travou e rolou sozinho". Ir direto para a posição final é mais respeitoso.
- `block: 'center'` — coloca o nó no centro vertical do viewport. Em telas pequenas (320×568), isso significa que o nó anterior ("em progresso") ainda fica visível em cima — o que ajuda a contextualizar "onde parei".
- `$nextTick` garante que o DOM esteja totalmente montado (evita race com componentes Alpine filhos).
- O nó "hoje" recebe `tabindex="-1"` para poder receber foco programático em uma fase futura (deep-link `#sessao-N` por exemplo) — não focamos automaticamente neste protótipo, pois foco automático em link rouba a primeira tecla "Tab" do usuário.

---

## 5. Componentes consumidos

Todos os componentes desta tela já estão catalogados em `doc/UX/02-componentes-base.md`. Nenhum componente novo é necessário — o nó da trilha é um **uso composto** do Card, Badge e Button existentes.

| Slot na tela | Componente | Variante | Referência |
|---|---|---|---|
| Topo fixo | **Header** | `compact` | ux-003 §5.3 — logo + ação. (Trocaremos para `expanded` na US-018 se quisermos breadcrumb "Semana 3 / Trimestre 2"; nesta versão, decisão conservadora §9.3.) |
| Cada um dos 7 nós | **Card** | `interactive` (clicável quando estado ≠ bloqueado) ou `flat` (apenas tooltip ao toque, em bloqueado) | ux-003 §3.3 |
| Nó "hoje" | **Card** | `interactive` + **modificador local** "destacado" (borda 2px coral + `--shadow-md`) | ux-003 §3 (sem variante nova; modificador via classe local) |
| Estado de cada nó | **Badge** | `primary` (HOJE), `success` (Concluído / Continuar), `neutral` (Bloqueado), `info` (Sábado), `danger` (não usado aqui — reservado para falhas) | ux-003 §7.3 |
| CTA do nó "hoje" | **Button** | `primary`, size `lg`, full-width | ux-003 §1.3 |
| Link "Trimestre" | **Button** | `ghost`, size `md` (icon-trailing seta `→`) | ux-003 §1.3, antecipa US-021/US-022 |
| Rodapé fixo | **Bottom-nav** | `bottom-nav` (item "Hoje" ativo) | ux-003 §6.3 |

**Componentes NÃO usados nesta tela (e por quê):**
- **Modal/Sheet** — toque em nó bloqueado mostra um *micro tooltip inline* (texto auxiliar dentro do próprio card é suficiente). Bottom sheet só viraria necessário se a explicação tivesse 2+ linhas; aqui é 1 linha.
- **Input** — trilha é navegação, não coleta de dados.
- **Toast** — não há feedback assíncrono na trilha vazia. Quando vier a US-018 real, toast pode aparecer **depois** de concluir uma sessão (na volta para a trilha), mas isso é estado fora deste protótipo.

---

## 6. Tokens consumidos

Todos referenciados literalmente de `doc/UX/01-design-tokens.md` (chore-ux-002 §10 — bloco `@theme`). Nenhum valor é reinventado. A coluna "onde aparece" amarra cada token a uma posição visual concreta.

### 6.1 Cores

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da app (toda a tela) |
| `--color-surface` | Fundo de cada card de nó + header + bottom-nav |
| `--color-surface-muted` | (reservado, não usado nesta tela) |
| `--color-divider` | Borda 1px dos cards `flat` (bloqueados e concluídos sem destaque), borda superior do bottom-nav |
| `--color-text-display` | Título da semana (h1) |
| `--color-text-body` | Nome da sessão dentro de cada nó, label "Trimestre" |
| `--color-text-muted` | Overline "SEMANA 3", subtítulo da meta, gancho dos cards bloqueados |
| `--color-primary` | Borda do nó "hoje" (2px), badge "HOJE" (texto), conector de "em progresso → hoje", CTA "Começar agora" |
| `--color-primary-hover` | Hover do CTA "Começar agora" (desktop) |
| `--color-primary-100` | Fundo do badge "HOJE" |
| `--color-primary-700` | Texto do badge "HOJE" |
| `--color-on-primary` | Texto do CTA "Começar agora" (branco sobre coral) |
| `--color-secondary-300` | Conector vertical entre nós concluídos |
| `--color-secondary-500` | Ícone check dos nós concluídos, barra preenchida do progresso |
| `--color-success-100` | Fundo dos badges "Concluído" e "Continuar" |
| `--color-success-700` | Texto dos badges "Concluído" e "Continuar" |
| `--color-info-100` | Fundo do badge "Sábado" |
| `--color-info-700` | Texto do badge "Sábado" |
| `--color-neutral-100` | Fundo da barra de progresso (vazio) |
| `--color-neutral-300` | Conector tracejado entre nós futuros, cor do ícone cadeado |
| `--color-focus-ring` | Anel de foco em cards interactive, no CTA, no link "Trimestre", nos itens do bottom-nav |

### 6.2 Tipografia

| Token | Onde aparece |
|---|---|
| `--font-display` | h1 da semana, badge "HOJE" |
| `--font-sans` | Todo o resto (subtítulo, nomes de sessão, microcopy auxiliar, labels do bottom-nav) |
| `--font-weight-regular` (400) | Microcopy auxiliar dos cards, subtítulo |
| `--font-weight-medium` (500) | Labels do bottom-nav (inativo) |
| `--font-weight-semibold` (600) | h1, nome da sessão em cada nó, labels dos badges, CTA, item ativo do bottom-nav |
| `--font-weight-bold` (700) | (não usado aqui — display-lg é hero, não trilha) |
| `--text-xs` | Overline "SEMANA 3", labels do bottom-nav, microcopy auxiliar |
| `--text-sm` | Subtítulo da meta, gancho de cada nó |
| `--text-base` | Nome de cada sessão dentro do nó, label do CTA |
| `--text-lg` | (reservado — usado se quisermos elevar o nome da sessão "hoje") |
| `--text-2xl` | h1 — título da semana |
| `--tracking-overline` | Overline "SEMANA 3" (uppercase + tracking +0.05em) |

### 6.3 Espaçamento

| Token | Onde aparece |
|---|---|
| `--space-1` (4px) | Gap entre ícone e label do bottom-nav, gap entre badge "Sábado" e badge "Bloqueado" empilhados |
| `--space-2` (8px) | Padding interno do badge, gap entre ícone de estado e nome da sessão |
| `--space-3` (12px) | Gap entre header do card e gancho |
| `--space-4` (16px) | **Padding lateral da tela** (chore-ux-001 §5.2), padding interno dos cards |
| `--space-5` (20px) | Padding interno do card `raised` (nó "hoje" e nó "em progresso") |
| `--space-6` (24px) | **Gap entre nós** da trilha (chore-ux-001 §5.7) |
| `--space-10` (40px) | Gap entre o bloco do cabeçalho da semana e o primeiro nó |
| `--space-11` (44px) | **Touch target mínimo** (RNF-A11Y-05): aplicado em `min-height` de todo card interativo, do CTA, dos itens do bottom-nav |
| `--space-12` (48px) | CTA "Começar agora" usa altura confortável `lg` |

### 6.4 Raio, sombra, motion, z-index

| Token | Onde aparece |
|---|---|
| `--radius-lg` (12px) | Cada card de nó (chore-ux-002 §5 — "card de sessão") |
| `--radius-full` (9999px) | CTA "Começar agora" (botão pill), badges |
| `--shadow-sm` | Header quando `scrollY > 8` |
| `--shadow-md` | Cards `raised` (nó "hoje", nó "em progresso") em repouso |
| `--shadow-lg` | (não usado aqui — reservado para modal) |
| `--shadow-focus` | Anel de foco em `:focus-visible` de qualquer elemento clicável |
| `--duration-fast` (120ms) | Transição de cor em hover/active do CTA |
| `--duration-base` (240ms) | Transição de sombra de cards interactive |
| `--ease-out-soft` | Easing de toda transição |
| `--z-sticky` (20) | Header e bottom-nav |

---

## 7. Estados e microcopy

**Todo texto em pt-BR.** Vetado vocabulário de cobrança/FOMO (P11): "não perca o streak", "você ficou para trás", "última chance", "vai zerar tudo". Vetado vocabulário moralista (P5): "você deveria", "Júlia, lembre-se de...". O tom é informativo e calmo — o app explica o que está acontecendo e libera Júlia para decidir o próximo passo.

### 7.1 Cabeçalho da semana

| Slot | Texto exato |
|---|---|
| Overline | `SEMANA 3` (uppercase via tracking, conteúdo já em caixa alta no HTML) |
| h1 | `<<TÍTULO DA SEMANA>>` (placeholder — virá do `fluxo-semana.md`) |
| Subtítulo | `<<Meta da semana em uma frase curta.>>` (placeholder; máximo ~80 caracteres) |
| Link | `Trimestre` (singular, sem "Ver". A seta `→` à direita já comunica o "ir para") |

### 7.2 Cards de nó — microcopy por estado

| Estado | Nome da sessão (linha 1) | Microcopy auxiliar (linha 2) | Label do badge |
|---|---|---|---|
| Concluído | `Sessão de <dia>` (placeholder) | — *(omitida; o badge já comunica)* | `Concluído` |
| Em progresso | `Sessão de <dia>` | `Você parou em <X> de <Y>.` (ex.: `Você parou em 3 de 5.`) | `Continuar` |
| Disponível (hoje) | `Sessão de quarta` (placeholder de "hoje") | `<<Gancho do tema do dia em uma linha.>>` (placeholder; ~70 caracteres) | `HOJE` |
| Bloqueado (futuro) | `Sessão de <dia>` | `Liberada em <dia-da-semana>, <DD/MM>.` (ex.: `Liberada em quinta, 21/05.`) | `Bloqueado` |
| Bloqueado (sábado por critério) | `Sessão de sábado` | `Termine mais <N> sessões para abrir o sábado.` (US-020) | `Sábado` (info) **+** `Bloqueado` (neutral) empilhados |

**Botão dentro do nó "hoje":** `Começar agora →` (a seta é um ícone-trailing SVG stroke, não o caractere Unicode).

### 7.3 Estados de borda — fora do feliz caminho

| Situação | Microcopy / comportamento |
|---|---|
| **Semana sem dados** (não deveria acontecer — a US-018 garante seed mínimo, mas tratamos por defesa) | Card único centralizado na lista: `Sua trilha aparece aqui assim que a semana começar.` Sem CTA. Sem ícone alarmante. |
| **Erro de carregamento** (rede caiu — relevante quando a US-018 vier com fetch real; fora desta tela "vazia", mas registrado) | Mensagem: `Não conseguimos carregar a semana agora. Toque para tentar de novo.` Botão `ghost` size `md` "Tentar de novo". Sem cobrança. |
| **Toque em nó bloqueado por data** | Tooltip/sheet de 1 linha: `Esta sessão abre em <dia-da-semana>, <DD/MM>.` + botão `ghost` "Entendi" (US-019 critério 2). |
| **Toque em nó bloqueado de sábado (critério)** | Tooltip/sheet de 1 linha: `Complete mais <N> sessões da semana para abrir o sábado.` + botão `ghost` "Entendi". |
| **Toque em nó concluído** | Abre sessão em modo revisão (US-019 critério 3) — UI dessa entrada não é responsabilidade desta tela; aqui apenas garantimos que o tap não dispara avanço de XP. |

### 7.4 O que evitamos (ancorado em P11)

- ❌ `Não perca seu streak!` — gera FOMO.
- ❌ `Você ficou 2 dias para trás.` — cobrança.
- ❌ `🔥 Continue forte!` — emoji como microcopy funcional + tom moralista.
- ❌ `Última chance para fazer a sessão de ontem!` — pânico.
- ✅ `Você parou em 3 de 5.` — fato neutro.
- ✅ `Liberada em quinta, 21/05.` — informação calma.
- ✅ `Termine mais 3 sessões para abrir o sábado.` — convite, sem prazo final.

---

## 8. Acessibilidade

### 8.1 Estrutura semântica

- `<header role="banner">` para o topo (papel implícito por `<header>` no nível raiz).
- `<main>` envolve o cabeçalho da semana + lista de nós.
- Lista de nós é `<ol>` (ordered list) — **a ordem é semanticamente significativa** (semana tem início e fim, dias têm sequência). Cada nó é um `<li>`.
- Cada nó com estado ≠ bloqueado renderiza um `<a href="/sessoes/<id>">` ao redor (ou inside) do card; nó bloqueado renderiza `<button type="button" aria-disabled="true">` que dispara o tooltip de motivo. Não usar `<div onclick>` (ux-003 §0.9).
- `<nav aria-label="Principal">` para o bottom-nav (ux-003 §6.7).

### 8.2 Labels e descrições

Cada nó interativo precisa de uma label completa para leitor de tela — o texto visível "Sessão de quarta · Continuar" pode ser ambíguo isoladamente. Padrão:

```html
<a href="/sessoes/4" aria-label="Sessão de quarta — disponível agora, abre <<Título da sessão>>">
  <!-- conteúdo visível do card -->
</a>
```

Variações por estado:

| Estado | Padrão de `aria-label` |
|---|---|
| Concluído | `Sessão de <dia> — concluída, abre revisão.` |
| Em progresso | `Sessão de <dia> — em progresso, <X> de <Y> blocos.` |
| Disponível (hoje) | `Sessão de <dia> — disponível agora, abre <<Título>>.` |
| Bloqueado por data | `Sessão de <dia> — bloqueada, libera em <dia-da-semana>, <DD/MM>.` |
| Bloqueado sábado (critério) | `Sessão de sábado — bloqueada, faltam <N> sessões para abrir.` |

### 8.3 Foco e ordem de tabulação

- Ordem de Tab no DOM: header (logo + ícone "Ajustes") → link "Trimestre" → cada nó em sequência (do mais antigo ao mais recente) → bottom-nav (Trilha → Hoje → Perfil → Ajustes). **Sem `tabindex` positivo** — ordem natural do DOM.
- Foco visível: todos os elementos clicáveis ganham anel `--shadow-focus` em `:focus-visible` (chore-ux-002 §2.6).
- **Nó "hoje" recebe `tabindex="-1"`** — não entra na ordem de Tab por padrão, mas pode receber foco programático no futuro (deep-link `#sessao-N`). Decisão conservadora: **não focamos automaticamente** ao abrir a tela, pois roubaria a primeira tecla "Tab" do usuário e quebraria a expectativa de leitura sequencial.
- Skip-link "Pular para conteúdo": registrado como **dívida** (entra na chore-ux-007 — auditoria WCAG completa). Não aparece neste protótipo porque a US-018 ainda precisará decidir o destino do skip — para `#no-hoje` ou para o `<main>` topo?

### 8.4 Contraste (WCAG AA mínimo — chore-ux-001)

Todas as combinações já estão validadas em `doc/UX/00-identidade-visual.md` §2:

| Combinação | Razão | Nível |
|---|---|---|
| Título da sessão (`neutral-900`) sobre card branco | 16.4:1 | AAA |
| Microcopy auxiliar (`neutral-500`) sobre card branco | 4.62:1 | AA |
| Badge "HOJE" texto (`primary-700`) sobre fundo (`primary-100`) | 8.91:1 | AAA |
| Badge "Concluído" texto (`success-700`) sobre (`success-100`) | 6.84:1 | AA |
| CTA "Começar agora" texto (branco) sobre (`primary-500`) | 4.62:1 | AA |
| Borda 2px coral do nó "hoje" sobre branco | (elemento gráfico ≥ 2px, não-texto) | — |

### 8.5 Redundância de canal

Estado **nunca** é comunicado apenas por cor. Cada nó combina:
- **Cor** (borda/fundo do card, cor do ícone de estado, cor do badge);
- **Forma** (ícone diferente por estado: check / play / círculo preenchido / cadeado);
- **Texto** (label do badge: "Concluído", "Continuar", "HOJE", "Bloqueado").

Em simulação de daltonismo (deuteranopia, protanopia, tritanopia) a leitura por ícone+badge continua inequívoca.

### 8.6 `prefers-reduced-motion`

- O `scrollIntoView` usa `behavior: 'instant'` — sem animação. Conforme.
- Transições de hover/active dos cards usam `--duration-base` (240ms) em `box-shadow` e `border-color`. A regra global de motion-reduced (chore-ux-002 §8 + chore-ux-003 §0.5) zera para `0.01ms`. Conforme.
- A microanimação Lottie "cadeado se abrindo" descrita na §3 **não** é parte deste protótipo; quando vier, deve respeitar `prefers-reduced-motion` exibindo o estado final estático.

---

## 9. Decisões e alternativas descartadas

### 9.1 Lista vertical (escolhida) vs. mapa sinuoso SVG (rejeitado)

Considerei abrir com um caminho sinuoso estilo Duolingo — nós em zigue-zague conectados por curvas, ilustração editorial entre eles. Rejeitei por **três custos** que não fazem sentido para o MVP:

1. **Largura útil em 320px.** Qualquer offset horizontal entre nós (mesmo 40px) reduz o card para ~180px de largura — o nome da sessão quebra em 2 linhas, o gancho some, o badge fica espremido.
2. **Acessibilidade da ordem.** Em zigue-zague, leitor de tela precisa de `aria-flowto` ou ordem DOM forçada — abre porta para inconsistência entre leitura visual e leitura assistiva.
3. **Custo de ilustração.** Mapa sinuoso só faz sentido com ilustrações de cenário (montanha, vale, igrejinha) — e ilustração editorial coerente é dúvida aberta (chore-ux-001 §7).

**Quando reabriríamos a decisão:** v2, depois de fechar o estilo concreto das ilustrações e validar com adolescentes reais que "trilha como caminho" é importante para a metáfora. Por enquanto, lista vertical entrega 100% da função com 0% do custo.

### 9.2 Badge como reforço de estado (escolhido) vs. cor pura (rejeitado)

Considerei deixar o estado representado apenas por **cor do card** (verde claro para concluído, branco com borda coral para hoje, cinza para bloqueado) — sem badge textual. Rejeitei porque (a) WCAG/RNF-A11Y exige redundância de canal (chore-ux-001 §5.3 — cor + forma + texto), e (b) em viewport pequeno, sem o texto "HOJE" o usuário hesita: "esse cor coral é o ativo ou é um erro?". O badge custa ~24px de altura por card mas elimina a ambiguidade.

**Quando reabriríamos:** se descobrirmos em teste de usabilidade que os badges poluem visualmente em trilhas com muitos dias concluídos. Mitigação possível: badge "Concluído" some após 24h e fica só o check; "HOJE" permanece sempre.

### 9.3 Header `compact` (escolhido) vs. `expanded` com breadcrumb (rejeitado para esta tela)

Considerei usar o header `expanded` (ux-003 §5.3 — 80px de altura com botão voltar + título + subtítulo + ação). Rejeitei porque (a) trilha é tela inicial — **não há "voltar"** desde a trilha (ela é a raiz da navegação principal), e (b) o título da semana já é o h1 dentro do `<main>`, repeti-lo no header é redundante. Header `compact` (56px) economiza ~24px de altura — significativo em viewport de 568px.

**Quando reabriríamos:** quando a trilha for acessada a partir de um trimestre histórico (US-021 critério 3) — aí o header **deve** ser `expanded` com botão "← voltar" e subtítulo "Semana 5 de 13". Esse caso é uma rota diferente; a trilha-raiz mantém `compact`.

### 9.4 Sem ilustração editorial no cabeçalho (escolhido) vs. ilustração de cabeçalho por tema (rejeitado para o protótipo)

A US-018 critério 2 menciona "ilustração de cabeçalho do tema". O protótipo **não** inclui essa ilustração porque (a) o estilo concreto das ilustrações é dúvida aberta (chore-ux-001 §7, "produção de ilustrações editoriais"), e (b) o protótipo precisa funcionar com placeholders — uma ilustração placeholder genérica ("paisagem stock") violaria P15 ("ilustração editorial > foto > ícone genérico"). Marcamos o espaço com o overline + h1 + subtítulo, e deixamos uma nota explícita no HTML (`<!-- Slot reservado para ilustração de cabeçalho — virá do banco de ilustrações editoriais. -->`).

**Quando reabriríamos:** assim que o estilo de ilustração for definido. A US-018 implementará o slot definitivamente.

### 9.5 `scrollIntoView({behavior: 'instant'})` (escolhido) vs. `behavior: 'smooth'` (rejeitado)

Considerei rolar suavemente ao "hoje" como gesto de descoberta — "olha, é aqui que você está". Rejeitei porque (a) `prefers-reduced-motion` exigiria desativar a animação, criando dois caminhos de código; (b) em conexão lenta, a rolagem animada começa atrasada e parece bug; e (c) `instant` é mais respeitoso com adolescente que abre o app sabendo o que quer fazer.

**Quando reabriríamos:** se teste de usabilidade mostrar que adolescentes "se perdem" sem o gesto de rolagem. Mitigação alternativa: indicador de rolagem (mini-mapa lateral) em vez de animação.

---

## 10. Pendências e ganchos para tasks futuras

- **Nomes próprios das sessões** — virão de `doc/conteudo/fluxo-semana.md`, ainda em refinamento (PRD §10). Os placeholders deste doc e do HTML serão substituídos na US-018.
- **Ilustração de cabeçalho do tema** — depende do estilo editorial (chore-ux-001 §7, dúvida aberta).
- **Microanimação Lottie de desbloqueio de sábado** — registrada em §3, implementada quando a US-020 entrar com a regra completa de progresso.
- **Skip-link "Pular para conteúdo"** — entra na chore-ux-007 (auditoria WCAG completa).
- **Smoke visual end-to-end** — chore-ux-008 consumirá este HTML como uma das telas-âncora.
- **Heatmap da US-022** — não aparece nesta tela; entra na visão de trimestre. Tokens `--color-heatmap-*` já estão prontos em chore-ux-002 §2.5.

---

## 11. Histórico de aprovação

| Data       | Item                              | Decisão               |
|------------|-----------------------------------|-----------------------|
| 2026-05-18 | Estrutura da trilha (9 seções)    | Proposto pelo Designer |
| —          | Aprovação pelo humano (Dioni)     | Pendente              |
