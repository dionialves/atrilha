# Agentes locais — Scout + Arquiteto + Codificador + Revisor (opencode)

> **Project-agnostic.** Esta pasta `.opencode/` é template portável entre projetos.
> Convenções de stack, comandos de teste, restrições de compliance e áreas
> off-limits **devem** estar no `AGENTS.md` (raiz do repo) — os agentes consultam
> ele em runtime e não embutem nada project-specific.

> **⚠️ Boilerplate obrigatório no `AGENTS.md` raiz.** Sem a seção "Subagent Routing
> (opencode)", a sessão principal tenta fazer o trabalho dos subagents diretamente
> e o pipeline quebra. Ao portar `.opencode/` para outro repo, copie essa seção.

Fluxo de desenvolvimento assistido por LLM rodando 100% local via **opencode**
(`https://opencode.ai`) + **LM Studio**. O LLM decide *o quê* fazer e *se aprova*;
o *como* do Git é mecânico, em shell scripts determinísticos em `.opencode/scripts/`.

## Modelo de trabalho (difere do `.qwen`)

**`1 demanda = 1 brief = 1 Issue-resumo = N specs de subtask = 1 worktree = 1 branch = 1 PR`.**

Uma User Story grande **não** vira N PRs. Vira **N subtasks dentro de uma única
worktree**, revisadas uma a uma; cada subtask aprovada é 1 commit squashed local
(sem push). Push da branch + abertura do PR único acontecem **só no fim**, quando
todas as subtasks foram aprovadas.

> **No planejamento**, demanda grande é fatiada pelo scout em **N briefs de subtask**
> (`<CODE>-a.md`, `<CODE>-b.md`, …) + um `<CODE>-slicing.md`, numa única passada
> (Tier 2). Isso fatia só o *planejamento*: o arquiteto consolida tudo em 1
> Issue-resumo e a entrega continua sendo 1 worktree = 1 PR. Demanda pequena (Tier 1)
> = 1 brief `<CODE>.md`.

```
humano descreve a demanda
   │
   ▼
[scout]        investiga (frugal) → mede tamanho (§4) e escreve:
   │             Tier 1 (pequena): .opencode/briefs/<CODE>.md
   │             Tier 2 (grande):  .opencode/briefs/<CODE>-slicing.md
   │                              + <CODE>-a.md, <CODE>-b.md, … (1 brief por subtask)
   ▼
[arquiteto F1] lê o(s) brief(s) → consolida em 1 Issue-resumo (visão geral + lista a/b/c)
   │
   ▼
[arquiteto F2] escreve 1 spec por subtask em .opencode/tasks/<CODE>-<letra>.md
   │
   ▼
[codificador]  start_us <CODE> → 1 worktree; implementa subtask a subtask
   │           finish_task <CODE>-<letra>  (test runner verde + SUMMARY)
   ▼
[revisor]      load_review <CODE>-<letra> → audita 4 camadas
   │              ├─ approve <CODE>-<letra>  → 1 commit LOCAL (sem push)
   │              └─ reject  <CODE>-<letra>  → REVIEW (volta ao codificador)
   │           (repete por subtask)
   ▼
[revisor]      open_pr <CODE>  (só com TODAS aprovadas) → push + 1 PR DRAFT
   ▼
humano revisa PR draft → ready → merge → Issue fecha via Closes #N
```

> **Planejamento em duas fases** (Scout → Arquiteto): o modelo do Arquiteto
> (`qwen3.6-27b-mlx`) é mais preciso em decisão fina mas tem janela apertada
> (~68k). O Scout (`qwen3.6-35b-a3b`, janela maior) faz a coleta exaustiva e
> entrega um brief compacto. Ambas as fases são opcionais: o humano pode criar a
> Issue e os specs à mão e pular para o Codificador.

## Como o opencode lida com agentes

Os papéis são **subagents** (`mode: subagent`) em `.opencode/agent/*.md`. A sessão
principal **delega** nomeando o agente na mensagem:

```
opencode
> Use o codificador para implementar US-042
```

Cada subagent carrega o system prompt do seu `.md` e o modelo pinado no frontmatter
(`model: lmstudio/qwen3.6-35b-a3b`, etc.). Há também o comando
`/orquestrar-pipeline-us US-042` que mostra a sequência canônica de invocações.

## Estrutura

```
.opencode/
├── README.md                   # este arquivo
├── .gitignore                  # ignora worktrees/, tmp/, node_modules
├── agent/
│   ├── scout.md                # subagent Scout       (lmstudio/qwen3.6-35b-a3b)
│   ├── arquiteto.md            # subagent Arquiteto   (lmstudio/qwen3.6-27b-mlx) — 2 fases
│   ├── codificador.md          # subagent Codificador (lmstudio/qwen3.6-35b-a3b)
│   └── revisor.md              # subagent Revisor     (lmstudio/qwen3.6-35b-a3b)
├── command/
│   └── orquestrar-pipeline-us.md   # guia a sequência de invocações
├── scripts/                    # scripts determinísticos (self-contained)
│   ├── _common.sh              #  helpers: paths, resolve issue, tip ref, slug
│   ├── _project.sh             #  auto-detecção do comando de teste
│   ├── validate_brief.sh       #  [Scout]      lint do brief
│   ├── validate_tasks.sh       #  [Arquiteto]  lint dos specs de subtask
│   ├── create_issue.sh         #  [Arquiteto F1] brief + body → 1 Issue-resumo
│   ├── start_us.sh             #  [Codificador] demanda → 1 worktree + branch + tip ref
│   ├── finish_task.sh          #  [Codificador] test runner + SUMMARY por subtask
│   ├── load_review.sh          #  [Revisor]    dossiê read-only por subtask
│   ├── approve.sh              #  [Revisor]    squash LOCAL da subtask (sem push)
│   ├── reject.sh               #  [Revisor]    REVIEW, preserva worktree
│   └── open_pr.sh              #  [Revisor]    push + 1 PR DRAFT (fim da demanda)
├── briefs/                     # handoff Scout → Arquiteto (ver briefs/README.md)
├── tasks/                      # specs de subtask (ver tasks/README.md)
├── tmp/                        # bodies/SUMMARY/REVIEW efêmeros (gitignored)
└── worktrees/                  # 1 por demanda, nomeada pelo <CODE> (gitignored)
```

