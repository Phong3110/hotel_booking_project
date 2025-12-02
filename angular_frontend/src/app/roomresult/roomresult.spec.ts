import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Roomresult } from './roomresult';

describe('Roomresult', () => {
  let component: Roomresult;
  let fixture: ComponentFixture<Roomresult>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Roomresult]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Roomresult);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
