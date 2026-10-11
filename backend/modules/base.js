/**
 * AndroMITM — modules/base.js
 * Classe de base de tous les modules : cycle start/stop, vérification des
 * dépendances, mode simulation et émission d'événements normalisés.
 */
'use strict';

const { SIM, whichTool, hasRoot, privileged, listInterfaces } = require('../lib/tools');
const { startProcess, stopProcess, isRunning } = require('../lib/proc');

class ModuleBase {
  /**
   * @param {string} id identifiant court (ex: 'arp')
   * @param {object} opts { requires: [{tool, reason}], needsRoot, attackModule }
   */
  constructor(id, opts = {}) {
    this.id = id;
    this.requires = opts.requires || [];
    this.needsRoot = !!opts.needsRoot;
    this.attackModule = !!opts.attackModule; // verrouillé tant que l'autorisation n'est pas acceptée
    this.engine = null;
    this.state = 'idle'; // idle | running | error
    this.startedAt = null;
    this.lastError = null;
    this.cfg = {};
  }

  attachEngine(engine) { this.engine = engine; }

  emit(level, type, data) { this.engine.emitEvent(this.id, level, type, data); }

  /** Vérifie dépendances + privilèges + autorisation. Retourne {ok, problems[]} */
  check(authOk) {
    const problems = [];
    if (this.attackModule && !authOk) problems.push({ code: 'not-authorized', reason: 'déclaration d\'usage autorisé non acceptée' });
    // en SIM, on ne teste ni binaires ni root (moteur factice) — mais l'autorisation reste exigée
    if (!SIM) {
      for (const req of this.requires) {
        if (!whichTool(req.tool)) problems.push({ code: 'missing-tool', tool: req.tool, reason: req.reason });
      }
      if (this.needsRoot && !hasRoot()) problems.push({ code: 'no-root', reason: 'privilèges root requis (su)' });
    }
    return { ok: problems.length === 0, problems };
  }

  /** État complet renvoyé à l'UI. */
  meta() {
    const deps = this.requires.map((r) => ({ tool: r.tool, found: !!whichTool(r.tool), reason: r.reason }));
    return {
      id: this.id,
      state: this.state,
      attackModule: this.attackModule,
      needsRoot: this.needsRoot,
      sim: SIM,
      startedAt: this.startedAt,
      lastError: this.lastError,
      deps,
      runtime: this.runtimeInfo ? this.runtimeInfo() : {},
    };
  }

  start(cfg = {}, authOk = false) {
    if (this.state === 'running') return { ok: false, error: 'already running' };
    const check = this.check(authOk);
    if (!check.ok) {
      this.state = 'error';
      this.lastError = check.problems.map((p) => p.code).join(',');
      return { ok: false, error: 'check-failed', problems: check.problems };
    }
    this.cfg = cfg;
    try {
      this.onStart(cfg);
      this.state = 'running';
      this.startedAt = new Date().toISOString();
      this.lastError = null;
      this.emit('info', 'module.started', this.safeCfg(cfg));
      return { ok: true };
    } catch (e) {
      this.state = 'error';
      this.lastError = String(e.message || e);
      this.emit('error', 'module.error', { message: this.lastError });
      return { ok: false, error: this.lastError };
    }
  }

  stop() {
    if (this.state !== 'running') return { ok: true };
    try { this.onStop(); } catch (e) { this.emit('warn', 'module.stop-error', { message: String(e.message || e) }); }
    this.state = 'idle';
    this.startedAt = null;
    this.emit('info', 'module.stopped', {});
    return { ok: true };
  }

  /** Contexte d'exécution partagé aux sous-classes. */
  get ctx() {
    return { SIM, whichTool, hasRoot, privileged, listInterfaces, startProcess, stopProcess, isRunning };
  }

  // --- à implémenter dans les sous-classes ---
  onStart(cfg) { throw new Error('not implemented'); }
  onStop() { throw new Error('not implemented'); }
  safeCfg(cfg) { return cfg; } // filtrer les champs sensibles si besoin
}

module.exports = { ModuleBase };
