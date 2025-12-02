import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Managebookings } from './managebookings';

describe('Managebookings', () => {
  let component: Managebookings;
  let fixture: ComponentFixture<Managebookings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Managebookings]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Managebookings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
