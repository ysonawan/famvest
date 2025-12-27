import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MfSipTableComponent } from './mf-sip-table.component';

describe('MfSipTableComponent', () => {
  let component: MfSipTableComponent;
  let fixture: ComponentFixture<MfSipTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfSipTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MfSipTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
