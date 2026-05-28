# US-009 — Edição do perfil do adolescente · UX Spec

**Código:** US-009
**GitHub Issue:** —
**Status:** Proposto (aguardando issue de implementação)
**Depende de:** US-001 (cadastro adolescente) · US-005 (faixa etária 13–17) · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/06-acessibilidade.md`
**Referências PRD:** §11.5 (LGPD — direito de correção) · §13 (US-009) · §17 (DoD)
**Protótipo:** [`doc/UX/prototypes/perfil-adolescente-editar.html`](prototypes/perfil-adolescente-editar.html)
**Escopo deste doc:** **layout** da tela única `/perfil` para o adolescente (Júlia). Cobre wireframe, tokens visuais, microcopy literal, estados, comportamento de edição com dirty state, modal de bloqueio por idade, action sheet de foto. **Não cobre** controller, persistência, gating de role (responsável tem outra US — US-010) ou pipeline de upload de imagem. Microcopy em pt-BR (P5 — sem moralismo, P11 — vocabulário ASD jovem). Nenhum elemento depende só de cor.

---

## 1. Princípios

1. **Direito de correção, não vigilância.** A tela existe pra Júlia ajustar o que mudou nela (apelido, foto, data). Carlos (responsável) **não** edita campos de Júlia — princípio P9. Esta tela é só do adolescente.
2. **E-mail é credencial — aparece, mas não se edita.** O e-mail aparece como campo `readonly` com cadeado visual e helper explicando "É o que você usa pra entrar. Por isso a gente não troca ele aqui." Decisão de produto (Dioni, 2026-05-27) — o CA-2 da US-009 que previa troca de e-mail fica **fora do MVP**.
3. **Progresso é sagrado.** Nenhuma edição apaga XP, streak, selos, versículos ou vinculação. O banner de sucesso reafirma isso ("Teu progresso continua igual.") — adolescente que tem medo de "perder tudo" não tem medo de editar.
4. **Salvar só quando há o que salvar.** A barra de ações é sticky no rodapé e só aparece quando o form está `dirty`. Em estado idle a tela parece um "perfil resumido", não um formulário aberto.
5. **Bloqueio por idade é porta fechada com saída educada.** Se Júlia muda a data e fica fora da faixa 13–17, abre modal `alertdialog` com **um único CTA** ("Entendi, manter como estava") que reverte a data. Sem dois botões, sem "tentar de novo" — a regra é dura, mas a copy é gentil.
6. **Foto é leveza, não obrigação.** O avatar mostra a inicial do apelido como padrão (`primary-100` de fundo, `primary-700` da letra). Trocar foto abre **action sheet bottom** (Tirar foto / Galeria / Tirar foto pra ficar com a inicial / Cancelar) — padrão mobile que adolescente já conhece de Instagram, WhatsApp.

---

## 2. Tela única — `/perfil`

**Rota:** `GET /perfil` → view `perfil/adolescente-editar`
**Acesso:** autenticado como `role=ADOLESCENT`. Responsável (`role=RESPONSIBLE`) acessando `/perfil` cai no equivalente da US-010 (fora deste doc).
**Entrada:** vem do menu/avatar de Júlia em `/trilha`. O header tem botão voltar apontando para `/trilha`.

### 2.1 Wireframe (mobile 320px — desktop usa o mesmo card centralizado, `max-width: 28rem`)

```
┌────────────────────────────────────┐
│ [<]    Meu perfil          [    ] │  ← header com voltar (esq) + título + slot vazio (dir)
├────────────────────────────────────┤
│                                    │
│   [banner success — só após save]  │
│                                    │
│         ┌─────────────┐            │
│         │             │            │
│         │      D      │ 📷         │  ← avatar 112×112 + badge câmera
│         │             │            │
│         └─────────────┘            │
│              Dioni                 │  ← apelido em display xl
│         dionialves@gmail.com       │  ← e-mail em text-sm muted
│                                    │
│   label · Como a gente te chama    │
│   [__________________________]     │
│   helper · De 3 a 20 caracteres…   │
│                                    │
│   label · Data de nascimento       │
│   [____/____/____]                 │
│   helper · Influencia a faixa…     │
│                                    │
│   label · 🔒 E-mail                │
│   [__readonly___]              🔒  │  ← input read-only com cadeado
│   helper · É o que você usa pra…   │
│                                    │
│   ─── espaço com gradiente ───     │
│   [    Salvar alterações    ]  ←   │  sticky save bar (aparece só se dirty)
│   [        Descartar        ]      │
│                                    │
└────────────────────────────────────┘

Sheet (foto):                 Modal (bloqueio idade):
┌──────────────────────┐      ┌──────────────────────┐
│        ───           │      │       [⚠ icon]       │
│ 📷 Tirar foto agora   │      │     Espera aí        │
│ 🖼 Escolher da galeria│      │  Essa data muda a    │
│ 🗑 Ficar com inicial  │      │  faixa etária…       │
│      Cancelar         │      │ [Entendi, manter…]   │
└──────────────────────┘      └──────────────────────┘
```

### 2.2 Estrutura HTML (referência, não obrigatória — Codificador decide o markup exato)

```html
<header>  [voltar] · Meu perfil · [spacer]  </header>

