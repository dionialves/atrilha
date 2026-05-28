# US-010 — Edição do perfil do responsável · UX Spec

**Código:** US-010
**GitHub Issue:** —
**Status:** Proposto (aguardando issue de implementação)
**Depende de:** US-002/US-003 (cadastro do responsável) · US-009 (perfil do adolescente — irmã desta) · US-012/US-013 (vinculação) · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/06-acessibilidade.md`
**Referências PRD:** §11.5 (LGPD — direito de correção) · §13 (US-010) · §17 (DoD)
**Protótipo:** [`doc/UX/prototypes/perfil-responsavel-editar.html`](prototypes/perfil-responsavel-editar.html)
**US irmã:** [`us-009-spec.md`](us-009-spec.md) — perfil do adolescente; muito do padrão visual desta tela vem de lá
**Escopo deste doc:** **layout** da tela única `/perfil` para o responsável (Carlos). Cobre wireframe, tokens visuais, microcopy literal, estados, comportamento de edição com dirty state, card read-only de vinculações ativas. **Não cobre** controller, persistência, edição de dados da adolescente (proibido por P9), pipeline de e-mail. Microcopy em pt-BR. Nenhum elemento depende só de cor.

---

## 1. Princípios

1. **Direito de correção, não vigilância.** Carlos edita **só os próprios dados**: nome. E-mail aparece read-only por ser credencial de login. Nada nesta tela permite alterar dados de Júlia — princípio P9 (não-vigilância). O card "Você é responsável por" mostra **apenas o apelido** das adolescentes vinculadas, sem foto, sem progresso, sem CTA.
2. **E-mail é credencial — aparece, mas não se edita.** Mesma regra da US-009 (decisão Dioni, 2026-05-27). O CA-2 da US-010 ("trocar e-mail dispara verificação") fica **fora do MVP**. Em vez disso, e-mail é `readonly` com cadeado e helper.
3. **Vinculação é sagrada.** CA-3 diz que nenhuma edição quebra a vinculação. O card read-only "Você é responsável por" reforça isso visualmente: Carlos vê concretamente que continua responsável pelas mesmas adolescentes. Banner de sucesso após save explicita: "Tua vinculação continua igual."
4. **Tela enxuta — 1 campo editável.** US-010 pede só nome (e-mail virou read-only). Significa que a tela é menor que a US-009. A simplicidade é a feature: Carlos abre o perfil, ajusta o nome, salva, volta.
5. **Tom adulto, mas a mesma marca.** Carlos é adulto (25+). A copy mantém o "tu/você" da marca, mas sem gírias adolescentes — helper diz "Pode ser teu nome completo ou só o primeiro." em vez de "Pode ser qualquer coisa".
6. **Salvar só quando há o que salvar.** Save bar sticky aparece quando há mudança. Idle = perfil resumido, sem botões competindo por atenção.

---

## 2. Tela única — `/perfil` (para `role=RESPONSIBLE`)

**Rota:** `GET /perfil` → view `perfil/responsavel-editar`
**Acesso:** autenticado como `role=RESPONSIBLE`. Adolescente (`role=ADOLESCENT`) acessando `/perfil` cai na US-009 — o controller decide pelo role na sessão.
**Entrada:** vem do menu/avatar de Carlos em `/painel-pais`. O header tem botão voltar apontando para `/painel-pais`.

### 2.1 Wireframe (mobile 320px — desktop usa o mesmo card centralizado, `max-width: 28rem`)

```
┌────────────────────────────────────┐
│ [<]    Meu perfil          [    ] │  ← header com voltar
├────────────────────────────────────┤
│                                    │
│   [banner success — após save]     │
│                                    │
│           ┌────────┐               │
│           │   C    │               │  ← avatar 96×96 só com inicial
│           └────────┘               │     (sem badge — não tem foto editável)
│         Carlos Oliveira            │
│           RESPONSÁVEL              │  ← overline indicando papel
│                                    │
│   label · Como você quer ser…      │
│   [__________________________]     │
│   helper · Pode ser teu nome…      │
│                                    │
│   label · 🔒 E-mail                │
│   [__readonly________]         🔒  │
│   helper · É o que você usa…       │
│                                    │
│   ┌──────────────────────────────┐ │
│   │ ✓ Você é responsável por      │ │  ← card read-only
│   │   1 adolescente vinculada     │ │
│   │   ┌──────────────────────┐    │ │
│   │   │ [D] Dioni             │    │ │
│   │   │     Vinculada em maio │    │ │
│   │   └──────────────────────┘    │ │
│   │ ⓘ Tu só vê quem está          │ │
│   │   vinculada — não dá pra…     │ │
│   └──────────────────────────────┘ │
│                                    │
│   ─── espaço com gradiente ───     │
│   [    Salvar alterações    ]      │  ← sticky save bar (só se dirty)
│   [        Descartar        ]      │
│                                    │
└────────────────────────────────────┘
```

### 2.2 Estrutura HTML (referência)

```html
<header>  [voltar] · Meu perfil · [spacer]  </header>

