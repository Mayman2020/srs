# 🧪 مستخدمو البيئة التجريبية (Demo Logins)

> هذه القائمة خاصة بـ **بيئة التطوير المحلية فقط** (الـ Spring profile `local`).
> كل المستخدمين منشؤون عبر سكربت **`V900__demo_data.sql`** الذي يعمل تلقائيا عند تشغيل الـ Backend بـ profile `local` (يقرأ من `classpath:db/demo`).
> الـ profiles `prod` و `staging` و `test` لا تُحمّل هذا السكربت — البيانات التجريبية لن تتسرّب إلى بيئة الإنتاج.

كل المستخدمين هنا مفعّلون (`is_active = TRUE`) وكلمات المرور مُخزّنة باستخدام `{noop}` (نص صريح، **للتطوير فقط**) لتسهيل تجربة الواجهات.

---

## 1️⃣ قائمة الحسابات

| # | اسم المستخدم | كلمة المرور | الدور / الأدوار            | الإدارة         | الصلاحية الأمنية | الاستخدام المقصود في العرض التوضيحي |
|---|----------------|----------------|-----------------------------|------------------|------------------|----------------------------------------|
| 1 | **admin**      | `admin`        | `SYS_ADMIN`                 | الجهة (ROOT)     | `NORMAL`         | الحساب البوتستراب الموجود من قبل — يدخل كل الصفحات الإدارية. |
| 2 | **manager**    | `manager`      | `CORRESP_MGR` + `APPROVER`  | مكتب الاتصالات   | `SECRET`         | مدير مراسلات: ينشئ المعاملات، يحيلها، ويعتمدها. |
| 3 | **clerk**      | `clerk`        | `CORRESP_CLERK`             | مكتب الاتصالات   | `NORMAL`         | موظف اتصالات: تسجيل الوارد/الصادر اليومي. |
| 4 | **staff**      | `staff`        | `STAFF`                     | الموارد البشرية  | `NORMAL`         | موظف عام: يستعرض صندوق إشعاراته ويحرر تفضيلاته. |
| 5 | **approver**   | `approver`     | `APPROVER`                  | الشؤون القانونية | `SECRET`         | معتمد إداري: يعتمد المعاملات بمستوى سري ومنخفض. |
| 6 | **auditor**    | `auditor`      | `AUDITOR`                   | تقنية المعلومات  | `SECRET`         | مدقق: قراءة فقط للسجلات والتقارير + سجل التدقيق. |
| 7 | **topsecret**  | `topsecret`    | `SYS_ADMIN` + `AUDITOR`     | تقنية المعلومات  | `TOP_SECRET`     | ضابط أمن معلومات: يرى كل شيء بما فيها الـ TOP_SECRET. |
| 8 | **deptmgr**    | `deptmgr`      | `DEPT_MANAGER` + `APPROVER` | الشؤون المالية   | `SECRET`         | مدير إدارة: لوحة قيادة الإدارة + اعتماد. |

> 💡 **قاعدة كلمة المرور:** اسم المستخدم = كلمة المرور (نفس النص حرفيا).
> هذه القاعدة فقط للبيئة المحلية. عند إنشاء حسابات حقيقية في الإنتاج، الـ `DelegatingPasswordEncoder` ينتقل تلقائيا إلى `{bcrypt}` وكلمات المرور تُهَش بـ bcrypt.

---

## 2️⃣ كيف تعمل البذور (How the seed runs)

```yaml
# srs-project/SRS_System_backend/src/main/resources/application-local.yml
spring:
  flyway:
    # local فقط — هو الذي يحمّل مجلد db/demo
    locations: classpath:db/migration,classpath:db/demo
```

| البيئة (Profile) | مجلدات Flyway المُحمَّلة                  | هل يعمل V900__demo_data؟ |
|-------------------|---------------------------------------------|---------------------------|
| `local`           | `classpath:db/migration` + `classpath:db/demo` | ✅ نعم                    |
| `prod`            | `classpath:db/migration` (موروث)           | ❌ لا                     |
| `staging`         | `classpath:db/migration` (موروث)           | ❌ لا                     |
| `test`            | `classpath:db/migration` (مُصرَّح به في `application-test.yml`) | ❌ لا |

كل تعليمات `INSERT` في `V900__demo_data.sql` مُحاطة بـ `WHERE NOT EXISTS (...)`، لذا تشغيل السكربت مرات متعددة آمن — لن يحدث أي تكرار أو فشل بسبب unique constraints.

---

## 3️⃣ ماذا ستجد بعد أول تشغيل؟

بعد تشغيل الـ backend بـ profile `local`، ستجد جاهزا:

