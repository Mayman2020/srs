(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    const s = GOV.requireSession();
    if(!s) return;

    const data = GOV.loadData();

    // KPIs (demo)
    const total = 1248;
    const done = 498;
    const inprog = 548;
    const incoming = 362;

    const set = (id, v)=>{ const el = document.getElementById(id); if(el) el.textContent = v; };
    set('kTotal', total.toLocaleString('ar-SA'));
    set('kDone', done.toLocaleString('ar-SA'));
    set('kInprog', inprog.toLocaleString('ar-SA'));
    set('kIn', incoming.toLocaleString('ar-SA'));

    // Latest transactions as table + pagination
    const tbl = document.getElementById('dashTxBody');
    const pager = document.getElementById('dashTxPager');

    let page = 1;
    const perPage = 5;

    function row(t){
      return `
        <tr>
          <td><span class="link" data-open="${t.id}">${t.id}</span></td>
          <td>${t.type}</td>
          <td>${t.subject}</td>
          <td>${t.created}</td>
          <td>${GOV.statusPill(t.status)}</td>
          <td><button class="btn" type="button" data-open="${t.id}">فتح</button></td>
        </tr>
      `;
    }

    function renderLatest(){
      if(!tbl) return;
      const list = data.tx.slice().reverse();
      const p = window.GOV_PAGINATION ? GOV_PAGINATION.paginate(list, page, perPage) : {slice:list, total:list.length, pages:1, page:1, perPage};
      tbl.innerHTML = p.slice.map(row).join('') || `<tr><td colspan="6" class="muted">لا توجد معاملات.</td></tr>`;
      if(window.GOV_PAGINATION && pager) GOV_PAGINATION.renderPager(pager, p);
    }

    if(pager){
      pager.addEventListener('click', (e)=>{
        const b = e.target.closest('.pager-btn');
        if(!b) return;
        page = Number(b.dataset.page || '1');
        renderLatest();
      });
    }

    if(tbl){
      tbl.addEventListener('click', (e)=>{
        const open = e.target.closest('[data-open]');
        if(!open) return;
        const id = open.dataset.open;
        localStorage.setItem('gov-selected-tx', id);
        location.href = './transaction-details.html';
      });
    }

    renderLatest();

    // existing dashboard charts
    if(window.GOV_CHARTS) GOV_CHARTS.renderDashboardCharts();

    // --- AI Analytics (Demo) ---
    function isLate(tx){
      const created = new Date((tx.created||'2026-01-01')+'T00:00:00');
      const threshold = new Date('2026-01-26T00:00:00');
      if(tx.status==='معادة') return true;
      if(tx.status==='قيد الإجراء' && created < threshold) return true;
      return false;
    }

    function priorityOf(tx){
      // Demo priority inferred from status/type
      if(isLate(tx)) return 'عالية';
      if(tx.status==='قيد الإجراء') return 'متوسطة';
      return 'منخفضة';
    }

    function renderAiAnalytics(){
      // KPIs
      const inP = data.tx.filter(t=>t.status==='قيد الإجراء').length;
      const late = data.tx.filter(isLate).length;
      const risk = Math.max(0, Math.min(95, Math.round(55 + late*8 + inP*2)));
      const conf = 76;
      const next = Math.round(data.tx.length * (1.08 + (late>1?0.04:0)));

      const riskEl = document.getElementById('aiRisk');
      const confEl = document.getElementById('aiConf');
      const nextEl = document.getElementById('aiNext');
      if(riskEl) riskEl.textContent = risk + '%';
      if(confEl) confEl.textContent = conf + '%';
      if(nextEl) nextEl.textContent = next.toLocaleString('ar-SA');

      if(!window.Chart) return;

      // Status distribution
      const statuses = ['جديدة','قيد الإجراء','معادة','منجزة','مرفوضة'];
      const statusCounts = statuses.map(s=> data.tx.filter(t=>t.status===s).length);

      // Priority distribution
      const pr = ['عالية','متوسطة','منخفضة'];
      const prCounts = pr.map(p=> data.tx.filter(t=>priorityOf(t)===p).length);

      // Destroy if exists
      if(window.__aiStatusDonut) window.__aiStatusDonut.destroy();
      if(window.__aiPriorityBar) window.__aiPriorityBar.destroy();

      const ctx1 = document.getElementById('aiStatusDonut');
      const ctx2 = document.getElementById('aiPriorityBar');
      if(ctx1){
        window.__aiStatusDonut = new Chart(ctx1, {
          type:'doughnut',
          data:{
            labels: statuses,
            datasets:[{
              data: statusCounts,
              backgroundColor:[
                'rgba(59,130,246,.20)',
                'rgba(245,158,11,.22)',
                'rgba(245,158,11,.18)',
                'rgba(34,197,94,.24)',
                'rgba(220,38,38,.20)'
              ],
              borderColor:[
                'rgba(59,130,246,.75)',
                'rgba(245,158,11,.75)',
                'rgba(245,158,11,.65)',
                'rgba(34,197,94,.75)',
                'rgba(220,38,38,.75)'
              ],
              borderWidth:1
            }]
          },
          options:{
            responsive:true,
            plugins:{ legend:{ position:'bottom' } }
          }
        });
      }

      if(ctx2){
        window.__aiPriorityBar = new Chart(ctx2, {
          type:'bar',
          data:{
            labels: pr,
            datasets:[{
              label:'عدد المعاملات',
              data: prCounts,
              backgroundColor:['rgba(220,38,38,.20)','rgba(245,158,11,.22)','rgba(34,197,94,.22)'],
              borderColor:['rgba(220,38,38,.75)','rgba(245,158,11,.75)','rgba(34,197,94,.75)'],
              borderWidth:1
            }]
          },
          options:{
            responsive:true,
            plugins:{ legend:{ display:false } },
            scales:{ y:{ beginAtZero:true } }
          }
        });
      }
    }

    renderAiAnalytics();
  });
})();
