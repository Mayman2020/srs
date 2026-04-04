(function(){
  function uuid(){
    const y = new Date().getFullYear();
    const m = String(Math.floor(Math.random()*9000)+1000);
    return `1445/${m}`;
  }

  function openDrawer(){
    document.documentElement.classList.add('drawer-open');
    const ov = document.getElementById('drawerOverlay');
    if(ov) ov.classList.add('show');
  }
  function closeDrawer(){
    document.documentElement.classList.remove('drawer-open');
    const ov = document.getElementById('drawerOverlay');
    if(ov) ov.classList.remove('show');
  }

  function getTagValues(boxId){
    const box = document.getElementById(boxId);
    if(!box) return [];
    return Array.from(box.querySelectorAll('.tag')).map(t=>t.dataset.value);
  }

  function addTag(boxId, value){
    value = (value||'').trim();
    if(!value) return;
    const box = document.getElementById(boxId);
    if(!box) return;

    const exists = Array.from(box.querySelectorAll('.tag')).some(t=>t.dataset.value===value);
    if(exists) return;

    const t = document.createElement('span');
    t.className='tag';
    t.dataset.value=value;
    t.innerHTML = `${value}<button type="button" class="tag-x" aria-label="حذف">×</button>`;
    box.appendChild(t);
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    GOV.requireSession();

    const btn = document.getElementById('btnCreateTx');
    const ov = document.getElementById('drawerOverlay');
    const close = document.getElementById('drawerClose');

    if(btn) btn.addEventListener('click', openDrawer);
    if(ov) ov.addEventListener('click', closeDrawer);
    if(close) close.addEventListener('click', closeDrawer);

    // tag inputs
    const toInput = document.getElementById('toInput');
    const ccInput = document.getElementById('ccInput');

    function wireTagInput(input, boxId){
      if(!input) return;
      input.addEventListener('keydown', (e)=>{
        if(e.key==='Enter' || e.key===','){
          e.preventDefault();
          addTag(boxId, input.value);
          input.value='';
        }
      });
    }
    wireTagInput(toInput,'toTags');
    wireTagInput(ccInput,'ccTags');

    document.getElementById('toTags').addEventListener('click',(e)=>{
      const x = e.target.closest('.tag-x');
      if(!x) return;
      x.parentElement.remove();
    });
    document.getElementById('ccTags').addEventListener('click',(e)=>{
      const x = e.target.closest('.tag-x');
      if(!x) return;
      x.parentElement.remove();
    });

    // submit
    document.getElementById('btnCreateSubmit').addEventListener('click', ()=>{
      const type = document.getElementById('cType').value;
      const subject = document.getElementById('cSubject').value.trim();
      const from = document.getElementById('cFrom').value.trim();
      const secrecy = document.getElementById('cSecrecy').value;
      const maxDays = Number(document.getElementById('cMaxDays').value || '5');
      const desc = document.getElementById('cDesc').value.trim();

      const to = getTagValues('toTags');
      const cc = getTagValues('ccTags');

      if(!subject || !from || !to.length){
        GOV.showToast('تنبيه','يرجى إدخال الموضوع والجهة المرسلة وإضافة جهة مستقبلة واحدة على الأقل.');
        return;
      }

      const d = GOV.loadData();
      const id = uuid();

      const created = new Date();
      const createdStr = created.toISOString().slice(0,10);

      d.tx.unshift({
        id,
        type,
        subject,
        from,
        to: to.length>1 ? 'مجموعة إدارات' : to[0],
        created: createdStr,
        status:'جديدة',
        meta:{
          desc,
          secrecy,
          maxDays,
          to,
          cc,
          durationDays:0,
          attachments:[],
          timeline:[
            {at: new Date().toISOString(), action:'إنشاء', by:'المستخدم التجريبي', note:'تم إنشاء المعاملة (Demo).'}
          ]
        }
      });

      GOV.saveData(d);
      localStorage.setItem('gov-selected-tx', id);
      GOV.showToast('تم','تم إنشاء المعاملة وإضافتها للقائمة (Demo).');
      closeDrawer();
      setTimeout(()=> location.href='./transaction-details.html', 350);
    });

    // esc close
    document.addEventListener('keydown',(e)=>{ if(e.key==='Escape') closeDrawer(); });
  });
})();
