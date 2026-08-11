# my-skills (canonical — D: only)

Part of the My Apps Cursor layout. Sibling folder: `../cursor-data/` (home, roaming, app).

All custom Cursor agent skills live in this folder only:

`d:\Apps Work\My Apps\my-skills\`

## How apps load them

Each app (and My Apps root) has a Windows **junction**:

```text
<app>\.cursor\skills  →  My Apps\my-skills
```

Cursor discovers project skills from `.cursor/skills/`. There is **no** copy under `C:\Users\...\ .cursor\skills` anymore.

## Apps wired

- My Apps (workspace root)
- aromaflow
- Clinic System
- erp Project
- hesabaty-project
- Inteanet
- Mazaad App
- pos-cashier-system
- Property_Managments
- resturant system
- Spicy Live App
- srs-project
- Vzeeta Project
- web portal

> If an app’s `.cursor/skills` is missing or not a junction, run (Cursor closed):
> `powershell -ExecutionPolicy Bypass -File "d:\Apps Work\My Apps\cursor-data\scripts\ensure-layout.ps1"`

## Skills catalog (42)

### Core playbook (apply in order)

1. `rms-project-playbook` — master order / audit
2. `rms-premium-shell` — login, sidebar, topbar, theme/lang, smart back
3. `estate-card-system` — stat-pill, table-card, entity-card
4. `rms-property-list-integration` — ListLoadController, pager, route↔API
5. `rms-dates-property-style` — dd/MM/yyyy, RmsDatePipe
6. `rms-fullstack-integration` — runtime-config, PageResponse, no DEMO
7. `estate-settings-security-roles` — JWT, RBAC, users, lookups, Flyway

### Property polish / satellites

8. `estate-user-mgmt-admin`
9. `estate-screen-module-visibility`
10. `estate-user-access-multi-role`
11. `estate-reference-lookups`
12. `estate-os-design-tokens` *(skip rebrand unless asked)*
13. `estate-i18n-rtl-parity`
14. `estate-lov-picker-system`
15. `estate-status-data-badges`
16. `estate-table-row-actions`
17. `estate-table-list-toolbar`
18. `estate-icon-tooltips`
19. `estate-page-header-actions`
20. `estate-delete-confirm-dialog`
21. `estate-identity-media-uploads`
22. `estate-detail-page-hero`
23. `estate-entity-audit-trail`
24. `estate-excel-export-toolbar`
25. `estate-in-app-notifications`
26. `estate-topbar-user-chrome`
27. `estate-sidebar-nav-sections`
28. `estate-my-profile-page`
29. `rms-transactional-email`
30. `estate-dashboard-kpi-motion`
31. `estate-kpi-drilldown-workspace`
32. `estate-srs-table-pager`
33. `estate-workspace-filter-strip`
34. `estate-contract-lifecycle-workspace` *(Property domain — usually skip)*
35. `estate-owner-approval-inbox` *(Property domain — usually skip)*
36. `estate-dual-source-unified-list` *(Property domain — usually skip)*

### Infra / ops

37. `appointment-reminder-scheduler` *(Clinic + Vzeeta)*
38. `environment-variable-hardening`
39. `password-reset-email-provider`
40. `process-restart-health-check-handling`
41. `smoke-test-validation-checklist`
42. `spring-boot-production-cors`

## Add a new skill

Create `my-skills/<skill-name>/SKILL.md` — every app picks it up via its junction.

Do **not** write skills into `~/.cursor/skills-cursor/` (Cursor built-ins only).
Do **not** copy skills into an app folder — edit here only.
