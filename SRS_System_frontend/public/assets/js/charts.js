// Charts using local Chart.js (no CDN)
// Requires: assets/vendor/chartjs/chart.umd.min.js loaded first.
// Labels: /assets/i18n/{ar|en}.json → legacyCharts.* (no hardcoded UI strings here).

(function () {
  const $ = (s, r = document) => r.querySelector(s);

  let _i18nBundle = null;
  let _i18nLang = null;

  function resolveLang() {
    const l = (document.documentElement.lang || 'ar').toLowerCase();
    return l.startsWith('en') ? 'en' : 'ar';
  }

  async function loadI18nBundle() {
    const lang = resolveLang();
    if (_i18nBundle && _i18nLang === lang) {
      return _i18nBundle;
    }
    try {
      const res = await fetch('/assets/i18n/' + lang + '.json', { cache: 'no-store' });
      _i18nBundle = await res.json();
      _i18nLang = lang;
    } catch (e) {
      console.warn('[charts] failed to load i18n bundle', e);
      _i18nBundle = {};
    }
    return _i18nBundle;
  }

  /** Strings for static prototype charts (legacy HTML pages under public/). */
  function legacyChartLabels(dict) {
    const lc = (dict && dict.legacyCharts) || {};
    return {
      donutStatusLabels: Array.isArray(lc.donutStatusLabels) ? lc.donutStatusLabels : [],
      barDeptLabels: Array.isArray(lc.barDeptLabels) ? lc.barDeptLabels : [],
      barDatasetLabel: typeof lc.barDatasetLabel === 'string' ? lc.barDatasetLabel : '',
      lineSeriesLabel: typeof lc.lineSeriesLabel === 'string' ? lc.lineSeriesLabel : '',
      lineQuarterLabels: Array.isArray(lc.lineQuarterLabels) ? lc.lineQuarterLabels : [],
      reportsDonutStatusLabels: Array.isArray(lc.reportsDonutStatusLabels)
        ? lc.reportsDonutStatusLabels
        : []
    };
  }

  function baseOptions() {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const grid = isDark ? 'rgba(255,255,255,.10)' : 'rgba(15,23,42,.08)';
    const text = isDark ? '#e5e7eb' : '#0f172a';
    const muted = isDark ? '#a8b1c0' : '#6B7280';

    return {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          labels: { color: text, font: { family: 'IBM Plex Sans Arabic', size: 12, weight: '700' } }
        },
        tooltip: {
          titleFont: { family: 'IBM Plex Sans Arabic' },
          bodyFont: { family: 'IBM Plex Sans Arabic' }
        }
      },
      scales: {
        x: { ticks: { color: muted }, grid: { color: grid } },
        y: { ticks: { color: muted }, grid: { color: grid } }
      }
    };
  }

  async function renderDashboardCharts() {
    const donut = $('#dashStatusDonut');
    const bar = $('#dashDeptBar');
    if (!window.Chart) return;

    const dict = await loadI18nBundle();
    const L = legacyChartLabels(dict);

    if (donut) {
      const ctx = donut.getContext('2d');
      window.__dashDonut && window.__dashDonut.destroy();
      window.__dashDonut = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: L.donutStatusLabels,
          datasets: [
            {
              data: [18, 44, 9, 29],
              backgroundColor: ['#0B6E4F', '#064635', '#F59E0B', '#22C55E'],
              borderWidth: 0
            }
          ]
        },
        options: {
          ...baseOptions(),
          cutout: '68%',
          plugins: {
            ...baseOptions().plugins,
            legend: { position: 'bottom', labels: baseOptions().plugins.legend.labels }
          }
        }
      });
    }

    if (bar) {
      const ctx = bar.getContext('2d');
      window.__dashBar && window.__dashBar.destroy();
      window.__dashBar = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: L.barDeptLabels,
          datasets: [
            {
              label: L.barDatasetLabel,
              data: [188, 142, 121, 108, 96],
              backgroundColor: 'rgba(11,110,79,.75)',
              borderRadius: 10
            }
          ]
        },
        options: {
          ...baseOptions(),
          plugins: { ...baseOptions().plugins, legend: { display: false } }
        }
      });
    }
  }

  async function renderReportsCharts(payload) {
    const line = $('#repTrendLine');
    const donut = $('#repStatusDonut');
    if (!window.Chart) return;

    const dict = await loadI18nBundle();
    const L = legacyChartLabels(dict);

    const trend = payload?.trend || [45, 52, 41, 66, 58, 72, 63];
    const status = payload?.status || [12, 46, 10, 32];

    const quarterLabels =
      L.lineQuarterLabels.length >= trend.length
        ? L.lineQuarterLabels.slice(0, trend.length)
        : trend.map(function (_v, i) {
            return 'P' + (i + 1);
          });

    if (line) {
      const ctx = line.getContext('2d');
      window.__repLine && window.__repLine.destroy();
      window.__repLine = new Chart(ctx, {
        type: 'line',
        data: {
          labels: quarterLabels,
          datasets: [
            {
              label: L.lineSeriesLabel,
              data: trend,
              fill: true,
              tension: 0.35,
              borderColor: '#0B6E4F',
              backgroundColor: 'rgba(11,110,79,.15)',
              pointBackgroundColor: '#0B6E4F',
              pointRadius: 4
            }
          ]
        },
        options: baseOptions()
      });
    }

    if (donut) {
      const ctx = donut.getContext('2d');
      window.__repDonut && window.__repDonut.destroy();
      window.__repDonut = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: L.reportsDonutStatusLabels,
          datasets: [
            {
              data: status,
              backgroundColor: ['#0B6E4F', '#064635', '#F59E0B', '#22C55E'],
              borderWidth: 0
            }
          ]
        },
        options: {
          ...baseOptions(),
          cutout: '68%',
          plugins: {
            ...baseOptions().plugins,
            legend: { position: 'bottom', labels: baseOptions().plugins.legend.labels }
          }
        }
      });
    }
  }

  function hookTheme() {
    const obs = new MutationObserver(function () {
      const page = document.body.getAttribute('data-page');
      if (page === 'dashboard') renderDashboardCharts().catch(function () {});
      if (page === 'reports') renderReportsCharts(window.__lastReportPayload).catch(function () {});
    });
    obs.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  }

  window.GOV_CHARTS = {
    renderDashboardCharts,
    renderReportsCharts
  };

  document.addEventListener('DOMContentLoaded', function () {
    hookTheme();
    const page = document.body.getAttribute('data-page');
    if (page === 'dashboard') renderDashboardCharts().catch(function () {});
  });
})();
