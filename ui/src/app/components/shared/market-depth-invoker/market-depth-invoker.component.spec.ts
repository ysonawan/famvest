import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MarketDepthInvokerComponent } from './market-depth-invoker.component';

describe('MarketDepthInvokerComponent', () => {
  let component: MarketDepthInvokerComponent;
  let fixture: ComponentFixture<MarketDepthInvokerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarketDepthInvokerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MarketDepthInvokerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
