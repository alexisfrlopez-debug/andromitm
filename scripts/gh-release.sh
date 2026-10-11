#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
# AndroMITM — gh-release.sh
# Crée une GitHub Release du repo andromitm et upload les APK de
# releases/ + SHA256SUMS.txt comme assets.
# Usage : bash scripts/gh-release.sh <tag> "<titre>" <body.md>
# Ex.   : bash scripts/gh-release.sh v0.4.0 "v0.4.0 — RootWizard multi-profils" /tmp/rel.md
# Prérequis : PAT dans .env (GITHUB_TOKEN) ou tokens/token_git.txt
# ════════════════════════════════════════════════════════════════
set -euo pipefail

TAG="${1:?tag requis (ex v0.4.0)}"
TITLE="${2:?titre requis}"
BODY_FILE="${3:?fichier de body requis}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PAT=""
if [ -f "$REPO_ROOT/.env" ] && grep -q '^GITHUB_TOKEN=' "$REPO_ROOT/.env"; then
  PAT="$(grep '^GITHUB_TOKEN=' "$REPO_ROOT/.env" | cut -d= -f2- | tr -d ' \n\r')"
fi
[ -z "$PAT" ] && [ -f "$REPO_ROOT/../tokens/token_git.txt" ] && \
  PAT="$(tr -d ' \n\r' < "$REPO_ROOT/../tokens/token_git.txt")"
[ -n "$PAT" ] || { echo "Aucun PAT (.env ou ../tokens/token_git.txt)"; exit 1; }

REPO="${ANDROMITM_REPO:-alexisfrlopez-debug/andromitm}"
BODY="$(cat "$BODY_FILE")"

echo "[gh-release] création de la release ${TAG}…"
python3 - "$PAT" "$REPO" "$BODY" "$TAG" "$TITLE" <<'PY' > /tmp/rel.json
import json, sys, urllib.request
pat, repo, body, tag, title = sys.argv[1:6]
data = json.dumps({
    "tag_name": tag, "target_commitish": "main",
    "name": title,
    "body": body, "draft": False, "prerelease": False
}).encode()
req = urllib.request.Request(
    f"https://api.github.com/repos/{repo}/releases", data=data, method="POST",
    headers={"Authorization": f"Bearer {pat}", "Accept": "application/vnd.github+json",
             "Content-Type": "application/json", "User-Agent": "andromitm"})
try:
    print(urllib.request.urlopen(req).read().decode())
except urllib.error.HTTPError as e:
    print(e.read().decode()); sys.exit(1)
PY

REL_ID=$(python3 -c "import json;d=json.load(open('/tmp/rel.json'));print(d.get('id',''))")
if [ -z "$REL_ID" ]; then
  echo "[gh-release] échec création (release déjà existante ?) — récupération de l'existante…"
  python3 - "$PAT" "$REPO" "$TAG" <<'PY' > /tmp/rel.json
import json, sys, urllib.request
pat, repo, tag = sys.argv[1:4]
req = urllib.request.Request(f"https://api.github.com/repos/{repo}/releases/tags/{tag}",
    headers={"Authorization": f"Bearer {pat}", "User-Agent": "andromitm"})
print(urllib.request.urlopen(req).read().decode())
PY
  REL_ID=$(python3 -c "import json;print(json.load(open('/tmp/rel.json'))['id'])")
fi
echo "[gh-release] release id=$REL_ID"

UPLOAD_URL=$(python3 -c "import json;print(json.load(open('/tmp/rel.json'))['upload_url'].split('{')[0])")

for f in "${REPO_ROOT}"/releases/*.apk "${REPO_ROOT}"/releases/SHA256SUMS.txt; do
  echo "[gh-release] upload $(basename "$f")…"
  python3 - "$PAT" "$UPLOAD_URL" "$f" "$(basename "$f")" <<'PY'
import sys, urllib.request
pat, url, path, name = sys.argv[1:5]
data = open(path, "rb").read()
req = urllib.request.Request(f"{url}?name={name}", data=data, method="POST",
    headers={"Authorization": f"Bearer {pat}", "Content-Type": "application/octet-stream",
             "Content-Length": str(len(data)), "User-Agent": "andromitm"})
try:
    r = urllib.request.urlopen(req)
    print(f"  OK ({r.status})")
except urllib.error.HTTPError as e:
    body = e.read().decode()
    if e.code == 422:
        print("  déjà présent (422) — ignoré")
    else:
        print(f"  ERREUR {e.code}: {body[:200]}"); sys.exit(1)
PY
done

echo "[gh-release] terminé → https://github.com/${REPO}/releases/tag/${TAG}"
