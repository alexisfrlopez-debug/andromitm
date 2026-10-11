/**
 * AndroMITM — modules/discovery.js
 * Découverte réseau : nmap -sn (ou arp-scan / ip neigh en repli).
 * Identifie les appareils Android (OUI constructeur + empreintes nmap si root).
 */
'use strict';

const { ModuleBase } = require('./base');

// OUI (préfixes MAC) courants des constructeurs Android — détection indicative.
const ANDROID_OUI = [
  ['Samsung', ['00:16:32','18:E8:29','24:0A:16','34:23:BA','38:01:67','3C:5A:B4','40:0E:85','48:D7:05','50:01:BB','54:92:BE','5C:0A:5B','60:AF:6D','64:A2:F9','6C:2F:2C','70:04:1D','74:DF:BF','78:25:AD','7C:11:CB','84:B5:9C','8C:77:12','90:18:7C','94:35:0A','9C:02:98','A0:07:98','A4:07:B6','A8:06:00','AC:5F:3E','B0:72:BF','B4:79:A7','B8:5A:73','BC:72:B1','C0:97:27','C4:42:02','C8:19:F7','CC:07:AB','D0:22:BE','D4:7A:E1','D8:57:EF','DC:71:44','E0:99:71','E4:58:B8','E8:50:8B','EC:1F:72','F0:25:B7','F4:09:D8','F8:E9:4E','FC:F1:52']],
  ['Google', ['30:FD:38','54:60:09','64:BC:0C','74:45:8A','78:9F:70','84:D6:D0','8C:29:37','A4:77:33','AC:DE:48','B0:E2:35','BC:9A:78','C0:63:94','D0:E4:86','E4:7C:F9','F0:B4:79','F4:F5:D8','F8:8F:C1']],
  ['Xiaomi', ['00:9E:C8','0C:1D:AF','10:2A:B3','14:F6:5A','18:59:36','1C:56:FE','20:82:C0','24:69:A5','28:6D:97','2C:5B:B8','30:74:96','34:CE:00','38:A4:ED','3C:BD:D8','40:B0:FA','44:23:7C','48:0F:CF','4C:49:E3','50:64:2B','54:A5:1B','58:44:98','5C:B3:95','60:AB:67','64:09:80','68:3E:34','6C:4D:73','70:18:8B','74:51:BA','78:02:F8','7C:1D:D9','80:AD:23','84:F3:EB','88:C3:97','8C:BE:BE','90:E7:C4','94:FE:22','98:FA:E3','9C:99:A0','A0:86:C6','A4:5E:60','A8:7E:EA','AC:C1:EE','B0:E4:20','B4:6D:83','B8:27:EB','BC:76:70','C0:EE:FB','C4:0B:CB','C8:58:C0','CC:79:CF','D0:17:C2','D4:97:0B','D8:F2:CA','DC:09:4C','E0:B9:4D','E4:46:DA','E8:4E:06','EC:D0:9F','F0:B4:29','F4:B8:5E','F8:A4:5F','FC:64:BA']],
  ['Huawei', ['00:46:4B','00:9A:CD','00:E0:FC','04:02:1F','04:33:C6','04:BD:88','04:F9:38','08:19:A6','08:63:61','08:7A:4C','0C:37:DC','0C:96:BF','10:1B:54','10:44:00','10:C6:1F','14:B9:68','18:DE:D7','1C:15:00','1C:B0:C8','20:08:ED','20:82:C0','24:09:95','24:44:26','24:69:A5','24:DB:AC','28:3E:0B','28:6D:97','2C:CF:67','30:87:30','30:D9:D1','34:00:A3','34:6B:D3','38:2C:4A','3C:97:0E','40:4E:36','44:55:B1','44:6D:6C','48:46:FB','4C:1F:CC','4C:8B:EF','50:01:9C','50:01:BB','54:A5:1B','58:1F:28','58:60:5F','5C:4C:A9','5C:B3:95','60:DE:44','64:16:F0','68:A0:F6','6C:B7:49','70:54:F5','70:72:3C','74:88:1A','78:1D:BA','78:6A:89','78:D7:52','7C:A2:3E','80:71:7A','80:B6:86','84:46:FE','84:BE:52','88:40:B3','88:66:A5','8C:34:FD','8C:F2:28','90:94:97','94:04:9C','98:E7:F5','9C:28:EF','9C:37:74','A0:F4:79','A4:99:47','A4:BE:2B','A8:DC:96','AC:61:5F','AC:E2:15','B0:5B:67','B4:30:52','B4:CD:27','B8:08:CF','BC:25:E0','BC:76:70','C0:70:09','C4:07:2F','C8:0C:C8','C8:D1:5E','CC:35:B4','D0:7A:B5','D4:40:F0','D4:61:2E','D4:6A:6A','D4:98:97','D8:49:0B','DC:09:4C','DC:D2:FC','E0:19:1D','E0:36:76','E4:35:C8','E4:68:A3','E8:BD:D1','EC:23:3D','EC:38:8F','F0:43:47','F4:4C:7F','F4:63:1F','F4:9F:F3','F4:C5:99','F4:CF:E2','F8:01:13','F8:98:B9','FC:48:14']],
  ['OnePlus', ['00:04:4B','04:4F:4C','08:7D:BB','0C:80:63','10:68:3F','14:CC:1D','1C:7B:24','24:0A:C4','28:CC:01','2C:AB:00','34:A7:1E','3C:2A:F4','40:46:55','44:D4:E0','48:5A:3F','4C:66:99','50:85:69','54:DF:0C','5C:B7:8E','64:A7:69','6C:B7:9B','74:23:44','78:02:F8','7C:9E:BD','84:25:DB','88:29:B9','8C:16:45','98:0C:82','A0:1E:C1','A4:42:3F','AC:08:F5','B0:F1:79','B4:FC:4B','C0:11:73','CC:C2:0E','D0:C5:F3','D4:CA:6D','E0:5F:45','E8:88:8B','F0:18:98','FC:26:30']],
];

