# US-011 — Sair de todos os dispositivos · UX Spec

**Código:** US-011
**GitHub Issue:** —
**Status:** Proposto (aguardando issue de implementação)
**Depende de:** US-001/US-002/US-003 (sessões existentes) · US-009/US-010 (tela "Meu perfil") · `doc/UX/00-identidade-visual.md` · `doc/UX/01-design-tokens.md` · `doc/UX/02-componentes-base.md` · `doc/UX/06-acessibilidade.md`
**Referências PRD:** RF-E1-12 (SHOULD) · §13 (US-011) · §17 (DoD)
**Protótipo:** [`doc/UX/prototypes/perfil-seguranca.html`](prototypes/perfil-seguranca.html)
**US relacionadas:** [`us-009-spec.md`](us-009-spec.md) · [`us-010-spec.md`](us-010-spec.md) — esta tela é entrada acessada por ambos os papéis
**Escopo deste doc:** **layout** da tela `/perfil/seguranca` (uma sub-página de "Meu perfil") com a ação "Sair de todos os dispositivos" e o modal de confirmação que atende CA-3. **Não cobre** invalidação das sessões no backend (TokenRepository, Spring Security session registry), critérios de segurança ou auditoria. Microcopy em pt-BR. Nenhum elemento depende só de cor.

---

## 1. Princípios

1. **Tela dedicada, não modal solto.** A US-011 dá "Sair de todos os dispositivos" como item de **Meu perfil > Segurança**. Em vez de espremer isso numa subseção da tela de perfil, ganha uma tela própria (`/perfil/seguranca`) — preparada pra crescer com alterar senha, 2FA, sessões individuais (quando saírem do "fora do escopo do MVP"). No MVP a tela tem 1 ação, mas o esqueleto é o futuro.
2. **Idle discreto, ação clara.** O botão da ação destrutiva é **outlined danger** (texto e borda `--color-danger-700`, fundo transparente). Não puxa atenção como um CTA primário, mas a cor sinaliza "isso desconecta coisas". O CTA primário (coral) aparece **só dentro do modal**, depois da confirmação consciente.
3. **Confirmação é dialog explícito, não toast desfeitível.** A ação é destrutiva (encerra sessões em outros dispositivos — usuário talvez perca trabalho não-salvo lá). Modal `alertdialog` com 2 CTAs (cancelar + confirmar) é o padrão correto. Diferente do bloqueio de idade da US-009 (porta fechada, 1 CTA), esta é decisão reversível-com-custo → 2 CTAs.
4. **CA-3 vira checkbox, não tela separada.** "Exceto se o usuário optar por sair também" não merece um segundo fluxo. Vira um checkbox dentro do modal de confirmação, **desmarcado por default** (segurança ergonômica: a decisão default é a menos disruptiva — manter sessão atual).
5. **Microcopy sem moralismo, sem pânico.** Frase do card explica situações concretas ("perdeu o celular, logou em algum lugar e esqueceu") sem dramatizar ("Sua conta pode estar comprometida!"). Tom: pragmático e tranquilizador.
6. **Sem listar dispositivos individualmente** — está explicitamente fora do escopo do MVP (US-011, "Fora do Escopo"). A copy não promete o que a tela não entrega: não diz "Você está logada em 3 dispositivos".

---

## 2. Tela única — `/perfil/seguranca`

**Rota:** `GET /perfil/seguranca` → view `perfil/seguranca`
**Acesso:** qualquer usuário autenticado (`role=ADOLESCENT` ou `role=RESPONSIBLE`). Mesma tela serve aos dois papéis no MVP — o conteúdo não muda por papel.
**Entrada:** vem de uma linha "Segurança" dentro de `/perfil` (US-009 ou US-010). Essa linha de navegação **não está no protótipo da US-009/US-010 atual** — ver §11 (pendências de integração).
**Endpoint da ação:** `POST /perfil/seguranca/sair-todos` com `{ includeCurrent: boolean }`. Detalhes de backend fora deste doc.

### 2.1 Wireframe (mobile 320px — desktop usa o mesmo card centralizado, `max-width: 28rem`)

