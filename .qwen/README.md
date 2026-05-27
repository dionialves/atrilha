# Agentes locais do atrilha — Arquiteto + Codificador + Revisor (Qwen Code)

Fluxo de desenvolvimento assistido por LLM rodando 100% local via **Qwen Code CLI** (`https://github.com/QwenLM/qwen-code`) + **LM Studio**.

Filosofia: o LLM decide *o quê* fazer e *se aprova*; o *como* do Git é mecânico via shell scripts determinísticos em `.qwen/scripts/`. Tudo neste diretório é self-contained — pode ser copiado/movido sem depender de outras pastas do repo.

## Filosofia

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

> O **Arquiteto** é opcional: Dioni pode criar Issues à mão (papel humano) e pular direto para o Codificador.

**Uma task = uma worktree isolada = uma branch (`<tipo>/<N>-<slug>`) = um PR = um commit squash.** A worktree e a branch são criadas juntas por `start_task.sh`; o Revisor faz o squash + push + abertura do PR via `approve.sh`. As worktrees vivem em `.qwen/worktrees/` (gitignorada).

## Como o Qwen Code lida com agentes

Diferente de outros runners onde você "muda de persona" com um comando, no Qwen Code os papéis são **subagents**: a sessão principal **delega** a tarefa pelo nome do agente.

```
qwen
> Use o codificador para iniciar a implementação da issue #61
```

O subagent carrega o **system prompt** descrito no arquivo `.md` correspondente e o **modelo pinado no frontmatter** (`model: openai:qwen3.6-35b-a3b-ud-mlx` para o codificador, por exemplo). Você pode invocar também explicitamente pelo nome (ex.: `"have the revisor agent audit #61"`).

Precedência de descoberta dos agentes:
1. **Projeto** (este diretório): `.qwen/agents/*.md` ← usado por este repo.
2. **Usuário**: `~/.qwen/agents/*.md`.
3. **Extensões instaladas**.

## Estrutura

```
.qwen/
├── README.md                   # este arquivo
├── .gitignore                  # ignora worktrees/, node_modules/, etc.
├── settings.json               # provider LM Studio + modelos + auth
├── scripts/                    # scripts determinísticos (self-contained)
│   ├── qwen.sh                 #  [wrapper] roda `qwen` com bodyTimeout do undici zerado
│   ├── no-undici-timeout.cjs   #  [preload] usado pelo qwen.sh
│   ├── start_task.sh           #  [Codificador] issue → worktree + branch
│   ├── finish_task.sh          #  [Codificador] mvn test + SUMMARY.md
│   ├── load_review.sh          #  [Revisor]    dossiê read-only
│   ├── approve.sh              #  [Revisor]    squash → push → PR DRAFT
│   └── reject.sh               #  [Revisor]    REVIEW.md, preserva worktree
├── agents/
│   ├── arquiteto.md            # subagent Arquiteto   (model: openai:qwen3.6-35b-a3b-mlx)
│   ├── codificador.md          # subagent Codificador (model: openai:qwen3.6-35b-a3b-ud-mlx)
│   └── revisor.md              # subagent Revisor     (model: openai:qwen3.6-35b-a3b-mlx)
└── worktrees/                  # criadas e removidas dinamicamente (gitignored)
```

## Setup

### 1. Instalar Qwen Code

```bash
npm install -g @qwen-code/qwen-code@latest
qwen --version
```

### 2. Subir o LM Studio Server

