# Agentes locais do atrilha — Arquiteto + Codificador + Revisor (Qwen Code)

Adaptação do fluxo do `.opencode/` para o **Qwen Code CLI** (`https://github.com/QwenLM/qwen-code`).
Mesmo ciclo, mesmos scripts determinísticos, mesma filosofia: o LLM decide *o quê*
fazer e *se aprova*; o *como* do Git é mecânico via shell scripts.

> Os scripts em `.qwen/scripts/` são um **symlink para `.opencode/scripts/`** —
> assim qualquer evolução de fluxo aplica-se aos dois runners sem duplicação.

## Diferença de paradigma vs OpenCode

| Aspecto | OpenCode | Qwen Code |
|---|---|---|
| Troca de papel | `/agent <nome>` muda a persona da sessão (`mode: primary`) | Subagents são **delegados** pela sessão principal (`"use o codificador para..."`) ou invocados explicitamente por nome |
| Config de provider | Global em `~/.config/opencode/opencode.json` | Projeto em `.qwen/settings.json` (este repo) — pode ser sobrescrito por `~/.qwen/settings.json` |
| Modelo por agente | Cada sessão usa o modelo escolhido com `/model` | Cada subagent pode **pinar** seu modelo no frontmatter (`model: openai:<id>`) |
| Onde acha os agentes | `.opencode/agents/*.md` | `.qwen/agents/*.md` (precedência sobre `~/.qwen/agents/`) |
| Scripts | `.opencode/scripts/*.sh` | `.qwen/scripts/*.sh` (symlink para `.opencode/scripts/`) |

## Filosofia (idêntica ao `.opencode/`)

```
Dioni descreve a demanda em linguagem natural
   │
   ▼
[subagent arquiteto]      investiga código → projeta plano TDD → gh issue create
   │                                                                │
   ▼                                                                ▼
[subagent codificador]    start_task → implementa → finish_task   (mvn test verde + SUMMARY.md)
   │
   ▼
[subagent revisor]        load_review → audita 3 camadas → approve | reject
   │                                       │         │
   │                                  PR DRAFT   REVIEW.md (volta ao Codificador,
   ▼                                                MESMA worktree)
Dioni revisa PR draft → ready → merge → Issue fecha via Closes #N
```

> O **Arquiteto** continua opcional: Dioni pode criar Issues à mão (papel humano) e pular direto para o Codificador.

**Uma task = uma worktree isolada = uma branch (`<tipo>/<N>-<slug>`) = um PR = um commit squash.** A worktree e a branch são criadas juntas por `start_task.sh`; o Revisor faz o squash + push + abertura do PR via `approve.sh`. As worktrees vivem em `.opencode/worktrees/` (compartilhadas com o runner OpenCode — não há diretório separado para qwen).

## Estrutura

```
atrilha/
├── AGENTS.md                       # contexto do projeto (auto-carregado pelo Qwen Code)
├── .opencode/
│   ├── ...                         # runner OpenCode (independente)
│   └── scripts/                    # FONTE ÚNICA dos scripts determinísticos
│       ├── start_task.sh
│       ├── finish_task.sh
│       ├── load_review.sh
│       ├── approve.sh
│       └── reject.sh
└── .qwen/
    ├── README.md                   # este arquivo
    ├── .gitignore                  # ignora worktrees, node_modules etc.
    ├── settings.json               # provider LM Studio + modelos + auth
    ├── scripts/  →  ../.opencode/scripts/   (symlink: fonte única)
    └── agents/
        ├── arquiteto.md            # subagent Arquiteto   (model: inherit — usa o que /model definir)
        ├── codificador.md          # subagent Codificador (model: openai:qwen3.6-35b-a3b-mlx)
        └── revisor.md              # subagent Revisor     (model: openai:qwen3.6-27b)
```

## Setup

### 1. Instalar Qwen Code

```bash
npm install -g @qwen-code/qwen-code@latest
qwen --version
```

### 2. Subir o LM Studio Server

- Aba **Developer** → **Start Server** (porta `1234`).
- Habilite **JIT loading** + **Keep models loaded** para evitar carga em runtime.
- Modelos esperados (já presentes na sua máquina):
  - `qwen3.6-35b-a3b-mlx` — Codificador.
  - `qwen3.6-27b` — Revisor.
  - `qwen3-14b-mlx` — fallback leve (configurado, opcional).

Validar:

```bash
curl -s http://localhost:1234/v1/models | jq '.data[].id'
```

### 3. Provider já está configurado em `.qwen/settings.json`

O arquivo neste diretório define:
- `env.LMSTUDIO_API_KEY = "lm-studio"` (LM Studio aceita qualquer valor não-vazio).
- `modelProviders.openai[]` com os três modelos apontando para `http://localhost:1234/v1`.
- `security.auth.selectedType = "openai"` — qwen-code conversa com LM Studio via protocolo OpenAI.
- `model.name = "qwen3.6-35b-a3b-mlx"` — modelo default da sessão raiz.