## Setup

### 1. Instalar opencode
```bash
brew install sst/tap/opencode      # ou: npm i -g opencode-ai
opencode --version
```

### 2. Subir o LM Studio Server
- Aba **Developer** → **Start Server** (porta `1234`).
- **JIT loading ON** e **"Keep models loaded" OFF** (ou máx. 1): só cabe **um**
  modelo grande por vez na RAM; o JIT troca automaticamente quando o opencode pede
  um `id` diferente do carregado.
- **`n_ctx` do modelo carregado deve casar com o `limit.context` do `opencode.json`.**
  Se o servidor subir com `n_ctx` menor que o cliente declara, ele estoura o budget
  e dropa a conexão no meio da geração.
  - `qwen3.6-35b-a3b` → **n_ctx ≥ 262144** (Scout + Codificador + Revisor).
  - `qwen3.6-27b-mlx` → **n_ctx ≥ 68000** (Arquiteto — janela apertada por design).

Validar: `curl -s http://localhost:1234/v1/models | jq '.data[].id'`

### 3. Provider configurado em `opencode.json` (raiz)
Declara o provider `lmstudio` (via plugin `opencode-lmstudio`) apontando para
`http://127.0.0.1:1234/v1` e os dois modelos. Sem wrapper de timeout: o opencode é
baseado em Bun, não sofre do `bodyTimeout` hardcoded do undici/Node que afligia o `.qwen`.

### 4. Ferramentas auxiliares
```bash
brew install gh && gh auth login
```

### 5. Verificar scripts
```bash
ls -l .opencode/scripts                 # 11 scripts
bash .opencode/scripts/start_us.sh      # sem args = mostra "uso: start_us <US_CODE>"
```

## Uso

> Nomeie o agente explicitamente na primeira mensagem. Frases ambíguas como
> "planeje a US-042" podem rotear para o agente errado. Use as frases canônicas.

```bash
cd <raiz-do-repo>
opencode

# Fase 1 — investigação (opcional)
> Use o scout para preparar o brief de US-042

# Fase 2 — Issue-resumo (opcional)
> Use o arquiteto para gerar a Issue de US-042

# Fase 2b — specs de subtask
> Use o arquiteto para detalhar as subtasks de US-042

# Implementação (1 worktree, subtask a subtask)
> Use o codificador para implementar US-042

# Revisão por subtask (repita a/b/c…)
> Use o revisor para auditar a subtask US-042-a

# Fechamento (só com TODAS aprovadas)
> Use o revisor para abrir o PR de US-042
```

Atalho: `/orquestrar-pipeline-us US-042` mostra a sequência + o estado atual da demanda.

### Após PR criado
1. Humano abre o PR, confere o diff, converte para **Ready for review**, mergeia.
2. Issue fecha via `Closes #<N>`.
3. Atualizar docs off-limits aos agentes (changelog, release notes — ver `AGENTS.md`).
4. Limpar: `git worktree remove .opencode/worktrees/<CODE>` e, opcional, `git branch -D <branch>`.

## Notas

- **PR sempre draft** — rede contra o Revisor local aprovar algo verde mas errado.
- **Sessões serializadas (1 modelo por vez).** Feche a persona anterior antes da
  próxima. Scout/Codificador/Revisor compartilham `qwen3.6-35b-a3b` (troca entre
  eles é zero-cost); só o Arquiteto (`27b-mlx`) dispara swap real (~30–90s).
- **Tip ref por demanda** (`refs/opencode/<code>/tip`): base do squash de cada
  subtask. Inicializado por `start_us` em `origin/main`; avança a cada `approve`.
- **SUMMARY e REVIEW vivem em `.opencode/tmp/`** (fora da worktree) para não sujar
  o squash. `reject` preserva a worktree; o Codificador retoma de onde parou.
- **QA** está no test runner obrigatório de `finish_task`/`load_review`/`approve`
  (verde é trava). Não há subagent QA dedicado.

## Troubleshooting

| Sintoma | Causa provável | Correção |
|---|---|---|
| opencode não acha o modelo | provider/modelo não declarado | conferir `opencode.json` (raiz) e `curl .../v1/models` |
| Subagent não aparece | frontmatter inválido | conferir YAML; `description`/`mode: subagent` obrigatórios |
| Modelo do subagent não muda | `model:` ausente/typo | usar `lmstudio/<id>` (ex.: `lmstudio/qwen3.6-27b-mlx`) |
| Subagent desconecta no meio | `n_ctx` carregado < `limit.context` do `opencode.json` | ajustar `n_ctx` no LM Studio para casar |
| 1ª resposta após trocar de agente trava | LM Studio carregando outro modelo (30–90s) | esperar; não ligar "Keep models loaded" (sem RAM p/ 2) |
| `start_us` não acha a Issue | Issue-resumo não criada ou título fora de `<CODE>: ...` | rodar arquiteto fase 1 antes |
| `open_pr` recusa | subtask sem commit aprovado | rodar `approve` das faltantes antes |
