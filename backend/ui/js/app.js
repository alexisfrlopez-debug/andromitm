/* AndroMITM — app.js : logique SPA (routes, modules, websocket, i18n) */
'use strict';

/* token API : servi par le backend au premier chargement via /api/status n'est pas exposé ;
   il est injecté au démarrage du serveur dans ce fichier courant (voir server.js inject),
   sinon fallback : le serveur est en localhost et les routes d'attaque vérifient aussi l'origine. */
let API_TOKEN = window.ANDROMITM_TOKEN || localStorage.getItem('andromitm.token') || '';
const $ = (sel, root) => (root || document).querySelector(sel);
const $$ = (sel, root) => [...(root || document).querySelectorAll(sel)];

const state = { status: null, ws: null, modulePanels: {} };

/* ─────────── helpers réseau ─────────── */
async function api(path, opts = {}) {
  const res = await fetch('/api' + path, {
    method: opts.method || 'GET',
    headers: { 'Content-Type': 'application/json', 'X-AndroMITM-Token': API_TOKEN },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw Object.assign(new Error(err.error || res.statusText), { err });
  }
  return res.json();
}

function modal(text) {
  $('#modal-text').textContent = text;
  $('#modal').classList.remove('hidden');
}
$('#modal-ok').addEventListener('click', () => $('#modal').classList.add('hidden'));

function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }

/* ─────────── websocket live ─────────── */
function connectWs() {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws';
  const ws = new WebSocket(`${proto}://${location.host}/ws`);
  state.ws = ws;
  ws.onopen = () => $('#ws-status').classList.replace('dot-off', 'dot-on');
  ws.onclose = () => { $('#ws-status').classList.replace('dot-on', 'dot-off'); setTimeout(connectWs, 2000); };
  ws.onmessage = (e) => {
    try { handleEvent(JSON.parse(e.data)); } catch (_) { /* ignore */ }
  };
}

const MAX_FEED = 120;
function handleEvent(evt) {
  if (evt.type === 'ws.hello') return;
  const feed = $('#event-feed');
  if (feed) {
    const div = document.createElement('div');
    div.className = `ev ${evt.level}`;
    div.innerHTML = `<span class="t">${esc(evt.ts.slice(11, 19))}</span><span class="m">${esc(evt.module)}</span><span class="msg">${esc(evt.type)} ${esc(JSON.stringify(evt.data)).slice(0, 140)}</span>`;
    feed.prepend(div);
    while (feed.children.length > MAX_FEED) feed.lastChild.remove();
  }
  // réactions ciblées
  if (evt.type.startsWith('discovery.')) refreshTargets();
  if (evt.type.startsWith('proxy.')) refreshModule('proxy');
  if (evt.type.startsWith('arp.')) refreshModule('arp');
  if (evt.type.startsWith('dns.')) refreshModule('dns');
  if (evt.type.startsWith('capture.')) refreshModule('capture');
}

/* ─────────── porte d'autorisation ─────────── */
function initAuth() {
  const st = state.status.authorized;
  if (st.authorized) { $('#auth-gate').classList.add('hidden'); return; }
  const gate = $('#auth-gate');
  gate.classList.remove('hidden');
  $('#auth-statement').textContent = st.statement[LANG] || st.statement.fr;
  const cb = $('#auth-checkbox');
  const btn = $('#auth-accept');
  cb.checked = false; btn.disabled = true;
  cb.onchange = () => { btn.disabled = !cb.checked; };
  btn.onclick = async () => {
    await api('/authorize', { method: 'POST', body: { accepted: true, lang: LANG } });
    gate.classList.add('hidden');
    boot();
  };
  $('#auth-decline').onclick = () => {
    gate.classList.add('hidden');
    modal(LANG === 'fr' ? 'Consultation seule : les modules d\'attaque resteront verrouillés.' : 'Read-only: attack modules stay locked.');
  };
}

/* ─────────── dashboard ─────────── */
function renderDashboard() {
  const s = state.status;
  $('#sim-badge').classList.toggle('hidden', !s.sim);
  $('#profile-label').textContent = 'profil: ' + s.profile;
  $('#dash-profile').innerHTML = `
    <div><b>${t('dash.profile')}</b> ${esc(s.profile)}</div>
    <div><b>version</b> ${esc(s.version)}</div>
    <div><b>mode</b> ${s.sim ? 'SIMULATION' : 'réel'}</div>
    <div><b>session</b> ${s.session ? esc(s.session.name) : '—'}</div>`;
  $('#dash-modules').innerHTML = s.modules.map((m) =>
    `<span class="chip ${m.state}" data-chip="${m.id}">${esc(m.id)} · ${m.state}</span>`).join('');
  refreshTargets();
}

