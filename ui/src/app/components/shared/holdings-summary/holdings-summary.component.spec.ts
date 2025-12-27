import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HoldingsSummaryComponent } from './holdings-summary.component';

describe('HoldingsSummaryComponent', () => {
  let component: HoldingsSummaryComponent;
  let fixture: ComponentFixture<HoldingsSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HoldingsSummaryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HoldingsSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
