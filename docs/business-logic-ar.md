# 📘 الدليل الشامل لمنطق العمل (Business Logic)
## نظام إدارة المراسلات الإدارية — SRS Correspondence Management

> هذه الوثيقة تشرح **كل المنطق الإداري والوظيفي** الذي طُوِّر في النظام حتى الآن (تشمل القاعدة الإنتاجية + جميع شرائح الـ Defense-Grade Hardening من Slice 1 إلى Slice 6).
> الهدف: أن يفهم المدير/المحلل/المُختبِر **ماذا يفعل النظام** و**لماذا** بدون الحاجة للرجوع إلى الكود.
> ملاحظة فنية: الوثائق التقنية بالإنجليزية موجودة في:
> [`architecture.md`](architecture.md), [`workflow.md`](workflow.md), [`runbook.md`](runbook.md), [`permissions-architecture.md`](permissions-architecture.md), [`enterprise-phase-defensive-hardening.md`](enterprise-phase-defensive-hardening.md).

---

## 1. نظرة عامة على النظام

### 1.1 الغرض
نظام مركزي لإدارة دورة حياة المراسلات الإدارية (وارد، صادر، داخلي، تعميم، قرار إداري) داخل جهة حكومية، مع:
- **توجيه (Routing)** متعدد المستويات (Q/L/K/S) بمحرك BPMN ‏Camunda.
- **تحكم في الصلاحيات** (RBAC) موحَّد بصلاحيات قانونية ضرورية.
- **سرية المعلومات** (Confidentiality) من `NORMAL` حتى `TOP_SECRET` مع تحقق من مستوى التصاريح.
- **تتبع كامل (Audit Trail)** يشمل القراءة، التعديل، الإحالة، الاعتماد، التحميل.
- **توقيعات رقمية** للمرفقات + **رموز QR** للتحقق العلني من الوثائق المطبوعة.
- **إشعارات** بقنوات متعددة (داخل النظام، بريد، Webhook، Microsoft Teams) مع تفضيلات لكل مستخدم.
- **سياسة استبقاء** (Retention) قابلة للتكوين + **حجز قانوني** يجمّد الحذف.

### 1.2 المعمارية ‏(High-level)
- **Modular monolith** (Spring Boot 3 + Angular 21) — وحدات ‏(modules) داخل JAR واحد مع حدود معمارية صارمة مفروضة بـ ArchUnit.
- **PostgreSQL 15+** بمخطط ‏`srs_system`. لا توجد ‏ENUMs على مستوى الـ DB — كل قيمة مفهرسة في جدول ‏lookup.
- **Flyway** هو المصدر الوحيد للحقيقة على مستوى الـ schema (Hibernate في وضع validate فقط).
- **Camunda 7.22** كمحرك BPMN داخل نفس الـ schema.
- **JWT (HS256)** للجلسات + ‏`@PreAuthorize` على كل endpoint.

---

## 2. النموذج التنظيمي (Org Model)

### 2.1 الهرم الإداري
```
ROOT (الجهة)
 ├── مكتب الاتصالات الإدارية (CORRESP_OFFICE)
 ├── الشؤون القانونية (LEGAL_AFFAIRS)
 ├── تقنية المعلومات (IT_DEPT)
 ├── الموارد البشرية (HR_DEPT)
 └── الشؤون المالية (FINANCE)
```
كل قسم له `level_code` ∈ {Q, L, K, S} يستعمله ‏`OrgRoutingService` للتوجيه في BPMN.

### 2.2 الأدوار (Roles)
| الكود              | الاسم العربي           | المسؤولية                                              |
|---------------------|------------------------|---------------------------------------------------------|
| `SYS_ADMIN`         | مدير النظام            | جميع الصلاحيات بدون استثناء.                            |
| `CORRESP_MGR`       | مدير الاتصالات         | يدير الوارد/الصادر، يعتمد ويحيل.                        |
| `CORRESP_CLERK`     | موظف اتصالات إدارية    | تسجيل المراسلات اليومية، إضافة مرفقات.                  |
| `DEPT_MANAGER`      | مدير إدارة             | يتابع معاملات إدارته، يفوّض المهام.                     |
| `STAFF`             | موظف                   | ينفذ المهام، يعلق، ينهي الإجراءات.                      |
| `APPROVER`          | معتمد                  | يعتمد/يرفض المعاملات.                                   |
| `AUDITOR`           | مدقق                   | قراءة فقط: التقارير، سجل التدقيق، حالة القراءة.         |