- Aba **Developer** → **Start Server** (porta `1234`).
- Habilite **JIT loading** e **deixe "Keep models loaded" DESLIGADO** (ou no máximo 1 modelo): nesta máquina só cabe **um** modelo grande por vez na RAM, então o LM Studio precisa descarregar o anterior para subir o próximo. JIT cuida desse swap automaticamente quando o `qwen` faz uma requisição para um `id` diferente do carregado.
- **Atenção ao `n_ctx` ao carregar o modelo no LM Studio.** O campo `contextWindowSize` em `.qwen/settings.json` é só o budget que o **cliente** declara — a janela real é a que o LM Studio carregou. Se o servidor subir o modelo com `n_ctx: 32768` e o cliente mandar prompt esperando 262144, o servidor **estoura o budget e dropa a conexão** (sintoma típico: desconexão no meio de geração, mesmo com `timeout` alto). Carregue cada modelo com `n_ctx` igual (ou maior) ao `contextWindowSize` declarado:
  - `qwen3.6-35b-a3b-ud-mlx` → **n_ctx ≥ 262144** (Codificador).
  - `qwen3.6-35b-a3b-mlx` → **n_ctx ≥ 262144** (Arquiteto + Revisor — mesmo modelo).
- Como **Arquiteto e Revisor usam o mesmo modelo** (`qwen3.6-35b-a3b-mlx`), um ciclo completo (planejamento → implementação → revisão) tem só **dois swaps**: `35b-a3b-mlx → 35b-a3b-ud-mlx → 35b-a3b-mlx`. Cada swap custa 30–90s (já coberto pelos `timeout: 1800000` ms / 30 min).
- Modelos esperados:
  - `qwen3.6-35b-a3b-mlx` — Arquiteto + Revisor (e default da sessão raiz).
  - `qwen3.6-35b-a3b-ud-mlx` — Codificador.
  - `qwen3-14b-mlx` — fallback leve (opcional).

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

Se preferir centralizar a config no perfil do usuário, copie o conteúdo para `~/.qwen/settings.json` (precedência: user > project). Manter aqui é o recomendado — todo o setup viaja com o repo.

### 4. Ferramentas auxiliares

```bash
brew install gh just         # gh = GitHub CLI; just (opcional, atalhos)
gh auth login                # autenticar no GitHub
```

### 5. Verificar scripts

```bash
ls -l .qwen/scripts                   # 7 scripts executáveis (5 shell + qwen.sh + preload)
bash .qwen/scripts/start_task.sh      # sem args = mostra "uso: start_task <numero>"
```

### 6. Wrapper `qwen.sh` (anti "Body Timeout Error")

O cliente HTTP do Node (`undici`) tem dois timeouts internos hardcoded em **5 min** que não são expostos pelo `generationConfig` do qwen-code:
- `headersTimeout` — tempo máximo até receber os headers (cobre TTFT).
- `bodyTimeout` — tempo máximo **entre chunks** do body de resposta.

Para um modelo grande rodando local em LM Studio (MLX 35B), com prompt de 40k+ tokens e pressão de RAM, é fácil estourar 5 min e ver:

- `qwen`: `✕ [API Error: terminated (cause: Body Timeout Error)]`
- LM Studio: `Client disconnected. Stopping generation...`

O `timeout: 1800000` (30 min) do `settings.json` **não cobre isso** — ele cobre o request inteiro via AbortController, mas o undici aborta antes pelo seu próprio body/headers timeout interno.

**Solução**: rodar `qwen` via `bash .qwen/scripts/qwen.sh` (ou alias) em vez de chamar `qwen` direto. O wrapper injeta um preload Node que faz `setGlobalDispatcher(new Agent({ headersTimeout: 0, bodyTimeout: 0 }))`. O teto de 30 min do `generationConfig` continua valendo via AbortController.

**Setup único** (depois é só usar o wrapper):

```bash
cd .qwen/scripts
npm init -y && npm install undici     # node_modules/ já está no .gitignore
cd -
```

**Uso** (substituindo `qwen` em todos os fluxos do README):

```bash
bash .qwen/scripts/qwen.sh                  # sessão interativa
bash .qwen/scripts/qwen.sh -p "pergunta"    # one-shot
QWEN_DEBUG_PRELOAD=1 bash .qwen/scripts/qwen.sh   # confirma que o patch foi aplicado
```

Sugestão de alias em `~/.zshrc` / `~/.bashrc`:

```bash
alias qwen='bash $HOME/sources/atrilha/.qwen/scripts/qwen.sh'
```