```
┌────────────────────────────────────┐
│ [<]    Segurança           [    ] │  ← header com voltar para /perfil
├────────────────────────────────────┤
│                                    │
│   [banner success — após save]     │
│                                    │
│   overline · MEU PERFIL            │
│   H1 — Segurança                   │
│   p   — Quando algo não tá certo…  │
│                                    │
│   SESSÕES                          │  ← section title overline
│   ┌──────────────────────────────┐ │
│   │ 🚪  Sair de todos os          │ │
│   │     dispositivos              │ │
│   │     Desconecta todos os       │ │
│   │     outros celulares e        │ │
│   │     computadores onde você…   │ │
│   │                               │ │
│   │     [ Sair de todos os outros ]│ │  ← btn-outline-danger
│   └──────────────────────────────┘ │
│                                    │
└────────────────────────────────────┘

Modal de confirmação (aberto sobre a tela):
┌──────────────────────────────┐
│            [🚪]              │  ← ícone warning
│                              │
│   Sair de todos os           │
│      dispositivos?           │
│                              │
│  Isso desconecta todos os    │
│  celulares e computadores…   │
│                              │
│  ┌──────────────────────┐    │
│  │ ☐ Sair também deste  │    │  ← checkbox (CA-3)
│  │   dispositivo        │    │
│  │   Vai voltar pra…    │    │
│  └──────────────────────┘    │
│                              │
│  [ Sair de todos os outros ] │  ← primary (texto muda se check)
│  [        Cancelar         ] │  ← ghost
└──────────────────────────────┘
```

### 2.2 Estrutura HTML (referência)

```html
<header>  [voltar] · Segurança · [spacer]  </header>

<main class="main">
  <!-- banner success após ação OK (caso usuário NÃO tenha saído daqui) -->
  <div role="status" class="alert alert--success">
    Pronto, outras sessões foram desconectadas. Este dispositivo continua logado.
  </div>

  <div class="intro">
    <p class="overline">Meu perfil</p>
    <h1>Segurança</h1>
    <p>Quando algo não tá certo — perdeu o celular, logou em algum lugar e esqueceu — você resolve aqui.</p>
  </div>

  <section class="section" aria-labelledby="sessions-title">
    <h2 id="sessions-title" class="section-title">Sessões</h2>

    <div class="action">
      <div class="action-header">
        <div class="action-icon">🚪</div>
        <div class="action-text">
          <p class="action-title">Sair de todos os dispositivos</p>
          <p class="action-desc">
            Desconecta todos os outros celulares e computadores onde você tá
            logada agora. Este dispositivo continua até você optar por sair
            também.
          </p>
        </div>
      </div>

      <button type="button" class="btn btn-outline-danger" data-action="open-modal">
        Sair de todos os outros
      </button>
    </div>
  </section>
</main>

<!-- MODAL de confirmação -->
<dialog class="modal-overlay" role="alertdialog" aria-modal="true">
  <div class="modal">
    <div class="modal-icon">🚪</div>
    <h2>Sair de todos os dispositivos?</h2>
    <p>Isso desconecta todos os celulares e computadores onde você está logada agora.
       Você vai precisar entrar de novo neles.</p>

    <label class="modal-check">
      <input type="checkbox" name="includeCurrent" />
      <span>
        Sair também deste dispositivo
        <span class="modal-check-hint">Vai voltar pra tela de login.</span>
      </span>
    </label>

    <form method="post" action="/perfil/seguranca/sair-todos">
      [csrf]
      <button type="submit" class="btn btn-primary">Sair de todos os outros</button>
      <button type="button" class="btn btn-ghost">Cancelar</button>
    </form>
  </div>
</dialog>
```

---

## 3. Especificações visuais

### 3.1 Layout do container

| Elemento | Token / valor |
|---|---|
| Fundo da página | `--color-bg` |
| `<main>` padding | `--space-8` topo · `--space-4` laterais · `--space-10` baixo |
| `max-width` | `28rem` |
| Centralização | `margin: 0 auto` |

### 3.2 Header

Idêntico à US-009 e US-010 (grid 3 colunas com voltar + título + spacer). Diferenças:

| Propriedade | Valor |
|---|---|
| Destino do voltar | `/perfil` (US-009 ou US-010, conforme papel) |
| Título | `"Segurança"` |

### 3.3 Intro (overline + H1 + lead)

| Elemento | Valor |
|---|---|
| Overline | `<p class="overline">` · `--text-xs` · uppercase · `--tracking-overline` · `--font-weight-semibold` · `--color-text-muted` · texto: `"Meu perfil"` (sinaliza onde a usuária está) |
| H1 | `<h1>` · `--font-display` · `--text-2xl` · `line-height: 1.15` · `--font-weight-semibold` · `--color-text-display` · texto: `"Segurança"` |
| Lead | `<p>` · `--text-sm` · `--color-text-muted` · texto: `"Quando algo não tá certo — perdeu o celular, logou em algum lugar e esqueceu — você resolve aqui."` |
| Margem inferior do bloco | `--space-6` |

### 3.4 Section "Sessões"