<main class="main">
  <!-- banner success após save (transient ~2.5s) -->
  <div role="status" data-state="saved" class="alert alert--success">
    Pronto, atualizamos. Teu progresso continua igual.
  </div>

  <!-- Identidade -->
  <section class="avatar-block">
    <div class="avatar">
      <span>D</span>                  <!-- ou <img src=... /> -->
      <button class="avatar-badge" aria-label="Alterar foto de perfil">📷</button>
    </div>
    <p class="avatar-name">Dioni</p>
    <p class="avatar-email">dionialves@gmail.com</p>
  </section>

  <form method="post" action="/perfil">
    [csrf]

    <div class="input-group">
      <label for="nickname">Como a gente te chama</label>
      <input id="nickname" name="nickname" type="text" minlength="3" maxlength="20" />
      <p class="input-helper">De 3 a 20 caracteres. Pode ser teu nome, apelido, qualquer coisa.</p>
      <p class="input-error" role="alert" hidden>...</p>
    </div>

    <div class="input-group">
      <label for="birthDate">Data de nascimento</label>
      <input id="birthDate" name="birthDate" type="date" />
      <p class="input-helper">Influencia a faixa etária da tua conta.</p>
    </div>

    <div class="input-group">
      <label for="email">🔒 E-mail</label>
      <div class="readonly-wrap">
        <input id="email" name="email" type="email" readonly value="{{email}}" />
        <span class="readonly-icon" aria-hidden="true">🔒</span>
      </div>
      <p class="input-helper">É o que você usa pra entrar. Por isso a gente não troca ele aqui.</p>
    </div>

    <!-- save bar — só aparece quando dirty -->
    <div class="save-bar" hidden>
      <button type="submit" class="btn btn-primary">Salvar alterações</button>
      <button type="button" class="btn btn-ghost">Descartar</button>
    </div>
  </form>
</main>

<!-- ACTION SHEET (foto) -->
<dialog class="sheet-overlay" role="dialog" aria-modal="true">
  <div class="sheet">
    <div class="sheet-handle"></div>
    <button class="sheet-item">📷 Tirar foto agora</button>
    <button class="sheet-item">🖼 Escolher da galeria</button>
    <button class="sheet-item sheet-item--danger">🗑 Tirar foto (ficar com a inicial)</button>
    <button class="sheet-item">Cancelar</button>
  </div>
</dialog>

<!-- MODAL bloqueio por idade -->
<dialog class="modal-overlay" role="alertdialog" aria-modal="true">
  <div class="modal">
    <div class="modal-icon">⚠</div>
    <h2>Espera aí</h2>
    <p>Essa data muda a faixa etária da tua conta…</p>
    <button class="btn btn-primary">Entendi, manter como estava</button>
  </div>
</dialog>
```

---

## 3. Especificações visuais

### 3.1 Layout do container

| Elemento | Token / valor |
|---|---|
| Fundo da página | `--color-bg` (`neutral-50` · `#F7F4F1`) |
| `<main>` padding | `--space-8` topo · `--space-4` laterais · `--space-12` baixo (folga pro save bar sticky) |
| `max-width` | `28rem` (448px) |
| Centralização | `margin: 0 auto` |

### 3.2 Header

| Propriedade | Valor |
|---|---|
| Posição | `sticky; top: 0; z-index: --z-sticky` |
| Fundo | `--color-surface` |
| Borda inferior | `1px solid --color-divider` |
| Display | `grid; grid-template-columns: --space-11 1fr --space-11; align-items: center` (3 colunas — voltar / título centralizado / slot vazio à direita para simetria) |
| Gap | `--space-2` |
| Padding | `--space-2 --space-3` |
| Altura mínima | `56px` |

**Botão voltar (`.icon-button`):**

| Propriedade | Valor |
|---|---|
| Tamanho mínimo | `--space-11 × --space-11` (44×44 touch target) |
| Display | `inline-grid; place-items: center` |
| Cor | `--color-text-body` |
| Hover | `background: --color-surface-muted` |
| Ícone | Chevron-left SVG 20×20, `stroke-width: 2.2`, `stroke-linecap: round` |
| `aria-label` | `"Voltar"` |
| Destino | `/trilha` (fluxo de entrada padrão) |

**Título:**

| Propriedade | Valor |
|---|---|
| Tag | `<h1 class="header-title">` |
| Conteúdo | `"Meu perfil"` |
| Estilo | `--font-sans` · `--text-base` · `--font-weight-semibold` · `--color-text-display` · `text-align: center` |

### 3.3 Avatar block

Bloco central acima do form: avatar grande + apelido + e-mail. Funciona como "resumo" do perfil — o que muda no form atualiza aqui em tempo real.

| Propriedade | Valor |
|---|---|
| Container | `display: flex; flex-direction: column; align-items: center; gap: --space-3; margin-bottom: --space-8` |

**Avatar (`.avatar`):**

| Propriedade | Valor |
|---|---|
| Dimensões | `112 × 112px` |
| Raio | `--radius-full` |
| Fundo (sem foto) | `--color-primary-100` (`#FFD9D6`) |
| Cor da letra (sem foto) | `--color-primary-700` (`#A8362F`) |
| Tipografia da inicial | `--font-display` · `--text-3xl` (32px) · `--font-weight-semibold` · `text-transform: uppercase` |
| Conteúdo (sem foto) | `<span>` com a 1ª letra do apelido (`(nickname || '?').charAt(0).toUpperCase()`) |
| Conteúdo (com foto) | `<img>` com `object-fit: cover; width: 100%; height: 100%` |
| `overflow` | `hidden` (cobre a imagem dentro do círculo) |
| Sombra | `--shadow-sm` (sutil — não vira card flutuante) |