### 2.3 الصلاحيات (Permissions)
- الكود الموحَّد بصيغة `SCREAMING_SNAKE_CASE` (مثل `CORRESPONDENCE_VIEW`).
- يوجد جدول ‏**`permission_alias`** يربط الكودات القديمة (`correspondence.view`, `CANCEL_TRANSACTION`, …) بكود واحد قانوني لمدة release واحدة.
- `EffectiveUserPermissionService` يحسب الصلاحيات الفعلية بطريقة **اتحاد كل صفوف `user_role` الصالحة + صلاحيات الغائبين الذين عُيِّن المتصل ‏Actingا عنهم** في استعلام SQL واحد.
- التحقق على مستوى الـ method:
  ```java
  @PreAuthorize("@effectivePermission.has('CORRESPONDENCE_APPROVE')")
  ```

---

## 3. دورة حياة المعاملة (Correspondence Lifecycle)

### 3.1 الأنواع
| الكود        | الاسم          | الاستخدام                                  |
|---------------|----------------|---------------------------------------------|
| `INBOUND`     | وارد           | استلام من جهة خارجية.                       |
| `OUTBOUND`    | صادر           | إصدار إلى جهة خارجية.                       |
| `INTERNAL`    | داخلي          | بين إدارات الجهة.                           |
| `EXTERNAL`    | خارجي          | حالات خاصة بدون توجيه داخلي.                |
| `CIRCULAR`    | تعميم          | توزيع على عدة جهات داخلية دفعة واحدة.       |
| `DECISION`    | قرار إداري     | يستلزم اعتماد رسمي.                         |

### 3.2 الحالات (Status)
`NEW → IN_PROGRESS → COMPLETED / REJECTED / RETURNED / ARCHIVED / CANCELLED`

كل تغيير حالة:
1. يُسجَّل في جدول `workflow_history` و `workflow_action` (سجل مزدوج للتدقيق).
2. ينتج عنه حدث ‏`audit_event` بـ hash متسلسل ‏(integrity chain) — أي حذف أو تلاعب ينكشف فورا.
3. قد يولّد إشعارات للمستلمين المعنيين عبر ‏Outbox.

### 3.3 السرية (Confidentiality)
| الكود          | الاسم         | يستلزم تصاريح؟ | يحظر التصدير؟ |
|-----------------|---------------|------------------|------------------|
| `NORMAL`        | عادي          | ❌                | ❌                |
| `LIMITED`       | محدود         | ❌                | ❌                |
| `SECRET`        | سري           | ❌                | ✅                |
| `TOP_SECRET`    | سري للغاية    | ✅ (clearance)    | ✅                |

- `CorrespondenceViewAuthorization.assertClearance` يرفض أي استعلام تفصيل لمعاملة `TOP_SECRET` لمستخدم بدون ‏`security_clearance_id = TOP_SECRET`.
- `CorrespondenceSpecifications.visibleByClearance` يحجب الصفوف من قوائم الـ list والـ export.

### 3.4 التوزيع الدائري (Circular Distribution)
- جدول `correspondence_recipient` يربط معاملة واحدة بعدد من الإدارات.
- لكل مستلم نتتبع: `first_read_at`, `last_read_at`, `read_count`.
- يستخدمه التعميم والقرار الإداري الذي يوزَّع على عدة جهات.

---

## 4. سير العمل (Workflow — Camunda)

### 4.1 الـ BPMNs الثلاثة
1. **`inbound-correspondence`** — وارد: تسجيل → فرز → توجيه ‏Q→L→K→S → تنفيذ → إغلاق.
2. **`outbound-correspondence`** — صادر: تحرير → مراجعة → اعتماد → إرسال.
3. **`internal-correspondence`** — داخلي: مماثل للصادر بدون جهة خارجية.

كل عملية تنتج Timer Metric:
```
workflow_task_duration_seconds{process="inbound-correspondence"}
```