Container que agrupa ações relacionadas. Mesmo padrão `.section` que serve de esqueleto pra futuras seções ("Senha", "2FA", "Sessões ativas" no pós-MVP).

| Propriedade | Valor |
|---|---|
| Background | `--color-surface` |
| Borda | `1px solid --color-divider` |
| Raio | `--radius-lg` |
| Sombra | `--shadow-sm` |
| Overflow | `hidden` (cobre `border-top` dos action items) |
| Margem inferior | `--space-5` |

**Título da section (`.section-title`):**

| Propriedade | Valor |
|---|---|
| Tag | `<h2>` |
| Estilo | `--text-xs` · uppercase · `--tracking-overline` · `--font-weight-semibold` · `--color-text-muted` |
| Padding | `--space-3 --space-5 --space-1` (espaço interno consistente; respiro pequeno antes do 1º item) |
| Texto | `"Sessões"` |

### 3.5 Action item (a linha "Sair de todos os dispositivos")

Padrão de "item de ação" dentro de uma section — pode haver múltiplos no futuro, separados por `border-top`.

| Propriedade | Valor |
|---|---|
| Container | `display: flex; flex-direction: column; gap: --space-3; padding: --space-5` |
| Separador entre items | `.action + .action { border-top: 1px solid --color-divider }` |

**Header do item (`.action-header`):**

| Propriedade | Valor |
|---|---|
| Display | `flex; gap: --space-3; align-items: flex-start` |

**Ícone (`.action-icon`):**

| Propriedade | Valor |
|---|---|
| Dimensões | `40 × 40px` |
| Raio | `--radius-md` (não círculo — diferencia de avatares de pessoa) |
| Fundo | `--color-warning-100` (`#FFF5DC`) |
| Cor | `--color-warning-700` (`#9A5A00`) |
| `flex-shrink` | `0` |
| Glyph | SVG log-out (porta com seta saindo) 20×20, `stroke-width: 2`, `stroke-linecap: round` |
| `aria-hidden` | `"true"` |

**Por que ícone warning (não danger):** a ação é destrutiva-leve (encerra outras sessões — recuperável re-fazendo login), não catastrófica (não apaga conta). Warning amarelo sinaliza "atenção" sem alarme excessivo. O botão é que carrega o coral-vermelho do danger via `btn-outline-danger`.

**Texto do item (`.action-text`):**

| Slot | Estilo |
|---|---|
| Título (`.action-title`) | `<p>` · `--text-base` · `--font-weight-semibold` · `--color-text-display` · `margin-bottom: --space-1` |
| Descrição (`.action-desc`) | `<p>` · `--text-sm` · `--color-text-muted` · `line-height: 1.45` |

**Botão "Sair de todos os outros" (`.btn-outline-danger`):**

| Propriedade | Valor |
|---|---|
| Classe | `.btn.btn-outline-danger` |
| Estilo base | `width: 100%; min-height: --space-12; padding: --space-3 --space-6; --radius-full` (idêntico aos outros pill buttons) |
| Background | `transparent` |
| Cor texto | `--color-danger-700` |
| Borda | `1.5px solid --color-danger-700` |
| Hover | `background: --color-danger-100` |
| Active | mesmo do hover (sem `transform` ou pressed visual extra) |
| Disabled | `opacity: 0.55; cursor: not-allowed` |

**Por que outline e não primary:** o botão idle não deve dominar a tela visualmente. A ação é segura (só dispara após confirmação), mas se for primário coral, o usuário pode clicar acidentalmente. Outline + cor danger = "isso é importante e perigoso, mas requer confirmação". Dentro do modal, o **CTA primário coral aparece** confirmando — aí sim, atenção total.

### 3.6 Banner de sucesso

Aparece **apenas** quando a ação completa **sem** marcar "Sair também daqui". Se marcou, há redirect — não há banner intermediário.

| Propriedade | Valor |
|---|---|
| Posição | Topo do `<main>`, antes da intro |
| Componente | `.alert.alert--success` (padrão do design system) |
| Texto | `"Pronto, outras sessões foram desconectadas. Este dispositivo continua logado."` |
| `role` | `"status"` · `aria-live="polite"` |
| Duração | Transient — `4000ms` (mais longo que o banner de save do US-009 porque a info é mais importante: confirma uma ação de segurança) |

### 3.7 Modal de confirmação

Padrão `alertdialog` centralizado. Mesmo "esqueleto" do modal de bloqueio da US-009, mas com 2 CTAs (não 1) porque a decisão é reversível-com-custo.