**Badge de edição (`.avatar-badge`):**

| Propriedade | Valor |
|---|---|
| Posicionamento | `absolute; right: 0; bottom: 0` (canto inferior-direito do avatar) |
| Dimensões | `36 × 36px` |
| Raio | `--radius-full` |
| Fundo | `--color-primary` |
| Cor do ícone | `--color-on-primary` |
| **Borda branca** | `border: 3px solid --color-surface` — isola o badge visualmente do avatar (gera "anel" branco entre os dois círculos) |
| Ícone | Câmera SVG 18×18 (`stroke-width: 2`, `stroke-linecap: round`) |
| Hover | `background: --color-primary-hover` |
| `aria-label` | `"Alterar foto de perfil"` |
| Click | Abre o action sheet (§3.7) |
| Touch target | 36×36 visual, mas como filho do avatar 112×112 o usuário consegue acertar; em viewport ≥ 360px aceitável. Em viewport menor, considerar aumentar para 40 ou estender área clicável invisível via padding |

**Apelido (`.avatar-name`):**

| Propriedade | Valor |
|---|---|
| Tag | `<p>` (não heading — já temos H1 no header) |
| Estilo | `--font-display` · `--text-xl` · `--font-weight-semibold` · `--color-text-display` |
| Conteúdo dinâmico | Espelha `x-model="nickname"` em tempo real conforme Júlia digita |
| Fallback | `"Sem apelido"` quando o input está vazio (durante edição) |

**E-mail (`.avatar-email`):**

| Propriedade | Valor |
|---|---|
| Tag | `<p>` |
| Estilo | `--text-sm` · `--color-text-muted` · `text-align: center` |
| `word-break` | `break-all` (e-mails longos não estouram em viewport 320px) |

### 3.4 Form — campos editáveis

Display do form: `flex; flex-direction: column; gap: --space-5`.

**Padrão `.input-group` (todos os 3 campos):**

| Elemento | Estilo |
|---|---|
| Container | `display: flex; flex-direction: column; gap: --space-2` |
| Label | `<label>` · `--text-sm` · `--font-weight-semibold` · `--color-text-body` · associado por `for` |
| Input padrão | `width: 100%` · `min-height: --space-11` · `padding: --space-3 --space-4` · `background: --color-surface` · `border: 1px solid --color-border` · `--radius-md` · `color: --color-text-body` |
| Hover (editável) | `border-color: --color-border-strong` |
| Focus | `outline: none; border-color: --color-focus-ring; box-shadow: --shadow-focus` |
| Erro (`aria-invalid="true"`) | `border-color: --color-danger-700` |
| Helper (`.input-helper`) | `--text-xs` · `--color-text-muted` |
| Mensagem de erro (`.input-error`) | `--text-xs` · `--color-danger-700` · `--font-weight-medium` · `role="alert"` |

**3.4.1 Campo "Apelido"**

| Propriedade | Valor |
|---|---|
| `id` | `nickname` |
| `name` | `nickname` |
| `type` | `text` |
| `minlength` / `maxlength` | `3` / `20` |
| Label | `"Como a gente te chama"` |
| Helper | `"De 3 a 20 caracteres. Pode ser teu nome, apelido, qualquer coisa."` |
| Erros (texto literal) | `< 3`: `"Precisa de pelo menos 3 caracteres."` · `> 20`: `"Máximo 20 caracteres."` |
| Validação | Em tempo real (`@input="markDirty()"` chama `validate()`) |
| Reflexo | `.avatar-name` espelha o valor enquanto Júlia digita |

**3.4.2 Campo "Data de nascimento"**

| Propriedade | Valor |
|---|---|
| `id` | `birthDate` |
| `name` | `birthDate` |
| `type` | `date` (nativo do browser — em mobile abre date picker do OS) |
| Label | `"Data de nascimento"` |
| Helper | `"Influencia a faixa etária da tua conta."` |
| Validação inline | Vazio: `"Coloca a data de nascimento."` |
| Validação de idade (13–17) | **Não** mostra erro inline — o bloqueio acontece **só ao tentar salvar** (modal §3.8). Helper neutro mantém o tom acolhedor durante a digitação. |

**3.4.3 Campo "E-mail" (read-only)**

| Propriedade | Valor |
|---|---|
| `id` | `email` |
| `name` | `email` |
| `type` | `email` |
| `readonly` | true (atributo HTML — não é `disabled`, pra continuar focável e selecionável; Júlia pode copiar o valor) |
| Label | `"E-mail"` + ícone cadeado 14×14 ao lado, em `--color-text-muted` |
| Helper | `"É o que você usa pra entrar. Por isso a gente não troca ele aqui."` |
| Estilo do input | `background: --color-surface-muted` (`#EDE8E4`) · `color: --color-text-muted` · `cursor: not-allowed` |
| Padding direito | `--space-10` (reserva espaço pro ícone) |
| Ícone interno (`.readonly-icon`) | Cadeado SVG 18×18 absolute `right: --space-3; top: 50%; transform: translateY(-50%)`, `color: --color-text-muted`, `pointer-events: none` |
| Focus | Sem anel coral, sem mudança de borda — visualmente "não interativo" |
| `aria-describedby` | `"email-help"` |

### 3.5 Banner de sucesso (após salvar)

