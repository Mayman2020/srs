---
name: rms-dates-property-style
description: >-
  Date/time display and pickers like Property_Managments — dd/MM/yyyy, Latin digits,
  Material Datepicker + DateFormatAdapter, RmsDatePipe. Use when fixing dates in
  Arabic/RTL, reservation forms, tables, notifications, or any RMS/Property Angular app.
---

# RMS Dates (Property Pattern)

Reference: `Property_Managments/property-frontend` + `restaurant-frontend` implementation.

## Rules

1. **Never** use `type="datetime-local"` in RTL apps — use Material datepicker + separate `type="time"`.
2. **Never** use `date:'short'` in templates — use `rmsDate` pipe or explicit `dd/MM/yyyy`.
3. **Always** show Western digits (0–9) even when UI language is Arabic.
4. Backend + frontend ports must match (`8083` for RMS); `environment.ts` and `runtime-config.js` must agree.

## Backend (Spring Boot)

`app.config.ts` / `application` providers:

```typescript
{ provide: LOCALE_ID, useValue: 'en-US' },
{ provide: MAT_DATE_LOCALE, useValue: 'en-GB' },
{ provide: DateAdapter, useClass: DateFormatAdapter },
{ provide: MAT_DATE_FORMATS, useValue: DD_MM_YYYY_DATE_FORMATS },
```

`DateFormatAdapter` parses/formats `dd/MM/yyyy` via `NativeDateAdapter` + regex `(\d{1,2})/(\d{1,2})/(\d{4})`.

## Frontend locale-format

File: `src/app/core/i18n/locale-format.ts`

- `ARABIC_LATIN_DIGITS_LANG = 'ar-u-nu-latn'` (not `latnb` typo)
- `formatDateTimeLatin()` with `toLatinDigits()` on output
- `DATE_DISPLAY_OPTIONS` / `DATE_TIME_DISPLAY_OPTIONS` for dd/MM/yyyy

`I18nService.formatDate()` / `formatDateTime()` delegate to locale-format.

## RmsDatePipe

File: `src/app/shared/pipes/rms-date.pipe.ts`

```html
{{ value | rmsDate:'date' }}
{{ value | rmsDate:'datetime' }}
```

`pure: false` so language switches refresh display.

## Form dialogs (date + time)

File: `src/app/shared/utils/date-utils.ts`

- `combineDateAndTime(date, 'HH:mm')` → `Date`
- `toApiLocalDateTime(date)` → `yyyy-MM-ddTHH:mm:ss`
- `defaultReservationDate()` / `defaultTimeSlot()`

Reservation dialog pattern:

```html
<input matInput [matDatepicker]="picker" formControlName="reservedDate">
<mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
<input matInput type="time" formControlName="reservedTime">
```

Imports: `MatDatepickerModule`, `MatNativeDateModule`.

## i18n keys

- `COMMON.DATE`, `COMMON.TIME`, `COMMON.INVALID_DATE`

## Checklist

- [ ] All templates use `rmsDate` not Angular `date` pipe
- [ ] Dialog forms use `mat-datepicker`, not `datetime-local`
- [ ] `locale-format.ts` uses `ar-u-nu-latn`
- [ ] API sends `toApiLocalDateTime()` for datetime fields
