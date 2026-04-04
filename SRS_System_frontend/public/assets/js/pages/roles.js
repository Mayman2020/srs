(function(){
  const roles = [
    { role:'مدير نظام', desc:'إدارة كاملة للنظام (Demo).', perms:{ view:1, add:1, edit:1, approve:1, forward:1 } },
    { role:'مدير إدارة', desc:'إدارة ومراجعة واعتماد ضمن الإدارة.', perms:{ view:1, add:1, edit:1, approve:1, forward:1 } },
    { role:'موظف', desc:'إنشاء/عرض/متابعة وتحويل حسب التفويض.', perms:{ view:1, add:1, edit:1, approve:0, forward:1 } },
    { role:'مراجعة', desc:'مراجعة فقط قبل الاعتماد النهائي.', perms:{ view:1, add:0, edit:0, approve:1, forward:0 } },
    { role:'عرض فقط', desc:'عرض المعاملات والتقارير بدون تعديل.', perms:{ view:1, add:0, edit:0, approve:0, forward:0 } },
  ];

  function pill(v){
    return v ? '<span class="pill ok">مسموح</span>' : '<span class="pill">غير مسموح</span>';
  }

  document.addEventListener('DOMContentLoaded', ()=>{
    GOV.requireSession();
    const tbody = document.getElementById('rolesBody');

    tbody.innerHTML = roles.map(r=>{
      return `
        <tr>
          <td><b style="color:var(--g-700)">${r.role}</b><div class="muted" style="margin-top:3px">${r.desc}</div></td>
          <td>${pill(r.perms.view)}</td>
          <td>${pill(r.perms.add)}</td>
          <td>${pill(r.perms.edit)}</td>
          <td>${pill(r.perms.approve)}</td>
          <td>${pill(r.perms.forward)}</td>
        </tr>
      `;
    }).join('');

    document.getElementById('btnAddRole').addEventListener('click', ()=>{
      GOV.showToast('معلومة', 'إضافة دور جديد ستكون ضمن المرحلة التالية (Demo).');
    });
  });
})();
