import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarketDepthComponent } from './market-depth.component';

describe('MarketDepthComponent', () => {
  let component: MarketDepthComponent;
  let fixture: ComponentFixture<MarketDepthComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarketDepthComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MarketDepthComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
