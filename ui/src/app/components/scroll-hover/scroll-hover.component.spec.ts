import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ScrollHoverComponent } from './scroll-hover.component';

describe('ScrollHoverComponent', () => {
  let component: ScrollHoverComponent;
  let fixture: ComponentFixture<ScrollHoverComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ScrollHoverComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ScrollHoverComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
