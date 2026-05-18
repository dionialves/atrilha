#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://atrilha.app}"
HOST="${BASE_URL#https://}"
FAIL=0

check() {
  local name="$1"; shift
  if "$@"; then
    printf "PASS  %s\n" "$name"
  else
    printf "FAIL  %s\n" "$name"
    FAIL=1
  fi
}

check "/health 200 + status UP" bash -c \
  "curl -fsS '$BASE_URL/health' | grep -q '\"status\":\"UP\"'"

check "/ 200 + html" bash -c \
  "curl -fsS '$BASE_URL/' | grep -qi '<html'"

check "/rota-inexistente-xyz 404 + pagina customizada" bash -c \
  "curl -sS -o /tmp/atrilha-404.html -w '%{http_code}' '$BASE_URL/rota-inexistente-xyz' | grep -q '^404$' && grep -q 'Página não encontrada' /tmp/atrilha-404.html"

check "HSTS header presente" bash -c \
  "curl -sSI '$BASE_URL/' | grep -qi '^strict-transport-security:'"

check "TLS valido por >30 dias" bash -c \
  "exp_epoch=\$(echo | openssl s_client -servername $HOST -connect $HOST:443 2>/dev/null | openssl x509 -noout -enddate | sed 's/notAfter=//' | xargs -I{} date -d {} +%s); now=\$(date +%s); (( (exp_epoch - now) / 86400 > 30 ))"

if [ "$FAIL" -ne 0 ]; then
  echo "Smoke FAILED"; exit 1
fi
echo "Smoke OK"
