import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Updatebooking } from './updatebooking';

describe('Updatebooking', () => {
  let component: Updatebooking;
  let fixture: ComponentFixture<Updatebooking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Updatebooking]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Updatebooking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
