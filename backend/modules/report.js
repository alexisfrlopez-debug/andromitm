/**
 * AndroMITM — modules/report.js
 * Génération de rapports de session : JSON + HTML autonome.
 * Agrège les événements de la session courante (ou passée en paramètre).
 * Aucun binaire requis — fonctionne partout, y compris Termux sans root.
 */
'use strict';

const { ModuleBase } = require('./base');
const fs = require('fs');
const path = require('path');

class ReportModule extends ModuleBase {
  constructor() {
    super('report', { requires: [] });
    this.lastReport = null;
  }

  onStart(cfg) { /* module à la demande : la génération passe par generate() */ }
  onStop() { /* rien */ }
  runtimeInfo() { return { lastReport: this.lastReport }; }

  /**
   * Génère le rapport d'une session. sessionDir = dossier avec events.jsonl + meta.json.
   * Retourne { jsonPath, htmlPath, stats }.
   */
  generate(sessionDir) {
    const eventsFile = path.join(sessionDir, 'events.jsonl');
    const metaFile = path.join(sessionDir, 'meta.json');
    const events = fs.existsSync(eventsFile)
      ? fs.readFileSync(eventsFile, 'utf8').split('\n').filter(Boolean).map((l) => { try { return JSON.parse(l); } catch (_) { return null; } }).filter(Boolean)
      : [];
    const meta = fs.existsSync(metaFile) ? JSON.parse(fs.readFileSync(metaFile, 'utf8')) : {};

    const stats = {
      total: events.length,
      byModule: {}, byType: {}, byLevel: {},
      first: events[0] ? events[0].ts : null,
      last: events[events.length - 1] ? events[events.length - 1].ts : null,
    };
    for (const e of events) {
      stats.byModule[e.module] = (stats.byModule[e.module] || 0) + 1;
      stats.byType[e.type] = (stats.byType[e.type] || 0) + 1;
      stats.byLevel[e.level] = (stats.byLevel[e.level] || 0) + 1;
    }
    const flows = events.filter((e) => e.type === 'proxy.flow');
    const dnsRes = events.filter((e) => e.type === 'dns.response');
    const hosts = events.filter((e) => e.type === 'discovery.host-new');

    const jsonPath = path.join(sessionDir, 'report.json');
    fs.writeFileSync(jsonPath, JSON.stringify({ meta, stats, flows, dnsRes, hosts }, null, 2));

    const htmlPath = path.join(sessionDir, 'report.html');
    fs.writeFileSync(htmlPath, this.renderHtml(meta, stats, { flows, dnsRes, hosts }));

    this.lastReport = { jsonPath, htmlPath, stats };
    this.emit('info', 'report.generated', { jsonPath, htmlPath, total: stats.total });
    return this.lastReport;
  }

  renderHtml(meta, stats, data) {
    const esc = (s) => String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const row = (cells) => `<tr>${cells.map((c) => `<td>${esc(c)}</td>`).join('')}</tr>`;
    const table = (title, rows, headers) => rows.length ? `
      <h2>${esc(title)}</h2>
      <table><thead><tr>${headers.map((h) => `<th>${esc(h)}</th>`).join('')}</tr></thead>
      <tbody>${rows.slice(0, 200).join('')}</tbody></table>` : '';

    const flowRows = data.flows.map((e) => row([e.ts, e.data.method, e.data.host, e.data.path, e.data.status, e.data.sim ? 'SIM' : 'live']));
    const dnsRows = data.dnsRes.map((e) => row([e.ts, e.data.domain, e.data.resolved, e.data.sim ? 'SIM' : 'live']));
    const hostRows = data.hosts.map((e) => row([e.ts, e.data.ip, e.data.mac, e.data.vendor || '-', e.data.android ? 'Android ✓' : '-']));
    const modRows = Object.entries(stats.byModule).map(([m, n]) => row([m, n]));
    const typeRows = Object.entries(stats.byType).map(([t, n]) => row([t, n]));

    return `<!doctype html><html lang="fr"><head><meta charset="utf-8">
<title>AndroMITM — Rapport ${esc(meta.name || '')}</title>
<style>
body{background:#0d1117;color:#c9d1d9;font-family:ui-monospace,monospace;max-width:960px;margin:2rem auto;padding:0 1rem}
h1,h2{color:#58a6ff}table{border-collapse:collapse;width:100%;margin:.5rem 0 1.5rem}
th,td{border:1px solid #30363d;padding:6px 10px;text-align:left;font-size:13px}
th{background:#161b22}tr:nth-child(even){background:#161b22}
.badge{display:inline-block;background:#1f6feb33;color:#58a6ff;border:1px solid #1f6feb;border-radius:6px;padding:2px 8px;margin-right:6px}
.warn{color:#f85149;font-size:12px}
</style></head><body>
<h1>AndroMITM — Rapport de session</h1>
<p><span class="badge">${esc(meta.name || '')}</span>
<span class="badge">${esc(stats.first || '')} → ${esc(stats.last || '')}</span>
<span class="badge">${stats.total} événements</span></p>
<p class="warn">Document de test d'intrusion autorisé — ne pas diffuser. Usage : audit de sécurité sur systèmes autorisés uniquement.</p>
${table('Flux HTTP(S) interceptés', flowRows, ['Horodatage', 'Méthode', 'Hôte', 'Chemin', 'Statut', 'Mode'])}
${table('Réponses DNS usurpées', dnsRows, ['Horodatage', 'Domaine', 'Résolu vers', 'Mode'])}
${table('Hôtes découverts', hostRows, ['Horodatage', 'IP', 'MAC', 'Constructeur', 'Android'])}
${table('Événements par module', modRows, ['Module', 'Nombre'])}
${table('Événements par type', typeRows, ['Type', 'Nombre'])}
</body></html>`;
  }
}

module.exports = { ReportModule };
