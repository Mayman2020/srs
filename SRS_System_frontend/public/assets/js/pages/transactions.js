(function () {
  document.addEventListener('DOMContentLoaded', () => {
    if (!window.GOV_I18N || typeof GOV_I18N.ready !== 'function') return;

    GOV_I18N.ready().then(function () {
      const t = GOV_I18N.t.bind(GOV_I18N);

      GOV.requireSession();

      const tbody = document.getElementById('txBody');
      const rc = document.getElementById('resultCount');
      const pagerEl = document.getElementById('pagerTx');

      let page = 1;
      let perPage = 10;
      let filtered = null;

      function txFields(x) {
        const typeLabel = t('legacyPages.txType.' + x.type);
        if (x.row) {
          const p = 'legacyPages.demo.' + x.row;
          return {
            type: typeLabel,
            subject: t(p + '.subject'),
            from: t(p + '.from'),
            to: t(p + '.to')
          };
        }
        return {
          type: x.type || '',
          subject: x.subject || '',
          from: x.from || '',
          to: x.to || ''
        };
      }

      function rowHtml(x) {
        const f = txFields(x);
        const openLabel = t('legacyPages.transactionsList.open');
        return (
          '<tr>' +
          '<td><span class="link" data-open="' +
          GOV.esc(x.id) +
          '">' +
          GOV.esc(x.id) +
          '</span></td>' +
          '<td>' +
          GOV.esc(f.type) +
          '</td>' +
          '<td>' +
          GOV.esc(f.subject) +
          '</td>' +
          '<td>' +
          GOV.esc(f.from) +
          '</td>' +
          '<td>' +
          GOV.esc(f.to) +
          '</td>' +
          '<td>' +
          GOV.esc(x.created) +
          '</td>' +
          '<td>' +
          GOV.statusPill(x.status) +
          '</td>' +
          '<td><div class="row-actions"><button class="btn" data-open="' +
          GOV.esc(x.id) +
          '" type="button">' +
          GOV.esc(openLabel) +
          '</button></div></td>' +
          '</tr>'
        );
      }

      function applyRender() {
        const d = GOV.loadData();
        const list = filtered || d.tx;

        const p = window.GOV_PAGINATION
          ? GOV_PAGINATION.paginate(list, page, perPage)
          : { slice: list, total: list.length, pages: 1, page: 1, perPage };

        rc.textContent = p.total;
        const emptyMsg = t('legacyPages.transactionsList.empty');
        tbody.innerHTML =
          p.slice.map(rowHtml).join('') || '<tr><td colspan="8" class="muted">' + GOV.esc(emptyMsg) + '</td></tr>';

        if (window.GOV_PAGINATION) GOV_PAGINATION.renderPager(pagerEl, p);
      }

      if (pagerEl) {
        pagerEl.addEventListener('click', (e) => {
          const b = e.target.closest('.pager-btn');
          if (!b) return;
          page = Number(b.dataset.page || '1');
          applyRender();
        });
      }

      document.getElementById('btnApply').addEventListener('click', () => {
        const d = GOV.loadData();
        const fNo = (document.getElementById('fNo').value || '').trim();
        const fSubject = (document.getElementById('fSubject').value || '').trim();
        const fFrom = (document.getElementById('fFrom').value || '').trim();
        const fType = document.getElementById('fType').value;
        const fStatus = document.getElementById('fStatus').value;

        filtered = d.tx.filter((x) => {
          const f = txFields(x);
          if (fNo && !x.id.includes(fNo)) return false;
          if (fSubject && !f.subject.includes(fSubject)) return false;
          if (fFrom && !f.from.includes(fFrom)) return false;
          if (fType && x.type !== fType) return false;
          if (fStatus && x.status !== fStatus) return false;
          return true;
        });
        page = 1;
        applyRender();
        GOV.showToast(t('legacyPages.transactionsList.toastTitleOk'), t('legacyPages.transactionsList.toastFilter'));
      });

      document.getElementById('btnReset').addEventListener('click', () => {
        ['fNo', 'fSubject', 'fFrom'].forEach((id) => (document.getElementById(id).value = ''));
        document.getElementById('fType').value = '';
        document.getElementById('fStatus').value = '';
        filtered = null;
        page = 1;
        applyRender();
        GOV.showToast(t('legacyPages.transactionsList.toastTitleOk'), t('legacyPages.transactionsList.toastReset'));
      });

      tbody.addEventListener('click', (e) => {
        const open = e.target.closest('[data-open]');
        if (!open) return;
        const id = open.dataset.open;
        localStorage.setItem('gov-selected-tx', id);
        location.href = './transaction-details.html';
      });

      applyRender();
    });
  });
})();