async function refreshTargets() {
  try {
    const { all } = await api('/targets');
    $('#dash-targets').innerHTML = all.length
      ? all.map((h) => `<div>${h.android ? '<span class="tag-android">◆</span>' : '○'} ${esc(h.ip)} — ${esc(h.vendor || h.hostname || '?')}</div>`).join('')
      : `<span class="hint">${LANG === 'fr' ? 'Lancer la découverte…' : 'Start discovery…'}</span>`;
    const tbody = $('#targets-table tbody');
    if (tbody) {
      tbody.innerHTML = all.map((h) => `<tr>
        <td>${esc(h.ip)}</td><td>${esc(h.mac || '')}</td><td>${esc(h.vendor || '-')}</td>
        <td>${esc(h.hostname || '-')}</td>
        <td>${h.android ? '<span class="tag-android">✓</span>' : '-'}</td>
        <td>${esc((h.lastSeen || '').slice(11, 19))}</td>
        <td><button class="btn btn-mini" onclick="useTarget('${esc(h.ip)}')">${LANG === 'fr' ? '→ cible' : '→ target'}</button></td>
      </tr>`).join('');
    }
  } catch (_) { /* ignore */ }
}
window.useTarget = (ip) => {
  const inp = $('#arp-targets');
  if (inp) { inp.value = JSON.stringify([ip]); location.hash = '#/arp'; }
};

/* ─────────── panneaux modules ─────────── */
const MODULE_FORMS = {
  arp: [
    ['iface', 'mod.field.iface', 'text', 'wlan0'],
    ['targets', 'mod.field.targets', 'textarea', '["192.168.1.10"]'],
    ['gateway', 'mod.field.gateway', 'text', ''],
    ['fullDuplex', 'mod.field.fullDuplex', 'checkbox', false],
  ],
  dns: [
    ['iface', 'mod.field.iface', 'text', 'wlan0'],
    ['entries', 'mod.field.entries', 'textarea', '[{"domain":"exemple-lab.test","ip":"192.168.1.66"}]'],
  ],
  proxy: [
    ['mode', 'mod.field.mode', 'select', 'regular', ['regular', 'transparent']],
    ['listenPort', 'mod.field.port', 'number', 8080],
  ],
  capture: [
    ['iface', 'mod.field.iface', 'text', 'wlan0'],
    ['filter', 'mod.field.filter', 'text', ''],
  ],
};

function renderModulePanel(id) {
  const el = $('#mod-' + id);
  if (!el || el.dataset.rendered) return;
  const meta = state.status.modules.find((m) => m.id === id) || { deps: [] };
  const fields = MODULE_FORMS[id] || [];
  el.innerHTML = `
    <div class="card">
      <h2>${t('mod.' + id + '.title')}</h2>
      <p class="hint">${t('mod.' + id + '.desc')}</p>
      <h3>${t('mod.cfg')}</h3>
      ${fields.map(([k, lbl, type, def, opts]) => type === 'checkbox'
        ? `<div class="form-row"><label><input type="checkbox" id="cfg-${id}-${k}" ${def ? 'checked' : ''}> ${t(lbl)}</label></div>`
        : type === 'textarea'
          ? `<div class="form-row"><label>${t(lbl)}</label><textarea id="cfg-${id}-${k}" rows="3">${esc(def)}</textarea></div>`
          : type === 'select'
            ? `<div class="form-row"><label>${t(lbl)}</label><select id="cfg-${id}-${k}">${opts.map((o) => `<option ${o === def ? 'selected' : ''}>${o}</option>`).join('')}</select></div>`
            : `<div class="form-row"><label>${t(lbl)}</label><input id="cfg-${id}-${k}" type="${type}" value="${esc(def)}"></div>`
      ).join('')}
      <div class="actions">
        <button class="btn btn-primary" onclick="moduleAction('${id}','start')">${t('common.start')}</button>
        <button class="btn btn-danger" onclick="moduleAction('${id}','stop')">${t('common.stop')}</button>
      </div>
      <ul class="problems" id="problems-${id}"></ul>
    </div>
    <div class="card">
      <h2>${t('mod.runtime')}</h2>
      <div class="kv" id="runtime-${id}"></div>
      ${id === 'proxy' ? `<h3>${t('mod.ca')}</h3><p class="hint">${t('mod.caHint')}</p><pre class="code" id="ca-path"></pre>` : ''}
    </div>`;
  el.dataset.rendered = '1';
  refreshModule(id);
}

