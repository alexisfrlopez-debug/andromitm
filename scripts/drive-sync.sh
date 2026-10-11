#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — drive-sync.sh
# Sauvegarde incrémentielle vers le dossier de travail Drive via rclone.
#  1. Capture de l'environnement (env/)     → scripts/env-snapshot.sh
#  2. Archive versionnée du projet          → backup/andromitm-AAAAMMJJ-HHMMSS.zip
#  3. Sync backup/                          → gdrive:backup/ (+ backup-dir old/<stamp>)
#  4. Sync releases/ (APK signées + SHA256) → gdrive:releases/
#  5. Sync data/ (sessions & rapports)      → gdrive:data/ (+ old/<stamp>)
#  6. RESTART.md                            → racine du dossier de travail
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

# 0) Capture de l'environnement (best effort)
bash "$REPO_ROOT/scripts/env-snapshot.sh" || echo "[andromitm] env-snapshot ignoré (non bloquant)"

# 1) Archive versionnée du projet (hors node_modules/data/tokens/builds)
mkdir -p "${REPO_ROOT}/backup"
cd "${REPO_ROOT}"
zip -qr "backup/andromitm-${STAMP}.zip" . \
  -x "node_modules/*" "data/*" "backup/*" "env/*" "tokens/*" ".env" "*.pcap" "backend/data/*" \
     "apk/.gradle/*" "apk/app/build/*" "apk-rootwizard/build/*"
echo "[andromitm] Archive : backup/andromitm-${STAMP}.zip"

# 2) Sync incrémentiel : backup/ → Drive, anciennes versions conservées
rclone sync "${REPO_ROOT}/backup/" "${REMOTE}:backup/" \
  --backup-dir "${REMOTE}:backup/old/${STAMP}" \
  --exclude "old/**"

# 3) APK signées → gdrive:releases/
if [ -d releases ] && ls releases/*.apk >/dev/null 2>&1; then
  rclone copy "${REPO_ROOT}/releases/" "${REMOTE}:releases/"
  echo "[andromitm] APK synchronisées → ${REMOTE}:releases/"
fi

# 4) Données runtime (sessions/rapports) → gdrive:data/
if [ -d data ] && [ -n "$(ls -A data 2>/dev/null)" ]; then
  rclone sync "${REPO_ROOT}/data/" "${REMOTE}:data/" \
    --backup-dir "${REMOTE}:data/old/${STAMP}"
  echo "[andromitm] Données runtime synchronisées → ${REMOTE}:data/"
fi

# 5) Fichier de reprise à la racine du dossier de travail
if [ -f "RESTART.md" ]; then
  rclone copy "RESTART.md" "${REMOTE}:" --ignore-times
  echo "[andromitm] RESTART.md mis à jour à la racine du dossier de travail"
fi

echo "[andromitm] Sauvegarde Drive terminée (remote=${REMOTE}, stamp=${STAMP})"
