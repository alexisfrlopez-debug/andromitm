#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — drive-sync.sh
# Sauvegarde incrémentielle vers le dossier de travail Drive via rclone.
#  - sync du dossier backup/ -> gdrive:backup/ (versions horodatées)
#  - --backup-dir conserve les fichiers remplacés (incrémentiel propre)
# Prérequis : rclone configuré (voir rclone/rclone.conf.example)
# Usage : bash scripts/drive-sync.sh
# ════════════════════════════════════════════════════════════════
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REMOTE="${ANDROMITM_DRIVE_REMOTE:-gdrive}"
STAMP="$(date +%Y%m%d-%H%M%S)"

command -v rclone >/dev/null 2>&1 || { echo "rclone absent — voir INSTALL.md"; exit 1; }

echo "[andromitm] Vérification du remote ${REMOTE}…"
rclone lsd "${REMOTE}:" >/dev/null 2>&1 || { echo "remote ${REMOTE} injoignable — vérifier rclone.conf"; exit 1; }

# 1) Archive versionnée du projet (hors node_modules/data/tokens)
mkdir -p "${REPO_ROOT}/backup"
cd "${REPO_ROOT}"
zip -qr "backup/andromitm-${STAMP}.zip" . \
  -x "node_modules/*" "data/*" "backup/*" "tokens/*" ".env" "*.pcap" "backend/data/*"
echo "[andromitm] Archive : backup/andromitm-${STAMP}.zip"

# 2) Sync incrémentiel : le contenu courant dans backup/current/, l'ancien vers backup/old/
rclone sync "${REPO_ROOT}/backup/" "${REMOTE}:backup/" \
  --backup-dir "${REMOTE}:backup/old/${STAMP}" \
  --exclude "old/**"

# 3) Copie du fichier de reprise à la racine du dossier de travail, s'il existe
if [ -f "RESTART.md" ]; then
  rclone copy "RESTART.md" "${REMOTE}:" --ignore-times
  echo "[andromitm] RESTART.md mis à jour à la racine du dossier de travail"
fi

echo "[andromitm] Sauvegarde Drive terminée (remote=${REMOTE}, stamp=${STAMP})"