<main class="main">
  <!-- banner success (transient ~2.5s) -->
  <div role="status" class="alert alert--success">
    Pronto, atualizamos. Tua vinculação continua igual.
  </div>

  <!-- Identidade -->
  <section class="avatar-block">
    <div class="avatar"><span>C</span></div>
    <p class="avatar-name">Carlos Oliveira</p>
    <p class="avatar-role">Responsável</p>
  </section>

  <form method="post" action="/perfil">
    [csrf]

    <div class="input-group">
      <label for="name">Como você quer ser chamado</label>
      <input id="name" name="name" type="text" minlength="2" maxlength="80" />
      <p class="input-helper">Pode ser teu nome completo ou só o primeiro. De 2 a 80 caracteres.</p>
    </div>

    <div class="input-group">
      <label for="email">🔒 E-mail</label>
      <div class="readonly-wrap">
        <input id="email" type="email" readonly value="{{email}}" />
        <span class="readonly-icon">🔒</span>
      </div>
      <p class="input-helper">É o que você usa pra entrar. Por isso a gente não troca ele aqui.</p>
    </div>

    <!-- Card vinculações ativas (read-only — CA-3) -->
    <section class="vinc-card" aria-labelledby="vinc-title">
      <div class="vinc-header">
        <div class="vinc-icon">✓</div>
        <div>
          <h2 id="vinc-title">Você é responsável por</h2>
          <p>1 adolescente vinculada</p>
        </div>
      </div>
      <ul class="vinc-list">
        <li class="vinc-item">
          <div class="vinc-item-avatar">D</div>
          <div>
            <span class="vinc-item-name">Dioni</span>
            <span class="vinc-item-meta">Vinculada em maio de 2026</span>
          </div>
        </li>
      </ul>
      <p class="vinc-footnote">
        ⓘ Tu só vê quem está vinculada — não dá pra editar dados delas por aqui.
        Cada uma cuida do próprio perfil.
      </p>
    </section>

    <!-- save bar — só aparece quando dirty -->
    <div class="save-bar" hidden>
      <button type="submit" class="btn btn-primary">Salvar alterações</button>
      <button type="button" class="btn btn-ghost">Descartar</button>
    </div>
  </form>
