(function(){
  function uuid(){
    const m = String(Math.floor(Math.random()*9000)+1000);
    return `1445/${m}`;
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

  function wireTags(){
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
  }

  function defaultLetterHtml(){
    const today = new Date();
    const d = today.toISOString().slice(0,10);
    return `
<div style="font-family:'Cairo','IBM Plex Sans Arabic',system-ui; direction:rtl; color:#0f172a;">
  <div style="display:flex; justify-content:space-between; gap:16px; align-items:flex-start;">
    <div>
      <div style="font-size:15px; font-weight:800; color:#064635;">المملكة العربية السعودية</div>
      <div style="font-size:13px; font-weight:700; color:#0B6E4F;">نظام الاتصالات الإدارية</div>
      <div style="font-size:12px; color:#6B7280;">خطاب رسمي (نموذج مرفق)</div>
    </div>
    <div style="text-align:left; min-width:220px;">
      <div style="font-size:12px; color:#6B7280;">التاريخ: <b style="color:#0f172a;">${d}</b></div>
      <div style="font-size:12px; color:#6B7280;">المرجع: <b style="color:#0f172a;">—</b></div>
    </div>
  </div>

  <hr style="border:none; border-top:1px solid #e5e7eb; margin:14px 0;"/>

  <div style="font-size:14px; line-height:1.95;">
    <div><b>سعادة/</b> <span style="color:#064635; font-weight:800;">المحترم</span> — حفظه الله</div>
    <div style="margin-top:8px;"><b>السلام عليكم ورحمة الله وبركاته،</b></div>
    <p style="margin:10px 0; color:#111827;">إشارةً إلى موضوع المعاملة، نأمل من سعادتكم التكرم بالاطلاع واتخاذ ما يلزم حسب الاختصاص، مع تزويدنا بما يفيد خلال المدة النظامية.</p>

    <div style="margin:14px 0 8px; font-weight:800; color:#064635;">بنود الخطاب</div>
    <table style="width:100%; border-collapse:collapse; font-size:13px;">
      <thead>
        <tr>
          <th style="text-align:right; padding:10px; border:1px solid #d1d5db; background:#DFF3EA; color:#064635;">البند</th>
          <th style="text-align:right; padding:10px; border:1px solid #d1d5db; background:#DFF3EA; color:#064635;">الوصف</th>
          <th style="text-align:right; padding:10px; border:1px solid #d1d5db; background:#DFF3EA; color:#064635;">المسؤول</th>
          <th style="text-align:right; padding:10px; border:1px solid #d1d5db; background:#DFF3EA; color:#064635;">المدة</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td style="padding:10px; border:1px solid #e5e7eb;">1</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">مراجعة الموضوع وإبداء المرئيات</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">الإدارة المختصة</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">يومين</td>
        </tr>
        <tr>
          <td style="padding:10px; border:1px solid #e5e7eb;">2</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">تدقيق المستندات وإرفاق الملاحظات</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">وحدة المتابعة</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">يوم</td>
        </tr>
        <tr>
          <td style="padding:10px; border:1px solid #e5e7eb;">3</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">اعتماد الإجراء المناسب</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">صاحب الصلاحية</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">يوم</td>
        </tr>
        <tr>
          <td style="padding:10px; border:1px solid #e5e7eb;">4</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">إشعار الجهة واستكمال الإقفال</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">مكتب الاتصالات</td>
          <td style="padding:10px; border:1px solid #e5e7eb;">يوم</td>
        </tr>
      </tbody>
    </table>

    <p style="margin:12px 0 0;">شاكرين لكم تعاونكم، وتقبلوا خالص التحية والتقدير.</p>
  </div>

  <div style="margin-top:18px; display:flex; justify-content:space-between; align-items:flex-end; gap:12px;">
    <div style="font-size:12px; color:#6B7280;">
      <div><b>ملاحظات:</b> هذا نموذج تجريبي داخل البروتوتايب.</div>
    </div>
    <div style="text-align:center; min-width:260px;">
      <div style="font-weight:900; color:#064635;">الموقّع</div>
      <div style="font-size:13px; margin-top:4px;">مدير الاتصالات الإدارية</div>
      <div style="height:34px;"></div>
      <div style="border-top:2px solid #0B6E4F; width:180px; margin:0 auto;"></div>
      <div style="font-size:12px; color:#6B7280; margin-top:6px;">(توقيع وختم)</div>
    </div>
  </div>
</div>
    `.trim();
  }

  function initEditor(){
    if(!window.tinymce) return;
    tinymce.init({
      selector: 'textarea#editor',
      height: 740,
      directionality: 'rtl',
      menubar: false,
      statusbar: true,
      branding: false,
      plugins: 'lists link table pagebreak code autoresize',
      toolbar: 'undo redo | blocks | bold italic underline | alignright aligncenter alignleft | bullist numlist | table | pagebreak | removeformat | code',
      content_style: `
        body{font-family:'Cairo','IBM Plex Sans Arabic',system-ui; direction:rtl; padding:24px; background:#fff;}
        table{width:100%;}
      `,
      setup: (ed)=>{
        ed.on('init', ()=>{
          ed.setContent(defaultLetterHtml());
        });
      }
    });
  }

document.addEventListener('DOMContentLoaded', ()=>{
    if(window.GOV) GOV.requireSession();

    wireTags();
    initEditor();

    const btn = document.getElementById('btnCreateSave');
    if(btn) btn.addEventListener('click', ()=>{
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

      // capture letter content
      let letterHtml = '';
      try{ letterHtml = (tinymce.get('editor')||{}).getContent ? tinymce.get('editor').getContent() : ''; }catch(e){}

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
          attachments:[
            {
              type:'إلكتروني',
              class:'خطاب',
              name:'خطاب رسمي (تم إنشاؤه)',
              secrecy,
              addedAt: new Date().toISOString(),
              by:'المستخدم التجريبي',
              contentHtml: letterHtml
            }
          ],
          timeline:[
            {at: new Date().toISOString(), action:'إنشاء', by:'المستخدم التجريبي', note:'تم إنشاء المعاملة (Demo) مع خطاب مرفق.'}
          ]
        }
      });

      GOV.saveData(d);
      localStorage.setItem('gov-selected-tx', id);
      GOV.showToast('تم','تم إنشاء المعاملة وإرفاق الخطاب. سيتم فتح صفحة التفاصيل.');
      setTimeout(()=> location.href='./transaction-details.html', 450);
    });
  });
})();
