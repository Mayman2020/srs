import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { LetterTemplateAdminComponent } from './letter-template-admin.component';
import { LetterTemplateApiService } from '../../core/api/letter-template-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';

class LetterTemplateApiStub {
  listAdmin = () => of([]);
}

class I18nStub {
  currentLang = () => 'en';
  instant = (k: string) => k;
}

class DialogStub {
  openConfirm = () => of(false);
}

class ToastStub {
  error = () => {};
  success = () => {};
}

describe('LetterTemplateAdminComponent', () => {
  let fixture: ComponentFixture<LetterTemplateAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LetterTemplateAdminComponent],
      providers: [
        { provide: LetterTemplateApiService, useClass: LetterTemplateApiStub },
        { provide: I18nService, useClass: I18nStub },
        { provide: DialogService, useClass: DialogStub },
        { provide: NotificationService, useClass: ToastStub }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(LetterTemplateAdminComponent);
    fixture.detectChanges();
  });

  it('creates', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
