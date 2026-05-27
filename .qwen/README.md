# Agentes locais do atrilha — Scout + Arquiteto + Codificador + Revisor (Qwen Code)

Fluxo de desenvolvimento assistido por LLM rodando 100% local via **Qwen Code CLI** (`https://github.com/QwenLM/qwen-code`) + **LM Studio**.

Filosofia: o LLM decide *o quê* fazer e *se aprova*; o *como* do Git é mecânico via shell scripts determinísticos em `.qwen/scripts/`. Tudo neste diretório é self-contained — pode ser copiado/movido sem depender de outras pastas do repo.

## Filosofia

```
Dioni descreve a demanda em linguagem natural
   │
   ▼
[subagent arquiteto-scout]   investiga código (frugal) → coleta dados → escreve .qwen/briefs/<CODE>.md
   │                                                                       │
   ▼                                                                       ▼
[subagent arquiteto]          lê APENAS o brief → decide arquitetura → gh issue create
   │
   ▼
[subagent codificador]        start_task → implementa → finish_task   (mvn test verde + SUMMARY.md)
   │
   ▼
[subagent revisor]            load_review → audita 3 camadas → approve | reject
   │                                          │         │
   │                                     PR DRAFT   REVIEW.md (volta ao Codificador,
   ▼                                                   MESMA worktree)
Dioni revisa PR draft → ready → merge → Issue fecha via Closes #N
```

> O **planejamento em duas fases** (Scout → Arquiteto) existe porque o modelo do Arquiteto (`qwen3.6-27b-mlx`) é mais inteligente em decisão fina mas tem janela ~68k tokens — não cabe explorar repo grande. O Scout (`qwen3.6-35b-a3b-mlx`, MoE, janela maior) faz a coleta exaustiva; o Arquiteto recebe um brief compacto e gasta sua inteligência só em decisão + redação da Issue extremamente detalhada.
>
> Ambas as fases são opcionais: Dioni pode criar Issues à mão e pular direto para o Codificador.

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
│   ├── arquiteto-scout.md      # subagent Scout       (model: openai:qwen3.6-35b-a3b-mlx) — coleta dados
│   ├── arquiteto.md            # subagent Arquiteto   (model: openai:qwen3.6-27b-mlx)     — decide + redige
│   ├── codificador.md          # subagent Codificador (model: openai:qwen3.6-35b-a3b-ud-mlx)
│   └── revisor.md              # subagent Revisor     (model: openai:qwen3.6-35b-a3b-mlx)
├── briefs/                     # handoff Scout → Arquiteto (gitignored, exceto README)
│   └── README.md               # convenção de nome e ciclo de vida
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
  - `qwen3.6-35b-a3b-mlx` → **n_ctx ≥ 262144** (Scout + Revisor — mesmo modelo).
  - `qwen3.6-35b-a3b-ud-mlx` → **n_ctx ≥ 262144** (Codificador).
  - `qwen3.6-27b-mlx` → **n_ctx ≥ 68000** (Arquiteto — janela apertada por design, brief compacto cabe).
- Como **Scout e Revisor usam o mesmo modelo** (`qwen3.6-35b-a3b-mlx`), o ciclo completo (scout → arquiteto → codificador → revisor) tem **três swaps**: `35b-a3b-mlx → 27b-mlx → 35b-a3b-ud-mlx → 35b-a3b-mlx`. Cada swap custa 30–90s (já coberto pelos `timeout: 1800000` ms / 30 min).
- Modelos esperados:
  - `qwen3.6-35b-a3b-mlx` — Scout + Revisor (e default da sessão raiz).
  - `qwen3.6-27b-mlx` — Arquiteto (decisão fina; janela apertada exige brief do Scout).
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

> **Como invocar subagents no Qwen Code**: o `qwen` não tem comando "mudar de persona"; você delega na própria mensagem inicial nomeando o agente. Frases canônicas abaixo. Sempre comece a sessão dentro do diretório do projeto e use o wrapper `qwen.sh` (ou alias `qwen`) para evitar `Body Timeout Error`.

### ⚠️ Armadilha de roteamento: NÃO use "planejar" sem citar o `arquiteto-scout`

O agente raiz do qwen-code faz match semântico entre a sua mensagem e a `description` dos subagents. Frases ambíguas como **"crie o planejamento da US-XXX"** ou **"planeje a US-XXX"** podem ser roteadas para o `arquiteto` (que cria a Issue) em vez do `arquiteto-scout` (que faz a investigação). O `arquiteto` é gatilho da **fase 2** e tem protocolo de recusa formal quando o brief não existe — mas o ideal é não chegar até essa recusa.

**Sempre nomeie o agente explicitamente na primeira mensagem da sessão**:

