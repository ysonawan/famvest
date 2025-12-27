import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MfSipsComponent } from './mf-sips.component';

describe('MfSipsComponent', () => {
  let component: MfSipsComponent;
  let fixture: ComponentFixture<MfSipsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MfSipsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MfSipsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