### 4.2 طبقات تعيين المهمة (Assignment Overlay)
في `TaskDelegationAssignmentResolver` (Slice 4) الترتيب:
```
Direct Workflow Assignee
   │
   ▼
Acting Assignment Overlay   (إذا كان المُعيَّن غائبا وتوجد تغطية صالحة)
   │
   ▼
Task Delegation              (إذا كان المُعيَّن مُفوِّضا لمستخدم آخر)
```
- يُسجَّل في `workflow_history.detail` كل من: `originalAssigneeUserId`, `actingDelegateUserId`, `taskDelegationId`, `actingAssignmentId`.
- التفويض يحترم `SlaClearanceFilter` — لا يُحال أي شيء إلى مستخدم بدون التصاريح المطلوبة.

### 4.3 التفويض المُهمي (Task Delegation — Slice 2)
- جدول `task_delegation` بـ scope (TASK / TYPE / CONFIDENTIALITY) + نافذة زمنية (valid_from/valid_to).
- `TaskDelegationExpiryJob` كل 10 دقائق يضع `revoked_by = NULL` على كل صف منتهي → سجل تاريخي نظيف.
- صلاحيات:
  - `TASK_DELEGATION_MANAGE_OWN` — كل موظف ينشئ تفويضاته الخاصة.
  - `TASK_DELEGATION_ADMIN` — للتدقيق والإدارة المركزية.

### 4.4 مدير بالنيابة (Acting Manager — Slice 4)
- جدول `acting_assignment`: غياب يومين → كل المعاملات المُسندة للغائب تظهر تلقائيا في ‏inbox الـ Acting.
- جوبات:
  - `ActingAssignmentExpiryJob` — يُنهي التغطية تلقائيا في يوم العودة.
  - `ActingAssignmentReconciliationJob` — يمسح مهام Camunda الجارية ويُعيد التعيين.
- صلاحيات:
  - `ACTING_ASSIGNMENT_VIEW`, `ACTING_ASSIGNMENT_MANAGE_OWN`, `ACTING_ASSIGNMENT_ADMIN`.

### 4.5 محرك الـ SLA (SLA Policy Engine — Slice 3)
- جدول `sla_policy`: قاعدة بسعة شروط (نوع، أولوية، سرية، org-level، action type) — يُختار الصف الأعلى تخصصا.
- جدول `sla_escalation_step`: قائمة مرتَّبة من الإجراءات بعد الـ breach:
  - `NOTIFY_MANAGER` — تنبيه المدير المباشر.
  - `REASSIGN_TO_DELEGATE` — تحويل تلقائي للمفوَّض (إذا تجاوز التصاريح).
  - `ESCALATE_TO_HIGHER_LEVEL` — رفع إلى المستوى التنظيمي الأعلى.
  - `NOTIFY_AUDIT_ADMIN` — إعلام التدقيق.
- جدول `sla_breach_event`: صف واحد لكل ‏task متأخر (unique على task_id) — يجمع idempotency + سؤال "ماذا تأخر؟".
- `SlaPolicyEvaluationJob` كل دقيقة يدور وينفذ الـ steps المستحقة.
- صلاحيات:
  - `SLA_POLICY_VIEW` — قراءة.
  - `SLA_POLICY_MANAGE` — CRUD على القواعد.
- مقاييس Prometheus:
  - `correspondence_sla_breach_total{outcome=…}`
  - `correspondence_sla_escalation_total{action=…}`
  - `correspondence_sla_overdue_active` (gauge)

---

## 5. المرفقات (Attachments)

### 5.1 النموذج الأساسي
- `attachment` = الوثيقة المنطقية + `attachment_version` = نسخ متتابعة.
- كل نسخة لها: `mime_type`, `byte_size`, `plaintext_sha256`, `created_by`.
- سقف الحجم الإجمالي للمعاملة الواحدة: 200MB (يفرضه `total_attachment_bytes`).

### 5.2 المرفقات المُصنَّفة (Classified Attachments — Slice 5)
- إذا كانت السرية `SECRET`/`TOP_SECRET`، النسخة تُشفَّر **AES-256-GCM**:
  - `encryption_algo = 'AES_256_GCM'`
  - `encryption_key_ref` = اسم المفتاح (env var)
  - `encryption_wrapped_dek` = DEK مغلَّف بـ KEK
  - `encryption_iv` = vector عشوائي 12 بايت
  - `ciphertext_sha256` + `plaintext_sha256` للتحقق المزدوج.