| Propriedade | Valor |
|---|---|
| Posição | Topo do `<main>`, antes do avatar block |
| Componente | `.alert.alert--success` (padrão do design system) |
| Fundo / cor | `--color-success-100` / `--color-success-700` |
| Ícone | Check SVG 18×18 |
| Texto | `"Pronto, atualizamos. Teu progresso continua igual."` |
| `role` | `"status"` (notificação não-crítica) `aria-live="polite"` |
| Duração | Transient — desaparece após `2500ms` (timer JS) e o estado volta a `idle` |
| Após salvar | `nicknameOriginal` / `birthDateOriginal` / `photoUrlOriginal` são atualizados com os valores recém-salvos (próximo dirty mede a partir daqui) |

### 3.6 Save bar (sticky no rodapé)

Aparece **somente** quando `dirty === true` ou `state === 'saving'`. Em estado idle a área é vazia e o form parece um "perfil resumido".

| Propriedade | Valor |
|---|---|
| Posição | `sticky; bottom: 0` |
| Fundo | `linear-gradient(180deg, rgba(247, 244, 241, 0) 0%, --color-bg 40%)` — fade do conteúdo abaixo, evita "corte" abrupto |
| Padding | `--space-6` topo · `0` laterais · `--space-2` baixo |
| Margem superior | `--space-8` |
| Display | `flex; flex-direction: column; gap: --space-3` |

**Botão primário "Salvar alterações":**

| Estado | Estilo / comportamento |
|---|---|
| Default | `.btn.btn-primary` — pill coral, `min-height: --space-12`, `--font-weight-semibold` |
| Hover | `background: --color-primary-hover` |
| Active | `background: --color-primary-active` |
| Disabled | `state === 'saving'` ou `hasErrors() === true` → `opacity: 0.55; cursor: not-allowed` |
| Loading | Spinner inline + texto `"Salvando…"`; `aria-busy="true"` |

**Botão ghost "Descartar":**

| Propriedade | Valor |
|---|---|
| Classe | `.btn.btn-ghost` |
| Estilo | Transparente, `--color-text-body`, hover `--color-surface-muted` |
| Ação | Reverte `nickname`, `birthDate`, `photoUrl` aos valores `Original` salvos; limpa `errors`; `dirty = false` |
| Disabled | `state === 'saving'` |

### 3.7 Action sheet — Trocar foto

Padrão **bottom sheet** (Android/iOS-like). Aberto por clique no badge da câmera ou pela chip "Trocar foto" da demo bar.

| Propriedade | Valor |
|---|---|
| Overlay (`.sheet-overlay`) | `position: fixed; inset: 0; z-index: --z-overlay; background: rgba(26,22,20,0.5)`; flex alinhando ao fim (vertical) e centro (horizontal); padding `--space-4` |
| Sheet (`.sheet`) | `width: 100%; max-width: 28rem; background: --color-surface; --radius-xl; padding: --space-3; --shadow-lg` |
| Display interno | `flex; flex-direction: column; gap: --space-1` |
| Handle visual | Pílula 36×4px `--radius-full`, `background: --color-neutral-200`, centrada, margem `--space-1 auto --space-3` |
| `role` | `"dialog"` · `aria-modal="true"` · `aria-labelledby="sheet-title"` |
| Título acessível | `<h2 id="sheet-title">` visualmente oculto (`position:absolute;left:-9999px`) com texto `"Trocar foto"` |
| Fechar | Click no overlay (`.self`) **ou** tecla `Escape` |

**Item `.sheet-item`:**

| Propriedade | Valor |
|---|---|
| Display | `flex; align-items: center; gap: --space-3; padding: --space-3 --space-4; --radius-md; min-height: --space-12; width: 100%; text-align: left` |
| Cor | `--color-text-body` |
| Hover | `background: --color-surface-muted` |
| Ícone | SVG 20×20, `--color-text-muted`, `flex-shrink: 0` |
| Touch target | 48px confortável |

**Variantes:**

| Item | Ícone | Cor especial | Ação |
|---|---|---|---|
| `"Tirar foto agora"` | câmera | padrão | abre câmera nativa (`<input type="file" accept="image/*" capture="user">` em produção) |
| `"Escolher da galeria"` | imagem com paisagem | padrão | abre seletor de arquivos (`<input type="file" accept="image/*">` em produção) |
| `"Tirar foto (ficar com a inicial)"` | lixeira | `--color-danger-700` (texto e ícone) | só aparece quando há foto setada; remove `photoUrl` |
| `"Cancelar"` | sem ícone | `--color-text-muted` · `justify-content: center` | fecha o sheet sem alterar nada |

### 3.8 Modal de bloqueio por idade (CA-4)

Padrão **modal centralizado** (não bottom sheet). Aparece **só quando Júlia clica Salvar** e a data nova faz a idade sair da faixa 13–17.

| Propriedade | Valor |
|---|---|
| Overlay (`.modal-overlay`) | Mesma estrutura do sheet, mas `align-items: center` (não `flex-end`) |
| Modal (`.modal`) | `max-width: 24rem; background: --color-surface; --radius-xl; padding: --space-6; --shadow-lg; text-align: center` |
| `role` | `"alertdialog"` (mais forte que `dialog` — leitor de tela anuncia como interrupção) |
| `aria-modal` | `"true"` |
| `aria-labelledby` | `"blocker-title"` |
| `aria-describedby` | `"blocker-text"` |
| Fechar | Tecla `Escape` **ou** botão único do CTA (não fecha por click no overlay — é uma decisão consciente, não acidental) |

