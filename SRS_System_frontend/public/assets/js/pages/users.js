(function(){
  document.addEventListener('DOMContentLoaded', ()=>{
    GOV.requireSession();
    const d = GOV.loadData();

    const tbody = document.getElementById('usersBody');
    const count = document.getElementById('usersCount');

    function render(list){
      tbody.innerHTML = list.map((u,i)=>{
        const st = u.status==='نشط' ? '<span class="pill ok">نشط</span>' : '<span class="pill bad">موقوف</span>';
        return `
          <tr>
            <td>${u.name}</td>
            <td>${u.nid}</td>
            <td>${u.dept}</td>
            <td>${st}</td>
            <td>
              <div class="row-actions">
                <button class="btn" data-act="toggle" data-i="${i}" type="button">تغيير الحالة</button>
                <button class="btn" data-act="edit" data-i="${i}" type="button">تعديل</button>
              </div>
            </td>
          </tr>
        `;
      }).join('');
      count.textContent = String(list.length);
    }

    render(d.users);

    document.getElementById('btnAddUser').addEventListener('click', ()=>{
      const name = document.getElementById('uName').value.trim();
      const nid = document.getElementById('uNid').value.trim();
      const dept = document.getElementById('uDept').value.trim();
      const status = document.getElementById('uStatus').value;

      if(!name || !nid || !dept){
        GOV.showToast('تنبيه', 'يرجى إدخال الاسم ورقم الهوية والإدارة.');
        return;
      }

      d.users.unshift({ name, nid, dept, status });
      GOV.saveData(d);
      render(d.users);
      GOV.showToast('تم', 'تمت إضافة المستخدم (Demo).');

      document.getElementById('uName').value='';
      document.getElementById('uNid').value='';
      document.getElementById('uDept').value='';
      document.getElementById('uStatus').value='نشط';
    });

    tbody.addEventListener('click', (e)=>{
      const b = e.target.closest('button');
      if(!b) return;
      const i = Number(b.dataset.i);
      const act = b.dataset.act;
      if(act==='toggle'){
        d.users[i].status = d.users[i].status==='نشط' ? 'موقوف' : 'نشط';
        GOV.saveData(d);
        render(d.users);
        GOV.showToast('تم', 'تم تحديث حالة المستخدم (Demo).');
      }
      if(act==='edit'){
        GOV.showToast('معلومة', 'تعديل المستخدم في النسخة التجريبية سيكون ضمن تحسينات لاحقة.');
      }
    });
  });
})();