- `KeyProvider` SPI يسمح بتبديل مزود المفاتيح (HSM في الإنتاج).

### 5.3 التوقيع الرقمي (Digital Signatures — Slice 5)
- جدول `document_signature`:
  - `algorithm` ∈ `ED25519` (افتراضي) أو `RSA_PSS_SHA256`.
  - `canonical_hash_sha256` يُحسب بطريقة موحدة (canonicalization).
  - `signature_bytes` التوقيع نفسه.
  - `verification_status` ∈ `VALID` / `INVALID` / `REVOKED` / `UNKNOWN`.
- المُوقِّع يُحفظ بـ FK إلى `signer_user_id`.
- `workflow_action_type.requires_signature = TRUE` يفرض على المعتمد توقيع كل المرفقات قبل تنفيذ ‏action معينة.
- صلاحيات: `ATTACHMENT_SIGN_VIEW`, `ATTACHMENT_SIGN_CREATE`, `ATTACHMENT_SIGNATURE_ADMIN`.

### 5.4 التنزيل الآمن (Secure Download Tokens — Slice 5)
- بدلا من URL مباشر، الـ FE يطلب نية تنزيل → يحصل على `attachment_download_token`:
  - الـ DB تخزن فقط SHA-256(token) — الـ raw يُعطى مرة واحدة.
  - له `expires_at` + `consumed_at` + ‏IP/UserAgent للمُصدِر.
- نقطتا النهاية:
  - `POST /api/v1/attachments/{id}/download-intent` — تُصدر التوكن.
  - `GET /api/v1/attachments/download/{token}` — تستهلكه (مرة واحدة).
- **🪦 Legacy Tombstone (Slice 6):** نقطة النهاية القديمة ‏`GET /api/v1/attachments/{id}/download` ترجع **410 Gone** بصيغة ‏`application/problem+json` مع حقل ‏`migrateTo` يشرح المسار الجديد. لا يُنزَّل أي ملف.

### 5.5 التحقق العلني بالـ QR (Public Verification — Slice 6)
- جدول `attachment_verification_token`:
  - يُخزَّن فقط SHA-256(token) — مماثل لـ download-token.
  - `access_count` + `last_accessed_at` + `revoked_at`.
- نقطة نهاية **علنية بدون مصادقة**: `GET /api/v1/public/verify/{token}`
  - `@PreAuthorize("permitAll()")` — لكنها مقيدة بـ Rate-Limit في الذاكرة (Caffeine, IP-based).
  - ترجع `AttachmentPublicVerificationDto` (DTO مُنقَّاة — بدون أسماء أو أرقام مرجعية).
- جدول `attachment_verification_access_log` يسجل كل محاولة (ناجحة أو فاشلة) — منفصل عن ‏`audit_event` لأن الفاعل مجهول الهوية.
- صلاحيات الإصدار: `ATTACHMENT_VERIFY_TOKEN_ISSUE`, `ATTACHMENT_VERIFY_TOKEN_VIEW`.
- شاشات الـ FE:
  - **حوار إصدار QR** (`VerificationQrDialogComponent`): يولِّد QR محليا، يُتيح نسخ الـ URL، طباعة.
  - **صفحة التحقق العلنية** (`PublicVerifyComponent`): حالات `loading` / `ok` / `not-found` / `rate-limited` / `error`.

---

## 6. تتبع القراءة والوصول (Read Tracking — Slice 1)

### 6.1 إيصال القراءة
- جدول `correspondence_read_receipt`:
  - فهرس فريد جزئي على `(correspondence_id, user_id) WHERE deleted_at IS NULL`.
  - يحفظ `first_opened_at`, `last_opened_at`, `acknowledged_at`.
- يُملأ تلقائيا عند فتح صفحة التفاصيل + endpoint إقرار صريح.
- صلاحية: `CORRESPONDENCE_READ_STATUS_VIEW`.

### 6.2 سجل وصول المرفقات
- جدول `attachment_access_log` (append-only):
  - كل تنزيل أو معاينة → صف يحوي ‏IP / UserAgent المستخرج.
