# Bugs & Issues — مسح فرونت/باك (نظام الاتصالات الإدارية)

**تاريخ المسح:** 2026-04-05  

**ملاحظة:** ملف `SRS_نظام_الاتصالات_الادارية.docx` غير موجود داخل المستودع؛ لم يُستطع مطابقة المتطلبات حرفياً من الوثيقة. العناصر أدناه مبنية على مقارنة شفرة `SRS_System_frontend` مع `SRS_System_backend`.

---

## 1) فجوات: واجهة أمامية بلا تكامل كامل مع الـ API (أو عكسه)

| الملاحظة | التفاصيل |
|----------|-----------|
| **إنشاء تعميمات (Circulars)** | الـ backend يوفّر `POST /api/v1/circulars` و`POST /api/v1/circulars/broadcast`. الخدمة `PlatformCircularApiService.create()` موجودة لكن **لا يُستدعى من أي مكوّن**؛ شاشة `/circulars` تعرض صندوق الوارد و«تمييز كمقروء» فقط — **لا واجهة لإنشاء تعميم أو بث إداري من الفرونت.** |
| **بث التعميم عبر المسار المخصص** | `POST /api/v1/circulars/broadcast` غير مستخدم في الفرونت (يمكن نظرياً استخدام `create` مع `broadcast: true`، لكن لا يوجد UI أصلاً). |
| **إرسال SMS** | الـ backend: `POST /api/v1/notifications/dispatch/sms`. الفرونت يوفّر `NotificationDispatchApiService` لـ **البريد فقط** — **لا استدعاء لمسار SMS.** |
| **سجل التدقيق (Audit)** | الـ backend: `GET/POST /api/v1/audit/events`. **لا يوجد في الفرونت** خدمة HTTP أو شاشة تستهلك أو تعرض أحداث التدقيق. |
| **مصادقة متعددة العوامل (MFA)** | `AuthApiService` يوفّر `mfaChallenge` و`mfaVerify`. شاشة `login.component.ts` عند استجابة `403` مع `MFA_REQUIRED` تعرض رسالة فقط **دون خطوة إدخال رمز أو استدعاء الـ challenge/verify** — **سير عمل MFA غير مكتمل من الواجهة.** |

---

## 2) تكرار في الفرونت (صيانة مزدوجة / استدعاءات مكررة)

| الملاحظة | التفاصيل |
|----------|-----------|
| **تاريخ سير العمل (Workflow history)** | `CorrespondenceApiService.getWorkflowHistory` و`PlatformWorkflowApiService.workflowTimeline` يضربان **نفس المسار** `GET .../correspondence/{id}/workflow-history`. الدالة `workflowTimeline` **غير مستخدمة** في أي مكوّن — تكرار في الطبقة الخدمية. |
| **`TransactionService` كغلاف** | معظم الدوال تعيد توجيهاً مباشراً إلى `CorrespondenceApiService` (إنشاء، إجراء سير عمل، تعليقات، إلخ) — طبقة إضافية قد تكرر التوقيعات دون قيمة واضحة لكل الاستدعاءات. |
| **لوحة التحكم والتقارير** | `DashboardComponent` و`ReportsComponent` يجمعان بيانات من `DashboardApiService` و`TransactionService.listPage` — تداخل محتمل في مصادر KPIs (نفس الفكرة من زوايا متعددة). |
| **تمييز الكل كمقروء (إشعارات)** | في `topbar.component.ts`، `markAllRead()` يحلق على غير المقروء ويستدعي `markRead` **لكل إشعار على حدة** — نمط N طلبات متتالية بدلاً من endpoint دفعة واحدة (إن وُجد لاحقاً في الباك). |
| **حذف/تحديث الإشعارات** | منطق مشابه لعرض القوائم والحذف يظهر في `notifications.ts` و`topbar.component.ts` (تكرار تجربة الإشعارات في مكانين). |

---

## 3) سير عمل معطوب، سلوك صامت، أو مخاطر UX

| الملاحظة | التفاصيل |
|----------|-----------|
| **MFA** | كما أعلاه — المستخدم يرى «مطلوب MFA» دون إكمال الدخول. |
| **تحميل مرفق بدون توكن** | `transaction-details.ts` (النسخة الحالية في `new_transaction_details`): `downloadWithAuth` إذا لم يوجد `token` يعمل `return` **دون رسالة للمستخدم.** |
| **فتح مستخدم في الإدارة** | `administration.component.ts`: `openViewUser` / `openEditUser` عند فشل `getOne` يستخدمان `error: () => {}` — **فشل صامت**؛ المستخدم قد يرى نموذجاً فارغاً أو قديماً. |
| **«المساعد» في الفقاعة الدردشية** | `chat-bubble.component.ts`: إجابات بناءً على **قواعد ونوايا على العميل** فوق `TransactionService.listPage()` (حتى ~500 صف)، **بلا backend للدردشة/الذكاء** — ليس خطأ شبكة لكنه ليس تكاملاً مع خادم محادثة. |

---

## 4) كود ميت / مضلل / غير موصول بالتوجيه

| الملاحظة | التفاصيل |
|----------|-----------|
| **نسخة قديمة من تفاصيل المعاملة** | المجلد `features/transaction-details/` يحتوي `TransactionDetailsComponent` بـ **بيانات تجريبية ثابتة (`// DEMO DATA`)** ولا يظهر في `app.routes.ts` (المسار `transactions/:id` يحمّل `new_transaction_details`). **مكوّنان بنفس الـ selector `app-transaction-details`** — خطر ارتباك ودمج خاطئ إن وُجد استيراد قديم. |
| **`PlatformWorkflowApiService.workflowTimeline`** | معرّف وغير مستخدم. |

---

## 5) ملخص سريع لمسارات الـ API (للمرجع)

- **مغطى من الفرونت (خدمات `core/api`) بشكل عام:** `auth`, `users`, `roles`, `departments`, `lookups`, `letter-templates`, `dashboard`, `correspondence` (+ delegate), `attachments`, `notifications` (قائمة/قراءة/حذف), `notifications/dispatch/email`, `reports/*`, `admin/*`, `circulars/inbox` + `read`, `system-issues/report`.
- **موجود في الباك دون تغطية فرونت واضحة في هذا المسح:** `audit/events`, `notifications/dispatch/sms`, إنشاء/بث التعميمات عبر UI، ومسار `circulars/broadcast` كاستدعاء مخصص.

---

*يمكن تحديث هذا الملف بعد إضافة ملف SRS إلى المستودع أو بعد تشغيل اختبارات تكامل فعلية.*
