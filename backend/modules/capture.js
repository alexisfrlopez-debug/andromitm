/**
 * AndroMITM — modules/capture.js
 * Capture brute via tcpdump vers un .pcap de session (analyse a posteriori).
 * Root requis sur Android pour capturer sur l'interface (sinon mode passif refusé proprement).
 */
'use strict';

const { ModuleBase } = require('./base');
const path = require('path');
const { DATA_DIR } = require('../lib/store');

class CaptureModule extends ModuleBase {
  constructor() {
    super('capture', {
      requires: [{ tool: 'tcpdump', reason: 'capture pcap (root requis pour l\'interface réseau)' }],
      needsRoot: true,
      attackModule: true,
    });
    this.pcapPath = null;
  }

  runtimeInfo() { return { pcapPath: this.pcapPath, bytes: this.bytes || 0 }; }
  safeCfg(cfg) { return { iface: cfg.iface, filter: cfg.filter }; }

  onStart(cfg) {
    const { SIM, whichTool } = this.ctx;
    const session = this.engine.session;
    this.pcapPath = path.join(session ? session.dir : DATA_DIR, `capture-${Date.now()}.pcap`);
    this.bytes = 0;

    if (SIM) {
      this.simTimer = setInterval(() => {
        this.bytes += Math.floor(Math.random() * 15000);
        this.emit('info', 'capture.tick', { bytes: this.bytes, sim: true });
      }, 1500);
      return;
    }

    if (!whichTool('tcpdump')) throw new Error('tcpdump introuvable — pkg install root-repo && pkg install tcpdump');
    const args = ['-i', cfg.iface || 'wlan0', '-w', this.pcapPath];
    if (cfg.filter) args.push(cfg.filter); // ex: 'host 192.168.1.10'
    this.ctx.startProcess('capture', 'tcpdump', args, {
      onLine: (line) => {
        const m = line.match(/capturing on|listening on|(\d+) packets captured/i);
        if (m) this.emit('info', 'capture.raw', { line });
      },
      onExit: (code) => { if (this.state === 'running') this.emit('warn', 'capture.exited', { code }); },
    });
  }

  onStop() {
    if (this.simTimer) { clearInterval(this.simTimer); this.simTimer = null; }
    this.ctx.stopProcess('capture');
    this.emit('info', 'capture.saved', { pcapPath: this.pcapPath });
  }
}

module.exports = { CaptureModule };