function readCfg(id) {
  const cfg = {};
  (MODULE_FORMS[id] || []).forEach(([k, , type]) => {
    const el = $(`#cfg-${id}-${k}`);
    if (!el) return;
    if (type === 'checkbox') cfg[k] = el.checked;
    else if (type === 'number') cfg[k] = parseInt(el.value, 10) || undefined;
    else cfg[k] = el.value.trim();
  });
  if (id === 'arp') { try { cfg.targets = JSON.parse(cfg.targets || '[]'); } catch (_) { cfg.targets = []; } }
  if (id === 'dns') { try { cfg.entries = JSON.parse(cfg.entries || '[]'); } catch (_) { cfg.entries = []; } }
  return cfg;
}

window.moduleAction = async (id, action) => {
  try {
    const res = await api(`/module/${id}/${action}`, { method: 'POST', body: action === 'start' ? readCfg(id) : {} });
    if (res.ok === false) {
      const ul = $(`#problems-${id}`);
      if (ul && res.problems) ul.innerHTML = res.problems.map((p) => `<li>${esc(p.code)}: ${esc(p.tool || p.reason || '')}</li>`).join('');
      else modal(LANG === 'fr' ? 'Échec : ' : 'Failed: ' + (res.error || '?'));
    }
    await refreshModule(id);
    await loadStatus();
    renderDashboard();
  } catch (e) { modal(String(e.message || e)); }
};

async function refreshModule(id) {
  const el = $(`#runtime-${id}`);
  if (!el) return;
  try {
    const m = await api(`/module/${id}`);
    el.innerHTML = Object.entries(m.runtime || {}).map(([k, v]) =>
      `<div><b>${esc(k)}</b> ${esc(typeof v === 'object' ? JSON.stringify(v).slice(0, 200) : v)}</div>`).join('')
      || `<span class="hint">—</span>`;
    if (id === 'proxy' && $('#ca-path')) $('#ca-path').textContent = m.runtime.caCertPath || '—';
    const chip = $(`[data-chip="${id}"]`);
    if (chip) { chip.className = `chip ${m.state}`; chip.textContent = `${id} · ${m.state}`; }
  } catch (_) { /* ignore */ }
}

/* ─────────── sessions & rapport ─────────── */
async function refreshSessions() {
  try {
    const { sessions } = await api('/sessions');
    const tbody = $('#sessions-table tbody');
    tbody.innerHTML = sessions.map((s) => `<tr>
      <td>${esc(s.name || s.id)}</td><td>${esc((s.startedAt || '').slice(0, 19))}</td>
      <td>${esc((s.endedAt || '').slice(0, 19) || '—')}</td>
      <td><button class="btn btn-mini" onclick="genReport('${esc(s.id)}')">${t('report.title')}</button></td>
    </tr>`).join('') || `<tr><td colspan="4" class="hint">—</td></tr>`;
  } catch (_) { /* ignore */ }
}
window.genReport = async (sessionId) => {
  try {
    const dir = '/home/z/.andromitm-sessions/' + sessionId; // fallback ; le backend utilise data/sessions
    const res = await api('/report/generate', { method: 'POST', body: { sessionId } });
    $('#report-card').classList.remove('hidden');
    $('#report-stats').innerHTML = Object.entries(res.stats.byModule).map(([m, n]) => `<div><b>${esc(m)}</b> ${n} evts</div>`).join('');
    const link = $('#report-link');
    link.href = '/api/session-report/' + encodeURIComponent(sessionId);
    link.textContent = t('report.open');
  } catch (e) { modal(String(e.message || e)); }
};

