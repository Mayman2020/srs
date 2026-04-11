(function () {
  document.addEventListener('DOMContentLoaded', () => {
    if (!window.GOV_I18N || typeof GOV_I18N.ready !== 'function') return;

    GOV_I18N.ready().then(function () {
      const t = GOV_I18N.t.bind(GOV_I18N);

      GOV.requireSession();
      const d = GOV.loadData();

      const tbody = document.getElementById('usersBody');
      const count = document.getElementById('usersCount');

      function resolveUser(u) {
        if (u.profile) {
          const p = 'legacyPages.demo.' + u.profile;
          const code = u.status || t(p + '.statusCode');
          return {
            name: t(p + '.name'),
            nid: u.nid,
            dept: t(p + '.dept'),
            statusCode: code === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE'
          };
        }
        return {
          name: u.name,
          nid: u.nid,
          dept: u.dept,
          statusCode: u.status === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE'
        };
      }

      function render(list) {
        tbody.innerHTML = list
          .map((raw, i) => {
            const u = resolveUser(raw);
            const ok = u.statusCode === 'ACTIVE';
            const st = ok
              ? '<span class="pill ok">' + GOV.esc(t('legacyPages.userStatus.ACTIVE')) + '</span>'
              : '<span class="pill bad">' + GOV.esc(t('legacyPages.userStatus.SUSPENDED')) + '</span>';
            return (
              '<tr>' +
              '<td>' +
              GOV.esc(u.name) +
              '</td>' +
              '<td>' +
              GOV.esc(u.nid) +
              '</td>' +
              '<td>' +
              GOV.esc(u.dept) +
              '</td>' +
              '<td>' +
              st +
              '</td>' +
              '<td><div class="row-actions">' +
              '<button class="btn" data-act="toggle" data-i="' +
              i +
              '" type="button">' +
              GOV.esc(t('legacyPages.usersPage.toggleStatus')) +
              '</button> ' +
              '<button class="btn" data-act="edit" data-i="' +
              i +
              '" type="button">' +
              GOV.esc(t('legacyPages.usersPage.edit')) +
              '</button>' +
              '</div></td>' +
              '</tr>'
            );
          })
          .join('');
        count.textContent = String(list.length);
      }

      render(d.users);

      document.getElementById('btnAddUser').addEventListener('click', () => {
        const name = document.getElementById('uName').value.trim();
        const nid = document.getElementById('uNid').value.trim();
        const dept = document.getElementById('uDept').value.trim();
        const statusSel = document.getElementById('uStatus').value;

        if (!name || !nid || !dept) {
          GOV.showToast(t('legacyPages.usersPage.alertTitle'), t('legacyPages.usersPage.toastValidation'));
          return;
        }

        const status = statusSel === 'SUSPENDED' ? 'SUSPENDED' : 'ACTIVE';
        d.users.unshift({ name, nid, dept, status });
        GOV.saveData(d);
        render(d.users);
        GOV.showToast(t('legacyPages.usersPage.toastTitleOk'), t('legacyPages.usersPage.toastAdded'));

        document.getElementById('uName').value = '';
        document.getElementById('uNid').value = '';
        document.getElementById('uDept').value = '';
        document.getElementById('uStatus').value = 'ACTIVE';
      });

      tbody.addEventListener('click', (e) => {
        const b = e.target.closest('button');
        if (!b) return;
        const i = Number(b.dataset.i);
        const act = b.dataset.act;
        if (act === 'toggle') {
          const raw = d.users[i];
          const cur = resolveUser(raw).statusCode;
          raw.status = cur === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
          GOV.saveData(d);
          render(d.users);
          GOV.showToast(t('legacyPages.usersPage.toastTitleOk'), t('legacyPages.usersPage.toastUpdated'));
        }
        if (act === 'edit') {
          GOV.showToast(t('legacyPages.usersPage.infoTitle'), t('legacyPages.usersPage.toastEditLater'));
        }
      });
    });
  });
})();