| Propriedade | Valor |
|---|---|
| Overlay (`.modal-overlay`) | `position: fixed; inset: 0; z-index: --z-overlay; background: rgba(26,22,20,0.5)`; flex centralizado |
| Padding do overlay | `--space-4` |
| Modal (`.modal`) | `width: 100%; max-width: 24rem; background: --color-surface; --radius-xl; padding: --space-6; --shadow-lg` |
| `role` | `"alertdialog"` |
| `aria-modal` | `"true"` |
| `aria-labelledby` | `"modal-title"` |
| `aria-describedby` | `"modal-text"` |
| Fechar | Click no overlay (`.self`) OU tecla `Escape` OU CTA "Cancelar" — **mas** os 3 caminhos viram no-op se `state === 'loading'` (não cancela durante POST em voo) |

**Ícone (`.modal-icon`):**

Idêntico ao da US-009 §3.8 — `56 × 56`, `--radius-full`, `background: --color-warning-100`, `color: --color-warning-700`, SVG log-out 28×28 centralizado, `margin: 0 auto --space-4`.

**Título e texto:**

| Slot | Estilo |
|---|---|
| Título (`.modal-title`) | `<h2>` · `--font-display` · `--text-xl` · `--font-weight-semibold` · `--color-text-display` · `text-align: center` · `margin-bottom: --space-2` · texto: `"Sair de todos os dispositivos?"` |
| Texto (`.modal-text`) | `<p>` · `--text-sm` · `--color-text-body` · `text-align: center` · `margin-bottom: --space-5` · `line-height: 1.5` · texto literal §4 |

**Checkbox "Sair também daqui" (`.modal-check`):**

| Propriedade | Valor |
|---|---|
| Container | `<label>` envolvendo todo o bloco (touch target completo é clicável) |
| Display | `flex; align-items: flex-start; gap: --space-3; padding: --space-3; --radius-md` |
| Fundo | `--color-surface-muted` (`#EDE8E4`) — destaca como bloco interativo dentro do modal |
| Cursor | `pointer` |
| Margem inferior | `--space-5` |
| Input | `width: 20px; height: 20px; accent-color: --color-primary` (usa accent-color nativo — não substitui o checkbox por SVG no MVP) |
| Texto label | `--text-sm` · `--color-text-body` · `line-height: 1.4` |
| Hint (sub-texto) | `<span class="modal-check-hint">` · `--text-xs` · `--color-text-muted` · `margin-top: 2px` · texto: `"Vai voltar pra tela de login."` |
| Default | **Desmarcado** (segurança ergonômica: ação default é menos disruptiva) |

**Botões do modal (`.modal-actions`):**

| Propriedade | Valor |
|---|---|
| Display | `flex; flex-direction: column; gap: --space-3` |

**CTA primário (texto dinâmico):**

| Estado do checkbox | Texto do botão |
|---|---|
| Desmarcado (default) | `"Sair de todos os outros"` |
| Marcado | `"Sair de todos (inclusive daqui)"` |

| Estado de loading | UI |
|---|---|
| `idle` no modal | Texto + sem spinner |
| `loading` (POST em voo) | Spinner inline + `"Desconectando…"`; `disabled`; botão Cancelar também `disabled` |

**Por que texto dinâmico em vez de "Confirmar":** "Confirmar" é genérico. O texto do botão deve descrever o que **acontece ao clicar**. Se a usuária marcou o check, o botão deve dizer que sim, ela também sai daqui — sem surpresa.

**CTA secundário "Cancelar":**

| Propriedade | Valor |
|---|---|
| Classe | `.btn.btn-ghost` |
| Estilo | Transparente, `--color-text-body`, hover `--color-surface-muted` |
| Ação | Fecha o modal, desmarca o checkbox, volta para `idle` |
| Disabled | `state === 'loading'` |

---

## 4. Microcopy (pt-BR — literal)

| Slot | Texto |
|---|---|
| `<title>` | Segurança |
| Header título | Segurança |
| Botão voltar (`aria-label`) | Voltar |
| Overline | Meu perfil |
| H1 | Segurança |
| Lead | Quando algo não tá certo — perdeu o celular, logou em algum lugar e esqueceu — você resolve aqui. |
| Section title | Sessões |
| Action title | Sair de todos os dispositivos |
| Action description | Desconecta todos os outros celulares e computadores onde você tá logada agora. Este dispositivo continua até você optar por sair também. |
| Botão idle | Sair de todos os outros |
| Modal título | Sair de todos os dispositivos? |
| Modal texto | Isso desconecta todos os celulares e computadores onde você está logada agora. Você vai precisar entrar de novo neles. |
| Checkbox label | Sair também deste dispositivo |
| Checkbox hint | Vai voltar pra tela de login. |
| Modal CTA (check desmarcado) | Sair de todos os outros |
| Modal CTA (check marcado) | Sair de todos (inclusive daqui) |
| Modal CTA (loading) | Desconectando… |
| Modal cancelar | Cancelar |
| Banner success | Pronto, outras sessões foram desconectadas. Este dispositivo continua logado. |

