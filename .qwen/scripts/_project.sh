#!/usr/bin/env bash
# _project.sh — auto-detecção de stack do projeto, sourced pelos scripts do .qwen/.
#
# Detecta o comando de teste apropriado em ordem de prioridade:
#   1. Variável de ambiente QWEN_TEST_CMD (override explícito)
#   2. Marcador no AGENTS.md raiz: <!-- qwen:test-command: ... -->
#   3. Auto-detecção por arquivos-âncora do projeto
#   4. Erro com instrução clara
#
# Exporta:
#   QWEN_TEST_CMD          — comando completo de teste (ex.: "./mvnw -q test", "pnpm test", "pytest -q")
#   QWEN_WARNINGS_REGEX    — regex para contar warnings de compilação na saída
#                             (default: vazio, sem checagem)
#
# Como sobrescrever no seu projeto:
#   a) Em um shell rc: `export QWEN_TEST_CMD="bun test"`
#   b) Em AGENTS.md (raiz), inclua uma linha HTML-comment:
#        <!-- qwen:test-command: ./gradlew test -->
#        <!-- qwen:warnings-regex: warning: -->

set -uo pipefail

_qwen_detect_test_cmd() {
  # 1. Override por env var
  if [[ -n "${QWEN_TEST_CMD:-}" ]]; then
    return 0
  fi

  # 2. Marcador no AGENTS.md raiz
  local repo_root
  repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
  if [[ -f "$repo_root/AGENTS.md" ]]; then
    local from_agents
    from_agents="$(grep -m1 -oE '<!-- *qwen:test-command: *[^>]*-->' "$repo_root/AGENTS.md" 2>/dev/null \
      | sed -E 's|<!-- *qwen:test-command: *(.*) *-->|\1|' | sed -E 's/ *$//')"
    if [[ -n "$from_agents" ]]; then
      export QWEN_TEST_CMD="$from_agents"
      return 0
    fi
    local warn_re
    warn_re="$(grep -m1 -oE '<!-- *qwen:warnings-regex: *[^>]*-->' "$repo_root/AGENTS.md" 2>/dev/null \
      | sed -E 's|<!-- *qwen:warnings-regex: *(.*) *-->|\1|' | sed -E 's/ *$//')"
    if [[ -n "$warn_re" ]]; then
      export QWEN_WARNINGS_REGEX="$warn_re"
    fi
  fi
  if [[ -n "${QWEN_TEST_CMD:-}" ]]; then
    return 0
  fi

  # 3. Auto-detecção por arquivo-âncora
  cd "$repo_root" 2>/dev/null || true
  if   [[ -x "./mvnw" ]];          then export QWEN_TEST_CMD="./mvnw -q test";        export QWEN_WARNINGS_REGEX="${QWEN_WARNINGS_REGEX:-\[WARNING\]}"
  elif [[ -x "./gradlew" ]];        then export QWEN_TEST_CMD="./gradlew test --quiet"; export QWEN_WARNINGS_REGEX="${QWEN_WARNINGS_REGEX:-warning:}"
  elif [[ -f "pom.xml" ]];          then export QWEN_TEST_CMD="mvn -q test";          export QWEN_WARNINGS_REGEX="${QWEN_WARNINGS_REGEX:-\[WARNING\]}"
  elif [[ -f "build.gradle"      ]] || [[ -f "build.gradle.kts" ]]; then export QWEN_TEST_CMD="gradle test --quiet"; export QWEN_WARNINGS_REGEX="${QWEN_WARNINGS_REGEX:-warning:}"
  elif [[ -f "package.json" ]];     then
        if   grep -q '"test"' package.json 2>/dev/null;             then export QWEN_TEST_CMD="npm test --silent"
        else echo "ERRO: package.json encontrado mas sem script 'test'. Defina QWEN_TEST_CMD em AGENTS.md." >&2; return 1
        fi
  elif [[ -f "pyproject.toml" ]] || [[ -f "setup.py" ]] || [[ -f "pytest.ini" ]]; then
        export QWEN_TEST_CMD="pytest -q"
  elif [[ -f "Cargo.toml" ]];       then export QWEN_TEST_CMD="cargo test --quiet"
  elif [[ -f "go.mod" ]];           then export QWEN_TEST_CMD="go test ./..."
  else
    cat >&2 <<EOF
ERRO: não consegui auto-detectar o comando de teste deste projeto.
Defina explicitamente uma das duas formas:
  1) Variável de ambiente:  export QWEN_TEST_CMD="seu comando de teste"
  2) Marcador no AGENTS.md raiz:  <!-- qwen:test-command: seu comando de teste -->
EOF
    return 1
  fi

  export QWEN_WARNINGS_REGEX="${QWEN_WARNINGS_REGEX:-}"
  return 0
}

_qwen_detect_test_cmd
