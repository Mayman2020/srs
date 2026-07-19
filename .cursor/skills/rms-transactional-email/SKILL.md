---
name: rms-transactional-email
description: >-
  Optional transactional EmailService for Spring Boot RMS apps — mail.enabled
  gate, ObjectProvider JavaMailSender, sendOptional, wire beside in-app
  notifications. Use when adding reminder/result emails or general mail beyond
  password-reset. Property scaffolds EMAIL channel only; Clinic is the sender
  reference.
---

# RMS Transactional Email

**Sender reference:** `Clinic System/clinic-backend`  
- `shared/mail/EmailService.java`  
- `config/MailConfig.java`  

**Password-reset only:** keep using `password-reset-email-provider` — do not duplicate that flow here.

**Property note:** in-app notifications are primary (`estate-in-app-notifications`). Property has `NotificationChannelType.EMAIL` + DB templates scaffold, but **no** SMTP sender yet — port `EmailService` from Clinic when email delivery is required.

## EmailService pattern

```java
@Service
public class EmailService {
  private final ObjectProvider<JavaMailSender> mailSender;
  @Value("${app.mail.enabled:false}") boolean enabled;
  @Value("${app.mail.from:noreply@app.local}") String from;

  /** Never throws to callers — returns false if disabled/misconfigured/failed. */
  public boolean sendOptional(String to, String subject, String body) { … }
}
```

Mail bean:

```java
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@Bean JavaMailSender javaMailSender(…) { … }
```

## Env / config

| Variable | Purpose |
|----------|---------|
| `MAIL_ENABLED` / `{app}.mail.enabled` | Master switch (default false in dev) |
| `MAIL_HOST` `MAIL_PORT` | SMTP |
| `MAIL_USERNAME` `MAIL_PASSWORD` | Auth |
| `MAIL_FROM` | From address |

Never bake production SMTP secrets into `application.yml` — see `environment-variable-hardening`.

## With notifications

1. Always create **IN_APP** notification (Property pattern).  
2. If channel/template includes EMAIL and user has email → `emailService.sendOptional(...)`.  
3. Failures log WARN; do **not** roll back the in-app notification.  
4. Subjects/bodies: i18n or bilingual — same keys as inbox when possible.

## Call sites (examples)

| Use | Pattern |
|-----|---------|
| Appointment reminder | Clinic `AppointmentReminderService` |
| Lab result ready | Clinic `LabService` |
| Password reset | `password-reset-email-provider` |

## Do not

- Crash boot when mail is off
- Return reset-token / SMTP errors to anonymous APIs
- Invent Property-only mail code without adapting Clinic `EmailService`
