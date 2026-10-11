# AndroMITM

**Suite de pentest MITM pour Android, avec interface graphique complète.**
Cœur Node.js (tourne dans Termux ou sur desktop) + dashboard web sombre style Kali + coquille Electron.

> ⚠ **Usage autorisé uniquement** — labo, réseau propre, mission de test d'intrusion avec accord écrit du propriétaire. Intercepter le trafic d'autrui sans autorisation est un délit. Une déclaration d'usage autorisé doit être acceptée dans l'interface avant le déverrouillage des modules d'attaque.

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  Dashboard web (Kali dark, i18n FR/EN)                        │
│  ← servi par le backend Node — s'ouvre dans Chrome/Termux-X11 │
├──────────────────────────────────────────────────────────────┤
│  Backend Node.js (express + WebSocket)                        │
│  ├─ Porte d'autorisation (déclaration obligatoire)            │
│  ├─ Sessions & journal d'événements (JSONL)                   │
│  └─ Modules :                                                 │
│      discovery   nmap / arp-scan / ip neigh                   │
│      arp         bettercap / arpspoof + ip_forward            │
│      dns         bettercap dns.spoof / dnsspoof               │
│      proxy       mitmdump (mitmproxy) + addon Python          │
│      capture     tcpdump → .pcap                              │
│      report      rapport JSON + HTML autonome                 │
├──────────────────────────────────────────────────────────────┤
│  Coquille Electron (desktop) — se connecte au backend local   │
│  ou distant (ex. téléphone Termux sur le LAN)                 │
└──────────────────────────────────────────────────────────────┘
```

## Démarrage rapide

### Termux (Android)
```bash
bash scripts/install-termux.sh        # dépendances (pkg + pip + npm + bettercap)
node backend/server.js                # mode réel
ANDROMITM_SIM=1 node backend/server.js  # mode simulation (démo, aucun outil requis)
```
Ouvrir **http://127.0.0.1:8443** dans Chrome (ou `$BROWSER`).

> Sans root : découverte réseau, dashboard, rapports et proxy explicite fonctionnent.
> Les modules ARP / DNS / capture exigent un appareil **rooté** (su) — l'UI affiche les prérequis manquants au lieu de planter.

### Desktop (Linux)
```bash
npm install
node backend/server.js          # dashboard sur http://127.0.0.1:8443
npm run electron                # coquille Electron optionnelle
```

## Scénarios couverts (bidirectionnels)

| Scénario | Où tourne AndroMITM | Modules actifs |
|---|---|---|
| PC → Android (téléphones cibles du LAN) | PC Linux (root) | tous |
| Android → LAN (téléphone rooté attaquant) | Termux root | tous |
| Android non rooté (reconnaissance + analyse) | Termux | discovery, dashboard, rapports, proxy explicite |
| Démo / apprentissage sans matériel | partout | `ANDROMITM_SIM=1` : moteur factice |

## Applications Android (APK)

Deux APK signées (keystore `andromitm`, SHA-256 cert `df934caa…`) — dans [`releases/`](releases/) et en pièce-jointe de la page [Releases GitHub](../../releases) :

| APK | Rôle |
|---|---|
| `AndroMITM-v0.2.0.apk` | **Cockpit MITM natif** (Material 3, FR/EN) : moteur bettercap v2.25 embarqué (armv7 + libusb), découverte live, ARP/DNS/sniff par switches, cibles au doigt, journal live, restauration ip_forward. Paramètre « chemin moteur custom » pour les appareils 64-bits-only. |
| `RootWizard-v0.2.0.apk` | **Assistant de root guidé multi-profils** : détection automatique de la marque + profils **Blackview / AOSP** (Xplore1 · DokeOS 4.2 · Android 15) et **Xiaomi / Redmi-POCO HyperOS** (ex. Redmi Note 14 : compte Mi, délai d'attente officiel, Mi Unlock Tool Windows, fastboot ROM, vbmeta anti-bootloop). Détection d'état (bootloader, OEM unlock, USB debug, Magisk, root), commandes adb/fastboot copiables, liens officiels, test de root intégré. |

> ⚠ **Honnêteté technique** : aucune APK ne peut rooter Android 14/15 « en un vrai clic » (pas d'exploit universel public) — les apps qui le prétendent sont des arnaques/malwares. RootWizard automatise tout ce qui est faisable depuis le téléphone ; le déverrouillage du bootloader et le flash exigent un PC (ou un second téléphone en OTG). C'est le prérequis pour rooter le téléphone et débloquer les modules d'attaque.

Sources : `apk/` (Gradle) et `apk-rootwizard/` (build aapt2/javac/d8 via `scripts/build-rootwizard.sh`). Empreintes : `releases/SHA256SUMS.txt`.

## Sauvegardes systématiques (GitHub + Drive)

**Une seule commande après chaque jalon :**
```bash
bash scripts/sync-all.sh "message du jalon"
```
- **GitHub** : commit + push (`git-push.sh`, PAT lu dans `.env` ou `tokens/token_git.txt`, jamais commité)
- **Drive** (`drive-sync.sh`, rclone) :
  - snapshot d'environnement → `env/` (inclus dans l'archive)
  - archive versionnée → `gdrive:backup/` (ancienne version conservée dans `old/`)
  - APK signées + SHA256 → `gdrive:releases/`
  - sessions & rapports runtime → `gdrive:data/`
  - `RESTART.md` → racine du dossier de travail
- Config rclone modèle : `rclone/rclone.conf.example`

## Documentation

- [INSTALL.md](INSTALL.md) — installation détaillée Termux / desktop, root, mitmproxy CA
- [docs/usage-fr.md](docs/usage-fr.md) — guide d'utilisation complet (FR)
- [docs/usage-en.md](docs/usage-en.md) — full user guide (EN)
- [RESTART.md](RESTART.md) — reprise de projet dans une nouvelle session

## English summary

AndroMITM is a GUI-based MITM pentest suite for Android engagements: Node.js backend (Termux or desktop), dark Kali-style web dashboard (FR/EN), optional Electron shell. Modules: network discovery (nmap), ARP spoofing (bettercap/arpspoof), DNS spoofing, HTTPS interception (mitmproxy), pcap capture (tcpdump), session reporting. **Two signed APKs ship in [releases/](releases/)**: the native bettercap cockpit (FR/EN) and RootWizard, a guided rooting assistant (Blackview Xplore1 / Android 15 — no app can truly one-click root modern Android; the wizard automates everything possible on-device). **Authorized use only** — an in-app statement must be accepted before attack modules unlock. See [docs/usage-en.md](docs/usage-en.md).

## Licence

MIT — voir [LICENSE](LICENSE).
