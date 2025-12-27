import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PositionSummaryComponent } from './position-summary.component';

describe('PositionSummaryComponent', () => {
  let component: PositionSummaryComponent;
  let fixture: ComponentFixture<PositionSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PositionSummaryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PositionSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
