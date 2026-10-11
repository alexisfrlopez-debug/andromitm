# INSTALL — AndroMITM

## 1. Termux (Android)

### 1.1 Prérequis
- Android 8+ ; pour les modules d'attaque : appareil **rooté** (MagiskKernelSU…)
- [Termux](https://github.com/termux/termux-app/releases) (F-Droid ou GitHub — pas le Play Store)
- `pkg update -y && pkg upgrade -y` avant tout

### 1.2 Installation automatique
```bash
cd andromitm
bash scripts/install-termux.sh
```
Le script installe : nodejs-lts, python, nmap, arp-scan, tcpdump (root-repo), mitmproxy (pip), bettercap (binaire arm64), dépendances npm.

### 1.3 Lancement
```bash
node backend/server.js            # réel
ANDROMITM_SIM=1 node backend/server.js   # simulation (démo)
```
Dashboard : **http://127.0.0.1:8443** dans Chrome.

Astuce : `termux-wake-lock` évite la mise en veille pendant un test.

### 1.4 Droits et limites Termux
| Capacité | Sans root | Avec root (su) |
|---|---|---|
| Découverte réseau (nmap -sn, arp-scan) | ✅ | ✅ |
| ARP spoofing | ❌ (sockets raw interdits) | ✅ |
| DNS spoofing | ❌ (port 53 < 1024) | ✅ |
| tcpdump live sur wlan0 | ❌ | ✅ |
| mitmproxy mode explicit | ✅ | ✅ |
| mitmproxy mode transparent (iptables) | ❌ | ✅ |
| Rapports, dashboard, sessions | ✅ | ✅ |

## 2. Interception HTTPS sur un appareil Android cible

1. Démarrer le module **proxy** (mitmdump). Le CA est généré dans `~/.mitmproxy/`.
2. Transférer `mitmproxy-ca-cert.pem` sur l'appareil cible (adb, scp, http local).
3. Sur l'appareil cible : **Paramètres → Sécurité → Chiffrement et identifiants → Installer un certificat → Autorité de certification**.
   - Android ≥ 7 : les apps n'acceptent le CA utilisateur que si elles déclarent `networkSecurityConfig` ; pour les apps qui épinglent les certificats, l'interception nécessite un framework d'analyse dédié et reste hors périmètre de la v1.
4. Proxy : `Paramètres → Réseau → APN/Proxy` → IP du poste : **8080** (mode regular), ou redirection iptables (mode transparent, root).

## 3. Desktop Linux

```bash
npm install
node backend/server.js            # 127.0.0.1:8443
ANDROMITM_HOST=0.0.0.0 node backend/server.js   # accessible depuis le LAN (attention)
npm run electron                  # coquille desktop (optionnel)
```

Dépendances système : `sudo apt install nmap arp-scan tcpdump dsniff`
```bash
# bettercap (recommandé) :
sudo apt install bettercap        # Debian/Kali
# ou binaire officiel : https://github.com/bettercap/bettercap/releases
# mitmproxy :
pip install mitmproxy
```

## 4. Electron à distance (PC → téléphone)

```bash
cd electron && npm install && npx electron . --url http://<IP-du-téléphone>:8443
```
Le backend Termux doit écouter sur le LAN : `ANDROMITM_HOST=0.0.0.0 node backend/server.js` — uniquement sur un réseau de labo, jamais sur un Wi-Fi public.

## 5. Sauvegardes

### Drive (rclone)
```bash
# une fois : copier rclone/rclone.conf.example vers ~/.config/rclone/rclone.conf et remplir les tokens
rclone lsd gdrive:                # test
bash scripts/drive-sync.sh        # archive versionnée + sync + RESTART.md
```

### GitHub
```bash
echo 'GITHUB_TOKEN=github_pat_xxx' > .env   # ou tokens/token_git.txt
bash scripts/git-push.sh "mon jalon"
```

## 6. Dépannage

| Symptôme | Cause probable | Correctif |
|---|---|---|
| `check-failed: no-root` | su absent / pas accordé | rooter l'appareil ou rester en modules non-root |
| `missing-tool: bettercap` | binaire non installé | `scripts/install-termux.sh` ou release GitHub |
| mitmdump ne démarre pas (Termux) | roues Python manquantes | `pip install mitmproxy --no-cache-dir` après `pkg install libxml2 libxslt` |
| Dashboard inaccessible depuis le PC | host 127.0.0.1 | `ANDROMITM_HOST=0.0.0.0` (labo uniquement) |
| ARP ne redirige pas le trafic | ip_forward off | le module l'active ; vérifier `cat /proc/sys/net/ipv4/ip_forward` |
| HTTPS illisible | CA non installé sur la cible | section 2 de ce fichier |