function vendorForMac(mac) {
  const m = (mac || '').toUpperCase().replace(/-/g, ':');
  for (const [vendor, prefixes] of ANDROID_OUI) {
    for (const p of prefixes) if (m.startsWith(p.toUpperCase())) return vendor;
  }
  return null;
}

class DiscoveryModule extends ModuleBase {
  constructor() {
    super('discovery', {
      requires: [
        { tool: 'nmap', reason: 'scan réseau actif (repli : arp-scan puis ip neigh)' },
        { tool: 'arp-scan', reason: 'repli ARP local (optionnel)' },
      ],
    });
    this.hosts = new Map(); // ip -> host record
    this.timer = null;
  }

  runtimeInfo() { return { hosts: [...this.hosts.values()], count: this.hosts.size }; }
  safeCfg(cfg) { return { cidr: cfg.cidr, interval: cfg.interval }; }

  onStart(cfg) {
    const { SIM, whichTool, privileged, listInterfaces } = this.ctx;
    const iface = cfg.iface || (listInterfaces().find((i) => i.name.startsWith('wlan') || i.name.startsWith('eth')) || {}).name;
    const cidr = cfg.cidr || (listInterfaces().find((i) => i.name === iface) || {}).cidr;
    if (!cidr && !SIM) throw new Error('cidr introuvable : préciser cfg.cidr (ex 192.168.1.0/24)');
    this.hosts.clear();
    const intervalMs = (cfg.interval || 60) * 1000;
    const run = () => this.scan(cidr, iface);
    run();
    this.timer = setInterval(run, intervalMs);
    this.emit('info', 'discovery.started', { cidr, iface, interval: cfg.interval || 60 });
  }

  onStop() {
    if (this.timer) { clearInterval(this.timer); this.timer = null; }
  }

