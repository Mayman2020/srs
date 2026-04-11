(function () {
  document.addEventListener('DOMContentLoaded', () => {
    if (!window.GOV_I18N || typeof GOV_I18N.ready !== 'function') return;

    GOV_I18N.ready().then(function () {
      const t = GOV_I18N.t.bind(GOV_I18N);
      const tj = GOV_I18N.tj.bind(GOV_I18N);

      GOV.requireSession();
      const d = GOV.loadData();

      const id = localStorage.getItem('gov-selected-tx');
      const tx = d.tx.find((x) => x.id === id) || d.tx[0];

      const set = (k, v) => {
        const el = document.getElementById(k);
        if (el) el.textContent = v;
      };

      const typeLabel = t('legacyPages.txType.' + tx.type);
      let subject = '';
      let from = '';
      let to = '';
      if (tx.row) {
        const p = 'legacyPages.demo.' + tx.row;
        subject = t(p + '.subject');
        from = t(p + '.from');
        to = t(p + '.to');
      } else {
        subject = tx.subject || '';
        from = tx.from || '';
        to = tx.to || '';
      }

      set('txId', tx.id);
      set('txType', typeLabel);
      set('txSubject', subject);
      set('txFrom', from);
      set('txTo', to);
      set('txCreated', tx.created);

      const st = document.getElementById('txStatus');
      if (st) st.innerHTML = GOV.statusPill(tx.status);

      const defSec = t('legacyPages.transactionDetails.secrecyDefault');
      const meta =
        tx.meta ||
        (tx.meta = {
          desc: '—',
          secrecy: defSec,
          maxDays: 5,
          to: [to],
          cc: [],
          attachments: [],
          timeline: []
        });
      set('txDesc', meta.desc || '—');
      set('txSecrecy', meta.secrecy || defSec);
      set('txMaxDays', String(meta.maxDays || 5));

      const attBody = document.getElementById('attBody');
      const attPager = document.getElementById('attPager');
      let attPage = 1;
      const attPerPage = 5;

      const demoRows = tj('legacyPages.transactionDetails.attachmentDemoRows') || [];
      const now = '2026-02-02';

      function secrecyPillClass(seg) {
        const x = seg || '';
        if (x.includes('سري جدا') || /top\s*secret|very\s*secret/i.test(x)) return 'bad';
        if (x.includes('سري') || /secret/i.test(x) && !/normal|عادي/i.test(x)) return 'warn';
        if (x.includes('محدود') || /limited/i.test(x)) return 'warn';
        return '';
      }

      if (attBody) {
        meta.attachments = meta.attachments || [];

        if (meta.attachments.length < 6 && demoRows.length) {
          meta.attachments = demoRows.map(function (a) {
            return {
              kind: a.kind,
              type: a.type,
              name: a.name,
              secrecy: a.secrecy,
              by: a.by,
              at: now
            };
          });
        }

        function renderAttachments() {
          if (!attBody) return;
          const ap = window.GOV_PAGINATION
            ? GOV_PAGINATION.paginate(meta.attachments, attPage, attPerPage)
            : {
                slice: meta.attachments,
                total: meta.attachments.length,
                pages: 1,
                page: 1,
                perPage: attPerPage
              };
          const baseIdx = (ap.page - 1) * ap.perPage;
          const vd = t('legacyPages.transactionDetails.viewDownload');

          attBody.innerHTML = ap.slice
            .map(function (a, idx) {
              const sec = secrecyPillClass(a.secrecy);
              return (
                '<tr>' +
                '<td>' +
                GOV.esc(a.kind) +
                '</td>' +
                '<td>' +
                GOV.esc(a.type) +
                '</td>' +
                '<td>' +
                GOV.esc(a.name) +
                '</td>' +
                '<td><span class="pill ' +
                sec +
                '">' +
                GOV.esc(a.secrecy) +
                '</span></td>' +
                '<td>' +
                GOV.esc(a.at) +
                '</td>' +
                '<td>' +
                GOV.esc(a.by) +
                '</td>' +
                '<td><div class="row-actions"><button class="btn" type="button" data-dl="' +
                (baseIdx + idx) +
                '">' +
                GOV.esc(vd) +
                '</button></div></td>' +
                '</tr>'
              );
            })
            .join('');

          if (window.GOV_PAGINATION && attPager) GOV_PAGINATION.renderPager(attPager, ap);
        }

        if (attPager) {
          attPager.addEventListener('click', (e) => {
            const b = e.target.closest('.pager-btn');
            if (!b) return;
            attPage = Number(b.dataset.page || '1');
            renderAttachments();
          });
        }

        renderAttachments();
      }

      const tl = document.getElementById('tlBody');
      const timelineDefaults = tj('legacyPages.transactionDetails.timelineDefaults') || [];

      function renderTimeline() {
        if (!tl) return;
        let tlItems;
        if (meta.timeline && meta.timeline.length) {
          tlItems = meta.timeline;
        } else {
          const offsets = [0, 8, 18, 28, 40];
          tlItems = timelineDefaults.map(function (row, i) {
            return {
              at: new Date(Date.now() - 1000 * 60 * offsets[i]).toISOString(),
              action: row.action,
              by: row.by,
              note: row.note
            };
          });
        }
        const totalL = t('legacyPages.transactionDetails.tlTotalLabel');
        const lastL = t('legacyPages.transactionDetails.tlLastLabel');
        const tlSummary =
          '<div class="tl-summary">' +
          '<div><b>' +
          GOV.esc(totalL) +
          '</b> ' +
          tlItems.length +
          '</div>' +
          '<div><b>' +
          GOV.esc(lastL) +
          '</b> ' +
          GOV.esc(tlItems[0].action) +
          '</div>' +
          '</div>';
        tl.innerHTML =
          tlSummary +
          tlItems
            .map(function (i) {
              return (
                '<div class="tli">' +
                '<div class="tldot"></div>' +
                '<div class="tlcard">' +
                '<div style="display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap">' +
                '<b>' +
                GOV.esc(i.action) +
                '</b>' +
                '<span class="muted">' +
                GOV.esc(i.at.replace('T', ' ').slice(0, 16)) +
                '</span>' +
                '</div>' +
                '<div class="muted" style="margin-top:6px">' +
                GOV.esc(i.by) +
                ' — ' +
                GOV.esc(i.note || '') +
                '</div>' +
                '</div>' +
                '</div>'
              );
            })
            .join('');
      }
      renderTimeline();

      const modal = document.getElementById('actModal');
      const ov = document.getElementById('actOverlay');
      const title = document.getElementById('actTitle');
      let currentAct = null;

      function openAct(code) {
        currentAct = code;
        title.textContent = t('legacyPages.transactionDetails.act.' + code);
        document.getElementById('actNote').value = '';
        ov.classList.add('show');
        modal.classList.add('show');
      }
      function closeAct() {
        ov.classList.remove('show');
        modal.classList.remove('show');
      }

      ['btnApprove', 'btnForward', 'btnReturn', 'btnReject'].forEach(function (id) {
        const b = document.getElementById(id);
        if (!b) return;
        b.addEventListener('click', () => {
          const map = {
            btnApprove: 'APPROVE',
            btnForward: 'FORWARD',
            btnReturn: 'RETURN',
            btnReject: 'REJECT'
          };
          openAct(map[id]);
        });
      });

      document.getElementById('actClose').addEventListener('click', closeAct);
      ov.addEventListener('click', closeAct);

      document.getElementById('actSubmit').addEventListener('click', () => {
        const note = document.getElementById('actNote').value.trim();
        if (!note) {
          GOV.showToast(t('legacyPages.transactionDetails.toastNoteTitle'), t('legacyPages.transactionDetails.toastNoteMsg'));
          return;
        }

        if (currentAct === 'APPROVE') tx.status = 'DONE';
        if (currentAct === 'REJECT') tx.status = 'REJECTED';
        if (currentAct === 'RETURN') tx.status = 'RETURNED';
        if (currentAct === 'FORWARD') tx.status = 'IN_PROGRESS';

        meta.timeline = meta.timeline || [];
        const du = t('legacyPages.transactionDetails.demoUser');
        meta.timeline.unshift({
          at: new Date().toISOString(),
          action: t('legacyPages.transactionDetails.act.' + currentAct),
          by: du,
          note: note
        });

        GOV.saveData(d);
        if (st) st.innerHTML = GOV.statusPill(tx.status);
        renderTimeline();
        GOV.showToast(t('legacyPages.transactionDetails.toastOkTitle'), t('legacyPages.transactionDetails.toastOkMsg'));
        closeAct();
      });

      if (attBody) {
        attBody.addEventListener('click', (e) => {
          const b = e.target.closest('[data-dl]');
          if (!b) return;
          GOV.showToast(t('legacyPages.transactionDetails.toastAttachTitle'), t('legacyPages.transactionDetails.toastAttachMsg'));
        });
      }

      if (window.GOV_EDITOR && document.getElementById('editor')) {
        GOV_EDITOR.init();
      }
    });
  });
})();
