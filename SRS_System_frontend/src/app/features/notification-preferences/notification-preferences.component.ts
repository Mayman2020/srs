import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  NotificationCatalogApiService,
  NotificationCatalogItemDto
} from '../../core/api/notification-catalog-api.service';
import {
  NotificationPreferenceRowDto,
  NotificationPreferenceUpsertDto,
  NotificationPreferencesApiService
} from '../../core/api/notification-preferences-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';

interface PrefCell {
  enabled: boolean;
  dirty: boolean;
}

interface PrefRow {
  eventType: NotificationCatalogItemDto;
  channels: Record<string, PrefCell>;
}

/**
 * Slice 6 — per-user notification preferences. Renders a `events × channels` grid; toggling a
 * cell stages the change locally so the user can save the whole grid or reset it. The PUT
 * endpoint accepts the full set, so we only send rows that the catalog actually contains (the
 * server replaces the user's preferences with the incoming list).
 */
@Component({
  selector: 'app-notification-preferences',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './notification-preferences.component.html',
  styleUrl: './notification-preferences.component.scss'
})
export class NotificationPreferencesComponent implements OnInit {
  private readonly catalogApi = inject(NotificationCatalogApiService);
  private readonly prefsApi = inject(NotificationPreferencesApiService);
  private readonly toast = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<PrefRow[]>([]);
  readonly channels = signal<NotificationCatalogItemDto[]>([]);
  readonly dirty = signal(false);

  private serverSnapshot: NotificationPreferenceRowDto[] = [];

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      catalog: this.catalogApi.load(),
      prefs: this.prefsApi.list()
    }).subscribe({
      next: ({ catalog, prefs }) => {
        this.serverSnapshot = prefs;
        this.channels.set(catalog.channels);
        this.rows.set(this.buildGrid(catalog.eventTypes, catalog.channels, prefs));
        this.dirty.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.rows.set([]);
        this.channels.set([]);
        this.error.set('errors.generic');
        this.loading.set(false);
      }
    });
  }

  toggle(row: PrefRow, channelCode: string): void {
    const cell = row.channels[channelCode];
    if (!cell) {
      return;
    }
    cell.enabled = !cell.enabled;
    const baseline = this.lookupBaseline(row.eventType.code, channelCode);
    cell.dirty = cell.enabled !== baseline;
    this.recomputeDirty();
  }

  reset(): void {
    const grid = this.rows();
    for (const row of grid) {
      for (const channel of this.channels()) {
        const baseline = this.lookupBaseline(row.eventType.code, channel.code);
        const cell = row.channels[channel.code];
        if (cell) {
          cell.enabled = baseline;
          cell.dirty = false;
        }
      }
    }
    this.rows.set([...grid]);
    this.dirty.set(false);
  }

  save(): void {
    if (this.saving() || !this.dirty()) {
      return;
    }
    const body: NotificationPreferenceUpsertDto[] = [];
    for (const row of this.rows()) {
      for (const channel of this.channels()) {
        const cell = row.channels[channel.code];
        if (!cell) {
          continue;
        }
        body.push({
          eventTypeCode: row.eventType.code,
          channelCode: channel.code,
          enabled: cell.enabled
        });
      }
    }
    this.saving.set(true);
    this.prefsApi.replace(body).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('notificationAdmin.prefs.savedToast');
        this.reload();
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('notificationAdmin.prefs.saveFailedToast');
      }
    });
  }

  labelFor(item: NotificationCatalogItemDto): string {
    return this.i18n.currentLang() === 'en' ? item.nameEn : item.nameAr;
  }

  private buildGrid(
    eventTypes: NotificationCatalogItemDto[],
    channels: NotificationCatalogItemDto[],
    prefs: NotificationPreferenceRowDto[]
  ): PrefRow[] {
    const byKey = new Map<string, boolean>();
    for (const p of prefs) {
      byKey.set(this.key(p.eventTypeCode, p.channelCode), p.enabled);
    }
    return eventTypes.map((eventType) => {
      const channelsMap: Record<string, PrefCell> = {};
      for (const channel of channels) {
        const enabled = byKey.get(this.key(eventType.code, channel.code));
        channelsMap[channel.code] = {
          enabled: enabled ?? this.defaultFor(channel.code),
          dirty: false
        };
      }
      return { eventType, channels: channelsMap };
    });
  }

  private defaultFor(channelCode: string): boolean {
    const c = channelCode.toUpperCase();
    return c === 'IN_APP' || c === 'EMAIL';
  }

  private lookupBaseline(eventTypeCode: string, channelCode: string): boolean {
    const key = this.key(eventTypeCode, channelCode);
    const hit = this.serverSnapshot.find(
      (p) => this.key(p.eventTypeCode, p.channelCode) === key
    );
    return hit ? hit.enabled : this.defaultFor(channelCode);
  }

  private recomputeDirty(): void {
    const any = this.rows().some((row) =>
      Object.values(row.channels).some((c) => c.dirty)
    );
    this.dirty.set(any);
  }

  private key(eventTypeCode: string, channelCode: string): string {
    return `${eventTypeCode}|${channelCode}`;
  }
}
