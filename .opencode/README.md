# Agentes locais do atrilha — Codificador + Revisor (OpenCode)

Adaptação do fluxo PI Agent (em `.pi/`) para o **OpenCode** (`https://opencode.ai`).
Mesmo ciclo, mesmos scripts determinísticos, mesma filosofia: o LLM decide *o quê*
fazer e *se aprova*; o *como* do Git é mecânico via shell scripts.

## Filosofia (idêntica ao `.pi/`)

```
Dioni descreve a demanda em linguagem natural
   │
   ▼
[sessão @arquiteto]     investiga código → projeta plano TDD → gh issue create
   │                                                                │
   ▼                                                                ▼
[sessão @codificador]   start_task → implementa → finish_task   (mvn test verde + SUMMARY.md)
   │
   ▼
[sessão @revisor]       load_review → audita 3 camadas → approve | reject
   │                                       │         │
   │                                  PR DRAFT   REVIEW.md (volta ao Codificador,
   ▼                                                MESMA worktree)
Dioni revisa PR draft → ready → merge → Issue fecha via Closes #N
```

> O **Arquiteto** é opcional: o Dioni pode continuar criando Issues à mão (papel humano) e pular direto para o Codificador. Use o agente quando quiser delegar o trabalho de investigação + planejamento.

**Uma task = uma worktree isolada = uma branch (`<tipo>/<N>-<slug>`) = um PR = um commit squash.** A worktree e a branch são criadas juntas por `start_task.sh`; o Revisor faz o squash + push + abertura do PR via `approve.sh`.

## Estrutura

```
atrilha/
├── AGENTS.md                       # contexto do projeto (auto-carregado pelo OpenCode)
├── .pi/
│   ├── SYSTEM.md                   # prompt equivalente p/ PI Agent
│   └── scripts/                    # scripts determinísticos do PI Agent
│       ├── start_task.sh           #  [Codificador] issue → worktree + branch
│       ├── finish_task.sh          #  [Codificador] mvn test + SUMMARY.md
│       ├── load_review.sh          #  [Revisor]    dossiê read-only
│       ├── approve.sh              #  [Revisor]    squash → push → PR DRAFT
│       └── reject.sh               #  [Revisor]    REVIEW.md, preserva worktree
└── .opencode/
    ├── README.md                   # este arquivo
    ├── opencode.json               # config OpenCode (schema apenas)
    ├── scripts/                    # cópia idêntica dos scripts (mesma semântica)
    │   ├── start_task.sh
    │   ├── finish_task.sh
    │   ├── load_review.sh
    │   ├── approve.sh
    │   └── reject.sh
    └── agents/
        ├── arquiteto.md            # papel Arquiteto   (analisa demanda → cria Issue via gh)
        ├── codificador.md          # papel Codificador (frontmatter + prompt)
        └── revisor.md              # papel Revisor    (frontmatter + prompt)
```

**Os scripts em `.opencode/scripts/` são cópia byte-a-byte dos de `.pi/scripts/`** —
mesma assinatura, mesmo comportamento, mesmas saídas. Vivem em pastas separadas
para que cada runner (PI ou OpenCode) seja autocontido em sua subárvore. Se um
script for evoluído, sincronize manualmente nas duas pastas (ou converta para
symlinks `ln -s ../../.pi/scripts/start_task.sh .opencode/scripts/start_task.sh`
caso prefira fonte única).

## Setup

### 1. Instalar OpenCode

```bash
brew install sst/tap/opencode   # ou: npm i -g opencode-ai
opencode --version
```

### 2. Provider de modelo

OpenCode lê config global em `~/.config/opencode/opencode.json`. Para usar LM Studio
local (mesmo provedor do PI), registre o provider:

```jsonc
{
  "$schema": "https://opencode.ai/config.json",
  "provider": {
    "lmstudio": {
      "npm": "@ai-sdk/openai-compatible",
      "name": "LM Studio",
      "options": { "baseURL": "http://localhost:1234/v1" },
      "models": {
        "qwen3.6-35b-a3b-mlx": { "name": "Qwen3.6 35B A3B (Codificador)" },
        "qwen3.6-27b-mlx":     { "name": "Qwen3.6 27B (Revisor)" }
      }
    }
  }
}
```

