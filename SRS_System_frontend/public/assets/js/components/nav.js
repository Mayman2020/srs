(function(){
  const NAV = [
    { key:'dashboard', href:'./dashboard.html', label:'لوحة التحكم', badge:'KPIs', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 13h8V3H3v10zM13 21h8V11h-8v10zM13 3h8v6h-8V3zM3 17h8v4H3v-4z"/></svg>` },
    { key:'transactions', href:'./transactions.html', label:'إدارة المعاملات', badge:'قائمة', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 7h10M7 12h10M7 17h10"/><path d="M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z"/></svg>` },
    { key:'users', href:'./users.html', label:'إدارة المستخدمين', badge:'RBAC', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M20 8v6"/><path d="M23 11h-6"/></svg>` },
    { key:'roles', href:'./roles.html', label:'الأدوار والصلاحيات', badge:'Matrix', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 1l3 5 5 1-3.5 4 1 6-5.5-3-5.5 3 1-6L4 7l5-1 3-5z"/></svg>` },
    { key:'reports', href:'./reports.html', label:'التقارير والإحصاءات', badge:'Charts', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19h16"/><path d="M6 16V8"/><path d="M10 16V4"/><path d="M14 16v-6"/><path d="M18 16v-10"/></svg>` },
    { key:'notifications', href:'./notifications.html', label:'الإشعارات', badgeId:'badgeNoti', icon:`<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 10-12 0c0 7-3 7-3 7h18s-3 0-3-7"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>` }
  ];

  function buildNav(){
    const nav = document.querySelector('.sidebar .nav');
    if(!nav) return;

    const page = document.body.getAttribute('data-page');
    const d = window.GOV ? GOV.loadData() : null;
    const unread = d ? d.notifications.filter(n=>!n.read).length : 0;

    nav.innerHTML = NAV.map(item=>{
      const active = (page===item.key) || (page==='transaction-details' && item.key==='transactions');
      const badge = item.badgeId ? `<span class="badge" id="${item.badgeId}">${unread}</span>` : `<span class="badge">${item.badge||''}</span>`;

      return `
        <button class="item ${active?'active':''}" data-nav="${item.key}" onclick="location.href='${item.href}'">
          <div class="left">${item.icon}${item.label}</div>
          ${badge}
        </button>
      `;
    }).join('');
  }

  document.addEventListener('DOMContentLoaded', buildNav);
})();
