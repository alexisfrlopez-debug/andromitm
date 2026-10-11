#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — sync-all.sh
# RÈGLE SYSTÉMATIQUE : une commande après CHAQUE jalon →
#   1. GitHub : commit + push (scripts/git-push.sh)
#   2. Drive  : env snapshot + archive versionnée + releases/ (APK)
#               + data/ runtime + RESTART.md à la racine
#               (scripts/drive-sync.sh)
# Usage : bash scripts/sync-all.sh "message du jalon"
# ════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MSG="${1:-jalon andromitm $(date +%Y%m%d-%H%M%S)}"

echo "═══════════════ [1/2] GitHub ═══════════════"
bash "$REPO_ROOT/scripts/git-push.sh" "$MSG"

echo "═══════════════ [2/2] Drive ════════════════"
bash "$REPO_ROOT/scripts/drive-sync.sh"

echo "[andromitm] Synchro complète terminée : GitHub ✓ Drive ✓"
