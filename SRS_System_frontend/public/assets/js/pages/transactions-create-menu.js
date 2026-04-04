(function(){
  function closeMenu(){
    const m = document.getElementById('createMenu');
    if(m) m.classList.remove('show');
  }
  function toggleMenu(){
    const m = document.getElementById('createMenu');
    if(!m) return;
    m.classList.toggle('show');
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    // If no split exists, do nothing
    const btnMain = document.getElementById('btnCreateTx');
    const btnMenu = document.getElementById('btnCreateTxMenu');
    const menu = document.getElementById('createMenu');
    if(!btnMain || !btnMenu || !menu) return;

    // main button behavior = open default create page
    btnMain.addEventListener('click', ()=> location.href='./transaction-create.html');
    btnMenu.addEventListener('click', (e)=>{ e.stopPropagation(); toggleMenu(); });

    menu.addEventListener('click', (e)=>{
      const item = e.target.closest('.menu-item');
      if(!item) return;
      const go = item.dataset.go;
      closeMenu();
      if(go) location.href = go;
    });

    document.addEventListener('click', (e)=>{
      if(e.target.closest('#createSplit')) return;
      closeMenu();
    });

    document.addEventListener('keydown', (e)=>{ if(e.key==='Escape') closeMenu(); });
  });
})();
