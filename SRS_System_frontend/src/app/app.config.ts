import {
  ApplicationConfig,
  ApplicationRef,
  inject,
  NgZone,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners
} from '@angular/core';
import { Title } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/api/auth.interceptor';
import { systemIssueReporterInterceptor } from './core/api/system-issue-reporter.interceptor';
import { zonePatchHttpInterceptor } from './core/api/zone-patch-http.interceptor';
import { catchError, firstValueFrom, of, timeout } from 'rxjs';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor';
import { successNotificationInterceptor } from './core/interceptors/success-notification.interceptor';

import { routes } from './app.routes';
import { I18nService, AppLang } from './core/i18n/i18n.service';
import { LookupLabelsService } from './core/lookup/lookup-labels.service';
import { AuthTokenService } from './core/auth/auth-token.service';
import { ThemeService } from './core/theme/theme.service';

function readStoredLang(): AppLang {
  try {
    const s = localStorage.getItem('lang');
    if (s === 'en' || s === 'ar') return s;
  } catch {
    /* ignore */
  }
  return 'ar';
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimations(),
    provideHttpClient(
      withInterceptors([
        zonePatchHttpInterceptor,
        authInterceptor,
        successNotificationInterceptor,
        httpErrorInterceptor,
        systemIssueReporterInterceptor,
      ])
    ),
    // inject() must run synchronously in the initializer — not after await (NG0203).
    // Do not return a Promise that waits on HTTP: if i18n or API hangs, bootstrap never runs
    // (white screen; tab title may still be set from a previous partial run). Run network work
    // in the background instead — see void runInit() below.
    provideAppInitializer(() => {
      const i18n = inject(I18nService);
      const title = inject(Title);
      const authToken = inject(AuthTokenService);
      const lookups = inject(LookupLabelsService);
      const ngZone = inject(NgZone);
      const appRef = inject(ApplicationRef);
      inject(ThemeService);

      const runInit = async (): Promise<void> => {
        try {
          await firstValueFrom(
            i18n.loadLang(readStoredLang()).pipe(
              timeout(15_000),
              catchError((err: unknown) => {
                console.warn('[AppInit] i18n load failed', err);
                return of(undefined);
              })
            )
          );
        } catch {
          /* ignore */
        }
        try {
          title.setTitle(i18n.instant('app.title'));
        } catch {
          title.setTitle('Admin Communications');
        }

        if (authToken.getToken()) {
          firstValueFrom(
            lookups.load().pipe(
              timeout(25_000),
              catchError((err: unknown) => {
                console.warn('[AppInit] lookup labels load failed', err);
                return of(undefined);
              })
            )
          )
            .then(() => {
              ngZone.run(() => {
                try {
                  appRef.tick();
                } catch {
                  /* ignore */
                }
              });
            })
            .catch(() => {});
        }

        ngZone.run(() => {
          try {
            appRef.tick();
          } catch {
            /* ignore — app may not be attached yet */
          }
        });
      };

      void runInit();
    })
  ]
};


