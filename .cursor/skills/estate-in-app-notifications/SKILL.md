---
name: estate-in-app-notifications
description: >-
  Property in-app notifications — topbar unread badge poll, inbox page with
  14-day tabs, mark-all-read, notification cards, deep-link open, bilingual
  titles, localized backend create. Use when porting notification UX across
  RMS apps.
---

# Estate In-App Notifications

**UI reference:** `Property_Managments/property-frontend`  
- Page: `features/notifications/notifications-page/`  
- Service: `core/services/notification.service.ts`  
- Open: `core/utils/notification-open.util.ts` + `notification-navigation.util.ts`  
- Display: `notification-display.util.ts`  
- Topbar badge: `layout/topbar/topbar.component.ts`  

**API reference:** `property-backend` `modules/notification/` + `BilingualNotificationText`

Combine with: `estate-i18n-rtl-parity`, `estate-status-data-badges`, `rms-premium-shell` (bell slot).

## Topbar bell

1. Unread badge absolute on bell (`tb-notify-count`).
2. Poll unread every **30s** + subscribe to shared `unreadCount$`.
3. Click → navigate to role inbox route (`notificationsInboxRoute`) — Property uses route, optional dropdown may reuse `.tb-notify-*` styles.
4. After mark-read / mark-all: update `unreadCount$` so badge stays in sync.

## Inbox page

```
page-header (smart back + Mark all read)
mat-tabs: Recent (≤14 days) | Older (>14 days)
app-card list of notification-row cards
app-table-pager (pageSize 5)
app-empty-state when empty
```

### Tabs
- `scope=recent|older` with **14-day** cutoff; default `recent`.
- Hide tab body panel CSS if tabs only switch scope (`animationDuration="0ms"`).

### Row card
- Grid: type **icon** | copy | open (**`app-icon-btn accent`** arrow)
- Title + body via i18n keys (`titleKey`/`bodyKey`/`vars`) **or** `pickBilingualSegment`
- Meta: actor • `dd/MM/yyyy` • `HH:mm`
- Read badge: `data-status="ACTIVE"` READ / `PENDING` NEW
- Unread row gets `.unread` tint; special origins (e.g. complaint) may tint icon

### Open action
```ts
openNotification(item, auth, router, notificationService)
// 1 mark read + decrementUnread
// 2 optional type-specific side effects (e.g. snooze overdue)
// 3 resolveNotificationTargetUrl → navigateByUrl (role-aware deep link)
```

### Mark all read
`PATCH .../my/read-all` → clear shared unread count → reload list.

## Backend emit

Prefer localized create:

```java
notificationService.createLocalized(userIds, type, titleKey, bodyKey, vars, navHints);
```

Raw bilingual fallback: `BilingualNotificationText` (`"ar | en"` / body `ar\n\n—\n\nen`).

## i18n keys

`NOTIFICATIONS.TITLE|SUBTITLE|MARK_ALL_READ|TAB_RECENT|TAB_OLDER|READ|NEW|BY|DATE|TIME|OPEN_LINK|EMPTY|EMPTY_TAB_HINT`

## Do not

- Client-filter a paged inbox incorrectly across tabs
- Navigate without marking read (badge goes stale)
- Hardcode English titles when keys/bilingual exist