- صلاحية: `ATTACHMENT_ACCESS_LOG_VIEW`.

---

## 7. سجل التدقيق (Audit — Slice الأصلية)

- جدول `audit_event` بـ **integrity chain**: كل صف يحوي `prev_hash` + `hash` (SHA-256 على المحتوى + الصف السابق).
- أي حذف أو تعديل أو نقص ينكشف بفحص السلسلة (موجود فحص أوتوماتيكي في ‏`AuditChainVerifier`).
- يُستهدف فيه: تسجيل دخول، تغيير صلاحيات، تبديل دور، حذف معاملة، فشل DLQ، تعليق رد على Webhook، …
- صلاحية: `ADMIN_AUDIT_VIEW`.

---

## 8. الإشعارات (Notifications — جميع الشرائح)

### 8.1 صندوق الوارد داخل النظام (In-App Inbox)
- جدول `notification`: العنوان، النص، الإشارة إلى المعاملة، حالة القراءة.
- المرسل: `NotificationService.send()` أو `NotificationOutboxService.enqueue()`.
- شاشة الـ FE: جرس + لوحة وارد + filter بحالة القراءة.

### 8.2 الـ Outbox (Slice 6)
- جدول `notification_outbox`:
  - `idempotency_key` فريد (يمنع التكرار).
  - `status` ∈ `PENDING` / `IN_FLIGHT` / `SENT` / `FAILED` / `DEAD`.
  - `attempt_count` + `next_attempt_at` (exponential backoff).
  - `last_error` للتشخيص.
  - `correlation_resource_type` + `correlation_resource_id` لربط الصف بمعاملة معينة.
- **NotificationOutboxDispatchJob**:
  - كل 30 ثانية (قابل للتكوين).
  - يستعمل `SELECT … FOR UPDATE SKIP LOCKED` فلا يتنازع inst-instances متعددة.
  - يستدعي مزود قناة (`NotificationChannelProvider`):
    - `IN_APP` — يكتب في جدول `notification`.
    - `EMAIL` — يستعمل ‏`JavaMailSender`.
    - `WEBHOOK` — POST مع `X-AC-Signature: hmac-sha256=<base64>`.
    - `TEAMS` — Microsoft Teams incoming webhook + توقيع HMAC.
  - فشل متكرر → الصف يصبح `DEAD` (DLQ) ويُسجَّل ‏`NOTIFICATION_OUTBOX_DEAD_LETTER` في audit.
- وضع التشغيل يتحكم به ‏`ac.notification.routing`:
  - `outbox` (افتراضي) = كل القنوات تمر بالـ outbox.
  - `inline` = (legacy) كتابة in-app مباشرة.

### 8.3 قنوات الإشعار + الـ Targets
- جدول `notification_channel`: `IN_APP`, `EMAIL`, `SMS`, `PUSH`, `WEBHOOK`, `TEAMS`.
- جدول `notification_channel_target` (Slice 6):
  - `channel_code` + `target_code` (مرجع).
  - `target_url` (مثل Teams incoming webhook URL).
  - **`signing_secret_ref`** = اسم متغير البيئة الذي يحوي السر **وليس السر نفسه**.
  - `enabled` لتعطيل قناة مؤقتا.
- شاشة الـ FE: `/admin/notifications/channels` — CRUD + تحقق صارم من ‏HTTPS + التحقق من تنسيق اسم متغير البيئة (لا يُقبل لصق مفتاح).
- صلاحية: `NOTIFICATION_CHANNEL_ADMIN`.

### 8.4 تفضيلات الإشعارات (Notification Preferences — Slice 6)
- جدول `notification_preference` (user × event_type × channel):
  - السلوك الافتراضي = **مفعَّل** (Opt-out صريح، لا Opt-in).
  - `NotificationOutboxService.enqueue` يستشير الجدول قبل أي ‏enqueue.
- شاشة الـ FE: `/profile/notifications` — مصفوفة Events × Channels مع toggle + save/reset + dirty-tracking.
- صلاحية: `NOTIFICATION_PREFERENCE_MANAGE` (تُمنح تلقائيا لكل دور يملك ‏`NOTIFICATION_VIEW`).

