import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddManageStraddleStrategyComponent } from './add-manage-straddle-strategy.component';

describe('AddTradingAccountComponent', () => {
  let component: AddManageStraddleStrategyComponent;
  let fixture: ComponentFixture<AddManageStraddleStrategyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AddManageStraddleStrategyComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AddManageStraddleStrategyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
