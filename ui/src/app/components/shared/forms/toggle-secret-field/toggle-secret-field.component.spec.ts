import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToggleSecretFieldComponent } from './toggle-secret-field.component';

describe('ToggleSecretFieldComponent', () => {
  let component: ToggleSecretFieldComponent;
  let fixture: ComponentFixture<ToggleSecretFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToggleSecretFieldComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToggleSecretFieldComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
