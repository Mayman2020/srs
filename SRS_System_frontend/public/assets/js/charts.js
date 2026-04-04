// Charts using local Chart.js (no CDN)
// Requires: assets/vendor/chartjs/chart.umd.min.js loaded first.

(function(){
  const $ = (s, r=document)=>r.querySelector(s);

  function baseOptions(){
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

  function renderDashboardCharts(){
    const donut = $('#dashStatusDonut');
    const bar = $('#dashDeptBar');
    if(!window.Chart) return;

    if(donut){
      const ctx = donut.getContext('2d');
      window.__dashDonut && window.__dashDonut.destroy();
      window.__dashDonut = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['جديدة','قيد الإجراء','معادة','منجزة'],
          datasets: [{
            data: [18,44,9,29],
            backgroundColor: ['#0B6E4F','#064635','#F59E0B','#22C55E'],
            borderWidth: 0
          }]
        },
        options: {
          ...baseOptions(),
          cutout: '68%',
          plugins: { ...baseOptions().plugins, legend: { position: 'bottom', labels: baseOptions().plugins.legend.labels } }
        }
      });
    }

    if(bar){
      const ctx = bar.getContext('2d');
      window.__dashBar && window.__dashBar.destroy();
      window.__dashBar = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: ['الاتصالات الإدارية','الموارد البشرية','المالية','الإدارة العامة','تقنية المعلومات'],
          datasets: [{
            label: 'منجزة',
            data: [188,142,121,108,96],
            backgroundColor: 'rgba(11,110,79,.75)',
            borderRadius: 10
          }]
        },
        options: {
          ...baseOptions(),
          plugins: { ...baseOptions().plugins, legend: { display: false } }
        }
      });
    }
  }

  function renderReportsCharts(payload){
    const line = $('#repTrendLine');
    const donut = $('#repStatusDonut');
    if(!window.Chart) return;

    const trend = payload?.trend || [45,52,41,66,58,72,63];
    const status = payload?.status || [12,46,10,32];

    if(line){
      const ctx = line.getContext('2d');
      window.__repLine && window.__repLine.destroy();
      window.__repLine = new Chart(ctx, {
        type: 'line',
        data: {
          labels: ['س1','س2','س3','س4','س5','س6','س7'],
          datasets: [{
            label: 'حركة المعاملات',
            data: trend,
            fill: true,
            tension: .35,
            borderColor: '#0B6E4F',
            backgroundColor: 'rgba(11,110,79,.15)',
            pointBackgroundColor: '#0B6E4F',
            pointRadius: 4
          }]
        },
        options: baseOptions()
      });
    }

    if(donut){
      const ctx = donut.getContext('2d');
      window.__repDonut && window.__repDonut.destroy();
      window.__repDonut = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: ['جديدة','قيد الإجراء','معادة','منجزة'],
          datasets: [{
            data: status,
            backgroundColor: ['#0B6E4F','#064635','#F59E0B','#22C55E'],
            borderWidth: 0
          }]
        },
        options: {
          ...baseOptions(),
          cutout: '68%',
          plugins: { ...baseOptions().plugins, legend: { position: 'bottom', labels: baseOptions().plugins.legend.labels } }
        }
      });
    }
  }

  // rerender on theme changes
  function hookTheme(){
    const obs = new MutationObserver(()=>{
      const page = document.body.getAttribute('data-page');
      if(page==='dashboard') renderDashboardCharts();
      if(page==='reports') renderReportsCharts(window.__lastReportPayload);
    });
    obs.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
  }

  window.GOV_CHARTS = {
    renderDashboardCharts,
    renderReportsCharts
  };

  document.addEventListener('DOMContentLoaded', ()=>{
    hookTheme();
    const page = document.body.getAttribute('data-page');
    if(page==='dashboard') renderDashboardCharts();
  });
})();