**Vetado:**

- "Sua conta pode estar comprometida", "Atenção: segurança!", "Suspeita de invasão" — pânico desnecessário. A ação serve pra higiene (perdi celular, esqueci de sair) e não pressupõe ataque.
- "Tem certeza?" como título do modal — vazio. O título descreve a ação ("Sair de todos os dispositivos?") e o texto explica o impacto.
- "Esta ação não pode ser desfeita" — falso e desnecessário. Pode ser desfeita: basta logar de novo em cada dispositivo. O custo é login extra, não perda permanente.
- "Logout completo", "Encerrar sessões", "Invalidar tokens" — termos técnicos. Usar "sair", "desconectar".
- "Tokens JWT", "sessão Spring Security", "cookies HTTPOnly" — vazado tecnológico. Usuária não precisa saber.
- Plural genérico "dispositivos" no botão idle ("Sair de todos os dispositivos") — esconde que daqui ela continua logada. Por isso o botão idle diz "**outros**" — honestidade sobre o que ele faz por default.

---

## 5. Estados visuais

| Estado | Trigger | UI |
|---|---|---|
| `idle` (default) | Tela carregada | Card visível, banner success ausente, modal fechado |
| `modal-open` | Click no botão "Sair de todos os outros" | Modal aparece centralizado; foco move para o checkbox (ou pro CTA primário, ver §7) |
| `loading` | Submit do form de confirmação | Modal continua aberto; CTA primário com spinner + "Desconectando…"; ambos botões `disabled`; ESC/click no overlay = no-op |
| `success` (sem signOutHere) | POST retornou OK + checkbox desmarcado | Modal fecha; banner `alert--success` aparece no topo da tela por 4s; volta a `idle` |
| `success` (com signOutHere) | POST retornou OK + checkbox marcado | Modal fecha; navegador redireciona para `/login?event=signed-out-all`. Não há banner intermediário nesta tela porque ela vai sair de cena. A tela `/login` é responsável por exibir o sinalizador "Você saiu de todos os dispositivos" (ver §11 — integração com US-007/login). |
| `error` (futuro) | POST falhou | Banner `alert--error` dentro do modal: `"Não consegui desconectar agora. Tenta de novo em alguns segundos."` Modal continua aberto, checkbox preserva estado. **Não no protótipo** — entra no spec de implementação. |

---

## 6. Comportamento

### 6.1 Abrir modal

Click no botão idle → `showModal = true`, `state = idle`, checkbox preserva `false`. Foco move para o checkbox (ver §7 sobre foco inicial).

### 6.2 Confirmar

```javascript
confirm() {
  if (state === 'loading') return;
  state = 'loading';

  // Em produção: POST /perfil/seguranca/sair-todos com { includeCurrent: signOutHere }
  setTimeout(() => {
    if (signOutHere) {
      // CA-3: sai também daqui → redireciona para /login
      window.location.href = '/login?event=signed-out-all';
    } else {
      showModal = false;
      state = 'success';
      setTimeout(() => state = 'idle', 4000);
    }
  }, 900);
}
```

### 6.3 Cancelar

Click em "Cancelar", ESC ou click no overlay → fecha modal, desmarca checkbox, volta para `idle`. **Bloqueado durante `loading`** — evita request duplicado e sessão fantasma.

### 6.4 Texto do botão muda com o checkbox

Reatividade simples (`x-text` do Alpine):

```javascript
btnText = signOutHere
  ? 'Sair de todos (inclusive daqui)'
  : 'Sair de todos os outros';
```

Quando o usuário marca/desmarca o check, o texto do botão muda em tempo real. Reforça a relação causa-efeito.

### 6.5 Pós-sucesso (sem signOutHere)

Banner aparece por 4s, depois some. Banner é informativo, não acionável. Sessão atual segue ativa sem mudança (nenhum recarregamento de página é necessário).

### 6.6 Pós-sucesso (com signOutHere)

Servidor invalida **todas** as sessões (inclusive a atual). Spring Security detecta cookie inválido no próximo request e redireciona para `/login`. Em produção, é recomendado o **POST handler já fazer o redirect** para `/login?event=signed-out-all`, sem depender de detecção no próximo request — UX mais imediata.

