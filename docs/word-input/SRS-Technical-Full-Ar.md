# الوثيقة التقنية الكاملة — نظام SRS Correspondence Management

**الإصدار:** مواءَم مع الكود الحالي (Spring Boot 3 + Angular 21 + Slice 6)  
**المخطط الافتراضي:** `srs_system` على PostgreSQL  
**الغرض:** مرجع تقني واحد للمطورين، التشغيل، والتكامل.

---

## 1. المكدس والإصدارات

| الطبقة | التقنية | ملاحظات |
|--------|---------|---------|
| Backend | Java 17، Spring Boot 3، Hibernate 6 | `ddl-auto: validate` |
| ORM migrations | Flyway 10 | المسار: `classpath:db/migration` (+ `classpath:db/demo` في profile `local` فقط) |
| DB | PostgreSQL 15+ | JDBC URL يجب أن يضبط `currentSchema=srs_system` |
| Workflow | Camunda BPM 7.22 | الجداول في نفس المخطط؛ `table-prefix: srs_system.` |
| Frontend | Angular 21 (standalone)، Signals حيث ينطبق | `HttpClient`، guards على المسارات |
| Build FE | npm، TypeScript 5.9 | بوابات: `check:i18n`, `check:routes`, `tsc` |
| Metrics | Micrometer + Prometheus | `/actuator/prometheus` |
| Security | Spring Security + JWT HS256 | `DelegatingPasswordEncoder` للكلمات |

---

## 2. هيكل المستودع (Repository layout)

```
srs-project/
├── SRS_System_backend/          ← تطبيق Spring Boot الوحيد (JAR)
│   └── src/main/java/com/gov/ac/
│       ├── common/              audit, exceptions, i18n
│       ├── config/              Security, JPA, Flyway
│       ├── security/            JWT, EffectivePermission
│       └── feature/             auth, users, roles, correspondence, workflow, attachment, …
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-local.yml
│       ├── application-prod.yml
│       └── db/migration/        V1 … V21 (+ db/demo/V900 للبيانات التجريبية محلياً)
├── SRS_System_frontend/         ← Angular SPA
│   └── src/app/
│       ├── app.routes.ts
│       ├── core/                API services, constants
│       └── features/            شاشات الميزات
└── docs/                        architecture, workflow, runbook, permissions, …
```

---

## 3. المعمارية البرمجية

- **Monolith معياري:** حدود الحزم مفروضة بـ ArchUnit (`ModuleBoundaryArchTest`).
- **Controllers → DTOs فقط:** لا تعرض الـ Entities على الشبكة.
- **كل endpoint في feature package:** يحمل `@PreAuthorize` (مستوى class أو method) ما عدا المسارات العامة المصرّح بها في `SecurityConfig`.
- **الاتساق:** `RestExceptionHandler` يعيد `application/problem+json` مع `traceId`.

---

## 4. الأمان (Security)

### 4.1 JWT
- التوقيع HS256؛ السر من `AC_JWT_SECRET` في الإنتاج (لا قيمة افتراضية آمنة في prod).
- الـ JWT يحمل دوراً نشطاً للعرض؛ **مصدر التفويض الفعلي** هو union صلاحيات المستخدم من DB (انظر `EffectiveUserPermissionService`).

### 4.2 RBAC
- صلاحيات canonical: `CORRESPONDENCE_VIEW`, `NOTIFICATION_CHANNEL_ADMIN`, …
- `permission_alias` يربط الرموز القديمة (`correspondence.view`) بالـ canonical.
- تعبير Spring: `@PreAuthorize("@effectivePermission.has('CODE')")`.

### 4.3 السرية والتصاريح
- `app_user.security_clearance_id` مقابل `confidentiality.requires_clearance`.
- تُفرض على التفاصيل والقوائم والتصدير.

### 4.4 CORS و HSTS
- `application-local.yml`: أنماط أصول محلية، HSTS معطّل.
- `application-prod.yml`: CORS من env؛ HSTS مفعّل.

---

## 5. قاعدة البيانات والهجرة (Flyway)

