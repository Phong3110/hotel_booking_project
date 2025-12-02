import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Findbooking } from './findbooking';

describe('Findbooking', () => {
  let component: Findbooking;
  let fixture: ComponentFixture<Findbooking>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Findbooking]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Findbooking);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
