import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateTransactionButton } from './create-transaction-button';

describe('CreateTransactionButton', () => {
  let component: CreateTransactionButton;
  let fixture: ComponentFixture<CreateTransactionButton>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateTransactionButton]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateTransactionButton);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