**Conteúdo:**

| Slot | Estilo / valor |
|---|---|
| Ícone | Container `56×56`, `--radius-full`, `background: --color-warning-100`, `color: --color-warning-700`; SVG circle-info 28×28 dentro; `margin: 0 auto --space-4` |
| Título (`.modal-title`) | `<h2>` · `--font-display` · `--text-xl` · `--font-weight-semibold` · `--color-text-display` · `margin-bottom: --space-2` · texto: `"Espera aí"` |
| Texto (`.modal-text`) | `--text-sm` · `--color-text-body` · `margin-bottom: --space-5` · texto literal §4 |
| CTA único | `.btn.btn-primary` · texto `"Entendi, manter como estava"` · click reverte `birthDate` ao `birthDateOriginal` e fecha o modal |

**Por que CTA único e não "Cancelar / Mudar mesmo assim":** o bloqueio é uma regra de produto (faixa etária da conta), não uma confirmação. Não há "mudar mesmo assim" — a porta está fechada. Oferecer dois botões implicaria que Júlia pode escolher, e ela não pode. Copy + 1 botão = honestidade.

---

## 4. Microcopy (pt-BR — literal, copiar como está)

| Slot | Texto |
|---|---|
| `<title>` | Meu perfil |
| Header título | Meu perfil |
| Botão voltar (`aria-label`) | Voltar |
| Avatar badge (`aria-label`) | Alterar foto de perfil |
| Avatar fallback (sem apelido) | Sem apelido |
| Label apelido | Como a gente te chama |
| Helper apelido | De 3 a 20 caracteres. Pode ser teu nome, apelido, qualquer coisa. |
| Erro apelido (< 3) | Precisa de pelo menos 3 caracteres. |
| Erro apelido (> 20) | Máximo 20 caracteres. |
| Label data | Data de nascimento |
| Helper data | Influencia a faixa etária da tua conta. |
| Erro data (vazia) | Coloca a data de nascimento. |
| Label e-mail | E-mail |
| Helper e-mail | É o que você usa pra entrar. Por isso a gente não troca ele aqui. |
| Save bar — primário (default) | Salvar alterações |
| Save bar — primário (loading) | Salvando… (com spinner) |
| Save bar — secundário | Descartar |
| Banner success | Pronto, atualizamos. Teu progresso continua igual. |
| Sheet item — câmera | Tirar foto agora |
| Sheet item — galeria | Escolher da galeria |
| Sheet item — remover | Tirar foto (ficar com a inicial) |
| Sheet item — cancelar | Cancelar |
| Modal bloqueio — título | Espera aí |
| Modal bloqueio — texto | Essa data de nascimento muda a faixa etária da tua conta. Pra ajustar isso direito, fala com a gente — vamos te ajudar sem perder nada do teu progresso. |
| Modal bloqueio — CTA | Entendi, manter como estava |

**Vetado:**

- "Atualizar e-mail", "Trocar e-mail", "Alterar credencial" — e-mail é imutável.
- "Você é menor de idade", "Conta restrita", "Idade inválida" — moralismo. O texto do bloqueio fala da **faixa da conta**, não da pessoa.
- "Sua foto foi alterada com sucesso!" — toast emocional desnecessário. O banner único cobre todas as edições (apelido + foto + data).
- Termos técnicos: "validação", "campo obrigatório", "formulário inválido", "request", "endpoint", "upload". Usar voz ativa: "Coloca…", "Não bate…", "Confere…".
- "Tem certeza?" em qualquer botão. Júlia já clicou; o sistema confia ou apresenta uma porta fechada com saída educada. Sem confirmações fofas.

---

## 5. Estados visuais

| Estado | Trigger | UI |
|---|---|---|
| `idle` (default) | Tela carregada, sem mudanças | Avatar + form preenchidos com valores atuais; save bar **invisível** |
| `dirty` | Qualquer input alterado (`markDirty()` detectou diff) | Save bar aparece; "Salvar alterações" habilita se sem erros |
| `validating` (transient) | Durante `validate()` em `@input` | Bordas vermelhas e mensagens inline aparecem/somem; estado segue `dirty` no fundo |
| `saving` | POST de save em voo | Botão primário com spinner + "Salvando…"; botão Descartar desabilita; campos seguem editáveis tecnicamente, mas usuário não deve mexer |
| `saved` | Save retornou OK | Banner success no topo; após 2500ms volta a `idle` com os novos valores como baseline |
| `error` (futuro) | Save retornou falha (não coberto no protótipo) | Banner `alert--error` com mensagem `"Não consegui salvar agora. Tenta de novo em alguns segundos."` (proposta) |
| Sheet aberto | Click no badge ou na chip | Overlay escuro + sheet bottom; foco move pra primeiro item |
| Modal bloqueio aberto | Click em Salvar com idade fora 13–17 | Overlay escuro + modal centralizado; foco move pro CTA único |

---

## 6. Comportamento

### 6.1 Dirty state

A função `markDirty()` é chamada em `@input` de cada campo editável e a cada `pickPhoto()` / `removePhoto()`:

```javascript
dirty = (
  nickname !== nicknameOriginal ||
  birthDate !== birthDateOriginal ||
  photoUrl !== photoUrlOriginal
);
```

