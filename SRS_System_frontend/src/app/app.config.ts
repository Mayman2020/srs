import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { I18nService, AppLang } from './core/i18n/i18n.service';
import { LookupLabelsService } from './core/lookup/lookup-labels.service';

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
    provideHttpClient(withFetch()),
    provideAppInitializer(async () => {
      const i18n = inject(I18nService);
      await firstValueFrom(i18n.loadLang(readStoredLang()));
      const lookups = inject(LookupLabelsService);
      await firstValueFrom(lookups.load());
    })
  ]
};



