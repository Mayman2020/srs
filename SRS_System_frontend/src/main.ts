import { registerLocaleData } from '@angular/common';
import localeAr from '@angular/common/locales/ar';
import localeEn from '@angular/common/locales/en';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

registerLocaleData(localeAr);
registerLocaleData(localeEn);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