Suba o LM Studio Server na porta `1234` (aba **Developer** → **Start Server**) com
os dois modelos carregados (JIT loading + keep models loaded).

Alternativa: usar provider Anthropic/OpenAI direto se preferir Sonnet/Opus na nuvem.
Os prompts dos agentes não fixam modelo — defina via `/model` por sessão.

### 3. Ferramentas auxiliares

```bash
brew install gh just         # gh = GitHub CLI; just (opcional, atalhos)
gh auth login                # autenticar no GitHub
```

### 4. Verificar scripts

```bash
ls -l .opencode/scripts/*.sh       # devem ser executáveis (chmod +x já está no repo)
bash .opencode/scripts/start_task.sh  # sem args = mostra "uso: start_task <numero>"
```

## Uso

OpenCode descobre agentes em `.opencode/agents/*.md` automaticamente. Em cada sessão:

### Sessão do Arquiteto (opcional — só se quiser delegar o planejamento)

```bash
cd /Users/dionia.oliveira/sources/atrilha
opencode
# /agent arquiteto
# /model anthropic/claude-sonnet-4-5     (raciocínio forte ajuda no plano)
# "Implemente a US-042 de cadastro com responsável." (ou bug/refactor descrito em linguagem natural)
#   → o agente lê código (Read/Grep/Glob), consulta gh issue list
#   → projeta plano TDD, escreve corpo da issue em /tmp/...md
#   → roda: gh issue create --title "US-042: ..." --label user-story --body-file ...
#   → devolve: #142 — https://github.com/.../issues/142 → próximo agente: codificador
```

### Sessão do Codificador

```bash
cd /Users/dionia.oliveira/sources/atrilha
opencode
# /agent codificador
# /model lmstudio/qwen3.6-35b-a3b-mlx     (ou sonnet, etc.)
# "Inicie a implementação da issue #61."
#   → o agente roda: bash .opencode/scripts/start_task.sh 61
#   → trabalha DENTRO da worktree .opencode/worktrees/feat-61-...
#   → "finalize" → bash .opencode/scripts/finish_task.sh 61
#   → edita SUMMARY.md (narrativa + LGPD)
```

### Sessão do Revisor (novo terminal, em paralelo se quiser)

```bash
cd /Users/dionia.oliveira/sources/atrilha
opencode
# /agent revisor
# /model lmstudio/qwen3.6-27b-mlx
# "Revise a issue #61."
#   → bash .opencode/scripts/load_review.sh 61
#   → audita 3 camadas (plano / qualidade / critérios)
#   → APROVADO: bash .opencode/scripts/approve.sh 61   →  PR DRAFT no GitHub
#   → AJUSTES:  bash .opencode/scripts/reject.sh 61 "motivo claro"  → volta ao Codificador
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

## Diferenças do `.pi/` (orientação ao OpenCode)

| Aspecto | PI Agent | OpenCode |
|---------|----------|----------|
| Carregamento de papéis | Prompt único em `.pi/SYSTEM.md` (papel escolhido por turno) | Um arquivo por papel em `.opencode/agents/` (escolha via `/agent <nome>`) |
| Config do projeto | `.pi/settings.json` (modelos habilitados) | `.opencode/opencode.json` (schema; modelos no global em `~/.config/opencode/`) |
| Scripts de Git | `.pi/scripts/*.sh` invocados via `bash` | `.opencode/scripts/*.sh` (cópia idêntica — manter em sync se um lado mudar) |
| Troca de papel | `/model` cicla entre Codificador/Revisor habilitados | `/agent` muda agente; `/model` muda modelo |

## Notas

- **PR sempre draft.** Rede contra Revisor local aprovar algo com teste verde mas
  lógica errada. Quando confiar nos pareceres, troque `--draft` em `approve.sh`.
- **Tasks em paralelo.** Worktrees isoladas permitem Codificador na #61 enquanto
  você revisa o PR da #58. LM Studio precisa dos dois modelos carregados.
- **`reject` preserva a worktree** e escreve `REVIEW.md` — o Codificador retoma
  de onde parou.
- **QA** está embutido no `mvn test` obrigatório de `finish_task`/`load_review`
  (verde é trava). Não há agente QA dedicado no atrilha.
- **Documentação canônica do ciclo**: `doc/workflow.md` (conceitual completo)
  e `AGENTS.md` (operacional).
