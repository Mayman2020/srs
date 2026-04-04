(function(){
  function fmtType(t){
    if(t==='تنبيه') return 'تنبيه';
    if(t==='تذكير') return 'تذكير';
    return 'معلومة';
  }

  function renderItems(list){
    if(!list.length) return `<div class="muted" style="padding:10px">لا توجد إشعارات حالياً.</div>`;

    return list.map((n, idx)=>{
      const tag = n.important ? '<span class="pill warn" style="padding:2px 8px">هام</span>' : '<span class="pill" style="padding:2px 8px">عادي</span>';
      const unread = !n.read ? '<span class="dot" aria-hidden="true"></span>' : '';
      return `
        <button type="button" class="np-item" data-idx="${idx}">
          <div class="np-left">${unread}<span class="np-type">${fmtType(n.type)}</span>${tag}</div>
          <div class="np-body">
            <div class="np-text">${n.text}</div>
            <div class="np-time">${n.time}</div>
          </div>
        </button>
      `;
    }).join('');
  }

  function mount(){
    const slot = document.getElementById('topbarRight');
    if(!slot) return;

    // avoid double mount
    if(document.getElementById('btnBell')) return;

    const data = GOV.loadData();
    const unread = data.notifications.filter(n=>!n.read).length;
    const latest = data.notifications.slice().sort((a,b)=> (a.time<b.time?1:-1)).slice(0,5);

    const wrap = document.createElement('div');
    wrap.className='np-wrap';
    wrap.innerHTML = `
      <button class="btn icon" id="btnBell" type="button" aria-label="الإشعارات" style="position:relative">
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 10-12 0c0 7-3 7-3 7h18s-3 0-3-7"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
        <span id="bellCount" class="badge" style="position:absolute; top:-8px; right:-8px; min-width:22px; text-align:center">${unread}</span>
      </button>
      <div class="np-pop" id="npPop" role="dialog" aria-label="قائمة الإشعارات">
        <div class="np-head">
          <b>الإشعارات</b>
          <div style="display:flex; gap:8px; align-items:center">
            <button class="btn" id="btnMarkAllRead" type="button" style="padding:8px 10px">تعيين الكل كمقروء</button>
            <button class="btn ghost" id="btnGoAll" type="button" style="padding:8px 10px">جميع الإشعارات</button>
          </div>
        </div>
        <div class="np-list" id="npList">
          ${renderItems(latest)}
        </div>
        <div class="np-foot">
          <span class="muted">عرض آخر 5 إشعارات</span>
          <button class="btn" id="btnClosePop" type="button" style="padding:8px 10px">إغلاق</button>
        </div>
      </div>
    `;

    slot.prepend(wrap);

    const btn = document.getElementById('btnBell');
    const pop = document.getElementById('npPop');

    function close(){ pop.classList.remove('show'); }
    function open(){ pop.classList.add('show'); }

    btn.addEventListener('click', (e)=>{
      e.stopPropagation();
      if(pop.classList.contains('show')) close(); else open();
    });

    document.addEventListener('click', (e)=>{
      if(!pop.contains(e.target) && e.target!==btn) close();
    });

    document.getElementById('btnGoAll').addEventListener('click', ()=> location.href='./notifications.html');
    document.getElementById('btnClosePop').addEventListener('click', close);

    document.getElementById('btnMarkAllRead').addEventListener('click', ()=>{
      const d = GOV.loadData();
      d.notifications = d.notifications.map(n=>({ ...n, read:true }));
      GOV.saveData(d);
      GOV.showToast('تم', 'تم تعيين جميع الإشعارات كمقروء (Demo).');
      // update badge
      const bc = document.getElementById('bellCount');
      if(bc) bc.textContent='0';
      // refresh list
      const latest2 = d.notifications.slice().sort((a,b)=> (a.time<b.time?1:-1)).slice(0,5);
      const list = document.getElementById('npList');
      if(list) list.innerHTML = renderItems(latest2);
    });

    // item click opens notifications page (demo)
    pop.addEventListener('click', (e)=>{
      const btnItem = e.target.closest('.np-item');
      if(!btnItem) return;
      location.href='./notifications.html';
    });
  }

  document.addEventListener('DOMContentLoaded', mount);
})();