| الإصدار | الموضوع |
|---------|---------|
| V1 | Baseline كامل للمخطط + بذور أساسية + مستخدم admin |
| V2 | Camunda schema |
| V3–V6 | إصلاحات عربية، مستويات تنظيم، مستلمون |
| V7 | توحيد الصلاحيات (canonical + alias + منح أدوار) |
| V8–V12 | workflow، SLA مبكر، قوالب خطابات، مسارات shell |
| V13 | مسارات UI canonical + `ui_screen` |
| V14 | read tracking + `attachment_access_log` |
| V15 | `task_delegation` |
| V16 | محرك SLA (`sla_policy`, `sla_escalation_step`, `sla_breach_event`) |
| V17 | `acting_assignment` |
| V18 | مرفقات مصنّفة + توقيع + `attachment_download_token` |
| V19 | `attachment_verification_token` + access log |
| V20 | retention + legal hold + `archive_transition_log` |
| V21 | notification outbox + preferences + channel targets + صلاحيات Slice 6 |
| V900 (db/demo) | بيانات تجريبية — **profile local فقط** |

---

## 6. واجهات REST رئيسية (أمثلة غير شاملة)

| المجال | مسار تقريبي | ملاحظات |
|--------|-------------|---------|
| Auth | `/api/v1/auth/login`, refresh | permitAll للـ login |
| Correspondence | `/api/v1/correspondences` … | CRUD + بحث |
| Workflow tasks | `/api/v1/workflow/...` | مهام Camunda |
| Attachments | `/api/v1/attachments/.../download-intent` | توكن لمرة واحدة |
| Legacy download | `GET …/attachments/{id}/download` | **410 Gone** tombstone |
| Public verify | `GET /api/v1/public/verify/{token}` | permitAll + rate limit |
| Notification prefs | `GET/PUT /api/v1/me/notification-preferences` | مصادق |
| Notification catalog | `GET /api/v1/notification-catalog` | مصادق |
| Channel targets | `/api/v1/notification-channel-targets` | admin |
| Outbox admin | `/api/v1/notification-outbox-admin/...` | admin |
| Retention | `/api/v1/retention/...` | سياسات، حجز، سجل |

التفاصيل الدقيقة: OpenAPI/Swagger عند تفعيل profile (مثلاً local).

---

## 7. الواجهة الأمامية (Angular)

### 7.1 التوجيه والحماية
- `app.routes.ts`: `canMatch: permissionCanMatch` مع `data.permission`.
- سكربت CI `check-route-permissions.js` يطابق الصلاحيات مع `ui_screen` في DB.

### 7.2 الترجمة
- `public/assets/i18n/en.json` و `ar.json`.
- سكربت `check-i18n-keys.js` يمنع مفاتيح ناقصة.

### 7.3 شاشات Slice 6 (مسارات تقريبية)
- `/profile/notifications` — تفضيلات الإشعارات
- `/admin/notifications/channels` — أهداف القنوات
- `/admin/notifications/outbox` — صندوق الإصدار
- `/admin/retention/policies` — سياسات الاستبقاء
- `/admin/retention/legal-holds` — الحجز القانوني
- `/admin/retention/archive-log` — السجل
- `/verify/:token` — تحقق علني (بدون تسجيل دخول)

---

## 8. الإشعارات — التصميم التقني (Slice 6)

- **جدول `notification_outbox`:** طابور دائم؛ `idempotency_key` فريد.
- **التوزيع:** `FOR UPDATE SKIP LOCKED` لعدة نسخ backend.
- **مزودو القنوات:** SPI `NotificationChannelProvider` — تنفيذات IN_APP, EMAIL, WEBHOOK, TEAMS.
- **التوقيع:** رأس `X-AC-Signature: hmac-sha256=<base64>` على جسم JSON خام.
- **الأسرار:** `signing_secret_ref` = اسم متغير بيئة فقط.
- **التفضيلات:** `notification_preference` — opt-out صريح.
- **التوجيه:** `ac.notification.routing` = `outbox` | `inline`.

---

## 9. الاستبقاء — التصميم التقني (Slice 6)

- **`RetentionLifecycleJob`:** دورية (ساعة)، advisory lock لكل سياسة.
- **`ac.retention.dry-run`:** عند true تُسجَّل `SKIPPED_DRY_RUN` بدون حذف.
- **`legal_hold`:** يمنع الحذف عبر `LegalHoldService.assertNotHeld`.
- **`archive_transition_log`:** append-only لكل قرار.

