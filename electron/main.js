/**
 * AndroMITM — electron/main.js
 * Coquille desktop (Linux/Windows/macOS x64 + arm64) autour du dashboard AndroMITM.
 *
 * Deux modes :
 *   1. Se connecter au backend qui tourne déjà (téléphone Termux, serveur…)
 *      → electron . --url http://192.168.1.66:8443
 *   2. Démarrer le backend Node local (desktop-root / desktop)
 *      → electron .  (spawne `node backend/server.js` depuis la racine du repo)
 *
 * NB : Electron ne tourne PAS nativement sous Android/Termux — sous Android,
 * utiliser directement le dashboard web (Chrome). Voir INSTALL.md.
 */
'use strict';

const { app, BrowserWindow, Menu, dialog } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const http = require('http');

let win = null;
let backendProc = null;

function argUrl() {
  const i = process.argv.indexOf('--url');
  return i > -1 ? process.argv[i + 1] : process.env.ANDROMITM_URL || null;
}

function waitFor(url, timeoutMs = 20000) {
  const started = Date.now();
  return new Promise((resolve, reject) => {
    const probe = () => {
      const req = http.get(url + '/api/status', (res) => { res.resume(); resolve(); });
      req.on('error', () => {
        if (Date.now() - started > timeoutMs) reject(new Error('backend injoignable : ' + url));
        else setTimeout(probe, 800);
      });
    };
    probe();
  });
}

async function startBackend() {
  const repoRoot = path.join(__dirname, '..');
  backendProc = spawn('node', [path.join(repoRoot, 'backend', 'server.js')], {
    cwd: repoRoot,
    env: { ...process.env, ANDROMITM_HOST: '127.0.0.1', ANDROMITM_PORT: process.env.ANDROMITM_PORT || '8443' },
    stdio: 'inherit',
  });
  await waitFor(`http://127.0.0.1:${process.env.ANDROMITM_PORT || 8443}`);
  return `http://127.0.0.1:${process.env.ANDROMITM_PORT || 8443}`;
}

function createWindow(url) {
  win = new BrowserWindow({
    width: 1280, height: 860,
    backgroundColor: '#0d1117',
    title: 'AndroMITM',
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, nodeIntegration: false },
  });
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    {
      label: 'AndroMITM',
      submenu: [
        { label: 'Recharger', accelerator: 'CmdOrCtrl+R', click: () => win.reload() },
        { type: 'separator' },
        { label: 'Quitter', role: 'quit' },
      ],
    },
  ]));
  win.loadURL(url);
}

app.whenReady().then(async () => {
  try {
    let url = argUrl();
    if (!url) url = await startBackend();
    createWindow(url);
  } catch (e) {
    dialog.showErrorBox('AndroMITM', String(e.message || e));
    app.quit();
  }
});

app.on('window-all-closed', () => {
  if (backendProc) { try { backendProc.kill('SIGTERM'); } catch (_) { /* ignore */ } }
  app.quit();
});
