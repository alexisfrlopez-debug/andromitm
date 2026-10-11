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

## Sauvegardes incrémentielles

- **GitHub** : `bash scripts/git-push.sh "message"` (PAT lu dans `.env` ou `tokens/token_git.txt`, jamais commité)
- **Drive** : `bash scripts/drive-sync.sh` (rclone — archive versionnée + sync + RESTART.md à la racine)
- Config rclone modèle : `rclone/rclone.conf.example`

## Documentation

- [INSTALL.md](INSTALL.md) — installation détaillée Termux / desktop, root, mitmproxy CA
- [docs/usage-fr.md](docs/usage-fr.md) — guide d'utilisation complet (FR)
- [docs/usage-en.md](docs/usage-en.md) — full user guide (EN)
- [RESTART.md](RESTART.md) — reprise de projet dans une nouvelle session

## English summary

AndroMITM is a GUI-based MITM pentest suite for Android engagements: Node.js backend (Termux or desktop), dark Kali-style web dashboard (FR/EN), optional Electron shell. Modules: network discovery (nmap), ARP spoofing (bettercap/arpspoof), DNS spoofing, HTTPS interception (mitmproxy), pcap capture (tcpdump), session reporting. **Authorized use only** — an in-app statement must be accepted before attack modules unlock. See [docs/usage-en.md](docs/usage-en.md).

## Licence

MIT — voir [LICENSE](LICENSE).
