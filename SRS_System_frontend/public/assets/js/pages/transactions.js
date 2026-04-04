(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    GOV.requireSession();

    const tbody = document.getElementById('txBody');
    const rc = document.getElementById('resultCount');
    const pagerEl = document.getElementById('pagerTx');

    let page = 1;
    let perPage = 10;
    let filtered = null;

    function rowHtml(t){
      return `
        <tr>
          <td><span class="link" data-open="${t.id}">${t.id}</span></td>
          <td>${t.type}</td>
          <td>${t.subject}</td>
          <td>${t.from}</td>
          <td>${t.to}</td>
          <td>${t.created}</td>
          <td>${GOV.statusPill(t.status)}</td>
          <td>
            <div class="row-actions">
              <button class="btn" data-open="${t.id}" type="button">فتح</button>
            </div>
          </td>
        </tr>
      `;
    }

    function applyRender(){
      const d = GOV.loadData();
      const list = filtered || d.tx;

      const p = window.GOV_PAGINATION ? GOV_PAGINATION.paginate(list, page, perPage) : {slice:list, total:list.length, pages:1, page:1, perPage};

      rc.textContent = p.total;
      tbody.innerHTML = p.slice.map(rowHtml).join('') || `<tr><td colspan="8" class="muted">لا توجد نتائج مطابقة.</td></tr>`;

      if(window.GOV_PAGINATION) GOV_PAGINATION.renderPager(pagerEl, p);
    }

    // wire pager
    if(pagerEl){
      pagerEl.addEventListener('click', (e)=>{
        const b = e.target.closest('.pager-btn');
        if(!b) return;
        page = Number(b.dataset.page || '1');
        applyRender();
      });
    }

    // filters
    document.getElementById('btnApply').addEventListener('click', ()=>{
      const d = GOV.loadData();
      const fNo = (document.getElementById('fNo').value||'').trim();
      const fSubject = (document.getElementById('fSubject').value||'').trim();
      const fFrom = (document.getElementById('fFrom').value||'').trim();
      const fType = document.getElementById('fType').value;
      const fStatus = document.getElementById('fStatus').value;

      filtered = d.tx.filter(t=>{
        if(fNo && !t.id.includes(fNo)) return false;
        if(fSubject && !t.subject.includes(fSubject)) return false;
        if(fFrom && !t.from.includes(fFrom)) return false;
        if(fType && t.type!==fType) return false;
        if(fStatus && t.status!==fStatus) return false;
        return true;
      });
      page = 1;
      applyRender();
      GOV.showToast('تم', 'تم تطبيق الفلاتر.');
    });

    document.getElementById('btnReset').addEventListener('click', ()=>{
      ['fNo','fSubject','fFrom'].forEach(id=> document.getElementById(id).value='');
      document.getElementById('fType').value='';
      document.getElementById('fStatus').value='';
      filtered = null;
      page = 1;
      applyRender();
      GOV.showToast('تم', 'تمت إعادة تعيين الفلاتر.');
    });

    // open details
    tbody.addEventListener('click', (e)=>{
      const open = e.target.closest('[data-open]');
      if(!open) return;
      const id = open.dataset.open;
      localStorage.setItem('gov-selected-tx', id);
      location.href = './transaction-details.html';
    });

    // init
    applyRender();
  });
})();
