/**
 * AndroMITM — engine.js
 * Bus d'événements + registre des modules + gestion de session.
 * Chaque module : { id, start(cfg), stop(), status(), meta }.
 * Tous les événements sont : { ts, module, level, type, data }.
 */
'use strict';

const { EventEmitter } = require('events');
const { SessionStore } = require('./lib/store');

class Engine extends EventEmitter {
  constructor() {
    super();
    this.modules = new Map();
    this.sessions = new SessionStore();
    this.setMaxListeners(50);
  }

  /** Enregistre un module. */
  register(module) {
    if (this.modules.has(module.id)) throw new Error('module déjà enregistré: ' + module.id);
    module.attachEngine(this);
    this.modules.set(module.id, module);
  }

  get(id) { return this.modules.get(id); }
  list() { return [...this.modules.values()].map((m) => m.meta()); }

  /** Événement centralisé : broadcast WS + persistance session. */
  emitEvent(moduleId, level, type, data) {
    const evt = { ts: new Date().toISOString(), module: moduleId, level, type, data };
    this.emit('event', evt);
    this.sessions.addEvent(evt);
    return evt;
  }

  /** Journal applicatif niveau serveur (hors modules). */
  log(level, type, data) { return this.emitEvent('core', level, type, data); }

  startSession(name) {
    if (this.sessions.currentSession) this.stopSession();
    const s = this.sessions.create(name);
    this.log('info', 'session.started', { id: s.id, name: s.meta.name });
    return s;
  }
  stopSession() {
    const closed = this.sessions.close();
    if (closed) this.log('info', 'session.stopped', { id: closed.id });
    return closed;
  }
  get session() { return this.sessions.currentSession; }
  listSessions() { return this.sessions.list(); }
}

module.exports = { Engine };
