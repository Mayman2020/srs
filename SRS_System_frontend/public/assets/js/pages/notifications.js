(function () {
  function extractTxNo(text) {
    const m = (text || '').match(/\b\d{4}\/\d{3,6}\b/);
    return m ? m[0] : '—';
  }

  document.addEventListener('DOMContentLoaded', () => {
    if (!window.GOV_I18N || typeof GOV_I18N.ready !== 'function') return;

    GOV_I18N.ready().then(function () {
      const t = GOV_I18N.t.bind(GOV_I18N);
      const ta = GOV_I18N.ta.bind(GOV_I18N);

      function notifText(n) {
        if (n.textKey) return t('legacyPages.demo.' + n.textKey + '.text');
        return n.text || '';
      }

      const s = GOV.requireSession();
      if (!s) return;

      const data = GOV.loadData();

      function buildRecommendedAction(n) {
        const k = n.type || 'DEFAULT';
        const key = 'legacyPages.notificationsPage.recommended.' + k;
        const msg = t(key);
        return msg === key ? t('legacyPages.notificationsPage.recommended.DEFAULT') : msg;
      }

      function computeSlaLevel(n) {
        if (n.important && !n.read) return { key: 'HIGH', cls: 'bad' };
        if (n.important) return { key: 'MEDIUM', cls: 'warn' };
        return { key: 'LOW', cls: '' };
      }

      function computePriority(n) {
        if (n.important && !n.read) return { key: 'HIGH', cls: 'bad' };
        if (n.important) return { key: 'MEDIUM', cls: 'warn' };
        return { key: 'LOW', cls: '' };
      }

      function computeOwner(txNo) {
        if (txNo === '—') return '—';
        const last = Number((txNo.split('/')[1] || '0').slice(-1));
        const map = ta('legacyPages.notificationsPage.ownerDepts');
        return map.length ? map[last % map.length] : '—';
      }

      function computeChannel(n) {
        if (n.type === 'ALERT' || n.type === 'REMINDER') return t('legacyPages.notificationsPage.channelSystem');
        return t('legacyPages.notificationsPage.channelManual');
      }

      function computeDueDate(n) {
        const sla = computeSlaLevel(n);
        const base = (n.time || '').split(' ')[0];
        if (!base || base === '—') return '—';
        const d = new Date(base + 'T00:00:00');
        const add = sla.key === 'HIGH' ? 1 : sla.key === 'MEDIUM' ? 2 : 5;
        d.setDate(d.getDate() + add);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        return yyyy + '-' + mm + '-' + dd;
      }

      function computeCounts() {
        const unread = data.notifications.filter((n) => !n.read).length;
        const badge = document.getElementById('badgeNoti');
        if (badge) badge.textContent = String(unread);
        GOV.saveData(data);
      }

      function render(list) {
        const body = document.getElementById('notiBody');
        if (!body) return;
        body.innerHTML = '';

        list.forEach((n, i) => {
          const text = notifText(n);
          const txNo = extractTxNo(text);
          const sla = computeSlaLevel(n);
          const pri = computePriority(n);
          const source =
            txNo !== '—' ? t('legacyPages.notificationsPage.sourceTx') : t('legacyPages.notificationsPage.sourceSys');
          const action = buildRecommendedAction(n);
          const owner = computeOwner(txNo);
          const due = computeDueDate(n);
          const channel = computeChannel(n);
          const typeLabel = t('legacyPages.notifType.' + n.type);
          const slaLabel = t('legacyPages.notificationsPage.slaShort.' + sla.key);
          const priLabel = t('legacyPages.notificationsPage.prioShort.' + pri.key);
          const readPill = n.read
            ? '<span class="pill ok">' + GOV.esc(t('legacyPages.notificationsPage.readPill')) + '</span>'
            : '<span class="pill warn">' + GOV.esc(t('legacyPages.notificationsPage.unreadPill')) + '</span>';
          const impPill = n.important
            ? '<span class="pill">' + GOV.esc(t('legacyPages.notificationsPage.impPill')) + '</span>'
            : '<span class="pill" style="opacity:.65">' + GOV.esc(t('legacyPages.notificationsPage.normalPill')) + '</span>';
          const detailsLabel = t('legacyPages.notificationsPage.detailsLink');
          const openLabel = t('legacyPages.notificationsPage.openLink');
          const toggleLabel = n.read
            ? t('legacyPages.notificationsPage.toggleUnread')
            : t('legacyPages.notificationsPage.toggleRead');
          const impToggleLabel = n.important
            ? t('legacyPages.notificationsPage.unmarkImportant')
            : t('legacyPages.notificationsPage.markImportant');
          const deleteLabel = t('legacyPages.notificationsPage.delete');

          const tr = document.createElement('tr');
          tr.innerHTML =
            '<td>' +
            GOV.esc(typeLabel) +
            '</td>' +
            '<td>' +
            GOV.esc(source) +
            '</td>' +
            '<td>' +
            GOV.esc(txNo) +
            '</td>' +
            '<td>' +
            GOV.esc(text) +
            '</td>' +
            '<td>' +
            GOV.esc(n.time) +
            '</td>' +
            '<td>' +
            readPill +
            '</td>' +
            '<td>' +
            impPill +
            '</td>' +
            '<td><span class="pill ' +
            sla.cls +
            '">' +
            GOV.esc(slaLabel) +
            '</span></td>' +
            '<td>' +
            GOV.esc(action) +
            '</td>' +
            '<td>' +
            GOV.esc(owner) +
            '</td>' +
            '<td>' +
            GOV.esc(due) +
            '</td>' +
            '<td>' +
            GOV.esc(channel) +
            '</td>' +
            '<td><span class="pill ' +
            pri.cls +
            '">' +
            GOV.esc(priLabel) +
            '</span></td>' +
            '<td>' +
            (txNo !== '—'
              ? '<a class="link" data-open="' +
                i +
                '">' +
                GOV.esc(detailsLabel) +
                '</a>'
              : '<span class="muted">—</span>') +
            '</td>' +
            '<td><div class="row-actions">' +
            '<a class="link" data-open="' +
            i +
            '">' +
            GOV.esc(openLabel) +
            '</a> ' +
            '<a class="link" data-toggle="' +
            i +
            '">' +
            GOV.esc(toggleLabel) +
            '</a> ' +
            '<a class="link" data-imp="' +
            i +
            '">' +
            GOV.esc(impToggleLabel) +
            '</a> ' +
            '<a class="link" data-del="' +
            i +
            '">' +
            GOV.esc(deleteLabel) +
            '</a></div></td>';
          body.appendChild(tr);
        });

        const rc = document.getElementById('notiCount');
        if (rc) rc.textContent = list.length;
        computeCounts();
      }

      function applyFilter() {
        const fRead = document.getElementById('fRead')?.value || 'all';
        const fImp = document.getElementById('fImp')?.value || 'all';
        const q = (document.getElementById('fQ')?.value || '').trim();

        const list = data.notifications.filter((n) => {
          if (fRead === 'unread' && n.read) return false;
          if (fRead === 'read' && !n.read) return false;
          if (fImp === 'important' && !n.important) return false;
          if (fImp === 'normal' && n.important) return false;
          if (q) {
            const hay = notifText(n) + ' ' + t('legacyPages.notifType.' + n.type);
            if (!hay.includes(q)) return false;
          }
          return true;
        });
        render(list);
        GOV.showToast(t('legacyPages.notificationsPage.toastTitleOk'), t('legacyPages.notificationsPage.toastFilter'));
      }

      render(data.notifications);

      document.getElementById('btnNotiApply')?.addEventListener('click', applyFilter);

      document.getElementById('btnNotiReset')?.addEventListener('click', () => {
        ['fRead', 'fImp'].forEach((id) => {
          const el = document.getElementById(id);
          if (el) el.value = 'all';
        });
        const q = document.getElementById('fQ');
        if (q) q.value = '';
        render(data.notifications);
        GOV.showToast(t('legacyPages.notificationsPage.toastTitleOk'), t('legacyPages.notificationsPage.toastResetFilters'));
      });

      document.getElementById('btnMarkAllRead')?.addEventListener('click', () => {
        data.notifications.forEach((n) => (n.read = true));
        render(data.notifications);
        GOV.showToast(t('legacyPages.notificationsPage.toastTitleOk'), t('legacyPages.notificationsPage.toastAllRead'));
      });

      const body = document.getElementById('notiBody');
      if (body) {
        body.addEventListener('click', (e) => {
          const open = e.target.getAttribute('data-open');
          const tg = e.target.getAttribute('data-toggle');
          const imp = e.target.getAttribute('data-imp');
          const del = e.target.getAttribute('data-del');

          if (open !== null) {
            const i = Number(open);
            const txNo = extractTxNo(notifText(data.notifications[i]));
            if (txNo !== '—') {
              localStorage.setItem('gov-selected-tx', txNo);
              location.href = './transaction-details.html';
            } else {
              GOV.showToast(t('legacyPages.notificationsPage.infoTitle'), t('legacyPages.notificationsPage.toastNoTx'));
            }
          }

          if (tg !== null) {
            const i = Number(tg);
            data.notifications[i].read = !data.notifications[i].read;
            render(data.notifications);
          }
          if (imp !== null) {
            const i = Number(imp);
            data.notifications[i].important = !data.notifications[i].important;
            render(data.notifications);
          }
          if (del !== null) {
            const i = Number(del);
            data.notifications.splice(i, 1);
            render(data.notifications);
            GOV.showToast(t('legacyPages.notificationsPage.toastTitleOk'), t('legacyPages.notificationsPage.toastDeleted'));
          }
        });
      }

      computeCounts();
    });
  });
})();
