(function () {
  document.addEventListener('DOMContentLoaded', () => {
    if (!window.GOV_I18N || typeof GOV_I18N.ready !== 'function') return;

    GOV_I18N.ready().then(function () {
      const t = GOV_I18N.t.bind(GOV_I18N);

      const s = GOV.requireSession();
      if (!s) return;

      const set = (id, v) => {
        const el = document.getElementById(id);
        if (el) el.textContent = v;
      };

      const run = document.getElementById('btnRunReport');
      if (run) {
        run.addEventListener('click', () => {
          const total = Math.floor(180 + Math.random() * 520);
          const done = Math.floor(total * (0.35 + Math.random() * 0.35));
          const late = Math.floor(total * (0.05 + Math.random() * 0.12));
          const avg = (1.8 + Math.random() * 2.8).toFixed(1);

          set('repTotal', total);
          set('repDone', done);
          set('repLate', late);
          set('repAvg', avg + t('legacyPages.common.day'));

          const trend = Array.from({ length: 7 }, () => Math.floor(20 + Math.random() * 90));
          const status = [
            Math.floor(10 + Math.random() * 30),
            Math.floor(30 + Math.random() * 50),
            Math.floor(5 + Math.random() * 15),
            Math.floor(15 + Math.random() * 40)
          ];

          window.__lastReportPayload = { trend, status };
          if (window.GOV_CHARTS) GOV_CHARTS.renderReportsCharts(window.__lastReportPayload).catch(function () {});
          GOV.showToast(t('legacyPages.reports.runSuccessTitle'), t('legacyPages.reports.runSuccessMsg'));
        });
      }

      const pdf = document.getElementById('btnExportPdf');
      if (pdf)
        pdf.addEventListener('click', () =>
          GOV.showToast(t('legacyPages.reports.pdfTitle'), t('legacyPages.reports.pdfMsg'))
        );
      const xls = document.getElementById('btnExportXls');
      if (xls)
        xls.addEventListener('click', () =>
          GOV.showToast(t('legacyPages.reports.xlsTitle'), t('legacyPages.reports.xlsMsg'))
        );

      if (window.GOV_CHARTS) GOV_CHARTS.renderReportsCharts().catch(function () {});
    });
  });
})();
