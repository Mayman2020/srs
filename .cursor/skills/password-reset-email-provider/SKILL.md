---
name: password-reset-email-provider
description: >-
  Implement password reset delivery with EmailService + PasswordResetDeliveryService
  abstraction, optional SMS, dev log fallback when mail disabled. Use for forgot-password
  flows in Spring Boot RMS apps.
---

# Password Reset Email Provider

## Backend flow

1. `POST /auth/forgot-password` — always return generic success (no email enumeration).
2. Save `PasswordResetToken` (24h expiry, single-use).
3. Call `PasswordResetDeliveryService.deliver(user, token)`.

## PasswordResetDeliveryService

- Build link: `${PASSWORD_RESET_BASE_URL}?token={token}` (env per app).
- **Email** via `EmailService.sendOptional()` → returns `boolean`.
- **SMS** (Clinic): `SmsService.sendOptional()` when `sms_enabled` in clinic settings.
- **Fallback**: log token + link at INFO when mail/SMS off (dev only; never expose in API response).

## EmailService abstraction

```java
@ConditionalOnProperty(name = "{app}.mail.enabled", havingValue = "true")
@Bean JavaMailSender ...
```

- `ObjectProvider<JavaMailSender>` — no crash when mail disabled.
- Env: `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.

## Config keys

| App | base-url key | mail.enabled |
|-----|--------------|--------------|
| Vzeeta | `vzeeta.password-reset.base-url` | `vzeeta.mail.enabled` |
| Clinic | `clinic.password-reset.base-url` | `clinic.mail.enabled` |

## Frontend

Wire forgot-password form to API; reset page at `/auth/reset-password?token=` calls `POST /auth/reset-password`.

## Reference

- `vzeeta-backend/.../shared/mail/PasswordResetDeliveryService.java`
- `clinic-backend/.../shared/mail/PasswordResetDeliveryService.java`
