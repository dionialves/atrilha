# Justfile — atalhos do fluxo de agentes do atrilha (PI Agent + LM Studio)
#
# O PI Agent (versão atual) configura modelo/provider via ~/.pi/agent/models.json
# e system prompt via .pi/SYSTEM.md — NÃO via flags de CLI. Por isso as receitas
# abaixo são finas: sobem o `pi` e você escolhe o papel/modelo dentro da sessão.
#
# Setup único (ver .pi/README.md):
#   1. cp pi-global/models.json ~/.pi/agent/models.json   (registra o LM Studio)
#   2. LM Studio: servidor ligado na porta 1234, ambos os modelos carregados
#   3. gh auth login
#
# Fluxo de papéis (um por sessão):
#   - Codificador: modelo qwen3.6-35b-a3b. Diga "atue como Codificador, issue 42".
#   - Revisor:     modelo qwen3.6-27b.     Diga "atue como Revisor, issue 42".
# Troque de modelo dentro do pi com /model (ou Ctrl+P para ciclar).

# sobe o pi no diretório do projeto (escolha o papel na conversa)
dev:
    pi

# sobe o pi já com o modelo do Codificador selecionado, se a flag existir nesta versão.
# Se a flag --model não for aceita, use apenas `just dev` e troque com /model.
coder:
    pi --model "lmstudio/qwen3.6-35b-a3b-mlx@4bit" || pi

# idem para o Revisor
revisor:
    pi --model "lmstudio/qwen3.6-27b-mlx" || pi

# --- atalhos de Git/worktree (independentes do pi) ---

# lista worktrees ativas
worktrees:
    @git worktree list

# remove a worktree de uma issue já mergeada: just clean 42
clean issue:
    #!/usr/bin/env bash
    set -euo pipefail
    REPO_ROOT="$(git rev-parse --show-toplevel)"
    WT_BASE="$(dirname "$REPO_ROOT")/$(basename "$REPO_ROOT")-worktrees"
    WT="$(find "$WT_BASE" -maxdepth 1 -type d -name "*-{{issue}}-*" | head -n1)"
    [ -n "$WT" ] || { echo "nenhuma worktree para #{{issue}}"; exit 0; }
    git worktree remove "$WT" && echo "worktree de #{{issue}} removida: $WT"

# verifica pré-requisitos
doctor:
    #!/usr/bin/env bash
    echo "== doctor =="
    command -v pi   >/dev/null && echo "OK pi"   || echo "FALTA pi"
    command -v gh   >/dev/null && echo "OK gh"   || echo "FALTA gh"
    command -v just >/dev/null && echo "OK just" || echo "FALTA just"
    gh auth status  >/dev/null 2>&1 && echo "OK gh autenticado" || echo "FALTA gh auth login"
    [ -f ~/.pi/agent/models.json ] && echo "OK models.json instalado" || echo "FALTA cp pi-global/models.json ~/.pi/agent/"
    MODELS="$(curl -s http://localhost:1234/v1/models 2>/dev/null)"
    if [ -n "$MODELS" ]; then
      echo "OK LM Studio respondendo"
      echo "$MODELS" | python3 -c 'import sys,json; d=json.load(sys.stdin); [print("   - carregado:", m["id"]) for m in d.get("data",[])]' 2>/dev/null || true
    else
      echo "FALTA LM Studio (ligue o servidor na aba Developer, porta 1234)"
    fi