## Uso

### Sessão 1 — delegando ao Arquiteto (opcional)

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
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
#   → o subagent roda automaticamente com modelo openai:qwen3.6-35b-a3b-ud-mlx
#   → executa: bash .qwen/scripts/start_task.sh 61
#   → trabalha DENTRO da worktree .qwen/worktrees/feat-61-...
#   → "finalize" → bash .qwen/scripts/finish_task.sh 61
#   → edita SUMMARY.md (narrativa + LGPD)
```

### Sessão 3 — delegando ao Revisor (sequencial: feche a sessão do Codificador antes)

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# >  Use o revisor para auditar a issue #61.
#   → o subagent roda com modelo openai:qwen3.6-35b-a3b-mlx
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
5. **Atualizar `doc/changelog.md` e `doc/release_notes/unreleased.md` (humano, pós-merge).** O Revisor está proibido de tocar `doc/**` em qualquer momento do ciclo — essa entrada é exclusiva do Dioni e acontece **depois** do merge, não no PR draft.
6. Limpar:
   ```bash
   git worktree remove .qwen/worktrees/<dir>
   git branch -D <branch>     # opcional, ramo local
   ```

## Notas

- **PR sempre draft.** Rede contra Revisor local aprovar algo com teste verde mas lógica errada. Quando confiar nos pareceres, troque `--draft` em `approve.sh`.
- **Sessões serializadas (1 modelo por vez).** A máquina não comporta dois modelos grandes simultâneos na RAM. Rode **uma persona de cada vez**: feche a sessão do Arquiteto antes de abrir a do Codificador, e a do Codificador antes da do Revisor. O LM Studio descarrega o anterior e sobe o próximo automaticamente via JIT no primeiro request. **Arquiteto → Revisor (ou Revisor → Arquiteto) não dispara swap**, pois compartilham `qwen3.6-35b-a3b-mlx`. **Não rode #61 e #58 em paralelo** — worktrees são isoladas, mas o modelo no LM Studio não é.
- **Primeiro request após swap é lento.** Carga de um modelo MLX leva 30–90s. Os `timeout: 1800000` (30 min) no `settings.json` cobrem isso com folga; só fique atento à primeira resposta de cada role-switch.
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
| Primeira resposta após trocar de agente trava | LM Studio carregando o outro modelo (30–90s) | Esperar — `timeout: 1800000` (30 min) no `settings.json` cobre. NÃO ligue "Keep models loaded": não há RAM para os dois. |
| LM Studio estoura RAM ao subir modelo | "Keep models loaded" ON forçando 2 modelos grandes | Desligar em Settings → manter no máximo 1 modelo residente |
| Subagent **desconecta no meio da geração** | `n_ctx` carregado no LM Studio menor que `contextWindowSize` declarado em `settings.json` (servidor estoura budget e dropa conexão) | LM Studio → modelo carregado → ajustar `n_ctx` para casar com `contextWindowSize` do `settings.json` (hoje todos em 262144). Sintomas: drop sem erro claro, geração cortada após N tokens, primeira resposta longa falha. |
| `qwen` mostra `terminated (cause: Body Timeout Error)` + LM Studio mostra `Client disconnected. Stopping generation...` | undici (cliente HTTP do Node) tem `bodyTimeout` interno hardcoded em 5 min entre chunks. Para modelo grande local com 40k+ tokens de prompt e/ou pressão de RAM, o stream pausa >5 min e o cliente aborta. Não é o `timeout` do `settings.json` — é interno ao Node. | Rodar via wrapper: `bash .qwen/scripts/qwen.sh` (zera `bodyTimeout`/`headersTimeout`). Setup único: `cd .qwen/scripts && npm init -y && npm install undici`. Ver §6 do Setup. |
| `mvn test` reclama de janela curta | Contexto do modelo subdimensionado | LM Studio → modelo carregado → `n_ctx` ≥ 32768 (recomendado 262144 para casar com `settings.json`) |