### 8.5 شاشة إدارة الـ Outbox + DLQ
- `/admin/notifications/outbox`:
  - فلتر بـ status.
  - بحث + pagination.
  - **Requeue**: يُعيد الصف إلى `PENDING` ويعيد ضبط `attempt_count = 0` و `next_attempt_at = now()`.
  - **Cancel**: يحول إلى `DEAD` يدويا (مع سبب).
  - لوحة تفاصيل: `attemptCount`, `nextAttemptAt`, `lastError`.
- صلاحية: `NOTIFICATION_CHANNEL_ADMIN`.

### 8.6 الكتالوج (Notification Catalog)
- نقطة نهاية ‏`GET /api/v1/notification-catalog`:
  - تُرجع قائمتي ‏event_types و channels بكوداتها وأسمائها العربية/الإنجليزية.
  - تستخدمها شاشات التفضيلات والقنوات حتى لا نُهاردكود الكودات في الـ FE.

---

## 9. الاستبقاء والحجز القانوني (Retention & Legal Hold — Slice 6)

### 9.1 سياسات الاستبقاء (`retention_policy`)
- لكل صنف مورد ‏(applies_to) يمكن تعريف سياسة:
  | المورد                       | السياسة الافتراضية المُهيأة |
  |-------------------------------|-----------------------------|
  | `CORRESPONDENCE`              | 7 سنوات → `HARD_DELETE`     |
  | `AUDIT_EVENT`                 | 10 سنوات → `HARD_DELETE`    |
  | `ATTACHMENT_ACCESS_LOG`       | 3 سنوات → `HARD_DELETE`     |
  | `NOTIFICATION`                | سنة → `HARD_DELETE`         |
  | `DOCUMENT_SIGNATURE`          | غير محدد (RETAIN_INDEFINITELY) |
  | `ATTACHMENT_DOWNLOAD_TOKEN`   | 30 يوما → `HARD_DELETE`     |
- `action_after` ∈ `HARD_DELETE` / `ANONYMIZE` / `RETAIN_INDEFINITELY`.
- نطاق اختياري: `correspondence_type_id` + `confidentiality_id` (سياسة مختلفة للسري).
- صلاحيات: `RETENTION_POLICY_VIEW`, `RETENTION_POLICY_MANAGE`.

### 9.2 الحجز القانوني (`legal_hold`)
- يُجمِّد سياسة الاستبقاء + يمنع الحذف على معاملة محددة (أو كل المعاملات لو ‏`correspondence_id IS NULL` = blanket hold).
- الحقول الأساسية: `reason`, `placed_by`, `placed_at`, `released_at`, `released_by`, `release_reason`.
- `LegalHoldService.assertNotHeld(corrId)` مُستدعى من ‏`CorrespondenceDeletionService` و ‏`AttachmentDeletionService` — لا يستطيع أحد حذف معاملة محتجزة قانونيا حتى لو كان admin.
- صلاحيات: `LEGAL_HOLD_VIEW`, `LEGAL_HOLD_MANAGE`.

### 9.3 وظيفة دورة الحياة (RetentionLifecycleJob)
- تعمل كل ساعة.
- تستعمل `pg_advisory_xact_lock` (مفتاح اسمي لكل سياسة) → آمنة مع instances متعددة.
- لكل سياسة:
  1. تختار الصفوف التي تجاوزت ‏`retain_for_days`.
  2. تستبعد صفوف فيها legal hold نشط.
  3. تنفذ الـ `action_after` (أو تكتفي بتسجيل ‏`SKIPPED_DRY_RUN` إذا ‏`ac.retention.dry-run=true`).
- جدول `archive_transition_log` (append-only):
  - كل قرار يُسجَّل: `applied_to`, `resource_id`, `policy_id`, `legal_hold_id`, `action`, `detail_json`.
  - `action` ∈ `HARD_DELETE` / `ANONYMIZE` / `SKIPPED_LEGAL_HOLD` / `SKIPPED_DRY_RUN` / `FAILED`.
- صلاحية: `RETENTION_LOG_VIEW`.

