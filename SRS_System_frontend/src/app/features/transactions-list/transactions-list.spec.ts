import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionsList } from './transactions-list';

describe('TransactionsList', () => {
  let component: TransactionsList;
  let fixture: ComponentFixture<TransactionsList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransactionsList]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransactionsList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
