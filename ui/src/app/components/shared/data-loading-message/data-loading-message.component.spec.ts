import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DataLoadingMessageComponent } from './data-loading-message.component';

describe('DataLoadingMessageComponent', () => {
  let component: DataLoadingMessageComponent;
  let fixture: ComponentFixture<DataLoadingMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataLoadingMessageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DataLoadingMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
