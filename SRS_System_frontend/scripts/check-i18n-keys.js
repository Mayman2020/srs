#!/usr/bin/env node
/*
 * i18n key-coverage gate.
 *
 * Scans every Angular template (.html and inline `template:` strings in .ts) for usages of the
 * `| t` pipe and the `i18n.instant('...')` helper, collects the referenced keys, and verifies that
 * every key exists in both `public/assets/i18n/ar.json` and `public/assets/i18n/en.json`. Also
 * reports keys that exist only in one language file (drift).
 *
 * Exit code 0 = clean; 1 = missing/drifting keys found.
 *
 * Usage:  node scripts/check-i18n-keys.js
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SRC_DIR = path.join(ROOT, 'src');
const I18N_DIR = path.join(ROOT, 'public', 'assets', 'i18n');
const LOCALES = ['ar', 'en'];

const PIPE_RX = /['"`]([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+)*)['"`]\s*\|\s*t\b/g;
const INSTANT_RX = /\bi18n\.instant\(\s*['"`]([a-zA-Z][a-zA-Z0-9_]*(?:\.[a-zA-Z0-9_]+)*)['"`]/g;

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'dist') continue;
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(p, files);
    } else if (/\.(html|ts)$/.test(entry.name)) {
      files.push(p);
    }
  }
  return files;
}

function collectKeys(files) {
  const keys = new Set();
  for (const f of files) {
    const src = fs.readFileSync(f, 'utf8');
    for (const rx of [PIPE_RX, INSTANT_RX]) {
      rx.lastIndex = 0;
      let m;
      while ((m = rx.exec(src)) !== null) {
        keys.add(m[1]);
      }
    }
  }
  return keys;
}

function flatten(obj, prefix = '', out = new Set()) {
  if (obj === null || typeof obj !== 'object') return out;
  for (const [k, v] of Object.entries(obj)) {
    const next = prefix ? `${prefix}.${k}` : k;
    if (v && typeof v === 'object' && !Array.isArray(v)) {
      flatten(v, next, out);
    } else {
      out.add(next);
    }
  }
  return out;
}

function readLocale(locale) {
  const file = path.join(I18N_DIR, `${locale}.json`);
  const json = JSON.parse(fs.readFileSync(file, 'utf8'));
  return { json, keys: flatten(json) };
}

const templateFiles = walk(SRC_DIR);
const usedKeys = collectKeys(templateFiles);

const locales = LOCALES.map((locale) => ({ locale, ...readLocale(locale) }));

let hasErrors = false;
for (const { locale, keys } of locales) {
  const missing = [...usedKeys].filter((k) => !keys.has(k)).sort();
  if (missing.length > 0) {
    hasErrors = true;
    console.error(`\n[i18n] ${missing.length} key(s) used in templates but missing from ${locale}.json:`);
    for (const k of missing) console.error(`  - ${k}`);
  } else {
    console.log(`[i18n] ${locale}.json: 0 missing keys`);
  }
}

const [ar, en] = locales;
const onlyAr = [...ar.keys].filter((k) => !en.keys.has(k)).sort();
const onlyEn = [...en.keys].filter((k) => !ar.keys.has(k)).sort();
if (onlyAr.length > 0) {
  hasErrors = true;
  console.error(`\n[i18n] ${onlyAr.length} key(s) present in ar.json but missing from en.json:`);
  for (const k of onlyAr) console.error(`  - ${k}`);
}
if (onlyEn.length > 0) {
  hasErrors = true;
  console.error(`\n[i18n] ${onlyEn.length} key(s) present in en.json but missing from ar.json:`);
  for (const k of onlyEn) console.error(`  - ${k}`);
}

if (hasErrors) {
  console.error('\n[i18n] FAILED.');
  process.exit(1);
}
console.log('\n[i18n] OK.');
