(function(){
  function isMobile(){
    return window.matchMedia('(max-width: 1024px)').matches;
  }

  function ensureButtons(){
    // Outside hamburger in topbar (mobile only)
    const topbar = document.querySelector('.topbar');
    if(topbar && !document.getElementById('btnHamburger')){
      const btn = document.createElement('button');
      btn.className = 'btn icon only-closed';
      btn.id = 'btnHamburger';
      btn.type = 'button';
      btn.setAttribute('aria-label','فتح القائمة');
      btn.innerHTML = `
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      `;
      const crumb = topbar.querySelector('.crumb');
      if(crumb) crumb.prepend(btn);
      else topbar.prepend(btn);
      btn.addEventListener('click', ()=> toggle(true));
    }

    // Inside close button in sidebar header
    const brand = document.querySelector('.sidebar .brand');
    if(brand && !document.getElementById('btnHamburgerIn')){
      const inBtn = document.createElement('button');
      inBtn.className = 'btn icon only-open';
      inBtn.id = 'btnHamburgerIn';
      inBtn.type = 'button';
      inBtn.setAttribute('aria-label','إغلاق القائمة');
      inBtn.innerHTML = `
        <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M18 6L6 18M6 6l12 12"/>
        </svg>
      `;
      // put it at start of brand row
      brand.prepend(inBtn);
      inBtn.addEventListener('click', ()=> toggle(false));
    }
  }

  function ensureOverlay(){
    let overlay = document.getElementById('sidebarOverlay');
    if(!overlay){
      overlay = document.createElement('div');
      overlay.id = 'sidebarOverlay';
      overlay.className = 'overlay';
      document.body.appendChild(overlay);
      overlay.addEventListener('click', ()=> toggle(false));
    }
  }

  function toggle(open){
    ensureOverlay();

    const shouldOpen = !!open && isMobile();
    document.documentElement.classList.toggle('sidebar-open', shouldOpen);

    const overlay = document.getElementById('sidebarOverlay');
    if(overlay) overlay.classList.toggle('show', shouldOpen);

    // Icon visibility rules:
    // when open -> show inside close, hide outside hamburger
    // when closed -> hide inside close, show outside hamburger
    const outBtn = document.getElementById('btnHamburger');
    const inBtn = document.getElementById('btnHamburgerIn');
    if(outBtn) outBtn.style.display = shouldOpen ? 'none' : '';
    if(inBtn) inBtn.style.display = shouldOpen ? '' : 'none';
  }

  function syncOnResize(){
    // if leaving mobile, reset overlay and show normal state
    if(!isMobile()){
      document.documentElement.classList.remove('sidebar-open');
      const overlay = document.getElementById('sidebarOverlay');
      if(overlay) overlay.classList.remove('show');
      const outBtn = document.getElementById('btnHamburger');
      const inBtn = document.getElementById('btnHamburgerIn');
      if(outBtn) outBtn.style.display = 'none'; // desktop: no hamburger
      if(inBtn) inBtn.style.display = 'none';
      return;
    }

    // mobile baseline: sidebar closed, hamburger visible
    const opened = document.documentElement.classList.contains('sidebar-open');
    const outBtn = document.getElementById('btnHamburger');
    const inBtn = document.getElementById('btnHamburgerIn');
    if(outBtn) outBtn.style.display = opened ? 'none' : '';
    if(inBtn) inBtn.style.display = opened ? '' : 'none';
  }

  function wireCloseOnEsc(){
    document.addEventListener('keydown', (e)=>{
      if(e.key === 'Escape') toggle(false);
    });
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    ensureButtons();
    wireCloseOnEsc();
    // initial state
    syncOnResize();
    window.addEventListener('resize', ()=>{
      ensureButtons();
      syncOnResize();
    });
  });
})();
