(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    GOV.requireSession();
    const d = GOV.loadData();

    const id = localStorage.getItem('gov-selected-tx');
    const tx = d.tx.find(x=>x.id===id) || d.tx[0];

    const set = (k,v)=>{ const el = document.getElementById(k); if(el) el.textContent = v; };

    set('txId', tx.id);
    set('txType', tx.type);
    set('txSubject', tx.subject);
    set('txFrom', tx.from);
    set('txTo', tx.to);
    set('txCreated', tx.created);

    const st = document.getElementById('txStatus');
    if(st) st.innerHTML = GOV.statusPill(tx.status);

    const meta = tx.meta || (tx.meta = { desc:'—', secrecy:'عادي', maxDays:5, to:[tx.to], cc:[], attachments:[], timeline:[] });
    set('txDesc', meta.desc || '—');
    set('txSecrecy', meta.secrecy || 'عادي');
    set('txMaxDays', String(meta.maxDays || 5));

    // Attachments (demo)
    const attBody = document.getElementById('attBody');
    const attPager = document.getElementById('attPager');
    let attPage = 1;
    const attPerPage = 5;
    if(attBody){
      meta.attachments = meta.attachments || [];

      // Ensure diverse demo attachments (6)
      if(meta.attachments.length < 6){
        const now = '2026-02-02';
        meta.attachments = [
          { kind:'إلكتروني', type:'PDF', name:'خطاب_إحالة.pdf', secrecy:'محدود التداول', by:'مستخدم تجريبي', at: now },
          { kind:'إلكتروني', type:'Word', name:'مسودة_رد.docx', secrecy:'عادي', by:'مستخدم تجريبي', at: now },
          { kind:'إلكتروني', type:'Excel', name:'نموذج_متابعة.xlsx', secrecy:'عادي', by:'مستخدم تجريبي', at: now },
          { kind:'إلكتروني', type:'صورة', name:'مرفق_توضيحي.png', secrecy:'عادي', by:'مستخدم تجريبي', at: now },
          { kind:'مادي', type:'مستند ورقي', name:'أصل الخطاب', secrecy:'سري', by:'مستخدم تجريبي', at: now },
          { kind:'إلكتروني', type:'ZIP', name:'مرفقات_مجمعة.zip', secrecy:'محدود التداول', by:'مستخدم تجريبي', at: now }
        ];
      }
      

    function renderAttachments(){
      if(!attBody) return;
      const ap = window.GOV_PAGINATION ? GOV_PAGINATION.paginate(meta.attachments, attPage, attPerPage) : {slice:meta.attachments,total:meta.attachments.length,pages:1,page:1,perPage:attPerPage};
      const baseIdx = (ap.page-1)*ap.perPage;

      attBody.innerHTML = ap.slice.map((a,idx)=>{
        const sec = a.secrecy.includes('سري جداً')? 'bad' : a.secrecy.includes('سري')? 'warn' : '';
        return `
          <tr>
            <td>${a.kind}</td>
            <td>${a.type}</td>
            <td>${a.name}</td>
            <td><span class="pill ${sec}">${a.secrecy}</span></td>
            <td>${a.at}</td>
            <td>${a.by}</td>
            <td><div class="row-actions"><button class="btn" type="button" data-dl="${baseIdx+idx}">عرض/تحميل</button></div></td>
          </tr>
        `;
      }).join('');

      if(window.GOV_PAGINATION && attPager) GOV_PAGINATION.renderPager(attPager, ap);
    }

    if(attPager){
      attPager.addEventListener('click', (e)=>{
        const b = e.target.closest('.pager-btn');
        if(!b) return;
        attPage = Number(b.dataset.page || '1');
        renderAttachments();
      });
    }

    renderAttachments();


    }

    // Timeline
    const tl = document.getElementById('tlBody');
    function renderTimeline(){
      if(!tl) return;
      const items = (meta.timeline && meta.timeline.length) ? meta.timeline : [
        {at: new Date().toISOString(), action:'استلام', by:'المستخدم التجريبي', note:'تم فتح تفاصيل المعاملة (Demo).'},
        {at: new Date(Date.now()-1000*60*8).toISOString(), action:'قيد التدقيق', by:'مكتب الاتصالات', note:'تم تسجيل المعاملة وتوثيق البيانات الأساسية (Demo).'},
        {at: new Date(Date.now()-1000*60*18).toISOString(), action:'إحالة', by:'النظام', note:'تمت إحالة المعاملة للإدارة المختصة (Demo).'},
        {at: new Date(Date.now()-1000*60*28).toISOString(), action:'اعتماد', by:'المدير المختص', note:'تم اعتماد المعاملة إلكترونياً (Demo).'},
        {at: new Date(Date.now()-1000*60*40).toISOString(), action:'أرشفة', by:'مكتب الاتصالات', note:'تمت الأرشفة وإغلاق المعاملة (Demo).'}
      ];
      const tlSummary = `
        <div class="tl-summary">
          <div><b>إجمالي الأحداث:</b> ${items.length}</div>
          <div><b>آخر إجراء:</b> ${items[0].action}</div>
        </div>
      `;
      tl.innerHTML = tlSummary + items.map(i=>{
        return `
          <div class="tli">
            <div class="tldot"></div>
            <div class="tlcard">
              <div style="display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap">
                <b>${i.action}</b>
                <span class="muted">${i.at.replace('T',' ').slice(0,16)}</span>
              </div>
              <div class="muted" style="margin-top:6px">${i.by} — ${i.note || ''}</div>
            </div>
          </div>
        `;
      }).join('');
    }
    renderTimeline();

    // action modal
    const modal = document.getElementById('actModal');
    const ov = document.getElementById('actOverlay');
    const title = document.getElementById('actTitle');
    let currentAct = null;

    function openAct(act){
      currentAct = act;
      title.textContent = act;
      document.getElementById('actNote').value='';
      ov.classList.add('show');
      modal.classList.add('show');
    }
    function closeAct(){
      ov.classList.remove('show');
      modal.classList.remove('show');
    }

    ['btnApprove','btnForward','btnReturn','btnReject'].forEach(id=>{
      const b = document.getElementById(id);
      if(!b) return;
      b.addEventListener('click', ()=>{
        const map = {btnApprove:'اعتماد', btnForward:'تحويل', btnReturn:'إرجاع', btnReject:'رفض'};
        openAct(map[id]);
      });
    });

    document.getElementById('actClose').addEventListener('click', closeAct);
    ov.addEventListener('click', closeAct);

    document.getElementById('actSubmit').addEventListener('click', ()=>{
      const note = document.getElementById('actNote').value.trim();
      if(!note){
        GOV.showToast('تنبيه','يرجى كتابة سبب/ملاحظة الإجراء.');
        return;
      }

      if(currentAct==='اعتماد') tx.status='منجزة';
      if(currentAct==='رفض') tx.status='مرفوضة';
      if(currentAct==='إرجاع') tx.status='معادة';
      if(currentAct==='تحويل') tx.status='قيد الإجراء';

      meta.timeline = meta.timeline || [];
      meta.timeline.unshift({ at:new Date().toISOString(), action: currentAct, by:'المستخدم التجريبي', note });

      GOV.saveData(d);
      if(st) st.innerHTML = GOV.statusPill(tx.status);
      renderTimeline();
      GOV.showToast('تم','تم تنفيذ الإجراء وتحديث حالة المعاملة (Demo).');
      closeAct();
    });

    // attachments demo action
    if(attBody){
      attBody.addEventListener('click', (e)=>{
        const b = e.target.closest('[data-dl]');
        if(!b) return;
        GOV.showToast('معلومة','عرض/تحميل في النسخة التجريبية بدون ملفات فعلية.');
      });
    }

    // init editor (editor.js auto inits; keep safety)
    if(window.GOV_EDITOR && document.getElementById('editor')){
      GOV_EDITOR.init();
    }
  });
})();