  async scan(cidr, iface) {
    const { SIM, whichTool, privileged } = this.ctx;
    const found = [];
    try {
      if (SIM) {
        found.push(
          { ip: '192.168.1.10', mac: 'a4:07:b6:11:22:33', vendor: 'Samsung', hostname: 'galaxy-s21', android: true },
          { ip: '192.168.1.20', mac: 'a4:5e:60:aa:bb:cc', vendor: 'Xiaomi', hostname: 'mi-11', android: true },
          { ip: '192.168.1.1', mac: '00:e0:fc:12:34:56', vendor: null, hostname: 'router.lan', android: false },
        );
      } else if (whichTool('nmap')) {
        const { cmd, args } = privileged('nmap', ['-sn', '-oG', '-', cidr]);
        const out = await this.runCapture(cmd, args);
        for (const line of out.split('\n')) {
          const m = line.match(/^Host:\s+([\d.]+)\s+\(([^)]*)\)\s+Status: Up\s+Address:\s+([0-9A-Fa-f:]{17})/);
          if (m) found.push({ ip: m[1], hostname: m[2] || '', mac: m[3], vendor: vendorForMac(m[3]), android: !!vendorForMac(m[3]) });
        }
      } else if (whichTool('arp-scan')) {
        const { cmd, args } = privileged('arp-scan', [iface || '--localnet']);
        const out = await this.runCapture(cmd, args);
        for (const line of out.split('\n')) {
          const m = line.match(/^([\d.]+)\s+([0-9A-Fa-f:]{17})\s*(.*)$/);
          if (m) found.push({ ip: m[1], mac: m[2], vendor: vendorForMac(m[2]) || (m[3] || '').trim() || null, android: !!vendorForMac(m[2]), hostname: '' });
        }
      } else {
        // Repli minimal : table ARP noyau
        const out = await this.runCapture('cat', ['/proc/net/arp']);
        for (const line of out.split('\n').slice(1)) {
          const cols = line.trim().split(/\s+/);
          if (cols.length >= 6 && cols[3] !== '00:00:00:00:00:00') {
            found.push({ ip: cols[0], mac: cols[3], vendor: vendorForMac(cols[3]), android: !!vendorForMac(cols[3]), hostname: '' });
          }
        }
      }
    } catch (e) {
      this.emit('error', 'discovery.scan-error', { message: String(e.message || e) });
      return;
    }

    const known = new Set();
    for (const h of found) {
      known.add(h.ip);
      const prev = this.hosts.get(h.ip);
      const rec = { ...h, lastSeen: new Date().toISOString(), isNew: !prev };
      this.hosts.set(h.ip, rec);
      if (!prev) this.emit('info', 'discovery.host-new', rec);
      else if (prev.mac !== h.mac) this.emit('warn', 'discovery.mac-changed', { ip: h.ip, old: prev.mac, new: h.mac });
    }
    for (const [ip, rec] of [...this.hosts]) {
      if (!known.has(ip)) { this.hosts.delete(ip); this.emit('info', 'discovery.host-lost', { ip, mac: rec.mac }); }
    }
    this.emit('info', 'discovery.scan-done', { cidr, total: this.hosts.size, android: [...this.hosts.values()].filter((h) => h.android).length });
  }

  runCapture(cmd, args) {
    return new Promise((resolve, reject) => {
      const { execFile } = require('child_process');
      execFile(cmd, args, { timeout: 120000, maxBuffer: 8 * 1024 * 1024 }, (err, stdout) => {
        if (err && !stdout) reject(err); else resolve(stdout || '');
      });
    });
  }

  /** Liste des cibles Android détectées (pour l'UI). */
  androidTargets() { return [...this.hosts.values()].filter((h) => h.android); }
  allTargets() { return [...this.hosts.values()]; }
}

module.exports = { DiscoveryModule, vendorForMac };