/* ─────────── paramètres ─────────── */
async function renderSettings() {
  const s = state.status;
  const depRows = [];
  for (const m of s.modules) {
    for (const d of m.deps) {
      if (depRows.find((r) => r.tool === d.tool)) continue;
      depRows.push(d);
    }
  }
  $('#set-deps').innerHTML = depRows.map((d) =>
    `<div class="dep ${d.found ? 'ok' : 'missing'}"><span>${esc(d.tool)}</span><span class="state">${d.found ? '✓' : '✗'} ${esc(d.reason || '')}</span></div>`).join('');
}

/* ─────────── routage ─────────── */
const ROUTES = ['dashboard', 'discovery', 'arp', 'dns', 'proxy', 'capture', 'sessions', 'settings'];
function route() {
  const name = (location.hash || '#/dashboard').replace('#/', '');
  const page = ROUTES.includes(name) ? name : 'dashboard';
  $$('.page').forEach((p) => p.classList.add('hidden'));
  $('#page-' + page).classList.remove('hidden');
  $$('.sidebar nav a').forEach((a) => a.classList.toggle('active', a.getAttribute('href') === '#/' + page));
  $('#page-title').textContent = t('nav.' + page);
  if (page === 'dashboard') { loadStatus().then(renderDashboard); }
  if (['arp', 'dns', 'proxy', 'capture'].includes(page)) { loadStatus().then(() => renderModulePanel(page)); }
  if (page === 'sessions') refreshSessions();
  if (page === 'settings') renderSettings();
}
window.addEventListener('hashchange', route);

/* ─────────── session boutons ─────────── */
$('#btn-session-start').addEventListener('click', async () => {
  const name = prompt(LANG === 'fr' ? 'Nom de la session :' : 'Session name:', 'pentest-' + new Date().toISOString().slice(0, 10));
  if (name === null) return;
  await api('/session/start', { method: 'POST', body: { name } });
  await loadStatus(); updateSessionBadge();
});
$('#btn-session-stop').addEventListener('click', async () => {
  await api('/session/stop', { method: 'POST' });
  await loadStatus(); updateSessionBadge();
});
function updateSessionBadge() {
  const b = $('#session-badge');
  b.classList.toggle('hidden', !state.status.session);
  if (state.status.session) b.textContent = state.status.session.name;
}
$('#btn-refresh-sessions').addEventListener('click', refreshSessions);

/* ─────────── langue ─────────── */
$('#lang-toggle').addEventListener('click', () => {
  LANG = LANG === 'fr' ? 'en' : 'fr';
  localStorage.setItem('andromitm.lang', LANG);
  applyI18n(); route(); initAuth();
});

/* ─────────── interfaces du scan ─────────── */
function fillIfaces() {
  const sel = $('#disc-iface');
  if (!sel || sel.options.length) return;
  (state.status.interfaces || []).forEach((i) => {
    const opt = document.createElement('option');
    opt.value = i.name; opt.textContent = `${i.name} (${i.address})`;
    sel.appendChild(opt);
  });
}
$('#disc-iface') && document.addEventListener('change', (e) => {
  if (e.target.id === 'disc-iface') {
    const i = (state.status.interfaces || []).find((x) => x.name === e.target.value);
    if (i) $('#disc-cidr').value = i.cidr || '';
  }
});

/* ─────────── découverte boutons ─────────── */
$$('[data-module="discovery"]').forEach((btn) => {
  btn.addEventListener('click', async () => {
    const action = btn.dataset.action;
    const body = action === 'start' ? {
      iface: $('#disc-iface').value,
      cidr: $('#disc-cidr').value.trim() || undefined,
      interval: parseInt($('#disc-interval').value, 10) || 60,
    } : {};
    try {
      const res = await api(`/module/discovery/${action}`, { method: 'POST', body });
      if (res.ok === false && res.problems) {
        modal((LANG === 'fr' ? 'Prérequis manquants : ' : 'Missing prerequisites: ') + res.problems.map((p) => p.tool || p.code).join(', '));
      }
      await loadStatus(); renderDashboard();
    } catch (e) { modal(String(e.message || e)); }
  });
});

/* ─────────── boot ─────────── */
async function loadStatus() {
  state.status = await api('/status');
  return state.status;
}

async function boot() {
  await loadStatus();
  API_TOKEN = state.status.apiToken || API_TOKEN;
  if (API_TOKEN) localStorage.setItem('andromitm.token', API_TOKEN);
  applyI18n();
  initAuth();
  fillIfaces();
  updateSessionBadge();
  route();
}

connectWs();
boot();
