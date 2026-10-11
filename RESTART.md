# RESTART — AndroMITM · Fichier de reprise de session

> **Dépose à la racine du dossier de travail Drive.**
> Objectif : permettre la reprise du projet **à jour** dans une nouvelle session (IA ou humain) en moins de 2 minutes.
> Dernière mise à jour : 2026-10-11 · version 0.1.0 · état : **v1 fonctionnelle, testée en mode simulation**

---

## 1. Le projet en 30 secondes

**AndroMITM** = suite de pentest MITM pour Android, 100% interface graphique.
- **Cœur** : backend Node.js (tourne dans Termux/Android ou desktop Linux) + dashboard web sombre style Kali (i18n FR/EN) servi sur `http://127.0.0.1:8443`.
- **Coquille Electron** incluse pour le mode desktop (Electron ne tourne PAS nativement sous Android — sur le téléphone, le dashboard web est l'interface, ouverte dans Chrome).
- **Modules** : découverte réseau (nmap/arp-scan), spoofing ARP (bettercap/arpspoof), spoofing DNS, interception HTTPS (mitmdump + addon Python), capture pcap (tcpdump), rapports JSON+HTML.
- **Sécurité intégrée** : déclaration d'usage autorisé obligatoire à la première ouverture (les modules d'attaque restent verrouillés sinon), restauration automatique d'`ip_forward`, token API anti-CSRF, écoute localhost par défaut.
- **Mode simulation** (`ANDROMITM_SIM=1`) : démo/développement sans aucun binaire externe ni root.

## 2. Où sont les choses

| Ressource | Emplacement |
|---|---|
| **Repo GitHub (privé, source de vérité du code)** | `https://github.com/alexisfrlopez-debug/andromitm` (branche `main`) |
| **Ce dossier Drive (sauvegardes)** | Folder ID `1Ps1dQJFWgM6H_xpYRSzTqKuOtVNsdDYj` = racine du remote rclone `gdrive:` |
| `gdrive:RESTART.md` | ce fichier |
| `gdrive:andromitm-current/` | arborescence source courante (navigable sur Drive) |
| `gdrive:backup/` | archives zip versionnées `andromitm-AAAAMMJJ-HHMMSS.zip` |
| **Tokens (NE JAMAIS COMMITTER)** | dossier Drive `1s9vpV60izRuE28bEthaH7uhwHqOFI5QQ` : `client_secret_*.json` (OAuth), `oauth_token_relais_*.json` (refresh token), `physichong-*.json` (service account), `token_git.txt` (PAT fine-grained GitHub) |
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

### 3.3 Jalon suivant — synchroniser les 3 canaux
```bash
bash scripts/git-push.sh "description du jalon"   # GitHub
bash scripts/drive-sync.sh                        # Drive (zip versionné + andromitm-current + RESTART.md)
```
⚠ Après toute modification notable, **mettre à jour la section 5 de ce RESTART.md** avant de relancer `drive-sync.sh`.

## 4. État technique à la date de ce fichier

- ✅ Backend complet testé : serveur express+ws, porte d'autorisation (403 sans déclaration), sessions JSONL, 6 modules, rapports JSON+HTML servis par `/api/session-report/<id>`, WebSocket `/ws`, ingestion mitmproxy `/api/internal/flow` (localhost only).
- ✅ Dashboard testé : i18n FR/EN persistée, thème sombre Kali, panneaux modules avec formulaires, table des cibles (bouton « → cible » pré-remplit l'ARP), flux live.
- ✅ Tests en mode simulation passés : discovery (3 hôtes dont 2 Android), ARP/DNS/proxy/capture start/stop, rapport généré, WS broadcast.
- ⚠️ Non testé en réel (pas de matériel réseau ici) : bettercap/arpspoof/tcpdump/mitmdump réels — prévu sur l'appareil cible ; le code gère l'absence de binaire proprement (UI affiche les prérequis).
- ⚠️ Electron non exécutable ici (pas d'affichage) — code standard, à tester sur un desktop avec X.

## 5. Historique & roadmap

### Jalons réalisés (v0.1.0)
1. Scaffold projet (package.json, .gitignore, .env.example)
2. Backend : moteur de modules, sessions, autorisation, 6 modules MITM, mode SIM
3. Dashboard web i18n FR/EN + WebSocket live
4. Coquille Electron + config rclone modèle
5. Scripts : install-termux, run-termux, drive-sync, git-push
6. Documentation FR/EN (README, INSTALL, usage-fr, usage-en), licence MIT + notice légale

### Prochaines étapes proposées (v0.2)
- [ ] Test réel sur appareil Android rooté (bettercap arm64, tcpdump, mitmdump Termux)
- [ ] Détection SSL-pinning : section guide + intégration éventuelle (hors périmètre v1)
- [ ] Module « identité de l'appareil » : fingerprint HTTP/DHCP des cibles Android
- [ ] Capture HTTP complète optionnelle (corps de requêtes) avec chiffrement du dossier de session
- [ ] Export PCAPNG + intégration tshark pour stats de capture
- [ ] Authentification dashboard (mot de passe local) pour le mode `ANDROMITM_HOST=0.0.0.0`
- [ ] Build Electron arm64 (pi/boîtier) + script d'empaquetage
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

# sauvegardes
bash scripts/git-push.sh "jalon X"      # push GitHub (PAT auto depuis .env ou tokens/)
bash scripts/drive-sync.sh              # zip + sync + RESTART.md → Drive

# installation Termux complète
bash scripts/install-termux.sh

# rapport d'une session passée (UI : Sessions & rapports → bouton Rapport)
# GET http://127.0.0.1:8443/api/session-report/<sess-id>  (report.html)
```
