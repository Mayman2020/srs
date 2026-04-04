(function(){
  const $ = (s, r=document)=>r.querySelector(s);
  const $$ = (s, r=document)=>Array.from(r.querySelectorAll(s));

  // --- Theme (Light default) ---
  const THEME_KEY = 'gov-theme';
  function setTheme(mode){
    const root = document.documentElement;
    root.setAttribute('data-theme', mode);
    localStorage.setItem(THEME_KEY, mode);
  }
  function initTheme(){
    const saved = localStorage.getItem(THEME_KEY);
    if(saved){ setTheme(saved); }
    else { setTheme('light'); }
  }

  // --- Toast ---
  function showToast(title, msg){
    const t = $('#toast');
    if(!t) return;
    $('#toastTitle').textContent = title;
    $('#toastMsg').textContent = msg;
    t.classList.add('show');
    window.clearTimeout(window.__toastTimer);
    window.__toastTimer = window.setTimeout(()=>t.classList.remove('show'), 2200);
  }

  // --- Demo user / session (no real auth) ---
  const SESSION_KEY = 'gov-session';
  function setSession(obj){ localStorage.setItem(SESSION_KEY, JSON.stringify(obj)); }
  function getSession(){
    try{ return JSON.parse(localStorage.getItem(SESSION_KEY) || 'null'); }catch(e){ return null; }
  }
  function requireSession(){
    const s = getSession();
    if(!s){
      // redirect to login
      if(!location.pathname.endsWith('login.html')) location.href = './login.html';
      return null;
    }
    return s;
  }

  // --- Shared demo data ---
  const DATA_KEY = 'gov-data';
  function defaultData(){
    return {
      tx: [
        {id:'1445/10293', type:'وارد', subject:'طلب اعتماد محضر لجنة', from:'وزارة / جهة عليا', to:'إدارة', created:'2026-01-22', status:'قيد الإجراء'},
        {id:'1445/10294', type:'صادر', subject:'إفادة بخصوص خطاب سابق', from:'جهة حكومية', to:'موظف', created:'2026-01-23', status:'منجزة'},
        {id:'1445/10295', type:'داخلية', subject:'تعميم إجراءات الأرشفة', from:'إدارة الاتصالات الإدارية', to:'مجموعة إدارات', created:'2026-01-24', status:'جديدة'},
        {id:'1445/10296', type:'خارجية', subject:'مراسلة مورد بخصوص عقد', from:'جهة حكومية', to:'إدارة', created:'2026-01-25', status:'قيد الإجراء'},
        {id:'1445/10297', type:'وارد', subject:'طلب تزويد بيانات', from:'جهة حكومية', to:'موظف', created:'2026-01-25', status:'معادة'},
        {id:'1445/10304', type:'وارد', subject:'طلب استكمال مستندات', from:'جهة حكومية', to:'إدارة', created:'2026-02-01', status:'جديدة'}
      ],
      notifications: [
        {type:'تنبيه', text:'لديك معاملة واردة جديدة برقم 1445/10304', time:'2026-02-01 14:22', read:false, important:true},
        {type:'تذكير', text:'اقتربت مدة الرد لمعاملة 1445/10293', time:'2026-02-02 09:10', read:false, important:true},
        {type:'معلومة', text:'تم إغلاق معاملة 1445/10294 بنجاح', time:'2026-02-02 12:40', read:true, important:false}
      ],
      users: [
        {name:'مستخدم تجريبي', nid:'1020304050', dept:'إدارة الاتصالات الإدارية', status:'نشط'},
        {name:'سارة الشهري', nid:'1010101010', dept:'الإدارة العامة', status:'نشط'},
        {name:'محمد الحربي', nid:'1090909090', dept:'إدارة الموارد البشرية', status:'موقوف'}
      ]
    };
  }
  function loadData(){
    try{
      const raw = localStorage.getItem(DATA_KEY);
      if(!raw){
        const d = defaultData();
        localStorage.setItem(DATA_KEY, JSON.stringify(d));
        return d;
      }
      const d = JSON.parse(raw);
      // Demo guarantee: ensure at least 2 unread notifications
      try{
        const unread = (d.notifications||[]).filter(n=>!n.read).length;
        if((d.notifications||[]).length>=2 && unread<2){
          d.notifications[0].read = false;
          d.notifications[1].read = false;
          localStorage.setItem(DATA_KEY, JSON.stringify(d));
        }
      }catch(e){}
      return d;
    }catch(e){
      const d = defaultData();
      localStorage.setItem(DATA_KEY, JSON.stringify(d));
      return d;
    }
  }
  function saveData(d){ localStorage.setItem(DATA_KEY, JSON.stringify(d)); }

  // --- Helpers for status pills ---
  function statusPill(status){
    if(status==='منجزة') return '<span class="pill ok">منجزة</span>';
    if(status==='مرفوضة') return '<span class="pill bad">مرفوضة</span>';
    if(status==='معادة') return '<span class="pill warn">معادة</span>';
    if(status==='قيد الإجراء') return '<span class="pill">قيد الإجراء</span>';
    return '<span class="pill">جديدة</span>';
  }

  // --- Global init ---
  function wireCommon(){
    // theme toggle
    const t = $('#btnTheme');
    if(t){
      t.addEventListener('click', ()=>{
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        setTheme(current==='dark' ? 'light' : 'dark');
        showToast('تم', current==='dark' ? 'تم تفعيل الوضع النهاري.' : 'تم تفعيل الوضع الليلي.');
      });
    }

    // logout
    const lo = $('#btnLogout');
    if(lo){
      lo.addEventListener('click', ()=>{
        localStorage.removeItem(SESSION_KEY);
        showToast('تم', 'تم تسجيل الخروج (Demo).');
        setTimeout(()=>location.href='./login.html', 250);
      });
    }

    // sidebar user display
    const s = getSession();
    if(s){
      const u = $('#sideUser');
      const r = $('#sideRole');
      if(u) u.textContent = s.name;
      if(r) r.textContent = s.role;
    }

    // active nav based on page
    const page = document.body.getAttribute('data-page');
    if(page){
      $$('.nav .item').forEach(b=> b.classList.toggle('active', b.getAttribute('data-nav')===page));
    }
  }

  // Expose a tiny API for page scripts
  window.GOV = {
    initTheme,
    setTheme,
    showToast,
    setSession,
    getSession,
    requireSession,
    loadData,
    saveData,
    statusPill
  };

  document.addEventListener('DOMContentLoaded', ()=>{
    initTheme();
    wireCommon();
  });
})();
