(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    const s = GOV.requireSession();
    if(!s) return;

    const set = (id, v)=>{ const el = document.getElementById(id); if(el) el.textContent = v; };

    const run = document.getElementById('btnRunReport');
    if(run){
      run.addEventListener('click', ()=>{
        const total = Math.floor(180 + Math.random()*520);
        const done = Math.floor(total*(0.35 + Math.random()*0.35));
        const late = Math.floor(total*(0.05 + Math.random()*0.12));
        const avg = (1.8 + Math.random()*2.8).toFixed(1);

        set('repTotal', total);
        set('repDone', done);
        set('repLate', late);
        set('repAvg', avg + ' يوم');

        const trend = Array.from({length:7}, ()=> Math.floor(20 + Math.random()*90));
        const status = [
          Math.floor(10 + Math.random()*30),
          Math.floor(30 + Math.random()*50),
          Math.floor(5 + Math.random()*15),
          Math.floor(15 + Math.random()*40)
        ];

        window.__lastReportPayload = { trend, status };
        if(window.GOV_CHARTS) GOV_CHARTS.renderReportsCharts(window.__lastReportPayload);
        GOV.showToast('تم', 'تم تشغيل التقرير وتحديث الرسوم.');
      });
    }

    const pdf = document.getElementById('btnExportPdf');
    if(pdf) pdf.addEventListener('click', ()=>GOV.showToast('تصدير PDF', 'Demo: سيتم ربطه لاحقًا.'));
    const xls = document.getElementById('btnExportXls');
    if(xls) xls.addEventListener('click', ()=>GOV.showToast('تصدير Excel', 'Demo: سيتم ربطه لاحقًا.'));

    // initial render charts with defaults
    if(window.GOV_CHARTS) GOV_CHARTS.renderReportsCharts();
  });
})();