### 9.4 شاشات الإدارة
- `/admin/retention/policies` — قائمة + تفعيل/تعطيل (CRUD كامل مُقيَّد لمصدر الحقيقة في Flyway).
- `/admin/retention/legal-holds` — وضع حجز جديد (UUID + سبب) + رفع حجز (سبب الرفع).
- `/admin/retention/log` — قراءة-فقط مع تصفُّح مرقَّم + تفاصيل JSON منسَّقة.
- كل عملية مدمِّرة محاطة بـ confirm dialog مع تأكيد نصي.

---

## 10. التقارير + لوحات القيادة (Reports & Dashboards)

### 10.1 المؤشرات الأساسية
- إجمالي المعاملات حسب الحالة / الإدارة / النوع / الأولوية.
- متوسط زمن المعالجة (`workflow_task_duration_seconds`).
- المعاملات المتأخرة عن الـ SLA (`correspondence_sla_overdue_active`).
- معدل قراءة المعاملات (`correspondence_read_receipt`).

### 10.2 التصدير
- Excel + CSV.
- يحترم سياسة السرية: لا تظهر أي صفوف ‏SECRET/TOP_SECRET في تصدير مستخدم بدون تصاريح.
- صلاحيات: `REPORT_VIEW`, `REPORT_EXPORT`.

---

## 11. الرسائل والتعميمات (Letter Templates)

- جدول `correspondence_letter_template` (V11) فيه قوالب جاهزة:
  | الكود                   | الاستخدام                    |
  |--------------------------|------------------------------|
  | `default`                | خطاب رسمي عام                |
  | `reminder`               | خطاب تذكير                   |
  | `approval`               | إفادة بالموافقة              |
  | `rejection`              | خطاب اعتذار / رفض            |
  | `admin-circular`         | تعميم إداري                  |
  | `ministerial-circular`   | تعميم وزاري                  |
  | `no-letter`              | بدون خطاب                    |
- الـ FE يحمِّلها من API ولا يحوي HTML مهاردكوداً.

---

## 12. الإجازات والتفويض الإداري (Leave + Authority Delegation)

### 12.1 طلبات الإجازة
- موظف يقدِّم طلب إجازة (Self-service)، المدير يعتمد.
- نقاط تكامل:
  - إذا تزامنت الإجازة مع تعيينه على معاملة، النظام يقترح ‏acting overlay.

### 12.2 التفويض السلطوي (Authority Delegation — V1)
- جدول `authority_delegation` للتفويض الشامل (الإمضاء بدلا عن مدير غائب).
- بقي للأعمال الإدارية الواسعة. التفويض المُهمي ‏(task_delegation - Slice 2) أدق وأحدث.

---

## 13. النصوص والـ I18N (Internationalization)

- الـ Backend يعتمد ‏`messages_ar.properties` + `messages_en.properties` للرسائل الخدمية وعلامات الـ ‏`application/problem+json`.
- الـ FE يعتمد ‏`public/assets/i18n/ar.json` + `en.json` (Angular i18n module).
- بوابة CI: `npm run check:i18n` يفشل أي drift بين اللغتين أو مفتاح مفقود.

---

## 14. الـ Observability + التشغيل

### 14.1 الفحوصات الصحية
- `/actuator/health` — liveness + readiness.
- `/actuator/prometheus` — كل المقاييس بـ Micrometer.

### 14.2 المقاييس المُخصَّصة
| المقياس                                          | المعنى                                |
|---------------------------------------------------|----------------------------------------|
| `workflow_task_duration_seconds`                  | زمن نهاية BPMN لكل process.            |
| `correspondence_sla_breach_total{outcome}`        | عدد مرات اكتشاف ‏breach + النتيجة.    |
| `correspondence_sla_escalation_total{action}`     | عدد ‏escalation steps المُنفَّذة.     |
| `correspondence_sla_overdue_active`               | عدد الـ tasks المتأخرة الآن.           |
| `notification_outbox_dispatch_total{result}`      | نتيجة كل dispatch (success/failure/skip). |

### 14.3 سجلات التتبع
- `traceId` يُمرَّر عبر MDC ويظهر في كل log + كل ‏`application/problem+json`.

---

## 15. كيف نتجوَّل في كل ميزة (Demo Flow)

> الجدول التالي مرتب حسب أفضل تسلسل للعرض.