---

## 7. Acessibilidade

- `<header role="banner">`; `<main>` único.
- H1 único na intro; section usa `<h2>` para `"Sessões"` (associada via `aria-labelledby` ao container `.section`).
- Botão idle: `<button type="button">` com texto descritivo. Sem `aria-label` redundante.
- Modal: `role="alertdialog"`, `aria-modal="true"`, `aria-labelledby="modal-title"`, `aria-describedby="modal-text"`.
- Checkbox: `<input type="checkbox">` envolvido por `<label>` (toda a área clicável é o label). Hint (`.modal-check-hint`) é texto adjacente, não `aria-describedby` separado — leitor de tela lê em sequência natural.
- Foco gerenciado:
  - Ao abrir modal → foco move para o **CTA primário** (botão "Sair de todos os outros"). Razão: é a ação principal; usuária que abriu o modal já decidiu, agora confirma. Se ela quiser marcar o check antes, Shift+Tab leva ao checkbox.
  - Ao fechar modal (cancelar ou sucesso) → foco volta para o **botão idle** que abriu o modal.
- Tab order dentro do modal: checkbox → CTA primário → cancelar → overlay (fim). Loop com Shift+Tab.
- Tecla `Escape` fecha o modal (exceto durante loading).
- `state === 'loading'`: CTA primário com `aria-busy="true"`.
- Banner success: `role="status"` (não interrompe; leitor anuncia educadamente).
- Touch target: tudo ≥ 44×44 (`--space-11`).
- Contraste calculado:
  - Botão `btn-outline-danger` texto `danger-700` (`#C8362B`) sobre `surface` (`#FFFFFF`) → **6.12:1** (AA — também AA Large).
  - Botão `btn-outline-danger` em hover (`danger-700` sobre `danger-100` `#FDECEA`) → **5.40:1** (AA).
  - Ícone warning `warning-700` (`#9A5A00`) sobre `warning-100` (`#FFF5DC`) → **6.74:1** (AA).
  - Modal CTA primário `on-primary` (branco) sobre `primary` (`#F25C54`) → ≥ **4.5:1** (chore-ux-001 §2.1).
  - Checkbox accent coral (`primary`) sobre `surface-muted` (`#EDE8E4`) → contraste do tick branco interno depende do browser; em iOS/Chrome ≥ 4.5:1 (browser default).
- `prefers-reduced-motion: reduce`: spinner sem rotação; transições zeram.

---

## 8. Reusos e proibições

### 8.1 Reusos obrigatórios

| Item | Origem | Não recriar |
|---|---|---|
| Tokens de cor, espaço, raio, sombra | `doc/UX/01-design-tokens.md` | Nenhum hex novo. |
| `btn btn-primary`, `btn btn-ghost` | `doc/UX/02-componentes-base.md` §1 | Sem variantes inéditas no primário ou ghost. |
| `alert--success` | `doc/UX/02-componentes-base.md` §2.4 | Idêntico ao do verify-email, US-009, US-010. |
| Header com voltar | Padrão `login.html` / US-009 / US-010 | Mesma estrutura. |
| Modal `alertdialog` | US-009 §3.8 | Mesmo esqueleto (overlay + modal + ícone topo + título + texto + ações). Diferença: 2 CTAs em vez de 1. |
| Padrão `intro` (overline + H1 + lead) | `login.html` `comecar.html` | Mesma estrutura. |
| Foco visível, touch target 44×44 | `doc/UX/06-acessibilidade.md` | Sem exceções. |

### 8.2 Padrões novos desta US (registrar para chore-ux futura)

- **`section` + `action`** — section card com section title overline + lista de action items. Cada action item tem ícone colorido + título + descrição + botão. Aplicável em qualquer tela de configurações (perfil, preferências, segurança no pós-MVP).
- **`btn-outline-danger`** — botão pill outlined com cor `danger-700` em texto e borda, fundo transparente. Aplicável em qualquer ação destrutiva-leve onde o idle deve ser discreto e a confirmação está em modal.
- **Modal `alertdialog` com 2 CTAs + texto dinâmico no primário** — variante do modal de bloqueio da US-009. Aplicável em qualquer confirmação onde uma opção (checkbox/radio) muda o impacto do CTA.
- **Checkbox-em-bloco** — `<label>` envolvendo input + texto + hint, com fundo `surface-muted` e padding. Aplicável em qualquer opção secundária dentro de modal ou form.

### 8.3 Proibições

