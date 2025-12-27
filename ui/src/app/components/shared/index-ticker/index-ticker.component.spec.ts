import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IndexTickerComponent } from './index-ticker.component';

describe('IndexTickerComponent', () => {
  let component: IndexTickerComponent;
  let fixture: ComponentFixture<IndexTickerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IndexTickerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IndexTickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