* 🏢 **5 إدارات** تحت `ROOT`: مكتب الاتصالات، الشؤون القانونية، تقنية المعلومات، الموارد البشرية، الشؤون المالية.
* 📄 **6 معاملات تجريبية** بأرقام مرجعية واقعية (`INC-2026-…`, `OUT-2026-…`, `INT-2026-…`) موزعة على أنواع وحالات وسرّيات مختلفة.
* 🔔 **2 إشعار In-App** في صناديق `staff` و `manager` (لكي يكون الجرس غير فارغ).
* 📨 **5 صفوف Outbox** تغطي جميع الحالات: `PENDING` × 2، `SENT`، `FAILED`، `DEAD`.
* 📡 **3 قنوات إخطار**: ايميل (مفعّل)، Webhook (مفعّل لـ HMAC)، Microsoft Teams (مُعطَّل).
* ⚙️ **3 تفضيلات إخطار** للمستخدم `staff` لتجربة شاشة `/profile/notifications`.
* ⚖️ **حجز قانوني نشط** على المعاملة السرية `INC-2026-0005`.
* 🗄️ **4 صفوف Archive Transition Log** بحالة DRY_RUN/SKIPPED_LEGAL_HOLD لتعبئة شاشة الاحتفاظ.

---

## 4️⃣ مصفوفة الصلاحيات الموصى بتجربتها

| الشاشة / الميزة                                  | الـ URL                                  | المستخدم الأنسب للتجربة                 |
|---------------------------------------------------|-------------------------------------------|------------------------------------------|
| لوحة القيادة الإدارية                             | `/admin/dashboard`                        | `admin` / `topsecret`                    |
| إدارة المستخدمين والأدوار                         | `/admin/users`, `/admin/roles`            | `admin`                                  |
| **تفضيلات الإشعارات** (مستخدم)                    | `/profile/notifications`                  | `staff` (مهيأ مسبقا) أو أي مستخدم        |
| **إدارة قنوات الإشعارات** (admin)                | `/admin/notifications/channels`           | `admin` / `topsecret`                    |
| **صندوق إصدار الإشعارات (Outbox + DLQ)**         | `/admin/notifications/outbox`             | `admin` / `topsecret`                    |
| **سياسات الاستبقاء**                              | `/admin/retention/policies`               | `admin`                                  |
| **الحجز القانوني**                                | `/admin/retention/legal-holds`            | `admin` / `topsecret`                    |
| **سجل دورة حياة الأرشيف**                         | `/admin/retention/archive-log`            | `auditor` / `admin`                      |
| إنشاء/إحالة معاملة                                | `/transactions/new`                        | `clerk` / `manager`                      |
| اعتماد معاملة                                     | `/transactions/{id}` → زر الاعتماد        | `approver` / `manager`                   |
| رؤية معاملة TOP_SECRET                            | `/transactions/c0000000-…-0005`           | فقط `topsecret` (الباقي يرى Forbidden)   |
| **QR / طباعة وتحقّق علني**                        | تفاصيل المعاملة → زر "إصدار QR"           | أي مستخدم لديه ATTACHMENT_VERIFY_TOKEN_ISSUE |
| **التحقق العلني** (بدون تسجيل دخول)              | `/verify/{token}`                          | متصفّح خاص بدون جلسة                     |
| التقارير + تصديرها                                 | `/reports`                                 | `auditor` / `admin`                      |
| سجل التدقيق                                        | `/admin/audit-log`                         | `auditor` / `admin`                      |

---

## 5️⃣ كيف تُشغّل البيئة (سريع)

```powershell
# 1) قاعدة بيانات نظيفة (لو احتجت)
psql -U postgres -f srs-project\docs\db\srs_system_full_clean_reset.sql

# 2) تشغيل الـ Backend بـ profile local
cd srs-project\SRS_System_backend
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run

# 3) تشغيل الـ Frontend
cd srs-project\SRS_System_frontend
npm install
npm start
# افتح http://localhost:4200 وادخل بأي مستخدم من الجدول أعلاه.
```

عند الإقلاع ستجد في الـ console سطر Flyway مشابها:

```
Migrating schema "srs_system" to version "900 - demo data"
Successfully applied 1 migration to schema "srs_system"
```

ثم تستطيع فتح أي شاشة في الجدول أعلاه ورؤية بيانات حقيقية فورا — بدون أي تهيئة إضافية.

---

## 6️⃣ احتياطات الأمان

* ⚠️ السكربت يُنشئ مستخدمين بكلمات مرور بنص صريح. **لا تستعملوا profile `local` في أي بيئة متصلة بالإنترنت.**
* 🔐 `signing_secret_ref` في الـ `notification_channel_target` يُخزَّن **اسم متغير البيئة** (مثل `AC_NOTIFICATION_WEBHOOK_SECRET`) — لا تُخزَّن السرّ الفعلي في قاعدة البيانات.
* 🛡️ المستخدم `topsecret` يملك صلاحية `TOP_SECRET`؛ يستعمل فقط لاختبار شاشة معاملة سرية للغاية.
* 🧹 لإزالة بيانات الـ demo من قاعدة بيانات محلية تخص فريقا آخر: شغّل `srs_system_full_clean_reset.sql` ثم أعد التشغيل بدون profile `local`.
