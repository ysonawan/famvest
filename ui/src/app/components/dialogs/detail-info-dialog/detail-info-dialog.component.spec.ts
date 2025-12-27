import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetailInfoDialogComponent } from './detail-info-dialog.component';

describe('DetailInfoDialogComponent', () => {
  let component: DetailInfoDialogComponent;
  let fixture: ComponentFixture<DetailInfoDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetailInfoDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetailInfoDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
