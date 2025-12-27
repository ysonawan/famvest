import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddTradingAccountComponent } from './add-trading-account.component';

describe('AddTradingAccountComponent', () => {
  let component: AddTradingAccountComponent;
  let fixture: ComponentFixture<AddTradingAccountComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddTradingAccountComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddTradingAccountComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
