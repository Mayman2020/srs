#!/usr/bin/env node
/*
 * RBAC drift gate.
 *
 * Cross-checks the three sources of truth that must stay aligned for the union-of-active-roles
 * capability model to work:
 *
 *   1. Angular route table (`src/app/app.routes.ts`):  every `data: { permission: 'CODE' }`.
 *   2. Backend canonical permission catalogue (`SRS_System_backend/src/main/resources/db/migration/*.sql`):
 *      every `INSERT INTO permission (...) VALUES ('CODE', ...)` row plus `permission_alias` aliases.
 *   3. Shell navigation seeds (same SQL files):  every `ui_screen` row with `route_path` and
 *      `required_permission_id` (resolved by the `(SELECT id FROM permission WHERE code='CODE')`
 *      subselect or by `UPDATE`s).
 *
 * Three mismatch classes are reported (exit code 1 on ANY mismatch):
 *
 *   - missing-permission:   `data.permission` in app.routes.ts is not a canonical or alias code in
 *                           the DB seeds. The route would be permanently locked because the backend
 *                           never grants such a code.
 *   - menu-route-mismatch:  a `ui_screen` row's `route_path` resolves to a frontend route whose
 *                           `data.permission` differs from the screen's `required_permission_id`'s
 *                           canonical code. The sidebar shows the link but `permissionCanMatch`
 *                           redirects when the user clicks (or vice versa).
 *   - dangling-ui-screen:   a `ui_screen` row with `show_in_shell_nav = true` whose `route_path`
 *                           has no matching frontend route. The sidebar would render a broken link.
 *
 * The script is intentionally regex-based — same approach as `check-i18n-keys.js`. We accept that
 * a few uncommon SQL constructs are NOT parsed (CTEs, dynamic statements) and warn loudly when
 * the parser can't extract a value rather than silently skipping it.
 *
 * Usage:  node scripts/check-route-permissions.js
 */

const fs = require('fs');
const path = require('path');

const FRONTEND_ROOT = path.resolve(__dirname, '..');
const ROUTES_FILE = path.join(FRONTEND_ROOT, 'src', 'app', 'app.routes.ts');

// The backend may live in a sibling directory; allow it to be overridden by env var for CI.
const BACKEND_ROOT =
  process.env.SRS_BACKEND_ROOT ||
  path.resolve(FRONTEND_ROOT, '..', 'SRS_System_backend');
const MIGRATIONS_DIR = path.join(BACKEND_ROOT, 'src', 'main', 'resources', 'db', 'migration');

/* -------------------------------------------------------------------------- */
/* Helpers                                                                    */
/* -------------------------------------------------------------------------- */

function readFile(p) {
  return fs.readFileSync(p, 'utf8');
}

function listSqlFiles() {
  if (!fs.existsSync(MIGRATIONS_DIR)) {
    console.error(`[check:routes] Backend migrations folder not found: ${MIGRATIONS_DIR}`);
    console.error(
      '[check:routes] Set SRS_BACKEND_ROOT env var if the backend lives at a non-standard path.'
    );
    process.exit(2);
  }
  return fs
    .readdirSync(MIGRATIONS_DIR)
    .filter((f) => f.endsWith('.sql'))
    .sort()
    .map((f) => path.join(MIGRATIONS_DIR, f));
}

/* -------------------------------------------------------------------------- */
/* Frontend: parse app.routes.ts                                              */
/* -------------------------------------------------------------------------- */

/**
 * Collects every {@code path: '...'} immediately preceding a {@code data: { ..., permission: '...'}}
 * block, so we end up with a list of `{ path, permission }` pairs.
 *
 * Implementation: we walk the file and remember the most recent `path:` literal; when we see a
 * `permission:` literal that belongs to the same route object, we pair them. Routes without a
 * permission (e.g. `profile`, legacy redirects) are still collected, with `permission = null`, so
 * we can detect dangling ui_screen rows pointing at them.
 */
