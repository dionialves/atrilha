# Protótipo do painel dos pais — atrilha

**Task:** chore-ux-006 (Issue #25)
**Sprint:** Sprint 2 — Identidade visual & Design system base
**Marco:** M5 — Protótipo do painel do responsável validado
**Status:** Proposto
**Depende de:** `doc/UX/00-identidade-visual.md` (chore-ux-001, aprovada) · `doc/UX/01-design-tokens.md` (chore-ux-002, aprovada) · `doc/UX/02-componentes-base.md` (chore-ux-003, aprovada) · `doc/UX/03-prototipo-trilha.md` (chore-ux-004, aprovada) · `doc/UX/04-prototipo-sessao.md` (chore-ux-005, aprovada)
**Bloqueia:** chore-ux-008 (smoke visual end-to-end)
**Antecipa:** US-042, US-043, US-044, US-045, US-046 (sprints 13–14)
**Referências:** PRD §4.2 (P8 — privacidade sagrada, P9 — painel não-vigilante, P11 — sem dark patterns) · PRD §6 ADR-003 (onboarding guiado obrigatório) · PRD §6 ADR-005 (compartilhamento opt-in por item) · PRD §6 ADR-008 (texto bíblico ARC) · PRD §8.4 (RNF-A11Y-01..05, RNF-COMP-04) · PRD §10 (RF-E7-01..10) · AGENTS.md (idioma pt-BR na UI, stack Thymeleaf + HTMX + Alpine + Lottie — ADR-011)

**Escopo deste doc:** especificar **em prosa** o painel do responsável "vazio" (sem dados de backend reais) — estrutura, tom visual, estados especiais, microcopy e regras de "sinais positivos". A **materialização navegável** vive em `doc/UX/prototypes/painel-pais.html`, em mock de **baixa atividade** (teste deliberado de tom: se o painel parece amigável quando a filha não está fazendo as sessões, ele resiste). Nada aqui vira código de produção em `src/**` — a implementação real entra nas US-042 a US-046 dos sprints 13–14.

**Declarações de escopo (essenciais antes de prosseguir):**

1. **Rótulos e textos finais virão das US-042–046.** Esta spec define **padrões de tom e estrutura**; toda microcopy abaixo é tratada como **placeholder revisável**. As US correspondentes nos sprints 13–14 podem refinar palavras desde que mantenham o contrato P8/P9/P11 deste doc.
2. **Nome do tema da semana, ilustração de cabeçalho e texto da pergunta familiar não são fechados aqui** — virão de `doc/conteudo/fluxo-semana.md` (pré-requisito da US-018 e US-044). Usamos placeholders explícitos: `<<Tema da semana>>`, `<<Pergunta de discussão familiar>>`.
3. **O onboarding obrigatório (US-042) é descrito em wireframe textual nesta spec; não é entregue como HTML separado.** O entregável HTML cobre o painel **pós-onboarding** em estado de baixa atividade. Um arquivo `painel-pais-onboarding.html` pode ser entregue se houver folga, mas não bloqueia aceitação desta task.
4. **Heatmap binário ≠ heatmap anual (RF-E6-10/US-041).** O heatmap anual da adolescente usa 3 tons de laranja crescentes (chore-ux-002 §2.5). O painel do **responsável** mostra apenas **uma escala binária da semana corrente** — fez/não fez. **Não importar tokens `--color-heatmap-1..3` aqui.**

---

## 1. Objetivo da tela

O painel do responsável é a **superfície mais politicamente sensível** do produto. Atende US-043 (sinais positivos da semana) e é a tela onde Carlos, recém-onboardado (US-042), chega para acompanhar a filha sem invadir o espaço dela. A jornada J5 ("Carlos sente que o painel é útil sem ser vigilante") é o critério único de sucesso: se a tela transmite controle, vigilância ou cobrança, o painel falhou — mesmo que cada elemento isolado esteja "correto" do ponto de vista de implementação. O painel cumpre quatro funções simultâneas: (a) mostrar que **algo está acontecendo** sem quantificar com precisão clínica (heatmap binário, sem horário, sem intensidade — US-043 critério 1); (b) dar a Carlos um **gancho de conversa real para o sábado** (pergunta de discussão familiar, US-044, e botão "Conversamos sobre isso", US-045); (c) celebrar conquistas concretas e duradouras da filha (versículos memorizados do trimestre, US-043 critério 4); e (d) **respeitar o que a adolescente decidiu manter privado** (US-046 — aba "Compartilhado por [apelido]" só aparece quando há item opt-in, nunca como timeline aberta). Tudo isso sem nunca soar como dashboard de produtividade adulta.

---

## 2. Estrutura do painel — wireframe textual (mobile, 320px)

A leitura é vertical, de cima para baixo, do topo do viewport até o `safe-area-inset-bottom` da bottom-nav. Toda a coluna útil tem `--space-4` (16px) de padding lateral; nenhum elemento ultrapassa essa borda. O painel é **uma tela só**, sem swipe entre cards de filhos — multi-filho é fora do MVP (US-043, "Fora do Escopo").

```
┌──────────────────────────────────────┐
│ Painel de Júlia              ⚙       │  Header compact (ux-003 §5)
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│  TEMA DA SEMANA                      │  overline (text-xs, tracking +0.05em, uppercase, text-muted)
│                                      │
│  <<Tema da semana>>                  │  h1 (display, semibold, text-2xl) — placeholder
│                                      │
│  <<Subtítulo: meta da semana>>       │  text-sm, text-muted
│                                      │
│  (slot reservado para ilustração     │  decisão §10.4: sem placeholder visual; comentário no HTML
│   de cabeçalho — virá do banco)      │
└──────────────────────────────────────┘
                  │ space-10 (40px)
┌──────────────────────────────────────┐
│  ESTA SEMANA                         │  overline
│                                      │
│  ◯  ◯  ◯  ●  ●  ◯  ◯                │  Heatmap binário 7 dias (dom→sáb)
│  D   S   T   Q   Q   S   S          │  Letra do dia abaixo, text-xs, text-muted
│                                      │
│  Esta semana: 2 de 7 sessões.        │  text-sm, neutral — descritivo, sem juízo
└──────────────────────────────────────┘
                  │ space-6 (24px)
┌──────────────────────────────────────┐
│  SEQUÊNCIA ATUAL                     │  overline
│                                      │
│       0                              │  display-lg, Bricolage 700, tabular-nums
│       dias                           │  text-sm, text-muted (em coluna sob o número)
└──────────────────────────────────────┘
                  │ space-6 (24px)
┌──────────────────────────────────────┐
│  PARA CONVERSAR EM FAMÍLIA           │  overline
│                                      │
│  <<Pergunta de discussão             │  text-lg, semibold, text-display
│   familiar da semana.>>              │
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Conversamos sobre isso        │  │  Botão secondary (ux-003 §1) full-width
│  └────────────────────────────────┘  │  Após toque: vira "Conversamos em DD/MM"
└──────────────────────────────────────┘
                  │ space-6 (24px)
┌──────────────────────────────────────┐
│  VERSÍCULOS MEMORIZADOS               │  overline
│  DESTE TRIMESTRE                     │
│                                      │
│  ┌────────────────────────────────┐  │
│  │ Salmos 23.1                    │  │  Card flat (ux-003 §3) — referência + data
│  │ memorizado em 12/05            │  │
│  └────────────────────────────────┘  │
│                                      │
│  (sem outros itens no mock —         │
│   estado de baixa atividade)         │
└──────────────────────────────────────┘
                  │ space-8 (32px)
┌──────────────────────────────────────┐
│  Como funciona  →                    │  Link ghost (ux-003 §1), text-sm, text-muted
└──────────────────────────────────────┘
                  │ space-10 (40px)
┌──────────────────────────────────────┐
│  Painel    Perfil    Sair            │  Bottom-nav 3 itens, "Painel" ativo
└──────────────────────────────────────┘
            ▔▔▔▔▔ safe-area-inset-bottom
```

### Por que **lista vertical sem abas no topo**

A primeira tentação é usar **abas** entre "Painel" e "Compartilhado por Júlia" no topo da tela (padrão de timeline de redes sociais). Rejeitamos porque (a) a aba "Compartilhado por" só existe quando há itens opt-in — em mock de baixa atividade ela não aparece, e duas abas com uma vazia desconfortam a leitura; (b) o painel não é uma timeline competitiva — é um **resumo único**, não um feed; (c) abas dão a sensação de "tem mais coisa para descobrir" e induzem o pai a buscar dados que **deliberadamente não estão lá** (P9). A solução adotada: o painel é uma página vertical única; quando houver item opt-in (US-046), uma **seção própria** aparece no final do scroll (antes do rodapé), claramente rotulada com o apelido da filha — não compete visualmente com os sinais quantitativos.

### Ancoragem ao "sinal positivo"

Cada card recebe **três reforços de leitura calma** (oposto da "redundância de canal de alarme"):

1. **Overline neutro `text-muted`** acima de cada card (ex.: "ESTA SEMANA", "SEQUÊNCIA ATUAL"). Sem cor de status — tudo no mesmo cinza-quente. Em padrões adultos de dashboard, esses overlines viriam coloridos por gravidade (verde/laranja/vermelho); aqui são todos iguais por princípio (P9).
2. **Tipografia descritiva, não imperativa.** "Esta semana: 2 de 7 sessões." em vez de "Apenas 2 sessões!" ou "Faltam 5!". O texto é fato; juízo fica com Carlos.
3. **Sem ícones de alerta em qualquer dado de atividade.** Nenhum `!`, `⚠`, sino, exclamação, seta para baixo. Ícones aparecem só em ações (engrenagem de Ajustes, seta do link "Como funciona") e no estado vazio do versículo, e mesmo assim em traço neutro.

---

## 3. Tom visual e regras de "sinais positivos"

A diferença entre "painel acolhedor" e "painel cobrador" é frequentemente uma única decisão visual mal-calibrada. Esta seção é o **contrato de tom** que toda decisão da implementação (US-042–046) deve respeitar.

### 3.1 Regra-mãe: nenhuma cor semântica de alarme em dados de atividade

Os tokens `--color-danger-*` e `--color-warning-*` (chore-ux-002 §2.4) **estão vetados** em qualquer elemento que represente atividade da adolescente — heatmap, streak, contagem de sessões, lista de versículos, marcação de conversa. Esses tokens só podem aparecer em **erros operacionais** do próprio painel (ex.: "Não conseguimos carregar o painel agora") — nunca em sinais sobre a filha. Da mesma forma, **`--color-success-*` está restrito ao toast de confirmação** ("Marcado como conversado"); não usar `bg-success-100` como fundo de "dia preenchido" no heatmap — virou comemoração; queremos discrição (ver §3.3).

### 3.2 Heatmap binário (US-043 critério 1)

**Exatamente dois estados visuais por célula.** Sem gradiente, sem intensidade, sem "muito" ou "pouco":

| Estado | Visual | Token |
|---|---|---|
| Preenchido (sessão feita no dia) | Quadrado preenchido, borda `--color-primary-300` (1px) | `bg: --color-primary-300`, `border: --color-primary-300` |
| Não-preenchido (sem sessão) | Quadrado **apenas contornado**, sem fundo | `bg: transparent`, `border: --color-neutral-300` (1px) |

**Por que `--color-primary-300` (e não 500 ou 700):** O coral primário 500 é a cor de marca, escassa por princípio (chore-ux-001 §5.6). Usar 500 no heatmap acende cada dia preenchido como CTA — visualmente compete com o botão "Conversamos sobre isso". A variação 300 (coral suave) lê como "presença calma", não como "alerta importante". Reforça o tom de "sinal positivo discreto".

**Por que não usar `--color-secondary-*` (lime/verde de progresso):** verde no dashboard parental ativa associação imediata de "OK / dever cumprido"; isto introduz o oposto (dia sem verde = "dever não cumprido"). Coral suave neutraliza essa leitura — não tem carga moral.

**Por que não usar 3 tons crescentes (RF-E6-10):** RF-E6-10 é o heatmap da **adolescente** (US-041 — visão dela, ano inteiro). No painel parental, intensidade vira "minha filha fez 2 sessões esse dia, mas no dia X fez só 1 — o que aconteceu?". Decisão registrada em §10.1.

### 3.3 Streak: o número é o número

O card de sequência mostra o número grande em `--text-3xl` (32px), Bricolage 700, tabular-nums. **Acompanhado apenas da palavra "dias" em `text-sm`/`text-muted` logo abaixo.** Sem ícone de chama, sem ícone de fogo, sem ícone de troféu, sem ícone de "X" quando zera. O zero é apenas um zero — exatamente como o 7 seria apenas um 7.

**Vetado:** chama (🔥), foguete (🚀), explosão (💥), troféu (🏆), seta para cima ou para baixo, "+1!", "RECORDE!", "Atenção!".

**OK:** o número, "dias" em caixa baixa, e (opcionalmente, decisão tomada nesta spec — ver §10.3) **nenhuma microcopy adicional** quando o streak é 0. Não dizer "tudo bem, recomeçar é uma opção" — isso seria reconhecer implicitamente que zero é um problema. Zero é um estado, não uma falha.

### 3.4 Linguagem positiva, nunca cobradora — tabela do que pode e do que não pode

Tabela completa de microcopy na §8. Princípios:

- **Descritivo > imperativo.** "Esta semana: 2 de 7 sessões." (fato) > "Faltam 5 sessões esta semana!" (cobrança).
- **Tempo presente da filha > tempo perdido.** "Sequência atual: 0 dias" > "Sua filha ficou sem entrar há X dias".
- **Permanência > urgência.** "Versículos memorizados deste trimestre" > "Já viram 1 versículo neste trimestre — pouco!".
- **Convite > julgamento.** "Para conversar em família" > "Pergunta da semana — não esqueça!".

---

## 4. Onboarding obrigatório de 3 telas (US-042) — wireframe textual

O onboarding é a defesa estrutural do painel: Carlos só vê o dashboard depois de entender **o que verá e o que não verá** (US-042 critério 5). Sem isso, o painel — por mais bem desenhado — viraria ferramenta de vigilância na cabeça de Carlos, que cobraria a filha pelo que **o painel deliberadamente não mostra**. O onboarding é o ADR-003 materializado em UI.

### 4.1 Estrutura comum às 3 telas

Modal full-screen em mobile (não é bottom sheet — precisa de protagonismo total), bloqueante (sem X visível para fechar), com focus trap. Header com indicador "Tela X de 3" e um espaço de progresso visual (3 pontos, o atual preenchido).

```
┌──────────────────────────────────────┐
│  TELA X DE 3              ● ○ ○      │  text-xs uppercase, text-muted | dots
└──────────────────────────────────────┘
┌──────────────────────────────────────┐
│                                      │
│  <<Ilustração editorial — slot>>     │  decisão §10.4: comentário, sem placeholder
│                                      │
│  <h1>                                │  text-2xl, Bricolage semibold
│                                      │
│  <Lista ou parágrafo curto>          │  text-base, body
│                                      │
│  ┌────────────────────────────────┐  │
│  │  Continuar         →           │  │  CTA primary lg full-width
│  └────────────────────────────────┘  │
│                                      │
│  (na tela 3: link "Voltar" ghost     │
│   abaixo do CTA principal)           │
└──────────────────────────────────────┘
```

**Botão "Voltar" disponível a partir da tela 2** (ghost, abaixo do CTA), permitindo revisar o que viu sem ter que reiniciar. **Botão "Pular" só aparece na tela 3** — US-042 critério 1: "só consegue pular após chegar à última". O botão final na tela 3 é "Entrar no painel".

### 4.2 Tela 1 — "O que você verá"

**H1:** `O que você verá aqui`

**Corpo:** Lista visual de ícones SVG stroke (24×24) com label curto:

- (ícone calendário) **Tema da semana** — o assunto que sua filha está estudando.
- (ícone grade 7) **Como foi a semana** — em quais dias houve sessão. Sem horário.
- (ícone número) **Sequência atual** — quantos dias seguidos com sessão.
- (ícone balão de fala) **Pergunta para conversar em família** — um gancho para o sábado.
- (ícone livro) **Versículos memorizados** — o que ela memorizou neste trimestre.

**Microcopy de fechamento:** `Tudo aqui é resumo. Nada substitui conversar com ela.`

### 4.3 Tela 2 — "O que você NÃO verá"

**H1:** `O que você não verá`

**Corpo:** Lista no mesmo padrão visual da tela 1, mas com tom afirmativo (não com X vermelho de proibição — seria contraditório com o tom):

- **As reflexões dela** — são o espaço privado da Júlia. Ela pode escolher compartilhar uma específica, e só então aparece aqui.
- **A que horas ela usa o app** — não acompanhamos horário.
- **Se ela acertou ou errou no quiz** — o erro ensina dentro do app, não vira nota aqui.
- **Quanto tempo ela passou em cada sessão** — não medimos.
- **Dias específicos sem atividade como "alerta"** — alguns dias têm sessão, outros não. É o ritmo dela.

**Microcopy de fechamento:** `Isso é por escolha. O painel respeita o espaço dela para que ela use o app com liberdade.`

### 4.4 Tela 3 — "Como conversar no sábado"

**H1:** `Como conversar no sábado`

**Corpo:** Parágrafo único, calmo:

> A cada semana, uma pergunta de discussão aparece no painel. É uma sugestão de assunto para vocês conversarem juntos no sábado — sobre o tema da semana, não sobre o uso do app.
>
> Depois que vocês conversarem, você pode tocar em "Conversamos sobre isso" para deixar registrado. A Júlia não vê essa marcação no app dela.

**CTA final:** `Entrar no painel` (variant primary, lg, full-width).

**Link secundário abaixo:** `Voltar` (ghost), `Pular tutorial` (ghost, ao lado do voltar) — US-042 critério 1 só permite pular **a partir desta tela**.

### 4.5 Revisitação (US-042 critério 6)

A partir do painel pós-onboarding, o link `Como funciona` no rodapé (ver §2 e §5) reabre o tutorial das 3 telas. Quando reaberto via revisitação, **o botão "Pular" fica disponível desde a tela 1** (Carlos já viu o conteúdo uma vez — não é onboarding bloqueante na segunda passagem).

---

## 5. Estados especiais

### 5.1 Semana inteiramente vazia (Júlia não fez nenhuma sessão)

- **Heatmap:** todos os 7 dias contornados, nenhum preenchido. Microcopy abaixo: `Esta semana: 0 de 7 sessões.`
- **Streak:** `0 dias`. Sem microcopy adicional.
- **Tema da semana:** aparece normalmente — não depende de atividade da filha.
- **Pergunta de discussão familiar:** aparece normalmente. **O sábado existe mesmo que a filha não tenha entrado no app durante a semana.**
- **Versículos memorizados do trimestre:** se houver de semanas anteriores, mostra. Se zero, mostra estado vazio (ver §5.4).
- **Aba "Compartilhado por":** ausente (não há item opt-in).

**Risco a evitar:** transformar a tela vazia em "página de alerta". Decisão: a tela vazia é **visualmente idêntica** à tela cheia, só com números menores. Sem mensagem extra, sem cor diferente, sem "atenção!".

### 5.2 Semana cheia (7/7)

- **Heatmap:** todos os 7 dias preenchidos. Microcopy: `Esta semana: 7 de 7 sessões.`
- **Streak:** número alto (ex.: `21 dias`).
- **Versículos memorizados:** lista mais longa.

**Risco a evitar:** comemorar exageradamente — quebraria o tom calmo. Sem confetes, sem "Excelente!", sem "Parabéns!". O número fala por si.

### 5.3 Sem pergunta familiar (conteúdo da semana sem campo)

A pergunta vem do YAML do conteúdo (US-044 contexto). Se a semana corrente **não tiver** pergunta cadastrada, o card de pergunta familiar **não aparece** — a tela não renderiza um estado vazio com "Nenhuma pergunta esta semana" (seria sublinhar uma ausência). Decisão conservadora: omissão silenciosa do card; demais cards continuam normais.

### 5.4 Sem versículos memorizados no trimestre

Card aparece com texto único centralizado, em tom calmo:

> `Os versículos memorizados aparecem aqui ao longo do trimestre.`

Sem ilustração, sem CTA, sem "incentive sua filha a memorizar". É só um informe.

### 5.5 Aba "Compartilhado por [apelido]" — quando aparece

- **Ausente** se a adolescente nunca usou o toggle de compartilhamento (US-047) em nenhuma reflexão. **Esta é a renderização do mock HTML.**
- **Presente** quando há ≥1 reflexão com toggle ligado. Aparece como **seção própria** ao final da página (antes do link "Como funciona"), com título `Compartilhado por <Apelido>` (`Apelido` é o que a Júlia configurou em US-047; nunca o nome legal completo).

Estrutura do card de reflexão compartilhada:

```
┌──────────────────────────────────────┐
│  Sessão de quarta, 12/05             │  text-sm, text-muted
│  <<Tema da semana>>                  │  text-base, semibold
│                                      │
│  <<Texto integral da reflexão.>>     │  text-base, body, ≤1000 caracteres
└──────────────────────────────────────┘
```

**Regras críticas (ADR-005 + US-046):**

- Cada reflexão é um card independente. Sem "ver mais", sem truncamento — texto integral sempre. (Se truncar, Carlos imagina o que foi cortado e a privacidade do que Júlia decidiu mostrar fica corrompida.)
- **Nada de sinais quantitativos misturados** — nenhum "ela compartilhou 3 reflexões esta semana" no topo da aba. Cada item é cada item.
- Se Júlia revogar (US-047), o item **some imediatamente** da próxima abertura do painel. Sem aviso no painel ("Júlia removeu uma reflexão"); a revogação é silenciosa do lado dela.

---

## 6. Componentes consumidos

Todos os componentes desta tela já estão catalogados em `doc/UX/02-componentes-base.md`. Documentamos lacunas que podem virar componente novo nas US-042–046.

| Slot na tela | Componente | Variante | Referência |
|---|---|---|---|
| Topo fixo | **Header** | `compact` | ux-003 §5.3 — saudação "Painel de Júlia" + ícone-botão "Ajustes". |
| Cabeçalho da semana (tema) | **Card** (decisão suave: pode virar bloco solto sem card) | (none) — bloco com overline + h1 + subtítulo | Decisão §10.5: não envelopar em card; o bloco é puramente tipográfico, como o cabeçalho da trilha (ux-004 §2). |
| Heatmap da semana | **Componente novo — heatmap binário** | `weekly-binary` | Lacuna documentada (§6.2). Não está em ux-003. Implementado como **CSS inline** no protótipo; pode virar fragment dedicado em US-043 ou continuar como composto local. |
| Card de streak | **Card** | `flat` | ux-003 §3.3 — densidade baixa, número grande dentro. |
| Pergunta familiar + botão | **Card** + **Button** | Card `flat` + Button `secondary` (não `primary` — ver §6.3) | ux-003 §3.3 + §1.3. |
| Versículos memorizados | **Card** | `flat` | ux-003 §3.3, com lista interna. |
| Aba "Compartilhado por" (quando presente) | **Card** | `flat` | ux-003 §3.3. **Não é uma aba real do componente Tabs** (que nem existe no catálogo) — é uma seção rotulada. |
| Link "Como funciona" | **Button** | `ghost` | ux-003 §1.3. |
| Rodapé fixo | **Bottom-nav** | `bottom-nav` | ux-003 §6.3. 3 itens: Painel · Perfil · Sair. |
| Onboarding modal (3 telas) | **Modal** | `critical` (não fecha por backdrop — US-042 critério 1) | ux-003 §4.3. |

### 6.1 Componentes NÃO usados nesta tela (e por quê)

- **Toast** — só aparece **após** a ação "Conversamos sobre isso" (toast de confirmação `success`, ux-003 §8.3). Não é estrutura permanente da tela.
- **Input** — painel é leitura, não coleta. Carlos não escreve nada aqui.
- **Badge** — uso intencionalmente reduzido. Nenhum badge "HOJE", "Concluído", "Novo" no painel; gera ruído de status. Excêção: se houver indicador discreto sobre o ícone de notificações no header (fora desta task).

### 6.2 Lacuna documentada — heatmap binário

O catálogo de componentes-base (ux-003) **não inclui** um componente "heatmap". O heatmap binário desta tela é **composição local** de 7 quadrados via `display: grid; grid-template-columns: repeat(7, 1fr)`. Decisão: **não promover a componente novo nesta task** — uma única tela usa, valores futuros (heatmap anual da US-041) terão semântica diferente (3 tons + empty), e generalizar prematuramente complica o catálogo. Quando a US-043 entrar com implementação real, o Codificador pode:

- Materializar como fragment local `components/heatmap-binary-week.html` (recomendação leve);
- OU manter como bloco CSS inline da página do painel.

Não bloqueia. Documentado para evitar surpresa.

### 6.3 Decisão: botão "Conversamos sobre isso" é `secondary`, não `primary`

O CTA primário coral é escasso (chore-ux-001 §5.6 — máximo 2 elementos coral por viewport). No painel, o coral aparece **só no heatmap** (em `--color-primary-300`). Se o botão fosse `primary`, ele competiria visualmente com o heatmap e roubaria atenção de tudo o que estiver acima — incluindo a própria pergunta. **`Secondary`** (fundo branco, borda `--color-border-strong`, texto `--color-text-body`) entrega ação clara sem virar elemento dominante. Aderente também à filosofia P9: nenhuma ação no painel é "urgente o suficiente" para virar coral.

---

## 7. Tokens consumidos

Todos referenciados literalmente de `doc/UX/01-design-tokens.md` (chore-ux-002). Nenhum valor é reinventado. Lista intencionalmente enxuta — o painel deve usar **menos cores que a trilha** (ver §10.6).

### 7.1 Cores

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da app (toda a tela) |
| `--color-surface` | Fundo dos cards (streak, pergunta, versículos, opcional aba compartilhado), header, bottom-nav |
| `--color-divider` | Borda 1px dos cards `flat`, borda superior do bottom-nav |
| `--color-text-display` | h1 do tema da semana, pergunta de discussão, número do streak |
| `--color-text-body` | Microcopy de cards, label "dias" do streak, label do botão `secondary` |
| `--color-text-muted` | Overlines, microcopy descritiva ("Esta semana: 2 de 7 sessões."), referência+data dos versículos, label dos dias do heatmap |
| `--color-primary-300` | **Único uso da família coral nesta tela.** Preenchimento e borda das células do heatmap em estado "feito" |
| `--color-neutral-300` | Borda das células do heatmap em estado "não-feito"; borda do botão `secondary` |
| `--color-focus-ring` | Anel de foco em `:focus-visible` de qualquer elemento clicável (header, botão, link, itens do bottom-nav) |

**Tokens explicitamente VETADOS nesta tela** (e por que):

- `--color-danger-100`, `--color-danger-500`, `--color-danger-700` — vetados em qualquer dado de atividade. Permitidos apenas em mensagem de erro de carregamento do painel (ex.: rede caiu).
- `--color-warning-100/500/700` — idem. Não usar para "expira em 2 dias", "atenção", etc.
- `--color-success-100/500/700` — vetados como background de heatmap, streak ou contagem. Permitidos só como toast de confirmação ("Marcado como conversado") após ação concreta.
- `--color-heatmap-1`, `--color-heatmap-2`, `--color-heatmap-3` — vetados aqui. São para o heatmap anual da adolescente (US-041), não para o painel parental binário.
- `--color-primary-500` (coral pleno) — vetado neste painel. Coral pleno é CTA primário; aqui não há CTA primário (P9 — nenhuma ação merece prioridade visual de "agora!"). O botão de conversa é `secondary`.

### 7.2 Tipografia

| Token | Onde aparece |
|---|---|
| `--font-display` | h1 do tema da semana, número grande do streak |
| `--font-sans` | Todo o resto |
| `--font-weight-regular` (400) | Microcopy descritiva, texto integral da reflexão (na aba opt-in) |
| `--font-weight-medium` (500) | Labels do bottom-nav (inativo) |
| `--font-weight-semibold` (600) | Overlines, pergunta familiar, referência do versículo, h1, label do botão `secondary`, label "Painel" ativo do bottom-nav |
| `--font-weight-bold` (700) | Número do streak (display-3xl) |
| `--text-xs` | Overlines ("ESTA SEMANA", "SEQUÊNCIA ATUAL", etc.), labels dos dias do heatmap, labels do bottom-nav |
| `--text-sm` | Subtítulo do tema, microcopy descritiva ("Esta semana: 2 de 7"), label "dias" do streak, data dos versículos, header "Tela X de 3" do onboarding |
| `--text-base` | Pergunta de discussão familiar, referência do versículo (ex.: "Salmos 23.1"), texto do botão `secondary`, corpo das telas de onboarding, texto integral da reflexão na aba opt-in |
| `--text-lg` | (reservado — se quisermos elevar a pergunta familiar) |
| `--text-2xl` | h1 do tema da semana, h1 das telas de onboarding |
| `--text-3xl` | Número grande do streak |
| `--tracking-overline` | Todos os overlines |
| `font-variant-numeric: tabular-nums` | Número do streak (não pulsa ao incrementar) |

### 7.3 Espaçamento, raios, sombras, motion

| Aspecto | Token |
|---|---|
| Padding lateral da tela | `--space-4` |
| Padding interno dos cards | `--space-5` |
| Gap entre cards consecutivos | `--space-6` |
| Gap entre seção do tema e início dos cards | `--space-10` |
| Gap entre último card e link "Como funciona" | `--space-8` |
| Touch target mínimo | `--space-11` (44px) — botão `secondary`, link `Como funciona`, itens do bottom-nav, ícone-botão "Ajustes" |
| Touch target confortável (CTA lg do onboarding) | `--space-12` (48px) |
| Raio dos cards | `--radius-lg` (12px) |
| Raio do botão `secondary` | `--radius-full` (pill) |
| Raio das células do heatmap | `--radius-sm` (4px) |
| Sombra do header quando `scrollY > 8` | `--shadow-sm` |
| Sombra do modal de onboarding | `--shadow-lg` |
| Anel de foco | `--shadow-focus` |
| Duração de toggle do botão pós-clique | `--duration-fast` (120ms) |
| Easing | `--ease-out-soft` |
| Z-index do header e bottom-nav | `--z-sticky` |
| Z-index do modal de onboarding | `--z-modal` (com `--z-overlay` no backdrop) |

---

## 8. Microcopy completa — tabela com justificativa P8/P9/P11 por linha

**Todo texto em pt-BR.** Toda linha desta tabela passou pelo teste do "olhar de pai ansioso": se a frase soasse como cobrança disfarçada, foi reescrita.

### 8.1 Header e navegação

| Slot | Texto | Justificativa |
|---|---|---|
| Header — título | `Painel de <Apelido>` (mock: `Painel de Júlia`) | **P8**: usa o apelido configurado pela adolescente (US-047), nunca o nome legal completo. **P9**: "Painel de" e não "Acompanhe X" — descritivo, não imperativo. |
| Header — botão de ajustes | `aria-label="Ajustes"` | Ícone-botão sem texto visível precisa de label acessível. Texto neutro. |
| Bottom-nav — item ativo | `Painel` | Sem termo de comando ("Acompanhar", "Monitorar"). |
| Bottom-nav — item perfil | `Perfil` | — |
| Bottom-nav — item sair | `Sair` | — |
| Link rodapé | `Como funciona →` | Convite calmo a entender o app, não a "obter mais dados". **P9.** |

### 8.2 Card do tema da semana

| Slot | Texto | Justificativa |
|---|---|---|
| Overline | `TEMA DA SEMANA` | Descritivo. |
| h1 | `<<Tema da semana>>` (placeholder — virá do conteúdo) | — |
| Subtítulo | `<<Meta da semana em uma frase curta.>>` (placeholder) | — |

### 8.3 Card "Esta semana" (heatmap)

| Slot | Texto | Justificativa |
|---|---|---|
| Overline | `ESTA SEMANA` | Descritivo. |
| Labels dos dias | `D S T Q Q S S` (1 letra cada — domingo a sábado) | Mínimo visual; aria-label de cada célula tem o nome completo. |
| Microcopy descritiva | `Esta semana: <X> de 7 sessões.` (mock: `Esta semana: 2 de 7 sessões.`) | **P9**: descritivo, sem juízo. Não "Apenas 2!", não "Faltam 5". **P11**: ausência de pressão temporal. |

### 8.4 Card "Sequência atual" (streak)

| Slot | Texto | Justificativa |
|---|---|---|
| Overline | `SEQUÊNCIA ATUAL` | Não "Streak" (estrangeirismo de gamificação adulta); não "Dias seguidos" (com gancho de "ela quebrou"). |
| Número | `0` (mock); pode ser `7`, `21`, etc. | **P11**: zero não é tratado como falha. |
| Label abaixo do número | `dias` | Caixa baixa; sem ponto final; descritivo. |

**Vetados explicitamente neste card** (e por quê, P11):

- ❌ `Não perca a sequência!` — FOMO direto.
- ❌ `🔥 Em chamas!` — emoji + tom moralista.
- ❌ `Sua filha está há X dias sem entrar.` — cobrança.
- ❌ `Recorde da Júlia: X dias` — competição contra si mesma exibida ao pai vira pressão.

### 8.5 Card "Para conversar em família" (pergunta + botão)

| Slot | Texto | Justificativa |
|---|---|---|
| Overline | `PARA CONVERSAR EM FAMÍLIA` | **P9**: convite, não dever. |
| Pergunta | `<<Pergunta de discussão da semana.>>` (placeholder; mock: `<<Pergunta de discussão familiar da semana.>>`) | Vem do YAML do conteúdo (US-044). **P8**: não revela dados privados da filha — é genérica do tema (US-044 critério 3). |
| Botão (default) | `Conversamos sobre isso` | Sem emoji, sem "✓", sem "marcar". **P9**: linguagem natural de família. |
| Botão (após clique) | `Conversamos em DD/MM` (mock pós-clique: `Conversamos em 19/05`) | Estado retentivo; informativo. |
| Toast de confirmação (US-045) | `Marcado como conversado.` | `aria-live="polite"`. Variant `success` — único uso permitido de `--color-success-*` nesta tela. |

**Vetados:** `Não esqueça de marcar!`, `Você ainda não conversou esta semana`, `Sua filha não viu nada disso, pode falar à vontade` (este último, embora verdadeiro, infantiliza o pai e expõe o contrato — mantemos no onboarding).

### 8.6 Card "Versículos memorizados deste trimestre"

| Slot | Texto | Justificativa |
|---|---|---|
| Overline | `VERSÍCULOS MEMORIZADOS DESTE TRIMESTRE` | Permanência (trimestre, não semana) **P9**. |
| Linha por versículo — referência | `<Livro> <Cap>.<Versículo>` (mock: `Salmos 23.1`) | ADR-008: usamos referências da ARC. Sem citar o texto do versículo aqui (sinal de "feito", não de "leia"). |
| Linha por versículo — data | `memorizado em <DD/MM>` (mock: `memorizado em 12/05`) | **P9**: data acompanha como contexto, não como pressão. |
| Estado vazio | `Os versículos memorizados aparecem aqui ao longo do trimestre.` | **P11**: sem cobrança ("Incentive-a!"). |

### 8.7 Aba "Compartilhado por <Apelido>" (condicional)

| Slot | Texto | Justificativa |
|---|---|---|
| Título da seção | `Compartilhado por <Apelido>` (ex.: `Compartilhado por Júlia`) | **P8 + ADR-005**: deixa explícito que é opt-in da filha; não é "Reflexões da Júlia" (sugere acesso pleno). |
| Por reflexão — data | `<Dia da semana>, <DD/MM>` (ex.: `Sessão de quarta, 12/05`) | Descritivo. |
| Por reflexão — tema | `<<Tema da semana de quando ela escreveu.>>` | Contexto. |
| Por reflexão — texto | `<<Texto integral.>>` | **P8**: integral, sem truncamento. |
| Estado quando ausente | (a seção não renderiza) | **P8 + US-046 critério 1**: ausência total > "Nenhuma reflexão compartilhada ainda" (induziria pai a esperar/cobrar). |

### 8.8 Onboarding (3 telas) — frases-chave

| Slot | Texto | Justificativa |
|---|---|---|
| Indicador de progresso | `TELA X DE 3` | Transparência sobre tamanho do tutorial. |
| Tela 1 — h1 | `O que você verá aqui` | Direto. |
| Tela 1 — fechamento | `Tudo aqui é resumo. Nada substitui conversar com ela.` | **P9**: o painel é meio, não fim. |
| Tela 2 — h1 | `O que você não verá` | Honesto sobre limitação intencional. |
| Tela 2 — fechamento | `Isso é por escolha. O painel respeita o espaço dela para que ela use o app com liberdade.` | **P8**: explica por quê. |
| Tela 3 — h1 | `Como conversar no sábado` | Convite à conversa, não ao monitoramento. |
| Tela 3 — corpo (1) | `A cada semana, uma pergunta de discussão aparece no painel. É uma sugestão de assunto para vocês conversarem juntos no sábado — sobre o tema da semana, não sobre o uso do app.` | **P9**: explicita que a conversa é sobre tema, não sobre app. |
| Tela 3 — corpo (2) | `Depois que vocês conversarem, você pode tocar em "Conversamos sobre isso" para deixar registrado. A Júlia não vê essa marcação no app dela.` | **P8 + US-045 critério 4**: ensina o contrato. |
| CTAs | `Continuar →` (telas 1 e 2) · `Entrar no painel` (tela 3) | Direto. |
| Ações secundárias na tela 3 | `Voltar` (ghost) · `Pular tutorial` (ghost) | Pular só disponível na última tela (US-042 critério 1). |

### 8.9 Erros operacionais (não-vigilância)

Esses são os **únicos** lugares onde tokens de estado (`danger`, `warning`) são permitidos no painel — porque referem-se ao **app**, não à filha.

| Slot | Texto | Justificativa |
|---|---|---|
| Falha de carregamento do painel | `Não conseguimos carregar o painel agora. Toque para tentar de novo.` + botão `ghost` "Tentar de novo" | Erro técnico, sem culpabilizar usuário. |
| Falha ao marcar "Conversamos" | Toast `error`: `Não conseguimos salvar agora. Tente em alguns segundos.` | Sem cobrança. |

### 8.10 Lista resumida do que é VETADO em qualquer lugar do painel

(P8, P9, P11 — síntese para o Codificador e Revisor verificarem rapidamente.)

- ❌ `Não perca`, `Última chance`, `Hoje é o último dia` — qualquer urgência manufaturada.
- ❌ `Sua filha não fez X`, `Já faz Y dias que ela não entra`, `Esta semana foi fraca` — cobrança.
- ❌ `% de acerto`, `tempo médio`, `melhor horário` — dados quantitativos íntimos.
- ❌ `Compare com a média` — competição.
- ❌ `Sua filha está atrasada em relação ao trimestre` — moralismo de progresso.
- ❌ `🔥`, `🚀`, `🏆`, `💪`, `⚠`, `❌` — emoji como mecânica visual.
- ❌ `Notifique-a`, `Mande uma mensagem para ela` — pai não vira braço operacional do app.
- ❌ Qualquer ícone vermelho/amarelo de "alerta" sobre atividade.

E o que é OK (síntese):

- ✅ `Esta semana: <X> de 7 sessões.` — fato descritivo.
- ✅ `Sequência atual: <N> dias` — neutro.
- ✅ `Para conversar em família` — convite, sem prazo.
- ✅ `Memorizado em DD/MM` — registro temporal calmo.
- ✅ `Conversamos em DD/MM` — após ação consciente.

---

## 9. Acessibilidade

### 9.1 Estrutura semântica

- `<header role="banner">` para o topo (papel implícito por `<header>` no nível raiz).
- `<main>` envolve cabeçalho do tema + cards do painel + (condicionalmente) aba opt-in + link "Como funciona".
- Cada seção do painel é uma `<section aria-labelledby="...">` com o overline servindo de heading visualmente (na verdade um `<h2>` com `sr-only` quando precisa de hierarquia; o overline é estilizado por CSS, mas semanticamente é heading da seção).
- Heatmap: `<div role="img" aria-label="Heatmap binário da semana: 2 dias com sessão (quarta, quinta); 5 dias sem sessão (domingo, segunda, terça, sexta, sábado).">` — leitura completa em uma label só; **as 7 células internas usam `aria-hidden="true"`** para não duplicar leitura. **Redundância de canal**: cada célula tem **tanto cor** (preenchida ou contornada) **quanto forma** (preenchimento sólido ≠ contorno) — daltônicos diferenciam pelo padrão de preenchimento.
- Card de versículos: lista `<ul>` semântica.
- Aba opt-in (quando presente): `<section aria-labelledby="reflexoes-compartilhadas-titulo">` — não é uma `<aside>` (não é colateral; é conteúdo principal opt-in).
- `<nav aria-label="Principal">` para o bottom-nav.
- Onboarding: `<dialog role="dialog" aria-modal="true" aria-labelledby="onboarding-titulo">` com `inert` no `<main>` enquanto aberto; focus trap via Alpine `x-trap.inert.noscroll`.

### 9.2 Labels e descrições

| Elemento | Padrão de label |
|---|---|
| Header — saudação | Visível ("Painel de Júlia") — não precisa de `aria-label` extra. |
| Header — botão Ajustes | `aria-label="Ajustes"`. |
| Heatmap (container) | `aria-label="Heatmap binário da semana: <N> de 7 dias com sessão. Dias com sessão: <lista>. Dias sem sessão: <lista>."` |
| Heatmap (células individuais) | `aria-hidden="true"` (a leitura inteira fica no container). |
| Botão "Conversamos sobre isso" (default) | Texto visível serve; sem `aria-label` extra. `aria-pressed="false"`. |
| Botão "Conversamos sobre isso" (após clique) | Texto visível atualizado para "Conversamos em DD/MM". `aria-pressed="true"`. `disabled` aplicado (US-045 critério 2 — desabilitado para nova marcação naquela semana). |
| Itens do bottom-nav | Texto visível. Item ativo: `aria-current="page"`. |
| Link "Como funciona" | Texto visível + `aria-label="Como funciona o painel — revisitar tutorial"` para esclarecer destino. |
| Onboarding — botão "Continuar" | Texto visível. |
| Onboarding — indicador "TELA X DE 3" | Visível + dots com `aria-hidden="true"` (a contagem textual já comunica). |

### 9.3 Foco, ordem de tabulação e teclado

- **Ordem de Tab no DOM** (sem `tabindex` positivo):
  1. Header — saudação ("Painel de Júlia") como `<a href="/painel">` (ou apenas heading se o painel for raiz); ícone Ajustes.
  2. (Painel pula direto para o `<main>` — sem skip-link explícito nesta task; entra na chore-ux-007 igual à trilha.)
  3. Heatmap (container focável com `tabindex="0"` para receber leitura da label).
  4. Botão "Conversamos sobre isso" (ou estado pós-clique).
  5. Link "Como funciona".
  6. Itens do bottom-nav (Painel → Perfil → Sair).
- **Foco visível:** todos os elementos clicáveis ganham anel `--shadow-focus` em `:focus-visible`.
- **Onboarding (modal):**
  - **Focus trap obrigatório** via Alpine `x-trap.inert.noscroll`.
  - Foco inicial vai para o título da tela atual (`<h1 tabindex="-1">`) — leitor de tela anuncia o título; teclado começa no CTA.
  - Tab/Shift+Tab circulam apenas entre elementos do modal.
  - `Esc` **não fecha** o onboarding bloqueante (US-042 critério 1: só pula após chegar à tela 3). Decisão registrada em §10.7 — esta é uma exceção ao padrão "Esc fecha modal" do componente Modal (ux-003 §4.7). É **modal `critical`** + `Esc` desligado.
  - Foco restaurado ao link "Como funciona" quando o tutorial é fechado via "Entrar no painel" ou "Pular tutorial" (na tela 3).

### 9.4 Contraste (WCAG AA mínimo — chore-ux-001)

Todas as combinações já estão validadas em `doc/UX/00-identidade-visual.md` §2:

| Combinação | Razão | Nível |
|---|---|---|
| Título do tema (`neutral-900`) sobre `surface` (branco) | 16.4:1 | AAA |
| Microcopy descritiva (`neutral-500`) sobre `surface` | 4.62:1 | AA |
| Botão `secondary` — texto (`neutral-700`) sobre `surface` | 10.8:1 | AAA |
| Heatmap — célula preenchida `primary-300` sobre `surface` | (elemento gráfico ≥ 24px, não-texto — AA Large para área visual) | AA Large |
| Heatmap — borda 1px de célula vazia (`neutral-300`) sobre `surface` | (elemento gráfico não-texto) | — |
| Número do streak (`neutral-900`) sobre `surface` | 16.4:1 | AAA |
| Botão "Pular tutorial" (ghost, `neutral-500`) sobre `surface` | 4.62:1 | AA |

### 9.5 `prefers-reduced-motion`

- Transição do botão "Conversamos sobre isso" (mudança de label e cor): `--duration-fast` (120ms). A regra global zera para 0.01ms. Conforme.
- Animação de entrada do onboarding (fade do modal): `--duration-slow` (400ms). A regra global zera. Conforme.
- **Sem animações decorativas no painel** — nada pulsa, nada "respira", nada acende. Decisão intencional (P11): movimento gratuito em dashboard transmite urgência.

---

## 10. Decisões e alternativas descartadas

### 10.1 Heatmap binário (escolhido) vs. heatmap gradiente / 3 tons (rejeitado)

Considerei abrir com o mesmo padrão de 3 tons crescentes do heatmap anual (US-041, RF-E6-10 — `--color-heatmap-1/2/3`). Rejeitei porque (a) **gradiente induz interpretação de "intensidade"** — Carlos olha o dia "claro" e pergunta "minha filha fez só meia sessão?" ou "ela fez sessão rápida?"; abre porta para vigilância de qualidade que o painel não pretende medir; (b) US-043 critério 1 é literal: "escala binária (preencheu/não preencheu) por dia, **sem intensidade nem horário**" — o critério é o contrato, não uma sugestão; (c) o heatmap anual da US-041 é da **adolescente** (visão dela, contexto motivacional próprio); aplicar o mesmo padrão ao pai mistura contextos.

**Quando reabriríamos:** nunca dentro do painel parental. Se a US-043 evoluir (v2), heatmap anual da filha pode aparecer como link "ver mais", levando à visão dela — não copiando o padrão dentro do painel pai.

### 10.2 Sem painel de "alerta de baixa atividade" (escolhido) vs. card "Atenção: sua filha não fez sessões há X dias" (rejeitado)

Apps de monitoramento parental tradicionais (Qustodio, Bark, FamilyTime) tipicamente incluem um card no topo do dashboard com **alertas**: "Sua filha não usou o app em 3 dias", "Atividade abaixo da média", etc. Rejeitei explicitamente por **três razões irreconciliáveis com P9**:

1. **Alerta sobre ausência é exatamente o que US-043 critério 5 veta:** "O painel NÃO exibe (...) marcadores específicos de dias sem atividade como 'alerta'." O critério é literal.
2. **Cria a cobrança que o produto promete evitar.** Carlos veria o alerta e mandaria mensagem para a filha cobrando "por que você não fez?". O painel vira ferramenta operacional de vigilância.
3. **Dark pattern: ansiedade do pai vira métrica de retenção do app.** "Como você vai garantir que sua filha use?" é o anti-pitch do atrilha. P11 explícito.

**Onde a "ausência" entra:** ela não entra. O heatmap mostra os dias sem preenchimento como qualquer outro estado — sem ênfase, sem cor de alarme, sem texto adicional. Carlos pode notar; o app não aponta.

**Quando reabriríamos:** nunca dentro do painel parental enquanto P9 estiver no PRD.

### 10.3 Streak sem chama, sem ícone, sem microcopy adicional (escolhido) vs. streak com chama + texto motivacional (rejeitado)

Considerei usar o ícone de chama 🔥 (clichê de gamificação Duolingo) ou um SVG stroke de chama + microcopy "Continue assim!" / "Recomeçar é uma opção!". Rejeitei porque (a) chama é **clichê de gamificação adulta competitiva** — fora do tom do atrilha (P15 + chore-ux-001 §6); (b) **microcopy motivacional sobre streak zero confessa que zero é problema** — o painel não trata zero como problema, é só um número; (c) **streak já é um clichê pesado em si** — usar com chama duplica a redução semântica.

A escolha foi a mais radical possível: número grande, palavra "dias", nada mais. Zero é exatamente como 7 seria, só com dígito diferente.

**Quando reabriríamos:** se teste de usabilidade real mostrar que Carlos não entende o card sem contexto. Mitigação leve possível: trocar overline para "DIAS SEGUIDOS COM SESSÃO" (descritivo); mantendo zero ícone.

### 10.4 Sem ilustração editorial placeholder (escolhido) — coerência com chore-ux-004 §9.4

A US-043 critério 3 prevê **ilustração de cabeçalho do tema da semana**. Esta spec **não** materializa a ilustração no protótipo HTML porque (a) o estilo concreto é dúvida aberta (chore-ux-001 §7); (b) qualquer placeholder genérico violaria P15 (sem foto stock, sem ícone genérico decorativo). Marcamos o espaço com overline + h1 + subtítulo e deixamos comentário explícito no HTML. **Mesma decisão da chore-ux-004 §9.4** — coerência entre protótipos.

### 10.5 Cabeçalho do tema como bloco tipográfico (escolhido) vs. envolvido em card (rejeitado)

Considerei envolver o título da semana + subtítulo em um Card `flat` (mesmo padrão dos demais blocos). Rejeitei porque (a) o cabeçalho é **identificação da página**, não um dado adicional; (b) cartões de identificação no topo competem com o h1 visual; (c) é o **mesmo padrão da trilha** (chore-ux-004 §2) — overline + h1 + subtítulo soltos no topo do `<main>`. Coerência entre telas.

### 10.6 Painel usa menos cores que a trilha (escolhido)

A trilha (chore-ux-004 §6.1) usa: coral (3 variações), lime (2 variações), success bg/text, info bg/text, neutros completos. **20+ tokens de cor em uso.**

O painel deliberadamente usa **muito menos**: `primary-300` em uma única posição (heatmap), neutros, foco. **9 tokens de cor em uso.**

Por que: a trilha é cheia de estado (concluído, em progresso, hoje, bloqueado, sábado), cada um precisa de afirmação visual. O painel é resumo calmo — toda variação cromática extra introduz ruído semântico ("o que essa cor diferente significa?"). Coral escasso, lime ausente, neutros ricos.

### 10.7 Onboarding sem `Esc` para fechar (escolhido) — exceção ao Modal padrão

O componente Modal (ux-003 §4.7) afirma: "Fecha com Esc (sempre, inclusive em critical — Esc é canal universal de 'cancelar')". O onboarding desta US **viola essa regra**: `Esc` não fecha enquanto Carlos não chegar à tela 3 (US-042 critério 1). Justificativa: o onboarding é **bloqueante por design contratual** — não é uma janela informativa que Carlos optou por abrir; é a porta de entrada do painel definida em ADR-003. Permitir `Esc` quebraria o critério 5 da US-042 ("Carlos só chega ao dashboard depois de avançar todas as 3 telas").

Decisão registrada como exceção. A US-042 deve adotar essa exceção explicitamente; a chore-ux-007 (auditoria WCAG) deve verificar se a violação compromete WCAG 2.1.1 (Keyboard) — entendimento atual: não compromete, porque "Pular tutorial" na tela 3 é alternativa de teclado para sair sem usar `Esc`.

### 10.8 Aba opt-in como seção vertical (escolhido) vs. abas no topo da página (rejeitado)

Considerei abas reais no topo: `[ Painel ] [ Compartilhado por Júlia ]`. Rejeitei porque (a) quando não há item opt-in, uma das abas fica vazia — quebra estética e introduz "tem algo aqui esperando", o que sugere ao pai cobrar a filha por compartilhar; (b) abas competem semanticamente — Carlos pode evitar a aba principal por gostar mais do "feed"; (c) abas como padrão de UI viraram associadas a redes sociais — aplicar no painel da filha contrabandeia a estética de timeline. **Seção vertical condicional** evita os três problemas: quando ausente, simplesmente não está lá; quando presente, fica clara como **algo separado** dos sinais quantitativos (US-046 critério 4).

---

## 11. Pendências e ganchos para tasks futuras

- **Texto do tema da semana, subtítulo, pergunta de discussão familiar** — virão de `doc/conteudo/fluxo-semana.md`, em refinamento. Os placeholders deste doc e do HTML serão substituídos nas US-018 (tema) e US-044 (pergunta).
- **Ilustração de cabeçalho do tema** — depende do estilo editorial (chore-ux-001 §7, dúvida aberta). Mesmo pendente que a trilha (chore-ux-004 §9.4).
- **Apelido da adolescente exibido em "Painel de <Apelido>" e "Compartilhado por <Apelido>"** — vem da configuração da Júlia (US-047, e provavelmente uma US de "perfil" anterior). Mock: `Júlia`.
- **Skip-link "Pular para conteúdo"** — entra na chore-ux-007 (auditoria WCAG completa). Não aparece neste protótipo.
- **Smoke visual end-to-end** — chore-ux-008 consumirá este HTML como uma das telas-âncora.
- **Verificação de tom em revisão humana** — recomenda-se que o Revisor (e o humano Dioni) abram o HTML em mock de baixa atividade e perguntem deliberadamente: "isso me dá vontade de cobrar?". É o teste subjetivo final desta task.
- **Heatmap binário como fragment dedicado (futuro)** — registrado em §6.2; decisão fica para a US-043 do Codificador.
- **Implementação do contrato P8/P9/P11 no Codificador** — a US-043 deve garantir que erros operacionais não vazem para tons de "alerta" sobre dados de atividade. A chore-ux-007 deve incluir esse contrato como item da auditoria.

---

## 12. Histórico de aprovação

| Data       | Item                                | Decisão                |
|------------|-------------------------------------|------------------------|
| 2026-05-19 | Estrutura do painel + onboarding    | Proposto pelo Designer |
| —          | Aprovação pelo humano (Dioni)       | Pendente               |
