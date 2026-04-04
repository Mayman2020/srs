(function(){
  const BOT_KEY = 'gov-ai-bot-state';
  const BOT_PREF_KEY = 'gov-ai-bot-pref';

  const SUGGESTIONS = [
    'اعرض المعاملات المتأخرة',
    'كم معاملة قيد الإجراء؟',
    'لخّص آخر 5 معاملات',
    'هات تفاصيل المعاملة 1445/10304',
    'ابحث عن الأرشفة'
  ];

  const DISCLAIMER = '<div class="ai-disclaimer">تنبيه: هذا مساعد تجريبي (بدون ذكاء اصطناعي حقيقي) يعتمد على بيانات المتصفح فقط.</div>';

  function escapeHtml(s){
    return String(s||'').replace(/[&<>\"]/g, c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;"}[c]));
  }

  function normalizeAr(s){
    return String(s||'')
      .toLowerCase()
      .replace(/[إأآ]/g,'ا')
      .replace(/ة/g,'ه')
      .replace(/ى/g,'ي')
      .replace(/ؤ/g,'و')
      .replace(/ئ/g,'ي')
      .replace(/\s+/g,' ')
      .trim();
  }

  function parseTxId(text){
    const m = String(text||'').match(/\b\d{4}\/\d{3,6}\b/);
    return m ? m[0] : null;
  }

  function parseLastN(text){
    const m = normalizeAr(text).match(/\b(?:اخر|آخر)\s*(\d+)\b/);
    return m ? Number(m[1]) : null;
  }

  function isLate(tx){
    // Demo heuristic: "متأخرة" not in schema; use status "معادة" as late-ish + created older than 2026-01-26
    // and also treat any tx with status 'قيد الإجراء' older than a threshold as late-ish.
    const created = new Date((tx.created||'2026-01-01')+'T00:00:00');
    const threshold = new Date('2026-01-26T00:00:00');
    if(tx.status==='معادة') return true;
    if(tx.status==='قيد الإجراء' && created < threshold) return true;
    return false;
  }

  function findTxById(data, id){
    return (data.tx||[]).find(t=>t.id===id) || null;
  }

  function searchTx(data, q){
    q = (q||'').trim();
    if(!q) return [];
    const low = normalizeAr(q);
    return (data.tx||[]).filter(t=>{
      const hay = normalizeAr([t.id,t.type,t.subject,t.from,t.to,t.status,t.created].filter(Boolean).join(' | '));
      return hay.includes(low);
    });
  }

  function openTx(id){
    localStorage.setItem('gov-selected-tx', id);
    location.href = './transaction-details.html';
  }

  function botCardTx(t){
    return `
      <div class="ai-card">
        <div class="ai-card-head">
          <div>
            <div class="ai-card-title">${escapeHtml(t.id)} <span class="ai-tag">${escapeHtml(t.type)}</span></div>
            <div class="ai-card-sub">${escapeHtml(t.subject)}</div>
          </div>
          <div class="ai-card-actions">
            <button class="btn" type="button" data-open-tx="${escapeHtml(t.id)}">فتح</button>
          </div>
        </div>
        <div class="ai-card-grid">
          <div><span class="muted">المرسل:</span> ${escapeHtml(t.from)}</div>
          <div><span class="muted">المستقبل:</span> ${escapeHtml(t.to)}</div>
          <div><span class="muted">التاريخ:</span> ${escapeHtml(t.created)}</div>
          <div><span class="muted">الحالة:</span> ${GOV.statusPill(t.status)}</div>
        </div>
      </div>
    `;
  }

  function linkifyTxIds(text){
    const re = /\b\d{4}\/\d{3,6}\b/g;
    const parts = String(text||'').split(re);
    const ids = String(text||'').match(re) || [];
    let out = '';
    parts.forEach((p, i)=>{
      out += escapeHtml(p);
      if(i < ids.length){
        const id = ids[i];
        out += ` <a class="ai-link" data-open-tx="${id}">${id}</a> `;
      }
    });
    return out;
  }

  function computeForecast(data){
    // Demo forecast: linear projection using last 6 tx counts as baseline
    const total = (data.tx||[]).length;
    const incoming = (data.tx||[]).filter(t=>t.type==='وارد').length;
    const inprog = (data.tx||[]).filter(t=>t.status==='قيد الإجراء').length;
    const late = (data.tx||[]).filter(isLate).length;
    const score = Math.max(0, Math.min(95, Math.round(55 + (late*8) + (inprog*2))));
    const nextMonth = Math.round(total * (1.08 + (late>1?0.04:0)));
    return { confidence: 76, riskScore: score, nextMonth, incoming, inprog, late };
  }

  function playSound(){
    try{
      const pref = JSON.parse(localStorage.getItem(BOT_PREF_KEY)||'{}');
      if(pref.mute) return;
      const a = new Audio('../assets/vendor/audio/ai-pop.wav');
      a.volume = 0.55;
      a.play().catch(()=>{});
    }catch(e){}
  }

  async function exportPdf(panel){
    if(!window.html2canvas || !window.jspdf){
      GOV.showToast('PDF','المكتبات غير متاحة.');
      return;
    }

    const body = panel.querySelector('#aiBody');
    const prev = panel.classList.contains('show');
    if(!prev) panel.classList.add('show');

    // Render just the conversation area
    const canvas = await html2canvas(body, { backgroundColor: null, scale: 2 });
    const imgData = canvas.toDataURL('image/png');

    const { jsPDF } = window.jspdf;
    const pdf = new jsPDF('p','mm','a4');
    const pageW = 210;
    const pageH = 297;
    const margin = 10;
    const imgW = pageW - margin*2;
    const imgH = canvas.height * imgW / canvas.width;

    let y = margin;
    let remaining = imgH;

    // First page
    pdf.setFontSize(12);
    pdf.text('سجل محادثة المساعد الذكي (Demo)', pageW - margin, 8, { align:'right' });
    pdf.addImage(imgData,'PNG', margin, 14, imgW, imgH);

    // Multi-page if needed
    let offset = imgH - (pageH - 20);
    while(offset > 0){
      pdf.addPage();
      pdf.addImage(imgData,'PNG', margin, 14 - offset, imgW, imgH);
      offset -= (pageH - 20);
    }

    const name = `chat-transcript-${new Date().toISOString().slice(0,10)}.pdf`;
    pdf.save(name);

    if(!prev) panel.classList.remove('show');
    GOV.showToast('PDF','تم تنزيل ملف PDF.');
  }

  function mount(){
    if(document.getElementById('aiFab')) return;

    // FAB (better icon)
    const fab = document.createElement('button');
    fab.id = 'aiFab';
    fab.className = 'ai-fab';
    fab.type = 'button';
    fab.setAttribute('aria-label','المساعد الذكي');
    fab.innerHTML = `
      <span class="ai-fab-ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 3c4.97 0 9 3.58 9 8 0 2.21-1.02 4.2-2.67 5.66L19 21l-4.35-1.31A11.2 11.2 0 0 1 12 20c-4.97 0-9-3.58-9-8s4.03-9 9-9z"/>
          <path d="M8.8 12.2h.01M12 12.2h.01M15.2 12.2h.01"/>
        </svg>
      </span>
      <span class="ai-fab-glow" aria-hidden="true"></span>
    `;

    // Panel
    const panel = document.createElement('section');
    panel.id = 'aiPanel';
    panel.className = 'ai-panel';
    panel.innerHTML = `
      <div class="ai-head">
        <div class="ai-brand">
          <div class="ai-badge">AI</div>
          <div>
            <b>المساعد الذكي</b>
            <div class="muted">أوامر طبيعية + بحث + تقارير سريعة</div>
          </div>
        </div>
        <div class="ai-head-actions">
          <button class="btn icon" id="aiMute" type="button" aria-label="الصوت" title="الصوت"><span aria-hidden="true">🔊</span></button>
          <button class="btn icon" id="aiPdf" type="button" aria-label="PDF" title="PDF"><span aria-hidden="true">⎙</span></button>
          <button class="btn icon danger" id="aiClear" type="button" aria-label="مسح" title="مسح"><span aria-hidden="true">🗑</span></button>
          <button class="btn icon" id="aiClose" type="button" aria-label="إغلاق" title="إغلاق"><span aria-hidden="true">×</span></button>
        </div>
      </div>

      <div class="ai-body" id="aiBody" aria-live="polite"></div>
      <div class="ai-suggest" id="aiSuggest"></div>

      <div class="ai-foot">
        <div class="ai-input-wrap">
          <input id="aiInput" placeholder="اسألني: رقم معاملة / متأخرة / قيد الإجراء / لخّص آخر 5…" />
          <button class="btn primary" id="aiSend" type="button">إرسال</button>
        </div>
        ${DISCLAIMER}
      </div>
    `;

    document.body.appendChild(fab);
    document.body.appendChild(panel);

    const body = panel.querySelector('#aiBody');
    const suggest = panel.querySelector('#aiSuggest');

    function push(role, html){
      const row = document.createElement('div');
      row.className = 'ai-msg ' + (role==='user' ? 'me' : 'bot');
      row.innerHTML = `
        <div class="ai-bubble" data-role="${role}">
          ${html}
          <div class="ai-time">${new Date().toLocaleTimeString('ar-SA',{hour:'2-digit',minute:'2-digit'})}</div>
        </div>
      `;
      body.appendChild(row);
      body.scrollTop = body.scrollHeight;
      persist();
      if(role==='bot') playSound();
    }

    function typingOn(){
      const t = document.createElement('div');
      t.className = 'ai-msg bot ai-anim-in';
      t.id = 'aiTyping';
      t.innerHTML = `<div class="ai-bubble"><span class="ai-typing"><i></i><i></i><i></i></span></div>`;
      body.appendChild(t);
      body.scrollTop = body.scrollHeight;
    }

    function typingOff(){
      const t = document.getElementById('aiTyping');
      if(t) t.remove();
    }

    function persist(){
      try{ localStorage.setItem(BOT_KEY, JSON.stringify({ html: body.innerHTML })); }catch(e){}
    }

    function restore(){
      try{
        const raw = localStorage.getItem(BOT_KEY);
        if(!raw) return false;
        const s = JSON.parse(raw);
        if(s && s.html){ body.innerHTML = s.html; return true; }
      }catch(e){}
      return false;
    }

    function renderSug(){
      if(!suggest) return;
      suggest.innerHTML = SUGGESTIONS.map(t=>`<button class="ai-chip" type="button" data-sug="${escapeHtml(t)}">${escapeHtml(t)}</button>`).join('');
    }

    function setMuteBtn(){
      let pref = {};
      try{ pref = JSON.parse(localStorage.getItem(BOT_PREF_KEY)||'{}'); }catch(e){}
      const b = panel.querySelector('#aiMute');
      if(b) b.innerHTML = pref.mute ? '<span aria-hidden="true">🔇</span>' : '<span aria-hidden="true">🔊</span>';
    }

    function boot(){
      const had = restore();
      if(!had){
        push('bot', `
          <div style="font-weight:1300">جاهز.</div>
          <div class="muted" style="margin-top:6px">اكتب رقم معاملة مثل <span class="mono">1445/10304</span>، أو جرّب: <span class="mono">اعرض المعاملات المتأخرة</span> أو <span class="mono">كم معاملة قيد الإجراء</span>.</div>
        `);
      }
      renderSug();
      setMuteBtn();
    }

    function intentReply(q){
      const data = GOV.loadData();
      const nq = normalizeAr(q);
      const txId = parseTxId(q);

      // 1) direct tx details
      if(txId){
        const t = findTxById(data, txId);
        if(t){
          return `
            <div>تم العثور على المعاملة <a class="ai-link" data-open-tx="${txId}">${txId}</a>.</div>
            ${botCardTx(t)}
          `;
        }
        return `لم أجد معاملة برقم <b>${escapeHtml(txId)}</b>.`;
      }

      // 2) count in progress
      if(nq.includes('كم') && (nq.includes('قيد الاجراء') || nq.includes('قيد الاجراء؟') || nq.includes('قيد'))){
        const c = (data.tx||[]).filter(t=>t.status==='قيد الإجراء').length;
        return `<div>عدد المعاملات <b>قيد الإجراء</b>: <span class="pill">${c}</span></div>`;
      }

      // 3) show late
      if(nq.includes('متاخر') || nq.includes('متاخره') || nq.includes('متأخر') || nq.includes('متأخرة')){
        const list = (data.tx||[]).filter(isLate).slice().reverse();
        if(!list.length) return `<div>لا توجد معاملات متأخرة وفق معيار Demo الحالي.</div>`;
        return `
          <div>المعاملات المتأخرة (Demo): <b>${list.length}</b></div>
          ${list.slice(0,5).map(botCardTx).join('')}
        `;
      }

      // 4) summary last N
      if(nq.includes('لخص') || nq.includes('لخّص') || nq.includes('ملخص')){
        const n = parseLastN(q) || 5;
        const list = (data.tx||[]).slice().reverse().slice(0,n);
        const byStatus = {};
        list.forEach(t=> byStatus[t.status] = (byStatus[t.status]||0)+1);
        const lines = Object.entries(byStatus).map(([k,v])=>`<div class="ai-kv"><span>${escapeHtml(k)}</span><b>${v}</b></div>`).join('');
        return `
          <div>ملخص آخر <b>${n}</b> معاملات:</div>
          <div class="ai-mini">${lines}</div>
          ${list.map(botCardTx).join('')}
        `;
      }

      // 5) show last N incoming
      if(nq.includes('اخر') && nq.includes('وارد')){
        const n = parseLastN(q) || 3;
        const list = (data.tx||[]).filter(t=>t.type==='وارد').slice().reverse().slice(0,n);
        return `
          <div>آخر <b>${n}</b> معاملات وارد:</div>
          ${list.map(botCardTx).join('') || '<div class="muted">لا توجد.</div>'}
        `;
      }

      // fallback: search
      const results = searchTx(data, q);
      if(!results.length){
        return `لا توجد نتائج مطابقة لعبارة: <b>${escapeHtml(q)}</b>.`;
      }
      const top = results.slice(0,5);
      return `
        <div>وجدت <b>${results.length}</b> نتيجة. أفضل ${top.length}:</div>
        ${top.map(botCardTx).join('')}
      `;
    }

    function send(){
      const input = panel.querySelector('#aiInput');
      const q = (input.value||'').trim();
      if(!q) return;

      push('user', linkifyTxIds(q));
      input.value='';

      typingOn();
      setTimeout(()=>{
        typingOff();
        const html = intentReply(q);
        push('bot', html);
      }, 420);
    }

    function openPanel(open){
      panel.classList.toggle('show', !!open);
      if(open) panel.querySelector('#aiInput')?.focus();
    }

    // wires
    fab.addEventListener('click', ()=> openPanel(!panel.classList.contains('show')));
    panel.querySelector('#aiClose').addEventListener('click', ()=> openPanel(false));
    panel.querySelector('#aiSend').addEventListener('click', send);
    panel.querySelector('#aiInput').addEventListener('keydown', (e)=>{
      if(e.key==='Enter') send();
    });

    suggest.addEventListener('click', (e)=>{
      const b = e.target.closest('[data-sug]');
      if(!b) return;
      panel.querySelector('#aiInput').value = b.dataset.sug;
      openPanel(true);
      send();
    });

    panel.addEventListener('click', (e)=>{
      const a = e.target.closest('[data-open-tx]');
      if(a) openTx(a.getAttribute('data-open-tx'));
    });

    panel.querySelector('#aiMute').addEventListener('click', ()=>{
      let pref = {};
      try{ pref = JSON.parse(localStorage.getItem(BOT_PREF_KEY)||'{}'); }catch(e){}
      pref.mute = !pref.mute;
      localStorage.setItem(BOT_PREF_KEY, JSON.stringify(pref));
      setMuteBtn();
      GOV.showToast('المساعد','تم تحديث إعداد الصوت.');
    });

    panel.querySelector('#aiPdf').addEventListener('click', ()=> exportPdf(panel));

    if(btnClear) btnClear.addEventListener('click', ()=>{
      if(!confirm('حذف سجل المحادثة؟')) return;
      body.innerHTML = '';
      localStorage.removeItem(BOT_KEY);
      renderSug();
      push('bot', `
          <div style="font-weight:1300">جاهز.</div>
          <div class="muted" style="margin-top:6px">اكتب رقم معاملة مثل <span class="mono">1445/10304</span>، أو استخدم الأوامر السريعة بالأسفل.</div>
        `);
      GOV.showToast('تم','تم حذف سجل المحادثة.');
    });

    boot();
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    const page = document.body.getAttribute('data-page');
    if(page==='dashboard') mount();
  });
})();
