/* AndroMITM — preload : bridge minimal et sûr (pas d'API sensible exposée) */
'use strict';
const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('andromitm', {
  platform: process.platform,
  isElectron: true,
});