---

## 10. المرفقات والتشفير والتوقيع (Slice 5–6)

- **التشفير:** AES-256-GCM؛ مراجع المفاتيح عبر env (`KeyProvider`).
- **التوقيع:** ED25519 افتراضياً؛ جدول `document_signature`.
- **تنزيل آمن:** hash فقط للتوكن في DB؛ استهلاك لمرة واحدة.
- **تحقق QR:** hash فقط؛ DTO علني مُنقّى؛ `attachment_verification_access_log`.

---

## 11. المراقبة والتشغيل (Observability)

- **Health:** `/actuator/health` (liveness/readiness في prod).
- **Prometheus:** مقاييس workflow و SLA و outbox (حسب التطبيق).
- **Logging:** `traceId` في MDC وفي استجابات الأخطاء.

---

## 12. البيئات (Profiles) والمتغيرات

| Profile | الاستخدام |
|---------|-----------|
| `local` | تطوير؛ Flyway يشمل `db/demo`؛ Swagger مفتوح غالباً |
| `staging` | ما قبل الإنتاج |
| `prod` | إنتاج؛ أعلام أمان صارمة |

متغيرات إلزامية تقريباً في prod (انظر `runbook.md`):  
`AC_JWT_SECRET`, `SPRING_DATASOURCE_*`, `AC_CORS_ALLOWED_ORIGIN_PATTERNS`, `CAMUNDA_BPM_ADMIN_PASSWORD`, `AC_STORAGE_ROOT`, …

---

## 13. البناء والاختبارات (Gates)

**Backend:**  
`mvnw -B test` — ArchUnit، وحدات، اختبارات تكامل (قد تتخطى Testcontainers إن لم يتوفر Docker).

**Frontend:**  
`npm run check:i18n`  
`npm run check:routes`  
`npx tsc --noEmit -p tsconfig.app.json`

---

## 14. التكامل مع أنظمة خارجية

- **SMTP:** `JavaMailSender` — إعدادات في `application.yml` / env.
- **Webhook / Teams:** POST HTTPS؛ التحقق من التوقيع على المستقبل.
- **HSM / PKI:** واجهات SPI (`KeyProvider`, `SigningKeyProvider`) جاهزة للتوسعة.

---

## 15. الأداء والتزامن

- فهارس DB على مفاتيح البحث الشائعة (معاملات، outbox queue، token hash).
- تجنب N+1 عبر استعلامات مجمّعة حيث طُبّق ذلك في الخدمات الحرجة.
- معالجة outbox متوازية آمنة بـ SKIP LOCKED.

---

## 16. النسخ الاحتياطي والاستعادة

- نسخ احتياطي لقاعدة PostgreSQL كاملة (المخطط `srs_system`).
- عند drift في Flyway history: تشغيل `docs/db/srs_system_full_clean_reset.sql` (تطوير فقط) ثم إعادة الهجرة.

---

## 17. استكشاف الأخطاء الشائعة

| العرض | السبب المحتمل |
|--------|----------------|
| 403 على معاملة | تصريح سرية أو صلاحية أو نطاق إدارة |
| Outbox DEAD | فشل HTTP متكرر أو سرّ Teams/Webhook غير مضبوط |
| Flyway validate error | تعارض بين الكود والـ DB — إصلاح التاريخ أو إعادة baseline |
| Docker tests skipped | بيئة بدون Docker — طبيعي في بعض الأجهزة |

---

## 18. مراجع الوثائق التفصيلية (لغة إنجليزية في المستودع)

- `docs/architecture.md` — المعمارية الإنجليزية الرسمية  
- `docs/workflow.md` — Camunda بالتفصيل  
- `docs/runbook.md` — التشغيل والإنتاج  
- `docs/permissions-architecture.md` — RBAC  
- `docs/enterprise-phase-defensive-hardening.md` — شرائح التصلب  
- `docs/business-logic-ar.md` — منطق الأعمال بالعربية  
- `docs/demo-users-ar.md` — مستخدمي التجربة المحلية  

---

## 19. توليد ملفات Word

من المجلد `docs/word-build`:  
`npm install` ثم `npm run build`  
يُنشئ ملفات `.docx` في `docs/word-output/`.
