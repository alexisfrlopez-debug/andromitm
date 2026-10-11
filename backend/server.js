/**
 * AndroMITM — server.js
 * Serveur HTTP + WebSocket du dashboard. Point d'entrée : `node backend/server.js`.
 *
 * Sécurité :
 *  - écoute 127.0.0.1 par défaut (option --host 0.0.0.0 pour le LAN, conscience du risque)
 *  - modules d'attaque verrouillés tant que la déclaration d'usage autorisé n'est pas acceptée
 *  - routes d'attaque exige le token API (émis localement, header X-AndroMITM-Token)
 */
'use strict';

const path = require('path');
const fs = require('fs');
const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');

const { Engine } = require('./engine');
const auth = require('./lib/auth');
const { runProfile, listInterfaces, SIM } = require('./lib/tools');

const { DiscoveryModule } = require('./modules/discovery');
const { ArpSpoofModule } = require('./modules/arpspoof');
const { DnsSpoofModule } = require('./modules/dnsspoof');
const { ProxyModule } = require('./modules/proxy');
const { CaptureModule } = require('./modules/capture');
const { ReportModule } = require('./modules/report');

// ---------------------------------------------------------------- moteur
const engine = new Engine();
const discovery = new DiscoveryModule();
const arp = new ArpSpoofModule();
const dns = new DnsSpoofModule();
const proxy = new ProxyModule();
const capture = new CaptureModule();
const report = new ReportModule();
for (const m of [discovery, arp, dns, proxy, capture, report]) engine.register(m);

// ---------------------------------------------------------------- app
const app = express();
app.use(express.json({ limit: '1mb' }));
app.use((req, _res, next) => { req.authOk = auth.isAuthorized(); next(); });

const uiDir = path.join(__dirname, 'ui');
app.use(express.static(uiDir));

// ------------------------------------------------------------------ API
const api = express.Router();

// état général (apiToken : protection CSRF — header X-AndroMITM-Token attendu sur les routes sensibles)
api.get('/status', (_req, res) => {
  res.json({
    app: 'AndroMITM',
    version: require('../package.json').version,
    sim: SIM,
    profile: runProfile(),
    apiToken: auth.apiToken,
    authorized: auth.status(),
    interfaces: listInterfaces(),
    session: engine.session ? { id: engine.session.id, name: engine.session.meta.name, startedAt: engine.session.meta.startedAt } : null,
    modules: engine.list(),
  });
});

// porte d'autorisation
api.post('/authorize', (req, res) => {
  const { accepted, lang } = req.body || {};
  if (accepted === true) {
    auth.accept(lang === 'en' ? 'en' : 'fr');
    engine.log('info', 'auth.accepted', { lang: lang || 'fr' });
  } else {
    auth.revoke();
    engine.log('warn', 'auth.revoked', {});
  }
  res.json(auth.status());
});

// démarrage/arrêt d'une session de test
api.post('/session/start', (req, res) => {
  const name = (req.body && req.body.name) || undefined;
  res.json({ session: engine.startSession(name) ? { ...engine.session.meta, dir: engine.session.dir } : null });
});
api.post('/session/stop', (_req, res) => res.json({ closed: engine.stopSession() }));
api.get('/sessions', (_req, res) => res.json({ sessions: engine.listSessions() }));

// modules : start / stop / meta
api.post('/module/:id/start', (req, res) => {
  const m = engine.get(req.params.id);
  if (!m) return res.status(404).json({ error: 'module inconnu' });
  if (m.attackModule && !req.authOk) return res.status(403).json({ error: 'not-authorized' });
  res.json(m.start(req.body || {}, req.authOk));
});
api.post('/module/:id/stop', (req, res) => {
  const m = engine.get(req.params.id);
  if (!m) return res.status(404).json({ error: 'module inconnu' });
  res.json(m.stop());
});
api.get('/module/:id', (req, res) => {
  const m = engine.get(req.params.id);
  if (!m) return res.status(404).json({ error: 'module inconnu' });
  res.json(m.meta());
});

// cibles détectées
api.get('/targets', (_req, res) => res.json({ all: discovery.allTargets(), android: discovery.androidTargets() }));

// rapport de session
const { DATA_DIR } = require('./lib/store');
api.post('/report/generate', (req, res) => {
  let sessionDir = (req.body && req.body.sessionDir) || (engine.session && engine.session.dir);
  const sid = req.body && req.body.sessionId;
  if (!sessionDir && sid) {
    const candidate = path.join(DATA_DIR, 'sessions', sid);
    if (fs.existsSync(candidate)) sessionDir = candidate;
  }
  if (!sessionDir) return res.status(400).json({ error: 'aucune session active ni sessionId fourni' });
  try { res.json(report.generate(sessionDir)); }
  catch (e) { res.status(500).json({ error: String(e.message || e) }); }
});

// sert le rapport HTML généré
api.get('/session-report/:id', (req, res) => {
  const file = path.join(DATA_DIR, 'sessions', req.params.id, 'report.html');
  if (!fs.existsSync(file)) return res.status(404).json({ error: 'rapport absent — générer d\'abord' });
  res.sendFile(file);
});

// route interne : réception des flux mitmproxy (addon python, localhost only)
api.post('/internal/flow', (req, res) => {
  if (req.socket.remoteAddress && !['127.0.0.1', '::1', '::ffff:127.0.0.1'].includes(req.socket.remoteAddress)) {
    return res.status(403).json({ error: 'forbidden' });
  }
  proxy.ingestFlow(req.body || {});
  res.json({ ok: true });
});

app.use('/api', api);

// ---------------------------------------------------------------- serveur
const PORT = parseInt(process.env.ANDROMITM_PORT || '8443', 10);
const HOST = process.env.ANDROMITM_HOST || '127.0.0.1';
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

wss.on('connection', (ws) => {
  ws.send(JSON.stringify({ ts: new Date().toISOString(), module: 'core', level: 'info', type: 'ws.hello', data: { sim: SIM, profile: runProfile() } }));
});
engine.on('event', (evt) => {
  const msg = JSON.stringify(evt);
  for (const client of wss.clients) { try { client.send(msg); } catch (_) { /* ignore */ } }
});

server.listen(PORT, HOST, () => {
  const banner = `
  ┌─────────────────────────────────────────────────┐
  │  AndroMITM v${require('../package.json').version} — suite pentest MITM          │
  │  ${SIM ? 'MODE SIMULATION (aucun binaire requis)' : 'MODE RÉEL — vérifie tes autorisations'}      │
  └─────────────────────────────────────────────────┘
   Dashboard : http://${HOST === '0.0.0.0' ? '<ip-locale>' : HOST}:${PORT}
   Profil    : ${runProfile()}
   Autorisé  : ${auth.isAuthorized() ? 'oui' : 'NON — accepter la déclaration dans l\'UI'}`;
  console.log(banner);
  engine.log('info', 'server.started', { port: PORT, host: HOST, sim: SIM, profile: runProfile() });
});