Save bar aparece quando `dirty === true`. Botão "Salvar" habilita quando `dirty && !hasErrors()`. Botão "Descartar" reverte os 3 campos para os `Original` e zera `errors`.

### 6.2 Validação inline

`validate()` roda em cada `@input` e antes de `save()`. Erros conhecidos:

| Campo | Regra | Mensagem |
|---|---|---|
| `nickname` | `< 3` caracteres | "Precisa de pelo menos 3 caracteres." |
| `nickname` | `> 20` caracteres | "Máximo 20 caracteres." |
| `birthDate` | Vazio | "Coloca a data de nascimento." |
| `birthDate` | Idade fora 13–17 | **Sem erro inline** — bloqueio só dispara em `save()` via modal |

### 6.3 Save flow

Pseudocódigo do protótipo:

```javascript
save() {
  validate();
  if (hasErrors()) return;

  // CA-4: data nova torna inelegível → modal bloqueio
  if (birthDate !== birthDateOriginal) {
    const age = ageFrom(birthDate);
    if (age < 13 || age >= 18) {
      showBlocker = true;
      return;
    }
  }

  setState('saving'); // → 900ms → 'saved' → 2500ms → 'idle'
}
```

Sequência completa: `idle` → input → `dirty` → click Salvar → `saving` (~900ms) → `saved` (banner ~2500ms) → `idle` com baseline atualizada.

### 6.4 Foto

`pickPhoto(source)` (no protótipo, simulado por data-URI SVG):

1. Click no badge ou na chip → `showSheet = true` → foco no 1º item.
2. Click em "Tirar foto" / "Galeria" → `photoUrl` recebe a nova foto; `showSheet = false`; `markDirty()`.
3. Click em "Tirar foto (ficar com a inicial)" → `photoUrl = ''`; mesmo fluxo.
4. Click em "Cancelar" ou no overlay → `showSheet = false`, sem alterações.

Em produção, os botões "Tirar foto" / "Galeria" disparam um `<input type="file">` oculto com `capture="user"` (câmera frontal) e sem `capture` (galeria), respectivamente.

### 6.5 Bloqueio por idade

`showBlocker = true` abre o modal. CTA único:

```javascript
@click="showBlocker = false; revertBirth();"
// revertBirth(): birthDate = birthDateOriginal; markDirty();
```

A data volta ao valor original; o `dirty` é recalculado (pode voltar a `false` se a data era a única mudança). Júlia continua na tela, sem perder o estado de edição dos outros campos.

---

## 7. Acessibilidade

- `<header role="banner">`; `<main>` único.
- `<h1>` único na página (no header) — apelido e e-mail no avatar block são `<p>`.
- Cada input com `<label for>` associado + `aria-describedby` apontando para helper OU error.
- Inputs com erro recebem `aria-invalid="true"`; mensagens de erro são `role="alert"`.
- E-mail read-only: continua focável (Tab passa), seleção permitida (Júlia pode copiar), `cursor: not-allowed`, `aria-describedby="email-help"`.
- Avatar badge: `<button type="button">`, `aria-label="Alterar foto de perfil"` (o ícone é só decorativo, `aria-hidden="true"`).
- Sheet (`role="dialog"` + `aria-modal="true"` + `aria-labelledby` apontando para `<h2>` visualmente oculto).
- Modal de bloqueio (`role="alertdialog"`) — leitor de tela anuncia como interrupção urgente.
- Foco managed:
  - Ao abrir sheet → foco no 1º `.sheet-item`.
  - Ao abrir modal de bloqueio → foco no CTA único.
  - Ao fechar → foco volta para o elemento que abriu (badge ou botão Salvar).
- Tecla `Escape` fecha sheet e modal.
- Save bar com sticky position: leitor de tela em ordem natural — botões aparecem **após** o último input no DOM, então a leitura linear faz sentido.
- Touch target: tudo ≥ 44×44 (`--space-11`). Badge da câmera é 36×36 visual, mas dentro do avatar 112×112 — em viewport ≥ 360px aceita; em viewport menor, considerar aumentar para 40 (RNF-A11Y-05).
- Contraste calculado:
  - Apelido `text-display` sobre `surface` → **16.4:1** (AAA).
  - E-mail `text-muted` sobre `surface` → **4.62:1** (AA).
  - E-mail read-only `text-muted` sobre `surface-muted` (`#EDE8E4`) → **4.20:1** (AA limite — aceito porque é texto não-essencial; o helper imediatamente abaixo em `text-muted` sobre `surface` mantém AA).
  - Avatar letra `primary-700` sobre `primary-100` → **6.8:1** (AAA Large).
  - Badge ícone `on-primary` sobre `primary` → ≥ **4.5:1** (validado em chore-ux-001 §2.1).
- `prefers-reduced-motion`: spinner desliga; transições de hover/focus zeram.

---

## 8. Reusos e proibições

### 8.1 Reusos obrigatórios

| Item | Origem | Não recriar |
|---|---|---|
| Tokens de cor, espaço, raio, sombra | `doc/UX/01-design-tokens.md` | Nenhum hex novo. |
| Padrão `input-group` (label, helper, error) | `doc/UX/02-componentes-base.md` §2 / US-001 | Mesma estrutura. |
| `btn btn-primary`, `btn btn-ghost` | `doc/UX/02-componentes-base.md` §1 | Sem variantes inéditas. |
| `alert--success`, `alert--warning` | `doc/UX/02-componentes-base.md` §2.4 | Idêntico ao do login e do verificar-email. |
| Header com voltar | Padrão `login.html` | Mesma estrutura (icon-button + title centralizado). |
| Foco visível, touch target 44×44 | `doc/UX/06-acessibilidade.md` | Sem exceções (badge da câmera é a única atenção — §3.3). |

