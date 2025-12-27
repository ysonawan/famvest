import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SmallChipComponent } from './small-chip.component';

describe('SmallChipComponent', () => {
  let component: SmallChipComponent;
  let fixture: ComponentFixture<SmallChipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SmallChipComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SmallChipComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
