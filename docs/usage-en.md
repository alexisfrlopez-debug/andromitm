# AndroMITM — User Guide (EN)

## 1. Usage frame

AndroMITM is built for **authorized penetration testing**: lab networks, your own estate, engagements with written consent. On first launch the dashboard shows an **authorized-use statement** that must be accepted to unlock attack modules (ARP, DNS, proxy, capture). Every action is logged into the local session (`data/sessions/…/events.jsonl`). Legal references: France — art. 323-1 et seq. of the penal code; elsewhere check your local jurisdiction.

## 2. First launch

```bash
node backend/server.js
# or simulation mode:
ANDROMITM_SIM=1 node backend/server.js
```
Open http://127.0.0.1:8443 — read the statement, tick the box, "Unlock the suite". The **FR / EN** switch at the bottom of the sidebar changes the whole UI.

## 3. Typical engagement workflow

### Step 1 — Create a session
"Start session" (top bar): all events and captures go to `data/sessions/sess-…/`.

### Step 2 — Discovery
**Discovery** page: interface + CIDR (auto when empty), scan interval. Hosts appear with their MAC vendor; Android devices are marked ◆. "→ target" button copies an IP into the ARP module form.

### Step 3 — ARP spoofing (root required)
**ARP Spoofing** page: interface, targets (JSON array), gateway (auto when empty), full duplex. The module enables `ip_forward`, starts bettercap (or arpspoof), and **restores `ip_forward` on stop**. Watch `arp.raw` events in the live feed.

### Step 4 — HTTPS interception
**HTTPS Proxy** page: `regular` mode (explicit proxy: point the target device's proxy at this host:8080) or `transparent` (after ARP + iptables redirect, root). Install the **mitmproxy CA** on the target (see INSTALL.md §2). Flows (method, host, path, status, sizes) stream live and persist to `flows.jsonl` — body contents are **not** stored in v1 (metadata only).

### Step 5 — DNS spoofing (root required)
**DNS Spoofing** page: entries `[{domain, ip}]`. The module writes a temporary hosts file, starts bettercap `dns.spoof` (or dnsspoof) and cleans up on stop.

### Step 6 — pcap capture (root required)
**pcap Capture** page: interface + BPF filter (e.g. `host 192.168.1.10`). The `.pcap` lands in the session folder — open it in Wireshark.

### Step 7 — Report
Close the session ("Close session") then **Sessions & reports** page → **Report** button: generates `report.json` + standalone `report.html` (stats per module/type, flows, DNS, hosts). Attach the HTML to the engagement deliverable.

## 4. Run profiles

| Profile | Detection | Behavior |
|---|---|---|
| `termux-root` | Termux + su | everything enabled |
| `termux` | Termux without su | attack modules denied cleanly (UI shows missing prerequisites) |
| `desktop-root` / `desktop` | Linux/macOS | same, PC side |
| `sim` | `ANDROMITM_SIM=1` | no external binaries needed; fake events for demo/dev |

## 5. Suite security

- Listens on **127.0.0.1** by default; `ANDROMITM_HOST=0.0.0.0` only in a closed lab.
- Local API token (header `X-AndroMITM-Token`) against CSRF from hostile pages in the same browser.
- Internal route `/api/internal/flow` only accepts localhost (mitmproxy addon).
- Drive/GitHub tokens are **never** committed (`.gitignore`: `tokens/`, `.env`).

## 6. Engagement best practices

1. Keep written consent + explicit IP scope in the engagement folder.
2. One AndroMITM session per engagement; name the session after the case reference.
3. Check `ip_forward` state before AND after the test (the module restores it, but verify).
4. Export the HTML report + events.jsonl into the client deliverable; keep the `.pcap` files.
5. Run `bash scripts/drive-sync.sh` daily (incremental Drive backup) and `bash scripts/git-push.sh` at each milestone.
