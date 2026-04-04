(function(){
  function paginate(items, page, perPage){
    const total = items.length;
    const pages = Math.max(1, Math.ceil(total / perPage));
    const p = Math.min(Math.max(1, page), pages);
    const start = (p-1)*perPage;
    const end = start + perPage;
    return { page:p, perPage, pages, total, slice: items.slice(start,end) };
  }

  function renderPager(container, state){
    if(!container) return;
    const {page, pages, total, perPage} = state;

    const mkBtn = (label, p, disabled=false)=>{
      const b = document.createElement('button');
      b.className = 'pager-btn';
      b.type='button';
      b.textContent = label;
      b.disabled = disabled;
      b.dataset.page = String(p);
      return b;
    };

    container.innerHTML='';
    const wrap = document.createElement('div');
    wrap.className='pager';

    const left = document.createElement('div');
    left.className='pager-left';
    left.innerHTML = `<span class="muted">الإجمالي:</span> <b>${total}</b> <span class="muted">|</span> <span class="muted">لكل صفحة:</span> <b>${perPage}</b>`;

    const right = document.createElement('div');
    right.className='pager-right';

    right.appendChild(mkBtn('السابق', page-1, page<=1));

    // show a small window of page numbers
    const windowSize = 5;
    let start = Math.max(1, page - Math.floor(windowSize/2));
    let end = Math.min(pages, start + windowSize - 1);
    start = Math.max(1, end - windowSize + 1);

    for(let i=start;i<=end;i++){
      const b = mkBtn(String(i), i, false);
      if(i===page) b.classList.add('active');
      right.appendChild(b);
    }

    right.appendChild(mkBtn('التالي', page+1, page>=pages));

    wrap.appendChild(left);
    wrap.appendChild(right);

    container.appendChild(wrap);
  }

  window.GOV_PAGINATION = { paginate, renderPager };
})();
