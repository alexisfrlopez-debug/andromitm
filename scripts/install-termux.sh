#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — install-termux.sh
# Installation complète des dépendances sous Termux (Android).
# Usage : bash scripts/install-termux.sh
# ════════════════════════════════════════════════════════════════
set -euo pipefail

say()  { printf '\033[1;32m[andromitm]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[andromitm]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[andromitm]\033[0m %s\n' "$*" >&2; exit 1; }

command -v pkg >/dev/null 2>&1 || die "pkg introuvable — ce script est pour Termux (Android)."

say "Mise à jour des dépôts Termux…"
pkg update -y && pkg upgrade -y

say "Paquets de base…"
pkg install -y coreutils root-repo x11-repo nodejs-lts python git openssl make clang binutils \
               nmap arp-scan openssh libffi libxml2 libxslt

say "tcpdump (dépôt root)…"
pkg install -y tcpdump || warn "tcpdump indisponible — capture pcap désactivée (nécessite root de toute façon)."

say "Dépendances Python pour mitmproxy…"
pip install --upgrade pip setuptools wheel || true
pip install mitmproxy || warn "pip install mitmproxy a échoué — tenter : pip install mitmproxy --no-cache-dir"

say "Dépendances Node du projet…"
cd "$(dirname "$0")/.." && npm install --omit=dev

say "bettercap (binaire linux/arm64 officiel)…"
if command -v bettercap >/dev/null 2>&1; then
  say "bettercap déjà présent : $(bettercap -version 2>/dev/null | head -1 || echo ok)"
else
  ARCH=$(uname -m)
  if [ "$ARCH" = "aarch64" ]; then BC_ARCH="linux_arm64"; else BC_ARCH="linux_amd64"; fi
  BC_URL=$(curl -s "https://api.github.com/repos/bettercap/bettercap/releases/latest" \
    | grep -o "https://[^\"]*${BC_ARCH}[^\"]*\.zip" | head -1 || true)
  if [ -n "${BC_URL:-}" ]; then
    TMP=$(mktemp -d)
    curl -L "$BC_URL" -o "$TMP/bc.zip" && unzip -o "$TMP/bc.zip" -d "$TMP"
    if [ -f "$TMP/bettercap" ]; then
      mv "$TMP/bettercap" "$PREFIX/bin/bettercap" && chmod +x "$PREFIX/bin/bettercap"
      say "bettercap installé dans \$PREFIX/bin"
    else
      warn "archive bettercap inattendue — installer manuellement depuis bettercap.org"
    fi
    rm -rf "$TMP"
  else
    warn "release bettercap introuvable — voir bettercap.org/persistence (recompilation possible)"
  fi
fi

say "Vérification des binaires…"
for tool in node python nmap arp-scan tcpdump mitmdump bettercap; do
  command -v "$tool" >/dev/null 2>&1 && say "  ✓ $tool" || warn "  ✗ $tool (optionnel selon modules)"
done

say "Terminé. Démarrer la suite :"
say "  node backend/server.js          # mode réel"
say "  ANDROMITM_SIM=1 node backend/server.js   # mode simulation (démo sans outils)"
