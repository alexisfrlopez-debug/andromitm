# RESTART — AndroMITM · Fichier de reprise de session

> **Dépose à la racine du dossier de travail Drive.**
> Objectif : permettre la reprise du projet **à jour** dans une nouvelle session (IA ou humain) en moins de 2 minutes.
> Dernière mise à jour : 2026-10-11 · version **0.3.0 (Node + 2 APK Android : AndroMITM & RootWizard)** · état : **v0.2.0 signée + RootWizard v0.1.0, distributions GitHub Releases + Drive actives**

---

## 1. Le projet en 30 secondes

**AndroMITM** = suite de pentest MITM pour Android, 100% interface graphique, **deux livrables** :

### A. APK Android native (v0.2.0) — `AndroMITM-v0.2.0.apk`
- Java + Material 3, thème sombre Kali, i18n FR/EN (suivant la langue de l'appareil).
- Écran de **déclaration d'usage autorisé** au premier lancement → cockpit 3 onglets : **Pilotage** (moteur + switches ARP/DNS/Sniffer), **Cibles** (hôtes découverts, badge Android, sélection au doigt), **Journal** (flux live).
- **Moteur bettercap v2.25 android-armv7 EMBARQUÉ** dans l'APK (jniLibs + libusb-1.0.so) : l'app le lance via `su`, lui parle en REST API locale (127.0.0.1:8081) et consomme `/api/events` en continu.
- Découverte live via `net.recon`, ARP/DNS spoof et net.sniff pilotés par switches ; `ip_forward` activé au lancement et **restauré à l'arrêt**.
- Paramètre **« chemin moteur custom »** pour les appareils 64-bits-only (le binaire embarqué est 32-bit) : pointer vers un bettercap aarch64 compilé/obtenu par ailleurs (ex. via Termux).
- Signature : keystore self-signed `andromitm.keystore` (dossier Drive « tokens » + local `tokens/`, mot de passe dans `keystore_pass.txt`). SHA-256 cert : `df934caaa0d80970c475f21c8a08813ef66b5840b11e79b227021e5df656a3b3`.

### B. Suite Node/Termux (v0.1.0) — dashboard web
Cœur Node.js (Termux ou desktop) + dashboard web sombre i18n FR/EN sur `http://127.0.0.1:8443` + coquille Electron. Modules : discovery (nmap), ARP (bettercap/arpspoof), DNS, proxy mitmproxy + addon, capture tcpdump, rapports JSON+HTML, mode simulation `ANDROMITM_SIM=1`.

### C. APK RootWizard (v0.1.0) — `RootWizard-v0.1.0.apk`
Assistant de root guidé, ciblé **Blackview Xplore1 (DokeOS 4.2 / Android 15)**, adaptatif MediaTek/Unisoc/Qualcomm. UI 100% programmatique (aucune dépendance, minSdk 26 / target 34), build sans Gradle (`scripts/build-rootwizard.sh` : aapt2 + javac + d8 + apksigner). Fonctions : détection d'état (bootloader via `ro.boot.verifiedbootstate`/`flash.locked`, OEM unlocking/adb via Settings.Global, Magisk via PackageManager+queries, test root via `su -c id`), 8 étapes guidées (sauvegarde → options dev → unlock bootloader → init_boot.img → patch Magisk v27+ → flash → vérification), boutons copier-commandes/lien firmware/lien Magisk. Affiche clairement qu'aucune APK ne peut rooter Android 15 en un clic réel ; mêmes signature/keystore que la suite.

## 2. Où sont les choses

| Ressource | Emplacement |
|---|---|
| **Repo GitHub (privé, source de vérité du code)** | `https://github.com/alexisfrlopez-debug/andromitm` (branche `main`) |
| **GitHub Releases (APK téléchargeables)** | page Releases du repo — tag `v0.3.0` : `AndroMITM-v0.2.0.apk` + `RootWizard-v0.1.0.apk` (+ `releases/` committé dans main) |
| **Ce dossier Drive (sauvegardes)** | Folder ID `1Ps1dQJFWgM6H_xpYRSzTqKuOtVNsdDYj` = racine du remote rclone `gdrive:` |
| `gdrive:RESTART.md` | ce fichier |
| `gdrive:andromitm-current/` | arborescence source courante (navigable sur Drive) |
| `gdrive:backup/` | archives zip versionnées `andromitm-AAAAMMJJ-HHMMSS.zip` (anciennes versions dans `old/`) |
| `gdrive:releases/` | APK signées + SHA256SUMS.txt |
| `gdrive:data/` | données runtime (sessions/rapports), si présentes |
| **Tokens (NE JAMAIS COMMITTER)** | dossier Drive `1s9vpV60izRuE28bEthaH7uhwHqOFI5QQ` : `client_secret_*.json` (OAuth), `oauth_token_relais_*.json` (refresh token), `physichong-*.json` (service account), `token_git.txt` (PAT fine-grained GitHub), `andromitm.keystore` + `keystore_pass.txt` (signature APK) |
| **Local (machine de cette session)** | `/home/z/my-project/andromitm/` (code) · `/home/z/my-project/tokens/` (secrets, hors repo) · `~/.config/rclone/rclone.conf` (remote `gdrive:` ancré sur le dossier de travail) |

## 3. Reprendre dans une nouvelle session — procédure

### 3.1 Contexte à donner à l'IA (copier-coller)
```
Reprise du projet AndroMITM (suite pentest MITM Android, GUI).
- Le fichier RESTART.md est à la racine du dossier de travail Drive — lis-le d'abord.
- Dossier Drive de travail ID : 1Ps1dQJFWgM6H_xpYRSzTqKuOtVNsdDYj
- Dossier Drive des tokens ID : 1s9vpV60izRuE28bEthaH7uhwHqOFI5QQ
- Repo GitHub privé : alexisfrlopez-debug/andromitm (PAT dans le dossier tokens, fichier token_git.txt)
- Le code à jour est sur GitHub (main) et sur Drive (andromitm-current/ + backup/*.zip).
- Règle de sécurité : jamais de tokens dans le repo (tokens/ et .env sont gitignorés).
```

### 3.2 Reconstruire l'environnement local (nouvelle machine)
```bash
# 1. Récupérer le code (GitHub en priorité)
git clone https://github.com/alexisfrlopez-debug/andromitm.git
cd andromitm && npm install

# … ou via Drive si GitHub inaccessible :
rclone copy gdrive:andromitm-current ./andromitm --exclude "node_modules/**"

# 2. Configurer rclone (tokens dans le dossier Drive « tokens »)
#    → modèle : rclone/rclone.conf.example (root_folder_id = 1Ps1dQJFWgM6H_xpYRSzTqKuOtVNsdDYj)

# 3. PAT GitHub → .env (jamais commité)
echo 'GITHUB_TOKEN=<contenu de token_git.txt>' > .env

# 4. Tester
ANDROMITM_SIM=1 node backend/server.js   # → http://127.0.0.1:8443
```

### 3.3 Après CHAQUE jalon — synchroniser les 3 canaux (UNE commande)
```bash
bash scripts/sync-all.sh "description du jalon"   # GitHub (commit+push) PUIS Drive
# équivaut à : git-push.sh + drive-sync.sh
```
`drive-sync.sh` couvre désormais : env snapshot → `env/`, archive versionnée → `gdrive:backup/`, **APK → `gdrive:releases/`**, données runtime → `gdrive:data/`, RESTART.md à la racine.
⚠ Après toute modification notable, **mettre à jour la section 5 de ce RESTART.md** avant de relancer `sync-all.sh`.

## 4. État technique à la date de ce fichier

- ✅ Backend complet testé : serveur express+ws, porte d'autorisation (403 sans déclaration), sessions JSONL, 6 modules, rapports JSON+HTML servis par `/api/session-report/<id>`, WebSocket `/ws`, ingestion mitmproxy `/api/internal/flow` (localhost only).
- ✅ Dashboard testé : i18n FR/EN persistée, thème sombre Kali, panneaux modules avec formulaires, table des cibles (bouton « → cible » pré-remplit l'ARP), flux live.
- ✅ Tests en mode simulation passés : discovery (3 hôtes dont 2 Android), ARP/DNS/proxy/capture start/stop, rapport généré, WS broadcast.
- ⚠️ Non testé en réel (pas de matériel réseau ici) : bettercap/arpspoof/tcpdump/mitmdump réels — prévu sur l'appareil cible ; le code gère l'absence de binaire proprement (UI affiche les prérequis).
- ⚠️ Electron non exécutable ici (pas d'affichage) — code standard, à tester sur un desktop avec X.

## 5. Historique & roadmap

### Jalons réalisés
1. **v0.1.0** — Scaffold projet (package.json, .gitignore, .env.example)
2. **v0.1.0** — Backend : moteur de modules, sessions, autorisation, 6 modules MITM, mode SIM
3. **v0.1.0** — Dashboard web i18n FR/EN + WebSocket live
4. **v0.1.0** — Coquille Electron + config rclone modèle
5. **v0.1.0** — Scripts : install-termux, run-termux, drive-sync, git-push
6. **v0.1.0** — Documentation FR/EN (README, INSTALL, usage-fr, usage-en), licence MIT + notice légale
7. **v0.1.0** — RESTART.md (ce fichier)
8. **v0.2.0** — **APK Android native compilée et signée** : cockpit Material 3, moteur bettercap v2.25 embarqué (armv7 + libusb), REST API + flux d'événements, cibles au doigt, i18n FR/EN. Sources dans `apk/` ; build : `cd apk && gradle assembleRelease` (JDK 17 + SDK Android requis, cf. §3bis).
9. **v0.3.0** — **Distribution APK sur GitHub** : cause du « je ne vois pas l'APK sur GitHub » identifiée (`.gitignore` excluais `*.apk`, pas de Release) → whitelist `!releases/*.apk`, APK binaires committées dans `releases/` (+ SHA256SUMS), **GitHub Release tag v0.3.0** avec les 2 APK en assets. Nouvelle **APK RootWizard v0.1.0** (root guidé Blackview Xplore1 / DokeOS 4.2 / Android 15, build sans Gradle). Synchro systématique renforcée : `sync-all.sh` (GitHub+Drive en 1 commande), env-snapshot (paquets/configs → zip Drive), `drive-sync.sh` → `gdrive:releases/` + `gdrive:data/`.

### Reconstruire l'environnement de build APK (§3bis)
- JDK 17 complet (Temurin) : `/home/z/android-build/jdk-17.0.20.1+1` (tarball Adoptium, pas d'apt)
- Android SDK : `/home/z/android-build/android-sdk` (cmdline-tools + platform-34 + build-tools 34.0.0)
- Gradle 8.7 : `/home/z/android-build/gradle-8.7`
- `apk/local.properties` → `sdk.dir=…` ; keystore lu dans `../../../tokens/`
- **APK AndroMITM (Gradle)** : `cd apk && JAVA_HOME=/home/z/android-build/jdk-17.0.20.1+1 gradle assembleRelease --no-daemon`
- **APK RootWizard (sans Gradle)** : `JAVA_HOME=/home/z/android-build/jdk-17.0.20.1+1 bash scripts/build-rootwizard.sh` → `releases/RootWizard-v0.1.0.apk`
- Les deux APK sortent dans `releases/` (committé sur GitHub, whitelisté dans .gitignore via `!releases/*.apk`)
- ⚠ bettercap v2.41+ ne publie plus de binaires ARM linux — binaire embarqué = release officielle android_armv7 v2.25 (+ libusb 1.0.30 arm de Termux)
- ⚠ Après un build : créer/mettre à jour la **GitHub Release** (voir §7) puis `sync-all.sh`

### Prochaines étapes proposées (v0.4)
- [ ] **Rooter le Xplore1 en suivant RootWizard** (déverrouillage bootloader + Magisk init_boot) — prérequis du test réel
- [ ] Test réel de l'APK sur le Xplore1 rooté (bettercap v2.25 armv7 ; si 64-bits-only : « chemin moteur custom »)
- [ ] Compilation d'un bettercap aarch64 récent pour appareils 64-bits-only (CGO + libpcap cross) — alimente le « chemin moteur custom »
- [ ] APK : onglet Flux HTTP (vue dédiée net.sniff), export du journal, thème clair optionnel
- [ ] Détection SSL-pinning : section guide + intégration éventuelle (hors périmètre)
- [ ] Module « identité de l'appareil » : fingerprint HTTP/DHCP des cibles Android
- [ ] Capture HTTP complète optionnelle (corps de requêtes) avec chiffrement du dossier de session
- [ ] Authentification dashboard (mot de passe local) pour le mode `ANDROMITM_HOST=0.0.0.0`
- [ ] Rotation des tokens Drive/GitHub (voir section 6)

## 6. Sécurité des tokens — règles permanentes

1. Les tokens vivent **uniquement** dans le dossier Drive « tokens » (et localement dans `/home/z/my-project/tokens/`), **jamais** dans le repo, jamais dans un commit, jamais dans le zip de backup (exclus par .gitignore et l'option -x de zip).
2. Le PAT GitHub est fine-grained : le limiter au repo `andromitm`, révoquer/renouveler régulièrement depuis https://github.com/settings/personal-access-tokens.
3. Le refresh token OAuth Drive : même discipline — le régénérer si soupçon de fuite ; révoquer via https://myaccount.google.com/permissions.
4. Si une session IA a besoin des tokens : les lire depuis le dossier Drive tokens au moment du besoin, ne pas les afficher en clair dans le chat (valeurs masquées).

## 7. Commandes de référence

```bash
# lancer (Termux ou desktop)
node backend/server.js                  # réel
ANDROMITM_SIM=1 node backend/server.js  # simulation
bash scripts/run-termux.sh [--sim]      # Termux : wake-lock + navigateur

# sauvegardes & distribution
bash scripts/sync-all.sh "jalon X"           # GitHub (commit+push) PUIS Drive (env+zip+releases+data+RESTART)
bash scripts/git-push.sh "jalon X"           # GitHub seul
bash scripts/drive-sync.sh                   # Drive seul

# build APK
JAVA_HOME=/home/z/android-build/jdk-17.0.20.1+1 bash scripts/build-rootwizard.sh   # RootWizard (sans Gradle)
cd apk && JAVA_HOME=/home/z/android-build/jdk-17.0.20.1+1 gradle assembleRelease --no-daemon  # AndroMITM
# GitHub Release (après build) : cf. procédure dans §7 / scripts ci-dessous

# installation Termux complète
bash scripts/install-termux.sh

# rapport d'une session passée (UI : Sessions & rapports → bouton Rapport)
# GET http://127.0.0.1:8443/api/session-report/<sess-id>  (report.html)
```

### Créer / mettre à jour une GitHub Release avec les APK
```bash
PAT=$(tr -d ' \n\r' < tokens/token_git.txt)
REPO=alexisfrlopez-debug/andromitm
# 1. créer la release (adapter tag/body)
curl -s -X POST -H "Authorization: Bearer $PAT" https://api.github.com/repos/$REPO/releases \
  -d '{"tag_name":"v0.3.0","target_commitish":"main","name":"v0.3.0 — APK","body":"AndroMITM + RootWizard","draft":false,"prerelease":false}' > /tmp/rel.json
# 2. uploader les assets (URL retournée dans upload_url, {name} à remplacer)
URL=$(python3 -c "import json;print(json.load(open('/tmp/rel.json'))['upload_url'].split('{')[0])")
curl -s -X POST -H "Authorization: Bearer $PAT" -H "Content-Type: application/octet-stream" \
  --data-binary @releases/AndroMITM-v0.2.0.apk "$URL?name=AndroMITM-v0.2.0.apk" >/dev/null
curl -s -X POST -H "Authorization: Bearer $PAT" -H "Content-Type: application/octet-stream" \
  --data-binary @releases/RootWizard-v0.1.0.apk "$URL?name=RootWizard-v0.1.0.apk" >/dev/null
```
