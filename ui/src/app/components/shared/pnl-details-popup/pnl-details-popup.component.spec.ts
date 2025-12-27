import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PnlDetailsPopupComponent } from './pnl-details-popup.component';

describe('PnlDetailsPopupComponent', () => {
  let component: PnlDetailsPopupComponent;
  let fixture: ComponentFixture<PnlDetailsPopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PnlDetailsPopupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PnlDetailsPopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

