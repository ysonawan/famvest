import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MfOrderTableComponent } from './mf-order-table.component';

describe('MfOrderTableComponent', () => {
  let component: MfOrderTableComponent;
  let fixture: ComponentFixture<MfOrderTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfOrderTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MfOrderTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
