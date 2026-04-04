(function(){
  // Inject topbar bell into pages that have #topbarRight slot
  document.addEventListener('DOMContentLoaded', ()=>{
    const slot = document.getElementById('topbarRight');
    if(!slot) return;

    const data = GOV.loadData();
    // Popover handles bell now
    const unread = data.notifications.filter(n=>!n.read).length;

    const wrap = document.createElement('div');
    wrap.style.display = 'flex';
    wrap.style.alignItems = 'center';
    wrap.style.gap = '8px';

    wrap.innerHTML = `
      <button class="btn" id="btnBell" type="button" style="position:relative">
        <span style="display:inline-flex; align-items:center; gap:8px">
          <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 10-12 0c0 7-3 7-3 7h18s-3 0-3-7"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
          الإشعارات
        </span>
        <span id="bellCount" class="badge" style="position:absolute; top:-8px; right:-8px; min-width:22px; text-align:center">${unread}</span>
      </button>
    `;

    slot.prepend(wrap);

    document.getElementById('btnBell').addEventListener('click', ()=>{
      location.href = './notifications.html';
    });
  });
})();
