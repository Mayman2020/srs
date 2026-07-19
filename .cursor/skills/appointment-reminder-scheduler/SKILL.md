---
name: appointment-reminder-scheduler
description: >-
  Hourly/daily appointment reminder scheduler with dedup via notifications table,
  system settings for lead time, in-app + optional email. Use for Vzeeta/Clinic
  booking reminder automation.
---

# Appointment Reminder Scheduler

## Components

| Layer | Responsibility |
|-------|----------------|
| `AppointmentReminderScheduler` | `@Scheduled(cron)` entry point |
| `AppointmentReminderService` | Find due appointments, dedup, dispatch |
| `NotificationService` | In-app notification per user |
| `EmailService` | Optional patient email |
| `SystemSetting` / DB | `appointment_reminder_hours` (Vzeeta) |

## Dedup

Before sending, check:
```java
notificationRepository.existsByTypeAndReferenceIdAndUserId(
    "APPOINTMENT_REMINDER", appointmentId, userId)
```

## Vzeeta window logic

- Cron: `${vzeeta.reminders.cron:0 0 * * * *}` (hourly).
- Lead time from `appointment_reminder_hours` setting (default 24).
- Window: `[now + hours, now + hours + 59min]` on `appointmentDate` + `startTime`.
- Statuses: `PENDING`, `CONFIRMED`.
- Notify patient + doctor users; email patient if configured.

## Clinic pattern

- Daily cron `clinic.reminders.cron` for tomorrow's appointments.
- Uses `AppointmentReminderService.sendScheduledReminders(LocalDate)`.

## Config

```yaml
vzeeta.reminders.enabled: ${REMINDERS_ENABLED:true}
vzeeta.reminders.cron: ${REMINDER_CRON:0 0 * * * *}
```

## Reference

- Vzeeta: `modules/notification/scheduler/AppointmentReminderScheduler.java`
- Clinic: `modules/notification/scheduler/AppointmentReminderScheduler.java`
