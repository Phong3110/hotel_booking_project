import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Roomsearch } from './roomsearch';

describe('Roomsearch', () => {
  let component: Roomsearch;
  let fixture: ComponentFixture<Roomsearch>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Roomsearch]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Roomsearch);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
