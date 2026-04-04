(function(){
  function extractTxNo(text){
    const m = (text||'').match(/\b\d{4}\/\d{3,6}\b/);
    return m ? m[0] : '—';
  }

  function buildRecommendedAction(n){
    if(n.type==='تذكير') return 'متابعة SLA';
    if(n.type==='تنبيه') return 'فتح المعاملة وتسجيل إجراء';
    return 'مراجعة عامة';
  }

  function computeSlaLevel(n){
    if(n.important && !n.read) return {label:'عالي', cls:'bad'};
    if(n.important) return {label:'متوسط', cls:'warn'};
    return {label:'منخفض', cls:''};
  }

  function computePriority(n){
    if(n.important && !n.read) return {label:'عالية', cls:'bad'};
    if(n.important) return {label:'متوسطة', cls:'warn'};
    return {label:'منخفضة', cls:''};
  }

  function computeOwner(txNo){
    if(txNo==='—') return '—';
    // demo mapping by last digit
    const last = Number((txNo.split('/')[1]||'0').slice(-1));
    const map = [
      'الاتصالات الإدارية',
      'الموارد البشرية',
      'الشؤون القانونية',
      'المالية',
      'المشتريات'
    ];
    return map[last % map.length];
  }

  function computeChannel(n){
    // demo: infer by type
    if(n.type==='تنبيه') return 'نظام';
    if(n.type==='تذكير') return 'نظام';
    return 'يدوي';
  }

  function computeDueDate(n){
    // demo: add 1/2/5 days based on SLA
    const sla = computeSlaLevel(n).label;
    const base = (n.time||'').split(' ')[0];
    if(!base || base==='—') return '—';
    const d = new Date(base+'T00:00:00');
    const add = sla==='عالي' ? 1 : sla==='متوسط' ? 2 : 5;
    d.setDate(d.getDate()+add);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth()+1).padStart(2,'0');
    const dd = String(d.getDate()).padStart(2,'0');
    return `${yyyy}-${mm}-${dd}`;
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    const s = GOV.requireSession();
    if(!s) return;

    const data = GOV.loadData();

    function computeCounts(){
      const unread = data.notifications.filter(n=>!n.read).length;
      const badge = document.getElementById('badgeNoti');
      if(badge) badge.textContent = String(unread);
      GOV.saveData(data);
    }

    function render(list){
      const body = document.getElementById('notiBody');
      if(!body) return;
      body.innerHTML = '';

      list.forEach((n, i)=>{
        const txNo = extractTxNo(n.text);
        const sla = computeSlaLevel(n);
        const pri = computePriority(n);
        const source = txNo!=='—' ? 'نظام المعاملات' : 'النظام';
        const action = buildRecommendedAction(n);
        const owner = computeOwner(txNo);
        const due = computeDueDate(n);
        const channel = computeChannel(n);

        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${n.type}</td>
          <td>${source}</td>
          <td>${txNo}</td>
          <td>${n.text}</td>
          <td>${n.time}</td>
          <td>${n.read ? '<span class="pill ok">مقروء</span>' : '<span class="pill warn">غير مقروء</span>'}</td>
          <td>${n.important ? '<span class="pill">هام</span>' : '<span class="pill" style="opacity:.65">عادي</span>'}</td>
          <td><span class="pill ${sla.cls}">${sla.label}</span></td>
          <td>${action}</td>
          <td>${owner}</td>
          <td>${due}</td>
          <td>${channel}</td>
          <td><span class="pill ${pri.cls}">${pri.label}</span></td>
          <td>${txNo!=='—' ? `<a class="link" data-open="${i}">تفاصيل</a>` : '<span class="muted">—</span>'}</td>
          <td>
            <div class="row-actions">
              <a class="link" data-open="${i}">فتح</a>
              <a class="link" data-toggle="${i}">${n.read ? 'تعيين غير مقروء' : 'تعيين مقروء'}</a>
              <a class="link" data-imp="${i}">${n.important ? 'إلغاء هام' : 'تمييز هام'}</a>
              <a class="link" data-del="${i}">حذف</a>
            </div>
          </td>
        `;
        body.appendChild(tr);
      });

      const rc = document.getElementById('notiCount');
      if(rc) rc.textContent = list.length;
      computeCounts();
    }

    function applyFilter(){
      const fRead = document.getElementById('fRead')?.value || 'all';
      const fImp = document.getElementById('fImp')?.value || 'all';
      const q = (document.getElementById('fQ')?.value || '').trim();

      const list = data.notifications.filter(n=>{
        if(fRead==='unread' && n.read) return false;
        if(fRead==='read' && !n.read) return false;
        if(fImp==='important' && !n.important) return false;
        if(fImp==='normal' && n.important) return false;
        if(q && !(n.text.includes(q) || n.type.includes(q))) return false;
        return true;
      });
      render(list);
      GOV.showToast('تم', 'تم تطبيق الفلترة.');
    }

    render(data.notifications);

    document.getElementById('btnNotiApply')?.addEventListener('click', applyFilter);

    document.getElementById('btnNotiReset')?.addEventListener('click', ()=>{
      ['fRead','fImp'].forEach(id=>{ const el=document.getElementById(id); if(el) el.value='all'; });
      const q = document.getElementById('fQ'); if(q) q.value='';
      render(data.notifications);
      GOV.showToast('تم', 'تم مسح الفلاتر.');
    });

    document.getElementById('btnMarkAllRead')?.addEventListener('click', ()=>{
      data.notifications.forEach(n=>n.read=true);
      render(data.notifications);
      GOV.showToast('تم', 'تم تعيين جميع الإشعارات كمقروءة.');
    });

    const body = document.getElementById('notiBody');
    if(body){
      body.addEventListener('click', (e)=>{
        const open = e.target.getAttribute('data-open');
        const t = e.target.getAttribute('data-toggle');
        const imp = e.target.getAttribute('data-imp');
        const del = e.target.getAttribute('data-del');

        if(open !== null){
          const i = Number(open);
          const txNo = extractTxNo(data.notifications[i].text);
          if(txNo !== '—'){
            localStorage.setItem('gov-selected-tx', txNo);
            location.href = './transaction-details.html';
          } else {
            GOV.showToast('معلومة','لا يوجد رقم معاملة مرتبط بهذا الإشعار.');
          }
        }

        if(t !== null){
          const i = Number(t);
          data.notifications[i].read = !data.notifications[i].read;
          render(data.notifications);
        }
        if(imp !== null){
          const i = Number(imp);
          data.notifications[i].important = !data.notifications[i].important;
          render(data.notifications);
        }
        if(del !== null){
          const i = Number(del);
          data.notifications.splice(i,1);
          render(data.notifications);
          GOV.showToast('تم', 'تم حذف الإشعار (Demo).');
        }
      });
    }

    computeCounts();
  });
})();
