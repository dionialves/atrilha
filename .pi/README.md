# Agentes locais do atrilha — Codificador + Revisor (PI Agent + LM Studio)

Fluxo de desenvolvimento do **atrilha** com LLMs locais (Qwen via LM Studio) no
PI Agent, num Mac M1 Max 64GB. Você (Dioni) faz PO/CTO/Designer e cria as Issues;
o agente assume **um papel por sessão** — Codificador ou Revisor.

## Filosofia

O fluxo mecânico de Git (worktree, branch, push, PR) **não está em prompt** — está
nos scripts de `.pi/scripts/`, que o agente executa via `bash`. O LLM decide *o quê*
fazer e *se aprova*; o *como* do Git é determinístico. Isso elimina a maior fonte de
erro de modelo local: compor comandos shell complexos.

```
Dioni cria Issue
   │
   ▼
[sessão Codificador]  start_task → implementa → finish_task   (testes verdes + SUMMARY.md)
   │
   ▼
[sessão Revisor]      load_review → audita → approve | reject
   │                                    │         │
   │                              PR draft    REVIEW.md (volta ao Codificador,
   ▼                                              mesma worktree)
Dioni revisa PR draft → ready → merge
```

## Como o PI carrega tudo

A versão atual do PI Agent **não usa flags** como `--system` ou `--tool`. Em vez disso:

- **Modelo/provider** → `~/.pi/agent/models.json` (registra o LM Studio).
- **System prompt do projeto** → `.pi/SYSTEM.md` (auto-descoberto; descreve os dois papéis).
- **Contexto do projeto** → `AGENTS.md` na raiz (auto-carregado).
- **Tools de Git** → scripts em `.pi/scripts/`, chamados pelo agente via `bash`.
- **Config do projeto** → `.pi/settings.json` (modelos habilitados, thinking).

## Estrutura

```
atrilha/
├── AGENTS.md                 # contexto do projeto (auto-carregado pelo pi)
├── Justfile                  # atalhos: just dev / just coder / just doctor
├── pi-global/
│   └── models.json           # COPIAR para ~/.pi/agent/models.json
├── doc/workflow.md           # fonte canônica do ciclo (referência conceitual)
├── .github/ISSUE_TEMPLATE/   # templates de issue (US, bug-fix, refactor/chore)
└── .pi/
    ├── README.md             # este arquivo
    ├── SYSTEM.md             # system prompt: descreve papéis Codificador/Revisor
    ├── settings.json         # modelos habilitados + thinking
    └── tools/
        ├── start_task.sh     # [Coder]  issue → worktree
        ├── finish_task.sh    # [Coder]  mvn test + SUMMARY.md
        ├── load_review.sh    # [Revisor] dossiê read-only
        ├── approve.sh        # [Revisor] squash → push → PR DRAFT
        └── reject.sh         # [Revisor] REVIEW.md, preserva worktree
```

## Setup

### 1. Modelos no LM Studio

Baixe os dois modelos MLX (aba de busca do LM Studio):

| Papel | Identifier LM Studio | Por quê |
|-------|----------------------|---------|
| **Codificador** | `qwen3.6-35b-a3b-mlx@4bit` | MoE (3B ativos) — rápido nos loops de tool call |
| **Revisor** | `qwen3.6-27b-mlx` | Denso — mais lento, julgamento mais apurado; roda 1x por task |

Settings recomendados (painel do modelo): `temperature=0.7, top_p=0.8, top_k=20,
repeat_penalty=1.05`. Contexto: **32K** (não use 256K nativo — mata latência).
Ative **Flash Attention** e **KV Cache Q8** nas opções de carga.

Ligue o servidor: aba **Developer** → **Start Server**, porta `1234`. Para os dois
modelos coexistirem (paralelo via worktree), ative **JIT loading** + **keep models
loaded** nas settings do servidor. Caução de RAM: ~20GB + ~17GB ≈ 37GB, cabe nos 64GB.

### 2. Registrar o LM Studio no PI

```bash
mkdir -p ~/.pi/agent
cp pi-global/models.json ~/.pi/agent/models.json
```

O `models.json` já inclui `compat.supportsDeveloperRole: false` — necessário porque o
LM Studio não entende o role "developer" usado por modelos com reasoning. Sem isso,
dá erro silencioso. Confirme os identifiers com `just doctor` (lista o que está
carregado) e ajuste o `models.json` se os nomes diferirem.

### 3. Ferramentas

```bash
brew install gh just
gh auth login
```

### 4. Copiar o pacote para o repo

Copie `AGENTS.md`, `Justfile`, `pi-global/`, `.pi/` e `.github/` para a raiz do repo
`atrilha`. Comite-os.

> **README:** este vive em `.pi/README.md` — o `README.md` da raiz é o do produto.
> **Templates de issue:** "New Issue" no GitHub oferece US / Bug Fix / Refactor. Cada
> um já traz plano + critérios no formato que o `start_task` lê. Edite a URL de
> Discussions no `config.yml`.

### 5. Verificar

```bash
just doctor
```

## Uso

O PI carrega o `SYSTEM.md` que descreve os dois papéis. Você escolhe o papel na
conversa e o modelo com `/model` (ou Ctrl+P para ciclar entre os dois habilitados).

**Sessão Codificador** (Terminal 1):

```bash
just dev          # ou: just coder
# /model → escolha qwen3.6-35b-a3b
# "Atue como Codificador. Comece a issue 42."
#   → o agente roda: bash .pi/scripts/start_task.sh 42
#   → implementa dentro da worktree
#   → "finalize" → bash .pi/scripts/finish_task.sh 42
#   → edita SUMMARY.md (narrativa + LGPD)
```

**Sessão Revisor** (Terminal 2):

```bash
just dev          # ou: just revisor
# /model → escolha qwen3.6-27b
# "Atue como Revisor. Revise a issue 42."
#   → bash .pi/scripts/load_review.sh 42
#   → audita 3 camadas
#   → aprova: bash .pi/scripts/approve.sh 42        (PR draft)
#   → ou: bash .pi/scripts/reject.sh 42 "motivo"    (volta ao Coder)
```

Quando o PR draft aparecer: revise, **converta para Ready**, faça merge. A issue fecha
sozinha (`Closes #N`). Depois:

```bash
just clean 42
```

## Notas

- **Troca de modelo por papel:** o jeito mais confiável é `/model` dentro do pi. As
  receitas `just coder`/`just revisor` tentam `--model` mas caem pra `pi` puro se a
  flag não existir nesta versão — confirme com `pi --help`.
- **Tasks em paralelo:** worktrees isoladas permitem Coder na #42 enquanto você revisa
  o PR da #38. O LM Studio precisa dos dois modelos carregados (passo 1).
- **reject preserva a worktree** e escreve `REVIEW.md` — o Coder retoma de onde parou.
- **PR sempre draft:** rede contra o Revisor local aprovar algo com teste verde mas
  lógica errada. Quando confiar nos pareceres, troque `--draft` em `approve.sh`.
- **QA:** coberto pelo `mvn test` obrigatório em `finish_task`/`load_review`.
- **Custom tools como extensions:** se um dia quiser que `start_task` etc. virem tools
  nativas do pi (em vez de chamadas bash), dá pra convertê-las em extensions TypeScript
  em `.pi/extensions/` — ver docs/extensions.md do PI. Mas via bash já funciona bem e é
  mais simples de manter.
