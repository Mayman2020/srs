(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    const s = GOV.requireSession();
    if(!s) return;

    const btnPdf = document.getElementById('btnExportPdf');
    const btnXls = document.getElementById('btnExportXls');

    function renderHeatmap(){
      const el = document.getElementById('slaHeatmap');
      if(!el) return;

      const depts = ['الاتصالات الإدارية','الموارد البشرية','الشؤون القانونية','المالية','المشتريات'];
      const weeks = ['أسبوع 1','أسبوع 2','أسبوع 3','أسبوع 4'];
      const vals = [
        [92, 88, 95, 90],
        [78, 82, 80, 85],
        [96, 94, 93, 97],
        [89, 91, 87, 92],
        [84, 79, 83, 81],
      ];

      const cls = (v)=> (v>=92 ? 'hm-cell ok' : v>=82 ? 'hm-cell warn' : 'hm-cell bad');

      el.innerHTML = `
        <div class="hm">
          <div class="hm-head">
            <div class="hm-corner"></div>
            ${weeks.map(w=>`<div class="hm-h">${w}</div>`).join('')}
          </div>
          ${depts.map((d,ri)=>{
            return `
              <div class="hm-row">
                <div class="hm-r">${d}</div>
                ${weeks.map((_,ci)=>{
                  const v = vals[ri][ci];
                  return `<div class="${cls(v)}" title="التزام SLA: ${v}%">${v}%</div>`;
                }).join('')}
              </div>
            `;
          }).join('')}
          <div class="hm-legend">
            <span class="muted">المؤشر:</span>
            <span class="pill ok">ممتاز</span>
            <span class="pill warn">بحاجة متابعة</span>
            <span class="pill bad">حرج</span>
          </div>
        </div>
      `;
    }

    function renderDeptProductivity(){
      const ctx = document.getElementById('deptBar');
      if(!ctx || !window.Chart) return;

      const labels = ['الاتصالات الإدارية','الموارد البشرية','الشؤون القانونية','المالية','المشتريات'];
      const done = [128, 96, 74, 88, 62];
      const late = [12, 18, 7, 10, 14];

      if(window.__deptChart) window.__deptChart.destroy();
      window.__deptChart = new Chart(ctx, {
        type:'bar',
        data:{
          labels,
          datasets:[
            { label:'منجزة', data: done, backgroundColor:'rgba(34,197,94,.30)', borderColor:'rgba(34,197,94,.85)', borderWidth:1 },
            { label:'متأخرة', data: late, backgroundColor:'rgba(245,158,11,.26)', borderColor:'rgba(245,158,11,.85)', borderWidth:1 }
          ]
        },
        options:{
          responsive:true,
          plugins:{ legend:{ position:'bottom' } },
          scales:{ y:{ beginAtZero:true } }
        }
      });
    }

    function mountAdvanced(){
      // Add advanced section if not present
      const main = document.querySelector('main.main');
      if(!main) return;
      if(document.getElementById('slaHeatmap')) return;

      const sec = document.createElement('section');
      sec.className = 'grid c2';
      sec.style.marginTop = '12px';
      sec.innerHTML = `
        <div class="card">
          <div class="title"><h3>إنتاجية الإدارات</h3><small>منجزة / متأخرة</small></div>
          <canvas id="deptBar" height="170"></canvas>
        </div>
        <div class="card">
          <div class="title"><h3>SLA Heatmap</h3><small>التزام الأقسام خلال الأسابيع</small></div>
          <div id="slaHeatmap"></div>
        </div>
      `;

      main.insertBefore(sec, document.getElementById('toast'));
    }

    function run(){
      mountAdvanced();
      renderHeatmap();
      renderDeptProductivity();
    }

    // hook on run report
    const btnRun = document.getElementById('btnRunReport');
    if(btnRun) btnRun.addEventListener('click', ()=> setTimeout(run, 0));

    // initial
    setTimeout(run, 0);

    if(btnPdf) btnPdf.addEventListener('click', ()=>GOV.showToast('تصدير PDF', 'تصدير تجريبي (بدون إنشاء ملف).'));
    if(btnXls) btnXls.addEventListener('click', ()=>GOV.showToast('تصدير Excel', 'تصدير تجريبي (بدون إنشاء ملف).'));
  });
})();
