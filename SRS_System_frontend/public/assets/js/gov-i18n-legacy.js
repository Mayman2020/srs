/**
 * Legacy static pages: resolve UI strings from /assets/i18n/{ar|en}.json (same bundle as Angular).
 * Usage: GOV_I18N.ready().then(() => { ... GOV_I18N.t('legacyPages.dashboard.open') ... })
 */
(function () {
  var bundle = null;
  var inflight = null;

  function lang() {
    var l = (document.documentElement.lang || 'ar').toLowerCase();
    return l.indexOf('en') === 0 ? 'en' : 'ar';
  }

  function walk(path, obj) {
    var parts = path.split('.');
    var o = obj;
    for (var i = 0; i < parts.length; i++) {
      if (o == null) return undefined;
      o = o[parts[i]];
    }
    return o;
  }

  window.GOV_I18N = {
    ready: function () {
      if (bundle) return Promise.resolve(bundle);
      if (inflight) return inflight;
      inflight = fetch('/assets/i18n/' + lang() + '.json', { cache: 'no-store' })
        .then(function (r) {
          return r.json();
        })
        .then(function (j) {
          bundle = j;
          return j;
        })
        .catch(function (e) {
          console.warn('[GOV_I18N] load failed', e);
          bundle = {};
          return bundle;
        })
        .finally(function () {
          inflight = null;
        });
      return inflight;
    },
    /** Dot path from JSON root, e.g. legacyPages.txStatus.NEW */
    t: function (path) {
      if (!bundle) return path;
      var v = walk(path, bundle);
      if (typeof v === 'string' || typeof v === 'number') return String(v);
      return path;
    },
    /** Array at path (Chart labels, etc.) */
    ta: function (path) {
      if (!bundle) return [];
      var v = walk(path, bundle);
      return Array.isArray(v) ? v : [];
    },
    /** Any JSON value at path (objects/arrays), or null */
    tj: function (path) {
      if (!bundle) return null;
      var v = walk(path, bundle);
      return v !== undefined ? v : null;
    },
    lang: lang
  };
})();
