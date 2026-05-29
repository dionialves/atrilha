#!/usr/bin/env bash
# _project.sh — auto-detecção de stack do projeto, sourced pelos scripts do .opencode/.
#
# Detecta o comando de teste apropriado em ordem de prioridade:
#   1. Variável de ambiente OPENCODE_TEST_CMD (override explícito) — fallback QWEN_TEST_CMD
#   2. Marcador no AGENTS.md raiz: <!-- opencode:test-command: ... --> — fallback <!-- qwen:test-command: ... -->
#   3. Auto-detecção por arquivos-âncora do projeto
#   4. Erro com instrução clara
#
# Exporta:
#   OPENCODE_TEST_CMD          — comando completo de teste (ex.: "./mvnw -q test", "pnpm test")
#   OPENCODE_WARNINGS_REGEX    — regex para contar warnings de compilação (default vazio)
#
# Como sobrescrever no seu projeto:
#   a) Em um shell rc: `export OPENCODE_TEST_CMD="bun test"`
#   b) Em AGENTS.md (raiz), inclua uma linha HTML-comment:
#        <!-- opencode:test-command: ./gradlew test -->
#        <!-- opencode:warnings-regex: warning: -->
#   (marcadores qwen:* continuam valendo como fallback — não precisa duplicar)

set -uo pipefail

# Lê um marcador HTML-comment do AGENTS.md, tentando o prefixo opencode: e caindo para qwen:.
# uso: _read_marker <repo_root> <chave>   (ex.: _read_marker "$root" test-command)
_read_marker() {
  local root="$1" key="$2" val=""
  [[ -f "$root/AGENTS.md" ]] || return 0
  for prefix in opencode qwen; do
    val="$(grep -m1 -oE "<!-- *${prefix}:${key}: *[^>]*-->" "$root/AGENTS.md" 2>/dev/null \
      | sed -E "s|<!-- *${prefix}:${key}: *(.*) *-->|\1|" | sed -E 's/ *$//')"
    if [[ -n "$val" ]]; then
      printf '%s' "$val"
      return 0
    fi
  done
}

_opencode_detect_test_cmd() {
  # 1. Override por env var (opencode tem precedência; qwen é fallback de compat)
  if [[ -z "${OPENCODE_TEST_CMD:-}" && -n "${QWEN_TEST_CMD:-}" ]]; then
    export OPENCODE_TEST_CMD="$QWEN_TEST_CMD"
  fi
  if [[ -z "${OPENCODE_WARNINGS_REGEX:-}" && -n "${QWEN_WARNINGS_REGEX:-}" ]]; then
    export OPENCODE_WARNINGS_REGEX="$QWEN_WARNINGS_REGEX"
  fi
  if [[ -n "${OPENCODE_TEST_CMD:-}" ]]; then
    return 0
  fi

  # 2. Marcador no AGENTS.md raiz
  local repo_root
  repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
  local from_agents warn_re
  from_agents="$(_read_marker "$repo_root" test-command)"
  if [[ -n "$from_agents" ]]; then
    export OPENCODE_TEST_CMD="$from_agents"
    warn_re="$(_read_marker "$repo_root" warnings-regex)"
    [[ -n "$warn_re" ]] && export OPENCODE_WARNINGS_REGEX="$warn_re"
    return 0
  fi

  # 3. Auto-detecção por arquivo-âncora
  cd "$repo_root" 2>/dev/null || true
  if   [[ -x "./mvnw" ]];          then export OPENCODE_TEST_CMD="./mvnw -q test";        export OPENCODE_WARNINGS_REGEX="${OPENCODE_WARNINGS_REGEX:-\[WARNING\]}"
  elif [[ -x "./gradlew" ]];        then export OPENCODE_TEST_CMD="./gradlew test --quiet"; export OPENCODE_WARNINGS_REGEX="${OPENCODE_WARNINGS_REGEX:-warning:}"
  elif [[ -f "pom.xml" ]];          then export OPENCODE_TEST_CMD="mvn -q test";          export OPENCODE_WARNINGS_REGEX="${OPENCODE_WARNINGS_REGEX:-\[WARNING\]}"
  elif [[ -f "build.gradle"      ]] || [[ -f "build.gradle.kts" ]]; then export OPENCODE_TEST_CMD="gradle test --quiet"; export OPENCODE_WARNINGS_REGEX="${OPENCODE_WARNINGS_REGEX:-warning:}"
  elif [[ -f "package.json" ]];     then
        if   grep -q '"test"' package.json 2>/dev/null;             then export OPENCODE_TEST_CMD="npm test --silent"
        else echo "ERRO: package.json encontrado mas sem script 'test'. Defina OPENCODE_TEST_CMD em AGENTS.md." >&2; return 1
        fi
  elif [[ -f "pyproject.toml" ]] || [[ -f "setup.py" ]] || [[ -f "pytest.ini" ]]; then
        export OPENCODE_TEST_CMD="pytest -q"
  elif [[ -f "Cargo.toml" ]];       then export OPENCODE_TEST_CMD="cargo test --quiet"
  elif [[ -f "go.mod" ]];           then export OPENCODE_TEST_CMD="go test ./..."
  else
    cat >&2 <<EOF
ERRO: não consegui auto-detectar o comando de teste deste projeto.
Defina explicitamente uma das duas formas:
  1) Variável de ambiente:  export OPENCODE_TEST_CMD="seu comando de teste"
  2) Marcador no AGENTS.md raiz:  <!-- opencode:test-command: seu comando de teste -->
EOF
    return 1
  fi

  export OPENCODE_WARNINGS_REGEX="${OPENCODE_WARNINGS_REGEX:-}"
  return 0
}

_opencode_detect_test_cmd
