#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — git-push.sh
# Commit + push incrémentiel vers le repo privé GitHub.
# Le PAT est lu depuis .env (GITHUB_TOKEN) ou tokens/token_git.txt — jamais commité.
# Usage : bash scripts/git-push.sh "message de commit"
# ════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

PAT=""
if [ -f .env ] && grep -q '^GITHUB_TOKEN=' .env; then
  PAT=$(grep '^GITHUB_TOKEN=' .env | cut -d= -f2- | tr -d ' \n\r')
fi
if [ -z "$PAT" ] && [ -f tokens/token_git.txt ]; then
  PAT=$(tr -d ' \n\r' < tokens/token_git.txt)
fi
if [ -z "$PAT" ]; then
  echo "[andromitm] Aucun PAT trouvé (.env GITHUB_TOKEN ou tokens/token_git.txt) — push anonyme impossible."
  exit 1
fi

REPO_SLUG="${ANDROMITM_REPO:-alexisfrlopez-debug/andromitm}"
MSG="${1:-jalon andromitm $(date +%Y%m%d-%H%M%S)}"

git add -A
git -c user.name="andromitm" -c user.email="andromitm@local" commit -m "$MSG" || echo "[andromitm] rien à committer"
git push "https://x-access-token:${PAT}@github.com/${REPO_SLUG}.git" HEAD:main 2>/dev/null \
  || git push "https://x-access-token:${PAT}@github.com/${REPO_SLUG}.git" HEAD:master

echo "[andromitm] Poussé vers github.com/${REPO_SLUG} — « ${MSG} »"
