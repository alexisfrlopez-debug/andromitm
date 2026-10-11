/**
 * AndroMITM — lib/auth.js
 * Porte d'autorisation : la suite ne déverrouille les modules d'attaque
 * qu'après acceptation explicite de la déclaration d'usage autorisé.
 * Ceci n'est pas une protection absolue : c'est un garde-fou de bon sens
 * aligné sur les pratiques des suites pro (zANTI, NetHunter).
 */
'use strict';

const crypto = require('crypto');
const { JsonStore } = require('./store');

const STATEMENT = {
  fr: "Je certifie utiliser AndroMITM uniquement sur des systèmes et réseaux dont je suis propriétaire ou pour lesquels je dispose d'une autorisation écrite (test d'intrusion, labo, enseignement). Je suis seul responsable de l'usage que j'en fais.",
  en: "I certify that I will use AndroMITM only on systems and networks I own or have written authorization to test (pentest, lab, teaching). I am solely responsible for my usage.",
};

const store = new JsonStore('state.json', { authorized: null });
const sessions = new Map(); // cookie -> { createdAt }

function newApiToken() { return crypto.randomBytes(24).toString('hex'); }

/** Statut d'autorisation pour l'UI. */
function status() {
  return {
    authorized: !!store.get('authorized', null),
    statement: STATEMENT,
    authorizedAt: store.get('authorizedAt', null) || null,
  };
}

/** Enregistre l'acceptation (irréversible sauf purge de data/state.json). */
function accept(lang = 'fr') {
  store.set('authorized', { statement: STATEMENT[lang] || STATEMENT.fr, lang });
  store.set('authorizedAt', new Date().toISOString());
  return status();
}

/** Réinitialise (usage local uniquement, ne désarme rien sur le réseau). */
function revoke() {
  store.set('authorized', null);
  store.set('authorizedAt', null);
  return status();
}

function isAuthorized() { return !!store.get('authorized', null); }

/** Vérifie le token API émis après acceptation (header X-AndroMITM-Token). */
let apiToken = store.get('apiToken', null);
if (!apiToken) { apiToken = newApiToken(); store.set('apiToken', apiToken); }

function checkToken(req) {
  return req.get('X-AndroMITM-Token') === apiToken;
}

module.exports = { STATEMENT, status, accept, revoke, isAuthorized, checkToken, apiToken };
