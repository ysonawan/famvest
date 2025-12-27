import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SipSummaryComponent } from './sip-summary.component';

describe('SipSummaryComponent', () => {
  let component: SipSummaryComponent;
  let fixture: ComponentFixture<SipSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SipSummaryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SipSummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
