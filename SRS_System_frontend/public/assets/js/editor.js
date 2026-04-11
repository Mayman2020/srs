// TinyMCE initialization (local vendor) for A4-like editing
// Requires: assets/vendor/tinymce/tinymce/js/tinymce/tinymce.min.js

(function(){
  function initA4TinyMCE(){
    if(!window.tinymce) return;

    tinymce.remove();

    tinymce.init({
      selector: 'textarea#editor',
      license_key: 'gpl',
      height: 720,
      menubar: false,
      branding: false,
      directionality: 'rtl',
      plugins: 'lists link table code pagebreak searchreplace visualblocks autoresize',
      toolbar: [
        'undo redo | blocks | bold italic underline | alignright aligncenter alignleft | bullist numlist | table | link | pagebreak | removeformat | code'
      ].join(' '),
      block_formats: 'فقرة=p; عنوان 2=h2; عنوان 3=h3; عنوان 4=h4',
      content_style: `
        body{font-family: "IBM Plex Sans Arabic","Cairo",Arial,sans-serif; direction: rtl; text-align: right; color:#0f172a;}
        .page{width: 820px; max-width: 100%; margin: 0 auto; background:#fff; border:1px solid rgba(15,23,42,.10); border-radius:14px; box-shadow: 0 18px 40px rgba(15,23,42,.10); padding: 26px 28px;}
        h2,h3,h4{color:#064635;}
        p{line-height:1.85; font-size: 14px;}
      `,
      setup: function(editor){
        editor.on('init', function(){
          const raw = localStorage.getItem('gov-selected-tx');
          const data = window.GOV ? GOV.loadData() : null;
          const tx = (data && raw) ? data.tx.find(x=>x.id===raw) : null;
          const savedHtml = tx && tx.meta && tx.meta.contentHtml ? tx.meta.contentHtml : '';

          if(savedHtml){
            editor.setContent(savedHtml);
          } else {
            editor.setContent(`
<div class="page" style="font-family:IBM Plex Sans Arabic,Cairo,Arial,sans-serif;">
  <div style="display:flex;justify-content:space-between;gap:16px;align-items:flex-start;">
    <div>
      <h2 style="margin:0;color:#064635">المملكة العربية السعودية</h2>
      <div style="color:#0B6E4F;font-weight:800">نظام تخطيط موارد المؤسسات الحكومي — المراسلات الإدارية</div>
      <div style="color:#6B7280;font-size:12px">خطاب رسمي (قالب افتراضي)</div>
    </div>
    <div style="text-align:left;min-width:210px;font-size:12px;color:#6B7280">
      التاريخ: <b style="color:#0f172a">${new Date().toISOString().slice(0,10)}</b><br/>
      المرجع: <b style="color:#0f172a">—</b>
    </div>
  </div>

  <hr style="border:none;border-top:1px solid #e5e7eb;margin:14px 0"/>

  <p style="line-height:1.9;font-size:14px">
    <b>سعادة/</b> المحترم — حفظه الله<br/>
    <b>السلام عليكم ورحمة الله وبركاته،</b><br/>
    نأمل من سعادتكم التكرم بالاطلاع على المعاملة واتخاذ ما يلزم حسب الاختصاص.
  </p>

  <div style="margin:12px 0 6px;font-weight:900;color:#064635">جدول البنود</div>
  <table style="width:100%;border-collapse:collapse;font-size:13px">
    <thead>
      <tr>
        <th style="text-align:right;padding:10px;border:1px solid #d1d5db;background:#DFF3EA;color:#064635">البند</th>
        <th style="text-align:right;padding:10px;border:1px solid #d1d5db;background:#DFF3EA;color:#064635">الوصف</th>
        <th style="text-align:right;padding:10px;border:1px solid #d1d5db;background:#DFF3EA;color:#064635">المسؤول</th>
        <th style="text-align:right;padding:10px;border:1px solid #d1d5db;background:#DFF3EA;color:#064635">المدة</th>
      </tr>
    </thead>
    <tbody>
      <tr><td style="padding:10px;border:1px solid #e5e7eb">1</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td></tr>
      <tr><td style="padding:10px;border:1px solid #e5e7eb">2</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td></tr>
      <tr><td style="padding:10px;border:1px solid #e5e7eb">3</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td></tr>
      <tr><td style="padding:10px;border:1px solid #e5e7eb">4</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td><td style="padding:10px;border:1px solid #e5e7eb">—</td></tr>
    </tbody>
  </table>

  <p style="margin-top:12px">وتقبلوا خالص التحية والتقدير.</p>

  <div style="margin-top:18px;display:flex;justify-content:space-between;align-items:flex-end;gap:12px">
    <div style="font-size:12px;color:#6B7280"><b>مرفقات:</b> —</div>
    <div style="text-align:center;min-width:240px">
      <div style="font-weight:900;color:#064635">الموقّع</div>
      <div style="font-size:13px">المسؤول المختص</div>
      <div style="height:34px"></div>
      <div style="border-top:2px solid #0B6E4F;width:180px;margin:0 auto"></div>
      <div style="font-size:12px;color:#6B7280;margin-top:6px">(توقيع وختم)</div>
    </div>
  </div>
</div>
          `.trim());
          }
        });

        editor.on('Change KeyUp', function(){
          const data = GOV.loadData();
          const id = localStorage.getItem('gov-selected-tx');
          const tx = data.tx.find(x=>x.id===id);
          if(!tx) return;
          tx.meta = tx.meta || {};
          tx.meta.contentHtml = editor.getContent();
          GOV.saveData(data);
        });
      }
    });
  }

  window.GOV_EDITOR = {
    init: function(){ initA4TinyMCE(); },
    initA4TinyMCE
  };

  document.addEventListener('DOMContentLoaded', ()=>{
    const page = document.body.getAttribute('data-page');
    if(page === 'transaction-details') initA4TinyMCE();
  });
})();
