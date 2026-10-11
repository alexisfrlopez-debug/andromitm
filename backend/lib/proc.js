/**
 * AndroMITM — lib/proc.js
 * Supervision de processus enfants (spawn, logs, arrêt propre, kill d'arbres).
 * Pensé pour Termux : les processus sont tués via SIGTERM puis SIGKILL.
 */
'use strict';

const { spawn } = require('child_process');

const running = new Map(); // key -> child process

/** Lance un processus supervisé. onLine(data, isErr) reçoit chaque ligne. */
function startProcess(key, cmd, args, { onLine, onExit, env } = {}) {
  if (running.has(key)) throw new Error(`process already running: ${key}`);
  const child = spawn(cmd, args, {
    env: { ...process.env, ...(env || {}) },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  running.set(key, child);

  const pump = (stream, isErr) => {
    let buf = '';
    stream.setEncoding('utf8');
    stream.on('data', (chunk) => {
      buf += chunk;
      const lines = buf.split('\n');
      buf = lines.pop() || '';
      for (const line of lines) if (line.trim() && onLine) onLine(line.trim(), isErr);
    });
  };
  pump(child.stdout, false);
  pump(child.stderr, true);

  child.on('exit', (code, signal) => {
    running.delete(key);
    if (onExit) onExit(code, signal);
  });
  child.on('error', (err) => {
    running.delete(key);
    if (onExit) onExit(-1, String(err && err.message || err));
  });
  return child;
}

/** Arrête un processus par clé (TERM puis KILL après 2s). */
function stopProcess(key) {
  const child = running.get(key);
  if (!child) return false;
  try { child.kill('SIGTERM'); } catch (_) { /* ignore */ }
  setTimeout(() => {
    try { if (!child.killed) child.kill('SIGKILL'); } catch (_) { /* ignore */ }
  }, 2000);
  return true;
}

function isRunning(key) { return running.has(key); }

/** Arrête tous les processus de la suite (utilisé à l'extinction du serveur). */
function stopAll() {
  for (const key of [...running.keys()]) stopProcess(key);
}

process.on('exit', () => stopAll());
for (const sig of ['SIGINT', 'SIGTERM']) {
  process.on(sig, () => { stopAll(); process.exit(0); });
}

module.exports = { startProcess, stopProcess, isRunning, stopAll };