- **Não** listar dispositivos individualmente. Está explicitamente fora do escopo do MVP (US-011, "Fora do Escopo"). Se a copy disser "Você está logada em N dispositivos", já passa expectativa errada.
- **Não** mostrar contagem de sessões ativas no idle ("Você tem 3 sessões"). Mesma razão.
- **Não** usar CTA primário coral no idle. Outline é o ponto de fricção saudável.
- **Não** auto-fechar o modal ao perder foco do browser (trocar de aba). Fechar é ação explícita.
- **Não** mostrar lista de horários de last_access ("Última atividade: 3h atrás"). Estímulo a paranoia sem ferramenta pra agir.
- **Não** mostrar a senha atual no modal pedindo re-autenticação. RF-E1-12 não exige re-auth — basta confirmação modal. (Em ataques onde alguém pegou o celular destravado, exigir senha de novo ajudaria, mas isso entra em US futura — fora do MVP.)
- **Não** disparar a ação ao clicar Enter no checkbox. Enter no checkbox alterna; Enter no botão é que confirma. Default do browser está correto.
- **Não** revelar texto técnico no microcopy.
- **Não** transformar a tela de perfil principal (US-009/US-010) num overlay com toda a Segurança dentro. Mantém tela dedicada.

---

## 9. Estados, validações e bordas

| Cenário | Comportamento esperado |
|---|---|
| Usuária abre `/perfil/seguranca` pela 1ª vez | Estado `idle`; card visível; sem banners |
| Usuária clica "Sair de todos os outros" | Modal abre; foco no CTA primário; checkbox desmarcado |
| Usuária pressiona ESC com modal aberto | Modal fecha; volta para `idle` |
| Usuária clica fora do modal (no overlay) | Modal fecha; volta para `idle` |
| Usuária clica Cancelar | Modal fecha; volta para `idle`; checkbox desmarca |
| Usuária marca o checkbox e clica Cancelar | Modal fecha; checkbox volta a `false` (estado limpo na próxima abertura) |
| Usuária marca o checkbox; texto do botão muda em tempo real | "Sair de todos (inclusive daqui)" |
| Usuária confirma com check desmarcado | Loading 900ms; banner success aparece por 4s; sessão atual continua válida |
| Usuária confirma com check marcado | Loading 900ms; redirect imediato para `/login?event=signed-out-all`. Em produção, o handler `/login` deve renderizar uma mensagem `"Você saiu de todos os dispositivos."` no topo |
| Usuária tenta ESC ou click no overlay durante `loading` | No-op — bloqueado para evitar request duplicado |
| Usuária clica Confirmar duas vezes rapidamente | Segundo click ignorado (`if (state === 'loading') return`) |
| POST falha (rede/servidor) | Banner `alert--error` dentro do modal; checkbox preserva estado; modal continua aberto. (Não no protótipo — flagged §11) |
| Sessão atual expira durante a tela aberta e usuária clica Confirmar | POST → 401 → redirect para `/login` (Spring padrão). Aceitável — ela tinha que logar de novo de qualquer jeito |
| Usuária navega para outra aba e volta horas depois | Estado preservado em memória (Alpine); se a sessão expirou no servidor, o próximo POST falha graciosamente |
| Viewport < 320px | Modal `max-width: 24rem` com `padding: --space-4` do overlay garante respiro; em viewport extrema, padding lateral colapsa pra `--space-2` |
| `prefers-reduced-motion: reduce` | Spinner sem rotação; transições zeram |

---

## 10. Tokens consumidos

| Token | Onde aparece |
|---|---|
| `--color-bg` | Fundo da página |
| `--color-surface` | Header, section, modal |
| `--color-surface-muted` | Hover de botão ghost, fundo do checkbox-block |
| `--color-divider` | Borda inferior do header, borda da section, separador entre actions |
| `--color-border` | (reserva — não usado diretamente na US-011) |
| `--color-text-display` | Título do header, H1, action title, modal title |
| `--color-text-body` | Modal text, checkbox label, botão ghost |
| `--color-text-muted` | Overline, lead, section title, action description, checkbox hint |
| `--color-primary` | Fundo do CTA primário do modal, `accent-color` do checkbox |
| `--color-primary-hover` / `active` | Estados do CTA primário |
| `--color-on-primary` | Texto do CTA primário |
| `--color-focus-ring` | Anel de foco padrão |
| `--color-success-100` / `700` | Banner success |
| `--color-warning-100` / `700` | Ícone do action item, ícone do modal |
| `--color-danger-100` / `700` | `btn-outline-danger` (texto, borda, hover bg) |
| `--font-display` | H1, modal title |
| `--font-sans` | Restante |
| `--text-xs` | Overline, section title, hint do checkbox |
| `--text-sm` | Lead, action description, modal text, checkbox label, banner |
| `--text-base` | Body, botões, action title |
| `--text-xl` | Modal title |
| `--text-2xl` | H1 |
| `--space-1` ... `--space-12` | Diversos espaçamentos e padding |
| `--radius-md` | Action icon, checkbox-block, icon-button do header |
| `--radius-lg` | Section, banner |
| `--radius-xl` | Modal |
| `--radius-full` | Botões pill, modal icon, anel de foco |
| `--shadow-sm` | Section |
| `--shadow-lg` | Modal |
| `--shadow-focus` | Foco padrão |
| `--duration-fast` · `--ease-out-soft` | Transições |
| `--z-sticky` | Header |
| `--z-overlay` | Overlay do modal |