</main>
```

---

## 3. Especificações visuais

### 3.1 Layout do container

Idêntico à US-009:

| Elemento | Token / valor |
|---|---|
| Fundo da página | `--color-bg` |
| `<main>` padding | `--space-8` topo · `--space-4` laterais · `--space-12` baixo |
| `max-width` | `28rem` |
| Centralização | `margin: 0 auto` |

### 3.2 Header

Idêntico à US-009 (mesmo grid 3 colunas com voltar + título + spacer). Diferenças:

| Propriedade | Valor |
|---|---|
| Destino do voltar | `/painel-pais` (em US-009 era `/trilha`) |
| Título | `"Meu perfil"` (idêntico) |

### 3.3 Avatar block

Mais simples que o da US-009 — **sem badge de edição de foto** (US-010 não pede foto). Apenas a inicial do nome com tipografia display.

| Propriedade | Valor |
|---|---|
| Avatar (`.avatar`) | `96 × 96px` (menor que os 112px da US-009 — sinaliza papel diferente, mantém respiro) |
| Raio | `--radius-full` |
| Fundo | `--color-primary-100` |
| Inicial | `--font-display` · `--text-2xl` · `--font-weight-semibold` · `--color-primary-700` · `text-transform: uppercase` · centro do círculo |
| Sombra | `--shadow-sm` |
| Conteúdo dinâmico | `(name || '?').charAt(0).toUpperCase()` |

**Apelido (`.avatar-name`):**

| Propriedade | Valor |
|---|---|
| Tag | `<p>` |
| Estilo | `--font-display` · `--text-xl` · `--font-weight-semibold` · `--color-text-display` · `text-align: center` |
| Conteúdo | Espelha `x-model="name"` em tempo real |
| Fallback | `"Sem nome"` quando vazio durante edição |

**Overline de papel (`.avatar-role`):**

| Propriedade | Valor |
|---|---|
| Tag | `<p>` |
| Estilo | `--text-xs` · `--font-weight-semibold` · `text-transform: uppercase` · `--tracking-overline` (0.05em) · `--color-text-muted` |
| Conteúdo | `"Responsável"` |
| Por que existe | Em sessão multi-papel (Carlos pode no futuro ter outro tipo de conta), reforça contexto. Mesmo no MVP single-role, ajuda a distinguir desta tela da US-009 sem precisar comparar lado-a-lado. |

### 3.4 Form — campos

Padrão `.input-group` idêntico à US-009 (label · input · helper/error).

**3.4.1 Campo "Nome"**

| Propriedade | Valor |
|---|---|
| `id` / `name` | `name` |
| `type` | `text` |
| `minlength` / `maxlength` | `2` / `80` |
| Label | `"Como você quer ser chamado"` |
| Helper | `"Pode ser teu nome completo ou só o primeiro. De 2 a 80 caracteres."` |
| Erros (texto literal) | `< 2`: `"Precisa de pelo menos 2 caracteres."` · `> 80`: `"Máximo 80 caracteres."` |
| Reflexo | `.avatar-name` e `.avatar` (inicial) espelham em tempo real |

**Por que 2 caracteres mínimo (vs 3 da US-009):** Carlos é adulto. Nomes curtos como "Lu", "Tó", "Vi" são comuns como apelido familiar. A US-009 tinha 3 por ser apelido inventado em comunidade adolescente (ASD), onde algo único era esperado.

**Por que 80 caracteres máximo (vs 20 da US-009):** Carlos pode usar nome completo. "João da Silva Pereira Santos Oliveira" cabe em 38 caracteres; 80 dá folga pra nomes compostos longos. Júlia usa apelido curto, então 20 é suficiente.

**3.4.2 Campo "E-mail" (read-only)**

Idêntico ao da US-009 §3.4.3. Mesmo padrão visual (cadeado, fundo muted, helper).

| Propriedade | Valor |
|---|---|
| `type` | `email` |
| `readonly` | true |
| Label | `"E-mail"` + ícone cadeado |
| Helper | `"É o que você usa pra entrar. Por isso a gente não troca ele aqui."` |

### 3.5 Card "Vinculações ativas" (read-only)

Adição além do mínimo da US-010 — reforça CA-3 visualmente. **Não é editável** (princípio P9).

| Propriedade | Valor |
|---|---|
| Container (`.vinc-card`) | `margin-top: --space-8` · `background: --color-surface` · `border: 1px solid --color-divider` · `--radius-lg` · `padding: --space-5` · `--shadow-sm` |
| `aria-labelledby` | `"vinc-title"` |

**Header do card (`.vinc-header`):**

| Propriedade | Valor |
|---|---|
| Display | `flex; align-items: center; gap: --space-3; margin-bottom: --space-4` |
| Ícone (`.vinc-icon`) | `36×36`, `--radius-full`, `background: --color-success-100`, `color: --color-success-700`, SVG check-circle 18×18 |
| Título (`.vinc-title`) | `<h2>` · `--text-sm` · `--font-weight-semibold` · `--color-text-display` · texto: `"Você é responsável por"` (ou `"Sem vinculações ainda"` no estado vazio) |
| Subtítulo (`.vinc-subtitle`) | `<p>` · `--text-xs` · `--color-text-muted` · texto: `"1 adolescente vinculada"` ou `"N adolescentes vinculadas"` |

**Lista (`.vinc-list`):**

| Propriedade | Valor |
|---|---|
| Tag | `<ul>` sem bullets (`list-style: none; padding: 0; margin: 0`) |
| Display | `flex; flex-direction: column; gap: --space-2` |

**Item da lista (`.vinc-item`):**

| Propriedade | Valor |
|---|---|
| Display | `flex; align-items: center; gap: --space-3; padding: --space-3; --radius-md; background: --color-surface-muted` |
| Avatar do item | `36×36`, `--radius-full`, `--color-primary-100` / `--color-primary-700`, inicial do apelido em `--font-display --text-base --font-weight-semibold`, **sem foto** (P9 — não-vigilância visual) |
| Texto | Coluna com gap 2px · nome (`--text-sm --font-weight-semibold --color-text-display`) · meta (`--text-xs --color-text-muted`) |
| Cursor | Default (não-clicável) — é só visualização |
| Hover | Sem hover — não vira CTA |

**Conteúdo de cada item:**

| Slot | Conteúdo |
|---|---|
| Avatar | Inicial do apelido (P9 — sem foto) |
| Nome | `apelido` da adolescente (não nome completo; respeita como ela se identifica) |
| Meta | `"Vinculada em {mês} de {ano}"` — formato editorial, não data ISO. Não mostra hora nem dia. |

**Estado vazio (`.vinc-empty`):**

| Propriedade | Valor |
|---|---|
| Display | `padding: --space-4; text-align: center; --text-sm; --color-text-muted; background: --color-surface-muted; --radius-md` |
| Texto | `"Quando uma adolescente vincular você como responsável, ela aparece aqui."` |

**Footnote do card (`.vinc-footnote`):**

| Propriedade | Valor |
|---|---|
| Display | `margin-top: --space-3; --text-xs; --color-text-muted; flex; gap: --space-2; align-items: flex-start` |
| Ícone | Info-circle SVG 14×14, `flex-shrink: 0`, `margin-top: 2px` (alinhamento ótico com a 1ª linha) |
| Texto | `"Tu só vê quem está vinculada — não dá pra editar dados delas por aqui. Cada uma cuida do próprio perfil."` |
| Por que existe | Honestidade explícita sobre o princípio P9. Carlos pode imaginar que "perfil dele" inclui dados de Júlia — esta nota corrige a expectativa sem ser repreensiva. |

### 3.6 Banner de sucesso

Idêntico à US-009 §3.5, com **texto adaptado ao contexto de responsável**:

| Propriedade | Valor |
|---|---|
| Texto | `"Pronto, atualizamos. Tua vinculação continua igual."` (em US-009 era "Teu progresso continua igual.") |
| Por que mudou | Carlos não tem progresso a defender — ele tem a vinculação. O banner alivia o medo específico dele ("será que ao salvar eu perco a Júlia?"). |

### 3.7 Save bar

Idêntico à US-009 §3.6. Mesma estrutura, mesmos botões, mesmos estados.

---

## 4. Microcopy (pt-BR — literal)

| Slot | Texto |
|---|---|
| `<title>` | Meu perfil |
| Header título | Meu perfil |
| Botão voltar (`aria-label`) | Voltar |
| Avatar fallback (sem nome) | Sem nome |
| Overline de papel | Responsável |
| Label nome | Como você quer ser chamado |
| Helper nome | Pode ser teu nome completo ou só o primeiro. De 2 a 80 caracteres. |
| Erro nome (< 2) | Precisa de pelo menos 2 caracteres. |
| Erro nome (> 80) | Máximo 80 caracteres. |
| Label e-mail | E-mail |
| Helper e-mail | É o que você usa pra entrar. Por isso a gente não troca ele aqui. |
| Save bar — primário (default) | Salvar alterações |
| Save bar — primário (loading) | Salvando… |
| Save bar — secundário | Descartar |
| Banner success | Pronto, atualizamos. Tua vinculação continua igual. |
| Card vinc — título (com vínculos) | Você é responsável por |
| Card vinc — título (sem vínculos) | Sem vinculações ainda |
| Card vinc — subtítulo (1 adolescente) | 1 adolescente vinculada |
| Card vinc — subtítulo (N adolescentes) | {N} adolescentes vinculadas |
| Card vinc — meta de item | Vinculada em {mês} de {ano} |
| Card vinc — empty | Quando uma adolescente vincular você como responsável, ela aparece aqui. |
| Card vinc — footnote | Tu só vê quem está vinculada — não dá pra editar dados delas por aqui. Cada uma cuida do próprio perfil. |

**Vetado:**

- "Atualizar e-mail", "Trocar e-mail" — e-mail é imutável.
- "Editar adolescente", "Gerenciar vinculada", "Ver perfil dela" — viola P9.
- "Sua adolescente", "Sua filha" — possessivo + assumir parentesco. Usar "adolescente vinculada".
- "Pais/responsáveis legais", "Tutor", "Guardião" — termos jurídicos. Manter "responsável".
- "Removido da vinculação", "Desvincular" — esta tela não desvincula. Fluxo de desvinculação é outra US.
- Tons de adulto autoritário: "Você precisa", "Não é permitido", "Acesso negado". Manter colaborativo: "Não dá pra…", "A gente não troca aqui".

---

## 5. Estados visuais

| Estado | Trigger | UI |
|---|---|---|
| `idle` (default) | Tela carregada, sem mudanças | Avatar + form preenchidos; save bar invisível |
| `dirty` | Campo nome alterado | Save bar aparece; Salvar habilita se sem erros |
| `validating` (transient) | Durante `validate()` em `@input` | Borda vermelha + mensagem inline aparecem/somem |
| `saving` | POST de save em voo | Spinner no botão + "Salvando…"; Descartar desabilita |
| `saved` | Save retornou OK | Banner success ~2500ms; depois `idle` com baseline atualizada |
| `error` (futuro) | Save falhou | Banner `alert--error` no topo (proposta — não no protótipo) |

**Estados da seção de vinculações (independente do estado do form):**

| Cenário | UI |
|---|---|
| `single` | Card mostra título + subtítulo "1 adolescente vinculada" + lista com 1 item + footnote |
| `multiple` | Mesmo card; subtítulo "N adolescentes vinculadas" + lista com N items |
| `empty` | Card mostra título "Sem vinculações ainda" + empty state textual + footnote |

---

## 6. Comportamento

### 6.1 Dirty state

`markDirty()` chama em `@input` do nome. Detecção simples (1 campo editável):

```javascript
dirty = (name !== nameOriginal);
```

Save bar aparece se `dirty`. Salvar habilita se `dirty && !hasErrors()`.

### 6.2 Validação inline

`validate()` roda em cada `@input` e antes de `save()`:

| Campo | Regra | Mensagem |
|---|---|---|
| `name` | `< 2` caracteres (trim) | "Precisa de pelo menos 2 caracteres." |
| `name` | `> 80` caracteres | "Máximo 80 caracteres." |

### 6.3 Save flow

```javascript
save() {
  validate();
  if (hasErrors()) return;
  setState('saving'); // → 900ms → 'saved' → 2500ms → 'idle'
}
```

Sequência: `idle` → input → `dirty` → click Salvar → `saving` (~900ms) → `saved` (banner ~2500ms) → `idle` com baseline atualizada.

### 6.4 Card de vinculações

Read-only puro. Sem cliques, sem hover, sem expansão. Conteúdo vem do servidor no GET (renderização Thymeleaf). Em produção, o controller passa lista de `{ apelido, desde }` de cada vinculação ativa.

### 6.5 Reflexo no avatar

`name` mudou no input → `avatar` (inicial) e `avatar-name` (texto completo) atualizam em tempo real. Carlos consegue ver como vai ficar antes de salvar.

---

## 7. Acessibilidade

- `<header role="banner">`; `<main>` único.
- `<h1>` único na página (header). Nome e papel no avatar block são `<p>`. Card de vinculações usa `<h2>` para o título "Você é responsável por".
- Cada input com `<label for>` + `aria-describedby` apontando para helper OU error.
- Input com erro: `aria-invalid="true"` + mensagem `role="alert"`.
- E-mail read-only: focável, selecionável, `cursor: not-allowed`, helper associado.
- Card de vinculações: `aria-labelledby="vinc-title"`; lista usa `<ul>` semântico; footnote é `<p>` com ícone `aria-hidden="true"`.
- Save bar com sticky position: leitor de tela lê após o último input no DOM — ordem natural.
- Touch target: tudo ≥ 44×44 (`--space-11`).
- Contraste calculado:
  - Nome `text-display` sobre `surface` → **16.4:1** (AAA).
  - Overline `text-muted` sobre `surface` → **4.62:1** (AA).
  - Avatar letra `primary-700` sobre `primary-100` → **6.8:1** (AAA Large).
  - Item de vinculação: nome `text-display` sobre `surface-muted` → **15.0:1** (AAA).
  - Meta `text-muted` sobre `surface-muted` → **4.20:1** (AA limite — texto auxiliar).
  - Ícone success-700 sobre success-100 → ≥ **6.84:1** (AA).
- `prefers-reduced-motion: reduce`: spinner sem rotação; transições zeram.

---

## 8. Reusos e proibições

### 8.1 Reusos obrigatórios

| Item | Origem | Não recriar |
|---|---|---|
| Tokens de cor, espaço, raio, sombra | `doc/UX/01-design-tokens.md` | Nenhum hex novo. |
| Padrão `input-group` | `doc/UX/02-componentes-base.md` §2 | Idêntico ao da US-009. |
| `btn btn-primary`, `btn btn-ghost` | `doc/UX/02-componentes-base.md` §1 | Sem variantes inéditas. |
| `alert--success` | `doc/UX/02-componentes-base.md` §2.4 | Idêntico ao do verify-email, US-009. |
| Header com voltar | Padrão `login.html` / US-009 | Mesma estrutura (grid 3 colunas). |
| Avatar block (versão sem badge) | US-009 §3.3 sem o badge | Variante simplificada. |
| `input--readonly` com cadeado | US-009 §3.4.3 | Idêntico. |
| Save bar sticky com gradiente | US-009 §3.6 | Idêntico. |
| Foco visível, touch target 44×44 | `doc/UX/06-acessibilidade.md` | Sem exceções. |

### 8.2 Padrões novos desta US (registrar para chore-ux futura)

- **`vinc-card`** (read-only card com header + lista + footnote) — variante "callout informativo com lista". Aplicável em outras telas onde o usuário precisa **ver** dados relacionados sem poder editá-los.
- **`vinc-item`** (item de lista com avatar pequeno + nome + meta) — padrão para listar entidades vinculadas/relacionadas. Aplicável em listas de adolescentes vinculadas, responsáveis vinculados (ex.: tela do administrador), histórico de sessões.

### 8.3 Proibições

- **Não** oferecer UI de alterar e-mail. Campo é `readonly` com helper.
- **Não** mostrar foto das adolescentes vinculadas no card. Inicial apenas (P9 — não-vigilância visual).
- **Não** adicionar CTA "Ver perfil" ou "Gerenciar" nos itens de vinculação. Eles são visualização pura.
- **Não** mostrar progresso, XP, streak ou metadados das adolescentes vinculadas (P9). Apenas "vinculada em {mês} de {ano}".
- **Não** mostrar dados sensíveis: data de nascimento, idade, e-mail das adolescentes. Eles **não** aparecem aqui.
- **Não** usar termos como "filha", "filho", "tutelada" — usar "adolescente vinculada".
- **Não** confirmar "tem certeza?" ao descartar.
- **Não** salvar automaticamente (autosave) — botão explícito.
- **Não** usar coral no botão Descartar.
- **Não** ofuscar ou mascarar o e-mail (`c***@gmail.com`). Mostrar inteiro — é o próprio e-mail do Carlos, ele já conhece.

---

## 9. Estados, validações e bordas

| Cenário | Comportamento esperado |
|---|---|
| Carlos abre `/perfil` pela 1ª vez | `idle`; campos preenchidos com valores atuais; card de vinculações mostra realidade do servidor |
| Carlos altera o nome para 1 caractere | Erro inline; Salvar `disabled` |
| Carlos cola 100 caracteres no nome | `maxlength="80"` recorta no DOM; se vier por colagem programática, validação mostra erro |
| Carlos digita o mesmo nome que já tinha | `dirty = false` (sem diff); save bar não aparece |
| Carlos clica Descartar com mudanças | Nome volta ao baseline; save bar some |
| Carlos clica Salvar com nome válido | `saving` ~900ms → `saved` ~2500ms → `idle` com novo baseline |
| Carlos abre a tela sem nenhuma vinculação ainda | Card mostra empty state textual + footnote (a footnote faz sentido mesmo sem vínculos — explica o que **vai** aparecer) |
| Carlos abre a tela com 3 adolescentes vinculadas | Lista mostra 3 itens, subtítulo "3 adolescentes vinculadas" |
| Carlos tenta editar o e-mail (clica no input) | Foco entra mas não aceita entrada; helper explica |
| Carlos tenta editar dados de uma adolescente (clica no item) | Item não é clicável — sem hover, sem cursor pointer, sem ação |
| Sessão expira e Carlos clica Salvar | POST → 401 → `/login` (Spring padrão); mudanças locais perdidas |
| Viewport < 320px | Layout não suportado oficialmente (RNF-COMP-04). Em emergência o card de vinculações encolhe; avatar 96×96 ainda cabe |
| `prefers-reduced-motion: reduce` | Spinner sem rotação |
| Vinculação removida entre o GET e o POST | Save responde OK; card no GET seguinte reflete a remoção. Banner de sucesso ainda diz "Tua vinculação continua igual" — texto é sobre **a vinculação que existir**, não sobre lista específica. **Aceitável.** |

---

## 10. Tokens consumidos

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da página, fade do save bar |
| `--color-surface` | Header, inputs, card de vinculações |
| `--color-surface-muted` | Hover de botão ghost, fundo do input read-only, fundo dos `.vinc-item`, fundo do empty state |
| `--color-divider` | Borda inferior do header, borda do card de vinculações |
| `--color-border` | Borda dos inputs em repouso |
| `--color-border-strong` | Borda dos inputs em hover |
| `--color-text-display` | Título do header, nome no avatar, nome no item de vinculação, título do card de vinculações |
| `--color-text-body` | Labels, botão ghost |
| `--color-text-muted` | Helper, overline de papel, meta do item, footnote, empty state, ícones decorativos |
| `--color-primary-100` | Fundo do avatar, fundo do avatar do item de vinculação |
| `--color-primary-700` | Letra do avatar |
| `--color-primary` (`500`) | Fundo do botão Salvar |
| `--color-primary-hover` (`600`) | Hover do botão Salvar |
| `--color-primary-active` (`700`) | Active do botão Salvar |
| `--color-on-primary` | Texto do botão Salvar |
| `--color-focus-ring` | Anel de foco padrão |
| `--color-success-100` / `700` | Banner success, ícone do card de vinculações |
| `--color-danger-700` | Borda de input com erro, texto de erro |
| `--font-display` | Inicial do avatar, nome no avatar, inicial do item de vinculação |
| `--font-sans` | Restante |
| `--text-xs` | Helper, erro inline, overline, subtítulo do card, meta do item, footnote |
| `--text-sm` | Labels, banner, título do card de vinculações, nome do item, texto do empty state |
| `--text-base` | Body geral, botões, inputs, inicial do item de vinculação |
| `--text-xl` | Nome no avatar |
| `--text-2xl` | Inicial dentro do avatar |
| `--space-1` ... `--space-12` | Diversos espaçamentos |
| `--radius-md` | Inputs, icon-button, items da lista de vinculação |
| `--radius-lg` | Banner, card de vinculações |
| `--radius-full` | Avatar, avatares de item, botões pill, ícone do card |
| `--shadow-sm` | Avatar, card de vinculações |
| `--shadow-focus` | Foco padrão |
| `--duration-fast` · `--ease-out-soft` | Transições |
| `--z-sticky` | Header |

---

## 11. Diferenças vs US-009

Esta tela é deliberadamente **uma versão simplificada** da US-009. Documenta as decisões.

| Aspecto | US-009 (adolescente) | US-010 (responsável) |
|---|---|---|
| Avatar | 112×112 com **badge de foto editável** | 96×96 só com inicial (sem badge) |
| Campos editáveis | Apelido + foto + data de nascimento | **Só nome** |
| Apelido vs nome | "Apelido" (3–20) | "Nome" (2–80) |
| E-mail | Read-only | Read-only (mesma regra) |
| Trocar foto | Action sheet bottom | **Sem action sheet** — não há foto |
| Data de nascimento | Editável + modal de bloqueio por idade | **Sem campo** (US-010 não pede; o cadastro original já checou maioridade) |
| Card extra | Nenhum | **Card de vinculações ativas** (read-only) — reforça CA-3 |
| Banner success | "Teu progresso continua igual." | "Tua vinculação continua igual." |
| Voltar para | `/trilha` | `/painel-pais` |
| Overline no avatar | Não tem | "RESPONSÁVEL" |

**Por que adicionar o card de vinculações além do mínimo da US-010:** o CA-3 ("nenhuma edição quebra a vinculação") é abstrato — Carlos só descobre isso testando. Mostrar a vinculação na tela transforma a promessa em prova visual. Custo de UX: baixo (read-only). Custo de implementação: baixo (consulta já existe). Ganho: confiança.

**Por que NÃO incluir foto editável:** o requisito não pede. Adicionar foto editaria o escopo da US-010 sem ganho claro (responsável raramente "se expressa" via foto de perfil em apps de gestão). Se métrica futura mostrar demanda, abre uma US de extensão.

---

## 12. Pendências de design

Nenhuma bloqueante. Pontos abertos:

1. **CA-2 da US-010 (troca de e-mail)** — explicitamente fora do MVP por decisão de produto (Dioni, 2026-05-27). Mesma situação da US-009. Se reabrir, requer o fluxo completo de verificação do novo endereço.
2. **Erro de save server-side** — banner `alert--error` no topo: `"Não consegui salvar agora. Tenta de novo em alguns segundos."` Não coberto no protótipo.
3. **Foto de perfil para responsável** — fora do escopo da US-010. Se métricas mostrarem demanda, criar US de extensão (`US-010.1`) reusando o padrão do avatar+badge+sheet da US-009.
4. **Desvinculação a partir desta tela** — fora do escopo. Fluxo de desvinculação é outra US. Se for adicionado, deve aparecer no card como CTA destrutivo separado, com modal de confirmação (variação do modal de bloqueio da US-009, mas com 2 CTAs porque é decisão reversível: "Manter vinculação" / "Desvincular mesmo assim").
5. **Mostrar nome do responsável que vinculou no card de cada adolescente** — pode ser relevante em casos de múltiplos responsáveis pela mesma adolescente. Fora do escopo atual.
6. **Última atualização do perfil** — desejável mostrar "Atualizado há 3 dias"? Mesma resposta da US-009: fora do escopo, trazer só se métrica mostrar valor.

---

## Referências cruzadas

- Protótipo executável: [`doc/UX/prototypes/perfil-responsavel-editar.html`](prototypes/perfil-responsavel-editar.html)
- US irmã (perfil do adolescente): [`us-009-spec.md`](us-009-spec.md)
- Tokens visuais: `doc/UX/01-design-tokens.md`
- Componentes base: `doc/UX/02-componentes-base.md`
- Acessibilidade: `doc/UX/06-acessibilidade.md`
- Regra "e-mail é credencial imutável": [`us-006-spec.md`](us-006-spec.md) §1.3 · memória do projeto [`email-imutavel-apos-cadastro.md`](../../.claude/projects/-Users-dionia-oliveira-sources-atrilha/memory/email-imutavel-apos-cadastro.md)
- Princípio P9 (não-vigilância): `doc/PRD.md` §3 (princípios)
- PRD §11.5 — LGPD, direito de correção
- PRD §13 — US-010