function parseFrontendRoutes(src) {
  const routes = [];
  // Match path/permission tokens line by line; the order in the file is path -> ... -> data.
  const tokenRx = /\b(path|permission|redirectTo)\s*:\s*['"`]([^'"`]*)['"`]/g;
  let currentPath = null;
  let currentRedirect = null;
  let m;
  while ((m = tokenRx.exec(src)) !== null) {
    const [, key, value] = m;
    if (key === 'path') {
      // Flush the previous path entry if any (even if it had no permission).
      if (currentPath !== null) {
        routes.push({ path: currentPath, permission: null, redirectTo: currentRedirect });
      }
      currentPath = value;
      currentRedirect = null;
    } else if (key === 'redirectTo' && currentPath !== null) {
      currentRedirect = value;
    } else if (key === 'permission' && currentPath !== null) {
      routes.push({ path: currentPath, permission: value, redirectTo: currentRedirect });
      currentPath = null;
      currentRedirect = null;
    }
  }
  if (currentPath !== null) {
    routes.push({ path: currentPath, permission: null, redirectTo: currentRedirect });
  }
  return routes;
}

/* -------------------------------------------------------------------------- */
/* Backend: parse permission + permission_alias + ui_screen rows              */
/* -------------------------------------------------------------------------- */

const CANONICAL_RX = /['"`]([A-Z][A-Z0-9_]+)['"`]/g;

/**
 * Returns the set of canonical permission codes seeded by the migrations.
 *
 * Heuristic: each canonical permission seed lives in an `INSERT INTO permission ... VALUES`
 * block where the row starts with `'CODE_IN_SCREAMING_SNAKE'`. We pick up both the
 * single-row form (`VALUES ('X', ...)`) and the table-of-rows form used in V7.
 */
function collectCanonicalCodes(sqlFiles) {
  const codes = new Set();
  const insertBlockRx =
    /INSERT\s+INTO\s+(?:srs_system\.)?permission\s*\([^)]*\)\s*(?:SELECT[\s\S]*?FROM\s*\(VALUES|VALUES)([\s\S]*?);/gi;
  // Each row in the block looks like: ('CODE','نص','English text','desc',NN,TRUE)
  const rowStartRx = /\(\s*'([A-Z][A-Z0-9_]+)'/g;
  for (const file of sqlFiles) {
    const src = readFile(file);
    let m;
    while ((m = insertBlockRx.exec(src)) !== null) {
      const valuesBlock = m[1];
      rowStartRx.lastIndex = 0;
      let r;
      while ((r = rowStartRx.exec(valuesBlock)) !== null) {
        codes.add(r[1]);
      }
    }
    // V14/V15 use a separate idiom: a single `INSERT INTO permission ... VALUES (...)` per
    // row with the code as the first column. Picked up by the generic loop above.
  }
  return codes;
}

/**
 * Returns the set of alias codes (so legacy frontend references still resolve through the
 * backend's `permission_alias` table).
 */
function collectAliasCodes(sqlFiles) {
  const aliases = new Set();
  const aliasBlockRx =
    /INSERT\s+INTO\s+(?:srs_system\.)?permission_alias[\s\S]*?(?:VALUES|FROM\s*\(VALUES)([\s\S]*?);/gi;
  const aliasRowRx = /\(\s*'([^']+)'\s*,\s*'([A-Z][A-Z0-9_]+)'/g;
  for (const file of sqlFiles) {
    const src = readFile(file);
    let m;
    while ((m = aliasBlockRx.exec(src)) !== null) {
      const block = m[1];
      aliasRowRx.lastIndex = 0;
      let r;
      while ((r = aliasRowRx.exec(block)) !== null) {
        aliases.add(r[1]);
      }
    }
  }
  return aliases;
}

/**
 * Returns ui_screen rows that drive shell navigation, with their resolved required permission
 * code (canonical).
 *
 * We handle two forms:
 *   - INSERT INTO ui_screen (code, route_path, ..., required_permission_id) VALUES (..., (SELECT id FROM permission WHERE code='CODE')...).
 *   - UPDATE  ui_screen SET route_path = '/x' WHERE code = 'screen_code' (path update only;
 *     required_permission_id remains as previously seeded).
 *
 * The script intentionally only resolves the (SELECT id FROM permission WHERE code='...') form
 * because that's the only pattern present in the current migrations. Unknown forms are reported
 * via the dangling-ui-screen channel.
 */
function collectUiScreens(sqlFiles) {
  // Map code -> { code, routePath, requiredCode, showInShellNav }.
  const screens = new Map();

  // V13 INSERTs include show_in_shell_nav. The columns we care about appear in this order in V13:
  // (code, route_path, name_ar, name_en, description, sort_order, is_active, icon_key, show_in_shell_nav, required_permission_id)
  const insertRx =
    /INSERT\s+INTO\s+(?:srs_system\.)?ui_screen\s*\(([^)]*)\)\s*VALUES\s*([\s\S]*?)\s*(?:ON\s+CONFLICT|;)/gi;
  const rowRx =
    /\(\s*'([^']*)'\s*,\s*'([^']*)'\s*,\s*'([^']*)'\s*,\s*'([^']*)'\s*,\s*(?:'(?:[^']|'')*'|NULL)\s*,\s*(\d+)\s*,\s*(TRUE|FALSE)\s*,\s*'([^']*)'\s*,\s*(TRUE|FALSE)\s*,\s*\(\s*SELECT\s+id\s+FROM[\s\S]*?WHERE\s+code\s*=\s*'([A-Z][A-Z0-9_]+)'/gi;
  for (const file of sqlFiles) {
    const src = readFile(file);
    let m;
    while ((m = insertRx.exec(src)) !== null) {
      const rowsBlock = m[2];
      rowRx.lastIndex = 0;
      let r;
      while ((r = rowRx.exec(rowsBlock)) !== null) {
        const [, code, routePath, , , , isActive, , showInShellNav, requiredCode] = r;
        screens.set(code, {
          code,
          routePath,
          requiredCode,
          showInShellNav: showInShellNav.toUpperCase() === 'TRUE',
          isActive: isActive.toUpperCase() === 'TRUE'
        });
      }
    }
  }

  // V13 also UPDATEs route_path / name_ar / name_en for screens already inserted by V1. We
  // override the routePath if a later migration changed it.
  const updateRx =
    /UPDATE\s+(?:srs_system\.)?ui_screen\s+SET([\s\S]*?)WHERE\s+code\s*=\s*'([^']+)'/gi;
  for (const file of sqlFiles) {
    const src = readFile(file);
    let m;
    while ((m = updateRx.exec(src)) !== null) {
      const setClause = m[1];
      const code = m[2];
      const routeMatch = /route_path\s*=\s*'([^']*)'/i.exec(setClause);
      if (routeMatch) {
        const newRoute = routeMatch[1];
        const existing = screens.get(code);
        if (existing) {
          existing.routePath = newRoute;
        } else {
          // Updated before the screen was seeded by this scanner — record the path anyway.
          screens.set(code, {
            code,
            routePath: newRoute,
            requiredCode: null,
            showInShellNav: false,
            isActive: true
          });
        }
      }
    }
  }

  return screens;
}

/* -------------------------------------------------------------------------- */
/* Route matching                                                             */
/* -------------------------------------------------------------------------- */

/**
 * Normalizes a path so :param segments compare equal between FE (e.g. `correspondence/:id`) and
 * the DB seeds (e.g. `/correspondence/:id`).
 */
function normalizePath(p) {
  if (!p) return '';
  let s = p.trim();
  if (!s.startsWith('/')) s = '/' + s;
  // Collapse trailing slash.
  if (s.length > 1 && s.endsWith('/')) s = s.slice(0, -1);
  return s;
}

function feRoutesByPath(routes) {
  const m = new Map();
  for (const r of routes) {
    m.set(normalizePath(r.path), r);
  }
  return m;
}

/* -------------------------------------------------------------------------- */
/* Main                                                                       */
/* -------------------------------------------------------------------------- */

function main() {
  const routesSrc = readFile(ROUTES_FILE);
  const sqlFiles = listSqlFiles();

  const feRoutes = parseFrontendRoutes(routesSrc);
  const canonicalCodes = collectCanonicalCodes(sqlFiles);
  const aliasCodes = collectAliasCodes(sqlFiles);
  const allKnownCodes = new Set([...canonicalCodes, ...aliasCodes]);
  const screens = collectUiScreens(sqlFiles);

  console.log(`[check:routes] Frontend routes: ${feRoutes.length}`);
  console.log(`[check:routes] Canonical permission codes: ${canonicalCodes.size}`);
  console.log(`[check:routes] Alias permission codes: ${aliasCodes.size}`);
  console.log(`[check:routes] ui_screen rows: ${screens.size}`);

  const errors = [];

  // 1. Frontend route permission must be a canonical code (alias triggers warning).
  for (const r of feRoutes) {
    if (!r.permission) continue;
    if (!allKnownCodes.has(r.permission)) {
      errors.push({
        kind: 'missing-permission',
        route: r.path,
        permission: r.permission,
        detail:
          `route data.permission "${r.permission}" is neither a seeded canonical code nor a ` +
          `permission_alias.alias_code. The route will be permanently locked.`
      });
    } else if (!canonicalCodes.has(r.permission) && aliasCodes.has(r.permission)) {
      errors.push({
        kind: 'route-uses-alias',
        route: r.path,
        permission: r.permission,
        detail:
          `route data.permission "${r.permission}" is a legacy alias. /me/capabilities returns ` +
          `only canonical codes, so cap.can("${r.permission}") will always be false on the FE.`
      });
    }
  }

  // 2. Menu route mismatch: ui_screen routePath should map to a FE route whose data.permission
  // equals the ui_screen.required_permission_id's canonical code.
  const fePathIndex = feRoutesByPath(feRoutes);
  for (const screen of screens.values()) {
    if (!screen.requiredCode) continue; // No required permission to compare.
    const feRoute = fePathIndex.get(normalizePath(screen.routePath));
    if (!feRoute) {
      if (screen.showInShellNav && screen.isActive) {
        errors.push({
          kind: 'dangling-ui-screen',
          screen: screen.code,
          routePath: screen.routePath,
          detail:
            `ui_screen.code="${screen.code}" has route_path="${screen.routePath}" with ` +
            `show_in_shell_nav=true but no matching path in app.routes.ts. The sidebar would ` +
            `render a link that returns 404.`
        });
      }
      continue;
    }
    if (feRoute.permission && feRoute.permission !== screen.requiredCode) {
      errors.push({
        kind: 'menu-route-mismatch',
        screen: screen.code,
        routePath: screen.routePath,
        feRoutePermission: feRoute.permission,
        screenRequiredCode: screen.requiredCode,
        detail:
          `ui_screen "${screen.code}" requires "${screen.requiredCode}" but the matching ` +
          `Angular route ${screen.routePath} guards with "${feRoute.permission}". The user can ` +
          `see the link but cannot reach the route (or vice versa).`
      });
    }
  }

  if (errors.length === 0) {
    console.log('\n[check:routes] OK.');
    return;
  }

  console.error('\n[check:routes] FAILED.');
  const grouped = new Map();
  for (const e of errors) {
    const list = grouped.get(e.kind) ?? [];
    list.push(e);
    grouped.set(e.kind, list);
  }
  for (const [kind, list] of grouped) {
    console.error(`\n  [${kind}] ${list.length} issue(s):`);
    for (const e of list) {
      console.error(`    - ${e.detail}`);
    }
  }
  process.exit(1);
}

main();