---

## 11. Integração com outras US e pendências

Esta US-011 **introduz uma sub-página** dentro de "Meu perfil" — o que exige integração com a US-009 e a US-010 que **ainda não foram implementadas**.

### 11.1 Integração necessária (acoplamento)

- **US-009 (perfil do adolescente) e US-010 (perfil do responsável)** precisam ganhar uma entrada para esta tela. Proposta: adicionar uma section "Segurança" abaixo dos forms, com um único item navegável que leva a `/perfil/seguranca`.
- Variação para Carlos (US-010): seu link "Segurança" aparece **abaixo** do card de vinculações. Variação para Júlia (US-009): aparece **abaixo** do form, antes do save bar — mas atenção, o save bar é sticky e pode encobrir; alternativa é colocar a section entre o form e o save bar, mas o save bar só aparece quando dirty, então não conflita.
- **Decisão pendente:** Adicionar essa entrada **junto** desta US-011 (escopo cresce) ou em uma chore separada de "integração de perfil + segurança". Recomendação: junto desta US — sem isso, a tela existe mas é inacessível.

### 11.2 Integração com `/login` (US-007/US-008)

Quando o usuário marca "Sair também daqui" e confirma, ele é redirecionado para `/login?event=signed-out-all`. A tela `/login` precisa **reconhecer esse parâmetro** e mostrar uma mensagem no topo: `"Você saiu de todos os dispositivos. Entra de novo aqui."`.

Esta integração não está coberta no protótipo `login.html` atual (o protótipo cobre `event=logged-out` simples). **Tarefa proposta:** estender o `login.html` para reconhecer `event=signed-out-all` (não viola escopo da US-007/US-008, é só mais um estado de info).

### 11.3 Pendências de design

1. **Erro de POST** — não coberto no protótipo. Em produção, banner `alert--error` dentro do modal com mensagem `"Não consegui desconectar agora. Tenta de novo em alguns segundos."` Estado modal continua aberto, checkbox preserva.
2. **Mensagem na tela `/login` após signed-out-all** — §11.2.
3. **Confirmação por senha** — RF-E1-12 não exige; fora do MVP. Se métricas de produto mostrarem que usuários estão clicando por engano e perdendo trabalho em outros dispositivos, considerar adicionar prompt de senha no modal (fluxo similar a Google/GitHub).
4. **Lista de sessões individuais (CA fora do escopo)** — quando entrar no roadmap, a tela cresce com section adicional "Outras sessões" mostrando cada device + última atividade + botão "Sair desta". Padrão `vinc-card` da US-010 é base reaproveitável.
5. **Logout único do dispositivo atual** ("Sair") — não está nesta US. Fluxo de logout simples (sem afetar outras sessões) é parte do menu principal/header, **fora desta tela**. Esta tela é só pra logout de **todos**.
6. **Frequência da ação** — se métricas mostrarem que usuárias usam isto raramente (esperado), considerar mover o card pra dentro de "Configurações avançadas" no futuro. No MVP, fica em destaque porque é o **único item** da seção Sessões.

---

## Referências cruzadas

- Protótipo executável: [`doc/UX/prototypes/perfil-seguranca.html`](prototypes/perfil-seguranca.html)
- US irmãs (perfil principal): [`us-009-spec.md`](us-009-spec.md) · [`us-010-spec.md`](us-010-spec.md)
- Tokens visuais: `doc/UX/01-design-tokens.md`
- Componentes base: `doc/UX/02-componentes-base.md`
- Acessibilidade: `doc/UX/06-acessibilidade.md`
- Padrão de modal `alertdialog`: [`us-009-spec.md`](us-009-spec.md) §3.8 (variante 1 CTA — bloqueio)
- PRD §13 — US-011
- PRD RF-E1-12 — SHOULD (logout de todos os dispositivos)
