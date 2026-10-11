/**
 * AndroMITM — lib/tools.js
 * Détection des binaires externes, des privilèges et du mode simulation.
 * Aucune dépendance externe : tout est géré avec les modules natifs Node.
 */
'use strict';

const { execFileSync, spawnSync } = require('child_process');
const os = require('os');
const fs = require('fs');

const SIM = process.env.ANDROMITM_SIM === '1';

/** Binaires externes utilisés par les modules (chemins Termux + Linux classiques). */
const TOOL_PATHS = {
  nmap: ['nmap', '/data/data/com.termux/files/usr/bin/nmap', '/usr/bin/nmap', '/usr/local/bin/nmap'],
  'arp-scan': ['arp-scan', '/data/data/com.termux/files/usr/bin/arp-scan', '/usr/bin/arp-scan'],
  bettercap: ['bettercap', '/data/data/com.termux/files/usr/bin/bettercap', '/usr/bin/bettercap', '/usr/local/bin/bettercap'],
  arpspoof: ['arpspoof', '/data/data/com.termux/files/usr/bin/arpspoof', '/usr/sbin/arpspoof'],
  dnsspoof: ['dnsspoof', '/data/data/com.termux/files/usr/bin/dnsspoof', '/usr/sbin/dnsspoof'],
  mitmdump: ['mitmdump', '/data/data/com.termux/files/usr/bin/mitmdump', '/usr/bin/mitmdump', '/usr/local/bin/mitmdump'],
  tcpdump: ['tcpdump', '/data/data/com.termux/files/usr/bin/tcpdump', '/usr/bin/tcpdump', '/data/data/com.termux/files/usr/sbin/tcpdump'],
  su: ['su', '/system/bin/su', '/system/xbin/su', '/sbin/su'],
  iptables: ['iptables', '/system/bin/iptables', '/data/data/com.termux/files/usr/bin/iptables'],
  ip: ['ip', '/data/data/com.termux/files/usr/bin/ip', '/usr/sbin/ip', '/sbin/ip'],
};

const cache = new Map();

/** Retourne le chemin complet d'un binaire s'il existe, sinon null. */
function whichTool(name) {
  if (cache.has(name)) return cache.get(name);
  let found = null;
  const candidates = TOOL_PATHS[name] || [name];
  for (const candidate of candidates) {
    try {
      const res = spawnSync('which', [candidate], { encoding: 'utf8', timeout: 3000 });
      if (res.status === 0 && res.stdout.trim()) { found = res.stdout.trim().split('\n')[0]; break; }
    } catch (_) { /* ignore */ }
  }
  cache.set(name, found);
  return found;
}

/** Détecte si l'on dispose d'un accès root (Android: su ; Linux: uid 0). */
function hasRoot() {
  if (SIM) return true;
  if (typeof process.getuid === 'function' && process.getuid() === 0) return true;
  return !!whichTool('su');
}

/** Envoie une commande via su -c si nécessaire (Android) sinon direct. */
function privileged(cmd, args) {
  if (SIM) return { cmd: 'sim', args: [cmd, ...args] };
  if (process.platform === 'android' && typeof process.getuid === 'function' && process.getuid() !== 0 && whichTool('su')) {
    return { cmd: 'su', args: ['-c', cmd + ' ' + args.join(' ')] };
  }
  return { cmd, args };
}

/** Liste les interfaces réseau disponibles avec leur IPv4. */
function listInterfaces() {
  if (SIM) {
    return [
      { name: 'wlan0', address: '192.168.1.66', cidr: '192.168.1.0/24', mac: 'aa:bb:cc:dd:ee:ff' },
      { name: 'lo', address: '127.0.0.1', cidr: '127.0.0.1/8', mac: '00:00:00:00:00:00' },
    ];
  }
  const out = [];
  const ifaces = os.networkInterfaces();
  for (const [name, addrs] of Object.entries(ifaces)) {
    for (const a of addrs || []) {
      if (a.family === 'IPv4') {
        out.push({ name, address: a.address, cidr: a.cidr, mac: a.mac || '' });
      }
    }
  }
  return out;
}

/** Lit l'état ip_forward (sûr si fichier absent). */
function ipForwardState() {
  try { return fs.readFileSync('/proc/sys/net/ipv4/ip_forward', 'utf8').trim() === '1'; }
  catch (_) { return false; }
}

/** Profil d'exécution courant : termux-root, termux, desktop-root, desktop, sim. */
function runProfile() {
  if (SIM) return 'sim';
  const isTermux = !!(process.env.PREFIX && process.env.PREFIX.includes('com.termux'));
  const root = hasRoot();
  if (isTermux) return root ? 'termux-root' : 'termux';
  return root ? 'desktop-root' : 'desktop';
}

module.exports = { SIM, whichTool, hasRoot, privileged, listInterfaces, ipForwardState, runProfile };
