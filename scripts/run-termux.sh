#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — run-termux.sh
# Lance la suite sous Termux : wake-lock + serveur + ouverture du navigateur.
# Usage : bash scripts/run-termux.sh [--sim]
# ════════════════════════════════════════════════════════════════
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock

if [ "${1:-}" = "--sim" ]; then
  export ANDROMITM_SIM=1
fi

# ouvre le navigateur après démarrage du serveur
( sleep 2; command -v termux-open-url >/dev/null 2>&1 && termux-open-url "http://127.0.0.1:8443" \
  || command -v termux-open >/dev/null 2>&1 && termux-open "http://127.0.0.1:8443" || true ) &

exec node backend/server.js