Se preferir centralizar a config no perfil do usuário, copie o conteúdo para `~/.qwen/settings.json` (precedência: user > project, segundo a documentação). Para este projeto, **manter no `.qwen/` deste repo é o recomendado** — mesma filosofia self-contained do `.opencode/`.

### 4. Ferramentas auxiliares

```bash
brew install gh just         # gh = GitHub CLI; just (opcional, atalhos)
gh auth login                # autenticar no GitHub
```

### 5. Verificar scripts e symlink

```bash
ls -l .qwen/scripts          # 5 scripts (resolvidos via symlink para .opencode/scripts/)
bash .qwen/scripts/start_task.sh    # sem args = mostra "uso: start_task <numero>"
```

## Uso

Qwen Code descobre subagents em `.qwen/agents/*.md` automaticamente. Diferente do OpenCode, **não há comando `/agent` para trocar de persona da sessão** — você invoca o subagent pelo nome (delegação) ou pede que ele assuma a tarefa.

### Sessão 1 — delegando ao Arquiteto (opcional)

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# (modelo default do settings.json — pode ajustar com /model openai:qwen3.6-27b)
# >  Use o arquiteto para planejar a US-042 (cadastro com responsável).
#   → o subagent arquiteto lê código (Read/Grep/Glob), consulta gh issue list
#   → projeta plano TDD, escreve corpo da issue em /tmp/...md
#   → roda: gh issue create --title "US-042: ..." --label user-story --body-file ...
#   → devolve: #142 — https://github.com/.../issues/142 → próximo agente: codificador
```

### Sessão 2 — delegando ao Codificador

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# >  Use o codificador para iniciar a implementação da issue #61.
#   → o subagent roda automaticamente com modelo openai:qwen3.6-35b-a3b-mlx
#   → executa: bash .qwen/scripts/start_task.sh 61
#   → trabalha DENTRO da worktree .opencode/worktrees/feat-61-...
#   → "finalize" → bash .qwen/scripts/finish_task.sh 61
#   → edita SUMMARY.md (narrativa + LGPD)
```

### Sessão 3 — delegando ao Revisor (novo terminal, em paralelo se quiser)

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# >  Use o revisor para auditar a issue #61.
#   → o subagent roda com modelo openai:qwen3.6-27b
#   → bash .qwen/scripts/load_review.sh 61
#   → audita 3 camadas (plano / qualidade / critérios)
#   → APROVADO: bash .qwen/scripts/approve.sh 61   →  PR DRAFT no GitHub
#   → AJUSTES:  bash .qwen/scripts/reject.sh 61 "motivo claro"  → volta ao Codificador
```

### Após PR criado

1. Dioni abre o PR no GitHub.
2. Confere o diff, converte para **Ready for review**.
3. Mergeia (estratégia merge commit — padrão do repo).
4. Issue fecha automaticamente via `Closes #<N>` no body.
5. Limpar:
   ```bash
   git worktree remove .opencode/worktrees/<dir>
   git branch -D <branch>     # opcional, ramo local
   ```

## Notas

- **Worktrees são compartilhadas com o runner OpenCode** (`.opencode/worktrees/`). Não rode `qwen` e `opencode` na mesma issue ao mesmo tempo.
- **PR sempre draft.** Rede contra Revisor local aprovar algo com teste verde mas lógica errada. Quando confiar nos pareceres, troque `--draft` em `approve.sh`.
- **Tasks em paralelo.** Worktrees isoladas permitem Codificador na #61 enquanto você revisa o PR da #58. LM Studio precisa dos dois modelos carregados — habilite **Keep models loaded** para evitar swap.
- **`reject` preserva a worktree** e escreve `REVIEW.md` — o Codificador retoma de onde parou.
- **QA** está embutido no `mvn test` obrigatório de `finish_task`/`load_review` (verde é trava). Não há subagent QA dedicado no atrilha.
- **Documentação canônica do ciclo**: `doc/workflow.md` (conceitual completo) e `AGENTS.md` (operacional).

## Troubleshooting

| Sintoma | Causa provável | Correção |
|---|---|---|
| `qwen` falha autenticando | LM Studio fora do ar | `curl http://localhost:1234/v1/models` para verificar |
| Subagent não aparece em `/agents` | Frontmatter inválido | Conferir indentação YAML; `name`/`description` obrigatórios |
| Modelo do subagent não muda | `model:` ausente ou typo | Usar prefixo `openai:` antes do id (ex.: `openai:qwen3.6-35b-a3b-mlx`) |
| Subagent não roda script | `tools` filtrado demais | Não defina `tools` — herda todas as ferramentas do parent |
| Resposta lenta no Revisor | Modelo grande em swap | LM Studio → Settings → Keep models loaded ON |
