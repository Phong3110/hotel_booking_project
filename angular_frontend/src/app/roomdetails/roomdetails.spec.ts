import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Roomdetails } from './roomdetails';

describe('Roomdetails', () => {
  let component: Roomdetails;
  let fixture: ComponentFixture<Roomdetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Roomdetails]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Roomdetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
