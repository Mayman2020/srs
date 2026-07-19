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

## Apps wired (14)

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

## Skills (13)

1. `rms-project-playbook`
2. `rms-premium-shell`
3. `estate-card-system`
4. `rms-property-list-integration`
5. `rms-dates-property-style`
6. `rms-fullstack-integration`
7. `estate-settings-security-roles`
8. `appointment-reminder-scheduler`
9. `environment-variable-hardening`
10. `password-reset-email-provider`
11. `process-restart-health-check-handling`
12. `smoke-test-validation-checklist`
13. `spring-boot-production-cors`

## Add a new skill

Create `my-skills/<skill-name>/SKILL.md` — every app picks it up via its junction.
Do **not** write skills into `~/.cursor/skills-cursor/` (Cursor built-ins only).
