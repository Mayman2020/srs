(function () {
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

  // --- Theme (Light default) ---
  const THEME_KEY = 'gov-theme';
  function setTheme(mode) {
    const root = document.documentElement;
    root.setAttribute('data-theme', mode);
    localStorage.setItem(THEME_KEY, mode);
  }
  function initTheme() {
    const saved = localStorage.getItem(THEME_KEY);
    if (saved) {
      setTheme(saved);
    } else {
      setTheme('light');
    }
  }

  // --- Toast ---
  function showToast(title, msg) {
    const t = $('#toast');
    if (!t) return;
    $('#toastTitle').textContent = title;
    $('#toastMsg').textContent = msg;
    t.classList.add('show');
    window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(() => t.classList.remove('show'), 2200);
  }

  // --- Demo user / session (no real auth) ---
  const SESSION_KEY = 'gov-session';

  function setSession(obj) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(obj));
  }
  function getSession() {
    try {
      return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null');
    } catch (e) {
      return null;
    }
  }
  function requireSession() {
    const s = getSession();
    if (!s) {
      if (!location.pathname.endsWith('login.html')) location.href = './login.html';
      return null;
    }
    return s;
  }

  // --- Shared demo data (codes + row keys; UI strings from i18n legacyPages.*) ---
  const DATA_KEY = 'gov-data-v2';

  function defaultData() {
    return {
      tx: [
        {
          id: '1445/10293',
          type: 'INBOUND',
          row: 'r1',
          created: '2026-01-22',
          status: 'IN_PROGRESS'
        },
        {
          id: '1445/10294',
          type: 'OUTBOUND',
          row: 'r2',
          created: '2026-01-23',
          status: 'DONE'
        },
        {
          id: '1445/10295',
          type: 'INTERNAL',
          row: 'r3',
          created: '2026-01-24',
          status: 'NEW'
        },
        {
          id: '1445/10296',
          type: 'EXTERNAL',
          row: 'r4',
          created: '2026-01-25',
          status: 'IN_PROGRESS'
        },
        {
          id: '1445/10297',
          type: 'INBOUND',
          row: 'r5',
          created: '2026-01-25',
          status: 'RETURNED'
        },
        {
          id: '1445/10304',
          type: 'INBOUND',
          row: 'r6',
          created: '2026-02-01',
          status: 'NEW'
        }
      ],
      notifications: [
        {
          type: 'ALERT',
          textKey: 'n1',
          time: '2026-02-01 14:22',
          read: false,
          important: true
        },
        {
          type: 'REMINDER',
          textKey: 'n2',
          time: '2026-02-02 09:10',
          read: false,
          important: true
        },
        {
          type: 'INFO',
          textKey: 'n3',
          time: '2026-02-02 12:40',
          read: true,
          important: false
        }
      ],
      users: [
        { profile: 'u1', nid: '1020304050' },
        { profile: 'u2', nid: '1010101010' },
        { profile: 'u3', nid: '1090909090' }
      ]
    };
  }

  function loadData() {
    try {
      const raw = localStorage.getItem(DATA_KEY);
      if (!raw) {
        const d = defaultData();
        localStorage.setItem(DATA_KEY, JSON.stringify(d));
        return d;
      }
      const d = JSON.parse(raw);
      try {
        const unread = (d.notifications || []).filter((n) => !n.read).length;
        if ((d.notifications || []).length >= 2 && unread < 2) {
          d.notifications[0].read = false;
          d.notifications[1].read = false;
          localStorage.setItem(DATA_KEY, JSON.stringify(d));
        }
      } catch (e) {}
      return d;
    } catch (e) {
      const d = defaultData();
      localStorage.setItem(DATA_KEY, JSON.stringify(d));
      return d;
    }
  }
  function saveData(d) {
    localStorage.setItem(DATA_KEY, JSON.stringify(d));
  }

  function esc(s) {
    return String(s ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /** Status pill HTML — requires GOV_I18N bundle loaded */
  function statusPill(status) {
    const t = window.GOV_I18N && GOV_I18N.t ? GOV_I18N.t.bind(GOV_I18N) : function (k) {
      return k;
    };
    const label = t('legacyPages.txStatus.' + status);
    if (status === 'DONE') return '<span class="pill ok">' + esc(label) + '</span>';
    if (status === 'REJECTED') return '<span class="pill bad">' + esc(label) + '</span>';
    if (status === 'RETURNED') return '<span class="pill warn">' + esc(label) + '</span>';
    if (status === 'IN_PROGRESS') return '<span class="pill">' + esc(label) + '</span>';
    return '<span class="pill">' + esc(label) + '</span>';
  }

  function wireCommon() {
    const t = window.GOV_I18N && GOV_I18N.t ? GOV_I18N.t.bind(GOV_I18N) : function (k) {
      return k;
    };
    const themeBtn = $('#btnTheme');
    if (themeBtn) {
      themeBtn.addEventListener('click', () => {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        setTheme(current === 'dark' ? 'light' : 'dark');
        showToast(
          t('legacyPages.common.done'),
          current === 'dark' ? t('legacyPages.theme.switchedToLight') : t('legacyPages.theme.switchedToDark')
        );
      });
    }

    const lo = $('#btnLogout');
    if (lo) {
      lo.addEventListener('click', () => {
        localStorage.removeItem(SESSION_KEY);
        showToast(t('legacyPages.common.done'), t('legacyPages.auth.logoutDemo'));
        setTimeout(() => (location.href = './login.html'), 250);
      });
    }

    const s = getSession();
    if (s) {
      const u = $('#sideUser');
      const r = $('#sideRole');
      if (u) u.textContent = s.name;
      if (r) r.textContent = s.role;
    }

    const page = document.body.getAttribute('data-page');
    if (page) {
      $$('.nav .item').forEach((b) => b.classList.toggle('active', b.getAttribute('data-nav') === page));
    }
  }

  window.GOV = {
    initTheme,
    setTheme,
    showToast,
    setSession,
    getSession,
    requireSession,
    loadData,
    saveData,
    statusPill,
    esc,
    DATA_KEY
  };

  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    if (window.GOV_I18N && typeof GOV_I18N.ready === 'function') {
      GOV_I18N.ready().then(() => wireCommon());
    } else {
      wireCommon();
    }
  });
})();