### 8.2 Proibições

- **Não** oferecer UI de alterar e-mail. O campo é `readonly` com helper explicando. Sem CTA, sem link, sem ícone de edição.
- **Não** colocar dois CTAs no modal de bloqueio. É um CTA único — "Entendi, manter como estava". Sem "Salvar mesmo assim", sem "Tentar de novo".
- **Não** disparar save automático no `@input` (autosave). Salvar é ação explícita; idle ≠ auto-commit. O banner "Pronto, atualizamos" só faz sentido se foi uma ação.
- **Não** mostrar contador de caracteres no apelido (`5/20`). Helper textual cobre. Contadores ao vivo são padrão de Twitter, não de perfil ASD.
- **Não** usar coral no botão Descartar ou no Cancelar do sheet. Coral é primário.
- **Não** substituir o action sheet por um popover ou menu suspenso desktop-style. Sheet bottom é o padrão mobile que adolescente reconhece (Instagram, WhatsApp). Em desktop o mesmo sheet aparece centralizado mas continua "bottom-aligned" visualmente — não vira modal.
- **Não** revelar texto técnico em microcopy.
- **Não** confirmar "tem certeza?" ao descartar — o botão é claro, o gesto é reversível (Júlia pode re-editar). Confirmações erodem agência.
- **Não** auto-fechar o sheet/modal ao perder foco do browser ou trocar de aba. Fechar é ação explícita (Escape, click no overlay no sheet, ou CTA no modal).

---

## 9. Estados, validações e bordas

| Cenário | Comportamento esperado |
|---|---|
| Júlia abre `/perfil` pela 1ª vez | Estado `idle`; campos preenchidos com `nickname`, `birthDate`, `email` atuais; foto = `photoUrl` atual ou inicial do apelido |
| Júlia altera apelido para 2 caracteres | Erro inline vermelho; save bar visível mas botão Salvar `disabled` |
| Júlia digita 21 caracteres | `maxlength="20"` do HTML impede o 21º; mas se vier por paste, validação mostra "Máximo 20 caracteres." |
| Júlia troca a foto e clica Salvar sem mexer em mais nada | Salva normalmente; banner success aparece; baseline da foto atualiza |
| Júlia clica Descartar com mudanças em todos os 3 campos | Todos voltam ao baseline; save bar some; `errors = {}` |
| Júlia muda a data para alguém de 12 anos | Inline neutro durante digitação; ao clicar Salvar, modal de bloqueio aparece; CTA único reverte a data |
| Júlia muda a data para alguém de 18 anos | Mesmo comportamento — modal de bloqueio (faixa 13–17 obrigatória no MVP) |
| Júlia muda apelido + data inelegível | Click em Salvar → modal abre, data reverte, **apelido continua em estado dirty** (a edição do apelido **não** é perdida) |
| Júlia abre o sheet de foto e clica fora | Sheet fecha sem alterar foto |
| Júlia pressiona Escape no sheet ou no modal | Sheet fecha; modal fecha sem reverter a data (precisa do CTA explícito). **Decisão:** Escape no modal não deveria reverter — apenas fechar. Em produção, considerar se Escape no modal também reverte (consistência com o CTA) ou se Escape apenas fecha (mantendo a data "dirty"). Recomendação: **Escape reverte e fecha**, igual ao CTA, para evitar estado inconsistente. |
| Júlia tenta editar o e-mail (clica no input read-only) | Input recebe foco mas não aceita entrada; visualmente "morto"; helper explica por quê. Em algumas plataformas o teclado abre — aceitável, ele não escreve. |
| Sessão expira durante edição e Júlia clica Salvar | POST retorna 401/302 para `/login` (Spring Security padrão). Mudanças locais são perdidas — aceitável; rebote acontece raramente |
| Viewport < 320px | Layout não suportado oficialmente (RNF-COMP-04 mínimo 320). Em emergência (folding phone fechado), o card encolhe e o avatar 112×112 ainda cabe |
| `prefers-reduced-motion: reduce` | Spinner sem rotação; transições zeram |

---

