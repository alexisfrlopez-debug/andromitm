/**
 * AndroMITM — lib/store.js
 * Persistance JSON simple (état global + sessions) sans dépendance.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const DATA_DIR = path.resolve(process.env.ANDROMITM_DATA || path.join(__dirname, '..', 'data'));

function ensureDir(p) { fs.mkdirSync(p, { recursive: true }); }

/** Store JSON générique avec écriture atomique. */
class JsonStore {
  constructor(file, defaults = {}) {
    this.file = path.join(DATA_DIR, file);
    this.defaults = defaults;
    this.data = this._load();
  }
  _load() {
    try { return JSON.parse(fs.readFileSync(this.file, 'utf8')); }
    catch (_) { return { ...this.defaults }; }
  }
  save() {
    ensureDir(path.dirname(this.file));
    const tmp = this.file + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(this.data, null, 2));
    fs.renameSync(tmp, this.file);
  }
  get(key, fallback) { return this.data[key] !== undefined ? this.data[key] : fallback; }
  set(key, value) { this.data[key] = value; this.save(); return value; }
}

/** Gestion des sessions de test (une session = un dossier avec événements + captures). */
class SessionStore {
  constructor() {
    this.sessionsDir = path.join(DATA_DIR, 'sessions');
    ensureDir(this.sessionsDir);
    this.current = null;
  }
  create(name) {
    const id = 'sess-' + new Date().toISOString().replace(/[:.]/g, '-');
    const dir = path.join(this.sessionsDir, id);
    ensureDir(dir);
    this.current = {
      id, name: name || 'Session ' + id, dir,
      startedAt: new Date().toISOString(),
      meta: { id, name: name || id, startedAt: new Date().toISOString(), targets: [], modules: [] },
    };
    this._writeMeta();
    return this.current;
  }
  close() {
    if (!this.current) return null;
    this.current.meta.endedAt = new Date().toISOString();
    this._writeMeta();
    const closed = this.current;
    this.current = null;
    return closed;
  }
  get currentSession() { return this.current; }
  addEvent(evt) {
    if (!this.current) return;
    fs.appendFileSync(path.join(this.current.dir, 'events.jsonl'), JSON.stringify(evt) + '\n');
  }
  setMeta(patch) {
    if (!this.current) return;
    Object.assign(this.current.meta, patch);
    this._writeMeta();
  }
  _writeMeta() {
    fs.writeFileSync(path.join(this.current.dir, 'meta.json'), JSON.stringify(this.current.meta, null, 2));
  }
  list() {
    try {
      return fs.readdirSync(this.sessionsDir).filter((d) =>
        fs.existsSync(path.join(this.sessionsDir, d, 'meta.json'))
      ).map((d) => {
        const meta = JSON.parse(fs.readFileSync(path.join(this.sessionsDir, d, 'meta.json'), 'utf8'));
        return { id: d, ...meta };
      });
    } catch (_) { return []; }
  }
}

module.exports = { DATA_DIR, JsonStore, SessionStore, ensureDir };
