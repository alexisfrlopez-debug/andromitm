# AndroMITM — Guide d'utilisation (FR)

## 1. Cadre d'usage

AndroMITM est conçu pour des **tests d'intrusion autorisés** : réseau de labo, parc dont vous êtes propriétaire, mission avec accord écrit. Au premier lancement, le dashboard affiche une **déclaration d'usage autorisé** qui doit être acceptée pour déverrouiller les modules d'attaque (ARP, DNS, proxy, capture). Toute action reste journalisée dans la session locale (`data/sessions/…/events.jsonl`). Références : en France, art. 323-1 et suivants du code pénal ; ailleurs, vérifier votre juridiction.

## 2. Premier lancement

```bash
node backend/server.js
# ou en simulation :
ANDROMITM_SIM=1 node backend/server.js
```
Ouvrir http://127.0.0.1:8443 — lire la déclaration, cocher, « Déverrouiller la suite ». Le sélecteur **FR / EN** en bas de la barre latérale bascule toute l'interface.

## 3. Workflow type d'un test

### Étape 1 — Créer une session
« Démarrer session » (barre supérieure) : tous les événements et captures seront rangés dans `data/sessions/sess-…/`.

### Étape 2 — Découverte
Page **Découverte** : interface + CIDR (auto si vide), intervalle de scan. Les hôtes apparaissent avec leur constructeur (OUI MAC) ; les appareils Android sont marqués ◆. Bouton « → cible » pour reprendre une IP dans le module ARP.

### Étape 3 — ARP spoofing (root requis)
Page **Spoofing ARP** : interface, cibles (JSON array), passerelle (auto si vide), duplex complet. Le module active `ip_forward`, démarre bettercap (ou arpspoof), et **restaure `ip_forward` à l'arrêt**. Vérifier le flux `arp.raw` dans le journal live.

### Étape 4 — Interception HTTPS
Page **Proxy HTTPS** : mode `regular` (proxy explicite : configurer le proxy de l'appareil cible vers ce poste :8080) ou `transparent` (après ARP + redirection iptables, root). Installer le **CA mitmproxy** sur la cible (voir INSTALL.md §2). Les flux (méthode, hôte, chemin, statut, tailles) remontent en live et sont persistés dans `flows.jsonl` — le contenu des corps n'est **pas** stocké en v1 (métadonnées uniquement).

### Étape 5 — DNS spoofing (root requis)
Page **Spoofing DNS** : entrées `[{domain, ip}]`. Le module écrit un fichier hosts temporaire, démarre bettercap `dns.spoof` (ou dnsspoof) et nettoie à l'arrêt.

### Étape 6 — Capture pcap (root requis)
Page **Capture pcap** : interface + filtre BPF (ex. `host 192.168.1.10`). Le `.pcap` est déposé dans le dossier de session — l'ouvrir avec Wireshark.

### Étape 7 — Rapport
Clôturer la session (« Clôturer session ») puis page **Sessions & rapports** → bouton **Rapport** : génère `report.json` + `report.html` (statistiques par module/type, flux, DNS, hôtes). Le HTML est autonome — à joindre au livrable de mission.

## 4. Modes d'exécution

| Profil | Détection | Comportement |
|---|---|---|
| `termux-root` | Termux + su | tout actif |
| `termux` | Termux sans su | modules attaque refusés proprement (UI : prérequis manquants) |
| `desktop-root` / `desktop` | Linux/macOS | idem côté PC |
| `sim` | `ANDROMITM_SIM=1` | aucun binaire requis ; événements factices pour démo/dev |

## 5. Sécurité de la suite elle-même

- Écoute **127.0.0.1** par défaut ; `ANDROMITM_HOST=0.0.0.0` uniquement en labo fermé.
- Token API local (header `X-AndroMITM-Token`) contre le CSRF de pages web malveillantes ouvertes dans le même navigateur.
- La route interne `/api/internal/flow` n'accepte que localhost (addon mitmproxy).
- Les tokens Drive/GitHub ne sont **jamais** committés (`.gitignore` : `tokens/`, `.env`).

## 6. Bonnes pratiques de mission

1. Consentement écrit à jour + périmètre IP explicite dans le dossier de mission.
2. Une session AndroMITM par mission ; nommer la session avec la référence du dossier.
3. Vérifier l'état `ip_forward` avant ET après le test (le module restaure, mais contrôler).
4. Exporter le rapport HTML + events.jsonl dans le livrable client ; conserver les `.pcap`.
5. `bash scripts/drive-sync.sh` en fin de journée (sauvegarde incrémentielle Drive) et `bash scripts/git-push.sh` à chaque jalon.