- ✅ `Use o arquiteto-scout para preparar o brief de US-004. Escopo: ...`
- ✅ `Use o arquiteto para gerar a Issue de US-004` (somente depois do scout ter escrito o brief)
- ❌ `Planeje a US-004` (ambíguo — pode ir para o `arquiteto`)
- ❌ `Crie o planejamento da US-004` (idem)
- ❌ `Investigue e crie a Issue da US-004` (idem; mistura as duas fases)

Se mesmo nomeando `arquiteto-scout` o qwen-code rotear para o `arquiteto`, o `arquiteto` deve recusar formalmente e devolver com instrução clara para invocar o scout. Se isso acontecer com frequência, abra Issue no qwen-code reportando o picker.

### Sessão 1a — delegando ao Scout (fase 1 do planejamento — opcional)

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# >  Use o arquiteto-scout para preparar o brief da US-042 (cadastro com responsável).
#   → o subagent arquiteto-scout roda com modelo openai:qwen3.6-35b-a3b-mlx
#   → lê código com política frugal (Grep + Read offset/limit), consulta gh issue list
#   → escreve .qwen/briefs/US-042.md (dados factuais: arquivos, snippets literais, migrations, testes, issues relacionadas, LGPD, stack)
#   → devolve: caminho do brief + "Use o arquiteto para gerar a Issue de US-042"
```

### Sessão 1a-bis — quando o Scout detecta demanda grande (slicing)

Se a demanda investigada estoura os limites de "brief único" (> 2 camadas, > 5 arquivos, > 1 migration, > 1 fluxo de UI, > 5 testes novos), o Scout NÃO escreve um brief gigante. Em vez disso, escreve uma **slicing proposal** em `.qwen/briefs/<CODE>-slicing.md` propondo 2-6 slices independentemente entregáveis com ordem topológica.

Fluxo completo nesse caso:

```bash
# 1ª passada — Scout detecta tamanho e propõe quebra:
> Use o arquiteto-scout para preparar o brief de US-042
#   → Scout descobre que é grande, escreve .qwen/briefs/US-042-slicing.md
#   → Devolve: "Revise .qwen/briefs/US-042-slicing.md e responda com 'gerar os briefs das slices aprovadas de US-042'"

# Humano abre US-042-slicing.md, lê a proposta, decide:
#   - Aprovar: comando abaixo
#   - Refinar: "Refaça a slicing proposal de US-042 considerando <ajuste>"

# 2ª passada — Scout escreve os briefs das slices aprovadas:
> Use o arquiteto-scout para gerar os briefs das slices aprovadas de US-042
#   → Scout produz .qwen/briefs/US-042-a.md, US-042-b.md, US-042-c.md, ...
#   → Cada brief com "Depende de: <CODE>-<letra>" (códigos, não #N)
#   → Devolve a lista em ordem topológica

# Para cada slice, em ordem topológica (Arquiteto resolve dependências via gh):
> Use o arquiteto para gerar a Issue de US-042-a   # cria #142
> Use o arquiteto para gerar a Issue de US-042-b   # resolve "Depende de US-042-a" → #142
> Use o arquiteto para gerar a Issue de US-042-c   # resolve dependências
# (Arquiteto recusa se você invocar fora de ordem; não cria Issue cuja dependência ainda não virou Issue)
```

Convenção e detalhes em [`.qwen/briefs/README.md`](briefs/README.md).

### Sessão 1b — delegando ao Arquiteto (fase 2 do planejamento — opcional)

> Pode rodar na MESMA sessão depois do Scout (não dispara swap se você ficar no mesmo modelo) OU em sessão nova. Como Scout usa 35b-a3b-mlx e Arquiteto usa 27b-mlx, **há um swap** entre as duas fases.

```bash
cd /Users/dionia.oliveira/sources/atrilha
qwen
# >  Use o arquiteto para gerar a Issue de US-042.
#   → o subagent arquiteto roda com modelo openai:qwen3.6-27b-mlx
#   → lê APENAS .qwen/briefs/US-042.md (não abre código do projeto)
#   → decide arquitetura, escreve corpo da Issue com detalhe extremo (zero-decisão para Codificador)
#   → roda: gh issue create --title "US-042: ..." --label user-story --body-file ...
#   → devolve: #142 — https://github.com/.../issues/142 → próximo agente: codificador
```

**Se o brief não existir** (`.qwen/briefs/US-042.md` ausente), o Arquiteto para e pede ao Dioni rodar o Scout antes.

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
- **Sessões serializadas (1 modelo por vez).** A máquina não comporta dois modelos grandes simultâneos na RAM. Rode **uma persona de cada vez**: feche a sessão do Scout antes de abrir a do Arquiteto, do Arquiteto antes da do Codificador, e do Codificador antes da do Revisor. O LM Studio descarrega o anterior e sobe o próximo automaticamente via JIT no primeiro request. **Scout → Revisor (ou Revisor → Scout) não dispara swap**, pois compartilham `qwen3.6-35b-a3b-mlx`. **Não rode #61 e #58 em paralelo** — worktrees são isoladas, mas o modelo no LM Studio não é.
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