## 10. Tokens consumidos

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da página, fade do save bar |
| `--color-surface` | Header, inputs, sheet, modal, anel branco do avatar badge |
| `--color-surface-muted` | Hover de botão ghost, fundo do input read-only, hover do sheet item |
| `--color-divider` | Borda inferior do header |
| `--color-border` | Borda dos inputs editáveis em repouso |
| `--color-border-strong` | Borda dos inputs em hover |
| `--color-text-display` | Título do header, apelido no avatar block, dígitos em campos, título do modal |
| `--color-text-body` | Labels, helper de modal, botão ghost, sheet item |
| `--color-text-muted` | Helper de inputs, e-mail no avatar, ícones decorativos, item "Cancelar" do sheet, texto read-only |
| `--color-primary-100` | Fundo do avatar (sem foto) |
| `--color-primary-700` | Letra inicial no avatar (sem foto), texto de banner info |
| `--color-primary` (`500`) | Fundo do badge da câmera, fundo do botão Salvar |
| `--color-primary-hover` (`600`) | Hover do botão Salvar e do badge |
| `--color-primary-active` (`700`) | Active do botão Salvar |
| `--color-on-primary` | Ícone do badge, texto do botão Salvar |
| `--color-focus-ring` | Anel de foco padrão |
| `--color-success-100` / `700` | Banner "Pronto, atualizamos…" |
| `--color-warning-100` / `700` | Ícone do modal de bloqueio |
| `--color-danger-700` | Borda de input com erro, texto de mensagem de erro, item "Tirar foto" do sheet |
| `--color-neutral-200` | Handle do sheet |
| `--font-display` | Inicial do avatar, apelido, título do modal |
| `--font-sans` | Restante do texto |
| `--text-xs` | Helper, erro inline |
| `--text-sm` | E-mail no avatar, labels, banners, texto do modal, sheet itens secundários |
| `--text-base` | Body geral, botões, sheet itens, input |
| `--text-xl` | Apelido no avatar, título do modal |
| `--text-3xl` | Inicial dentro do avatar |
| `--space-1` ... `--space-12` | Diversos espaçamentos verticais e padding |
| `--radius-md` | Inputs, botão icon-button, sheet items, modal icon-internal |
| `--radius-lg` | Banners |
| `--radius-xl` | Sheet, modal, ilustração de bloqueio |
| `--radius-full` | Avatar, badge, botões pill, handle do sheet |
| `--shadow-sm` | Avatar |
| `--shadow-md` | (reserva pra cards futuros se a tela ganhar seções) |
| `--shadow-lg` | Sheet, modal |
| `--shadow-focus` | Foco padrão |
| `--duration-fast` · `--ease-out-soft` | Transições de hover e estado |
| `--z-sticky` | Header |
| `--z-overlay` | Overlay de sheet e modal |

---

## 11. Padrões novos (candidatos a virar componente em chore-ux-003)

Esta tela introduz **5 padrões novos** que não estavam catalogados em `02-componentes-base.md`. Quando uma chore-ux for aberta para expandir o design system, considerar:

1. **`avatar-block`** — avatar grande (112×112) com badge sobreposto + apelido + e-mail. Aplicável em `/perfil` (US-009 e US-010), tela de "Quem é você?" pós-login, header do painel do responsável.
2. **`sheet`** (bottom sheet) — overlay com sheet alinhado ao fim do viewport, handle visual, itens com ícone + texto. Aplicável em todas as ações de "trocar/escolher" em mobile (trocar foto, escolher dia da trilha, etc.).
3. **`save-bar`** sticky com gradient-fade — barra de ações no rodapé que só aparece quando há mudanças. Aplicável em qualquer form de edição.
4. **`modal`** centralizado (`alertdialog`) com ícone topo + título + texto + CTA único — variante "porta fechada com saída educada". Distinta do sheet (que é seleção de opção) e do modal de confirmação (que tem 2 CTAs). Aplicável em bloqueios de regra de negócio.
5. **`input--readonly`** com cadeado interno e fundo muted — variante visual do input padrão. Aplicável em qualquer campo "credencial visível mas imutável" no futuro (e-mail, ID externo, etc.).

---

## 12. Pendências de design

Nenhuma bloqueante. Pontos abertos:

1. **CA-2 da US-009 (troca de e-mail)** — explicitamente **fora do MVP** por decisão de produto (Dioni, 2026-05-27). Se algum dia voltar, requer: (a) novo fluxo de verificação do novo endereço, (b) política sobre login durante o período de transição, (c) atualização da memória `email-imutavel-apos-cadastro`, (d) revisão do helper de e-mail desta tela.
2. **Erro de save server-side** — protótipo não cobre. Em produção, banner `alert--error` no topo com `"Não consegui salvar agora. Tenta de novo em alguns segundos."` Estado `error` deve voltar a `dirty` (não a `saved`).
3. **Pipeline de upload de foto** — controle de tamanho (max 5MB), tipo (jpeg/png/webp), recorte para 1:1, compressão antes do POST. Fora deste doc; entra no spec de implementação.
4. **Avatar badge em viewport pequena** — em < 360px o badge 36×36 pode ficar apertado. Considerar aumentar para 40×40 ou estender área clicável via padding invisível.
5. **Persistência de "última edição feita"** — desejável mostrar "Atualizado há 3 dias" abaixo do e-mail? Fora do escopo da US-009; trazer só se métrica de produto mostrar valor.

---

## Referências cruzadas

- Protótipo executável: [`doc/UX/prototypes/perfil-adolescente-editar.html`](prototypes/perfil-adolescente-editar.html)
- Tokens visuais: `doc/UX/01-design-tokens.md`
- Componentes base (botão, alert, input): `doc/UX/02-componentes-base.md`
- Acessibilidade: `doc/UX/06-acessibilidade.md`
- Regra "e-mail é credencial imutável" (mesmo princípio aplicado em US-006): [`us-006-spec.md`](us-006-spec.md) §1.3 · memória do projeto [`email-imutavel-apos-cadastro.md`](../../.claude/projects/-Users-dionia-oliveira-sources-atrilha/memory/email-imutavel-apos-cadastro.md)
- US irmã (perfil do responsável): US-010 (não tem spec UX ainda — quando vier, deve seguir o mesmo padrão visual desta)
- PRD §11.5 — LGPD, direito de correção
- PRD §13 — US-009