| # | الميزة                              | المسار                              | المستخدم الموصى       |
|----|--------------------------------------|--------------------------------------|-------------------------|
| 1  | تسجيل الدخول وتبديل الدور            | `/login`                             | `admin` ثم `manager`    |
| 2  | إنشاء معاملة وارد جديدة              | `/transactions/new`                  | `clerk`                 |
| 3  | إضافة مرفق + توقيع رقمي              | تفاصيل المعاملة → مرفقات             | `approver`              |
| 4  | إصدار QR للنسخة المطبوعة             | تفاصيل المرفق → "إصدار QR"          | `manager`               |
| 5  | فتح صفحة التحقق علنيا                | متصفح خاص → `/verify/{token}`        | بدون جلسة               |
| 6  | تفويض مهمة إلى زميل                   | `/delegations/task`                  | `staff` → `clerk`       |
| 7  | تعيين مدير بالنيابة                  | `/acting-assignments`                | `deptmgr`               |
| 8  | إنشاء/تعديل سياسة SLA                | `/admin/sla/policies`                | `admin`                 |
| 9  | شاشة تفضيلات الإشعارات الشخصية       | `/profile/notifications`              | `staff`                 |
| 10 | إدارة قنوات الإخطار + Webhook        | `/admin/notifications/channels`      | `admin`                 |
| 11 | متابعة الـ Outbox + إعادة المحاولة   | `/admin/notifications/outbox`        | `admin`                 |
| 12 | شاشة سياسات الاستبقاء                | `/admin/retention/policies`          | `admin`                 |
| 13 | وضع حجز قانوني على معاملة            | `/admin/retention/legal-holds`       | `topsecret`             |
| 14 | استعراض سجل دورة حياة الأرشيف        | `/admin/retention/log`               | `auditor`               |
| 15 | فتح معاملة TOP_SECRET                | `/transactions/c0000000-…-0005`     | فقط `topsecret`         |
| 16 | فحص محاولة فتح TOP_SECRET بدون تصاريح | نفس الرابط بمستخدم آخر               | `manager` (Forbidden)   |
| 17 | تصدير تقرير KPI                       | `/reports`                            | `auditor`               |
| 18 | عرض سجل التدقيق                       | `/admin/audit-log`                    | `auditor`               |

---

## 16. القواعد الذهبية للأمان

1. **لا سر في الـ DB.** كل ‏`signing_secret_ref` هو **اسم متغير بيئة**.
2. **لا تنزيل مباشر.** كل تنزيل يمر بـ `download-intent` + token قابل للاستهلاك مرة واحدة.
3. **لا تصدير بدون تصاريح.** السرّية تطبَّق على كل من ‏List و Export و Search.
4. **لا حذف لمعاملة محتجزة.** `LegalHoldService` يفرضها على كل المسارات المُدمِّرة.
5. **لا تكرار للإشعار.** `notification_outbox.idempotency_key` فريد.
6. **لا تلاعب في التدقيق.** سلسلة hash تكشف أي إدخال خارجي.
7. **لا تنفيذ مدمر على ‏prod بالخطأ.** Retention يبدأ بـ `ac.retention.dry-run=true` (تقرير فقط).

---

## 17. مراجع سريعة

- **بيانات تجريبية:** `srs-project/SRS_System_backend/src/main/resources/db/demo/V900__demo_data.sql`
- **قائمة المستخدمين للدخول:** [`demo-users-ar.md`](demo-users-ar.md)
- **معمارية:** [`architecture.md`](architecture.md)
- **سياسات الصلاحيات:** [`permissions-architecture.md`](permissions-architecture.md)
- **دليل التشغيل:** [`runbook.md`](runbook.md)
- **التطوير + سير العمل:** [`workflow.md`](workflow.md)
- **خطة التصلب الدفاعي (Slices 1-6):** [`enterprise-phase-defensive-hardening.md`](enterprise-phase-defensive-hardening.md)

---

> **آخر تحديث:** بعد إنجاز Slice 6 — التحقق العلني بالـ QR + محرك الاستبقاء + الإشعارات بقنوات متقدمة + ‏Tombstone لنقطة التنزيل القديمة + كل شاشات الإدارة الـ ‏FE.
