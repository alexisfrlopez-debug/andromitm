#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — env-snapshot.sh
# Capture l'environnement d'exécution (système, versions, paquets,
# configs shell/termux) dans env/ — embarqué dans le zip Drive et
# synchronisé vers gdrive:backup (via l'archive). Sert à reconstruire
# l'environnement Termux/desktop après perte.
# Usage : bash scripts/env-snapshot.sh
# ════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_DIR="$REPO_ROOT/env"
mkdir -p "$ENV_DIR"

{
  echo "# AndroMITM — snapshot d'environnement"
  echo "date: $(date -Is)"
  echo
  echo "## système"
  uname -a 2>/dev/null || true
  echo
  echo "## versions"
  (java -version 2>&1 | head -2) || true
  (node -v) 2>/dev/null || true
  (npm -v) 2>/dev/null || true
  (python3 -V) 2>/dev/null || true
  (rclone version 2>/dev/null | head -1) || true
  (git --version) 2>/dev/null || true
  (gradle --version 2>/dev/null | grep Gradle) || true
  echo
  echo "## paquets installés (pkg/dpkg)"
  if command -v pkg >/dev/null 2>&1; then
    pkg list-installed 2>/dev/null || true
  else
    dpkg -l 2>/dev/null | tail -n +6 | awk '{print $2" "$3}' || true
  fi
} > "$ENV_DIR/system.txt"

[ -f "$HOME/.bashrc" ] && cp -f "$HOME/.bashrc" "$ENV_DIR/bashrc" 2>/dev/null || true
if [ -d "$HOME/.termux" ]; then
  mkdir -p "$ENV_DIR/termux"
  cp -f "$HOME/.termux/"* "$ENV_DIR/termux/" 2>/dev/null || true
fi
[ -f "$REPO_ROOT/.env.example" ] && cp -f "$REPO_ROOT/.env.example" "$ENV_DIR/" || true

echo "[andromitm] Environnement capturé dans env/ (inclus dans l'archive Drive)"
