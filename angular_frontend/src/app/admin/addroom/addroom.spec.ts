import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Addroom } from './addroom';

describe('Addroom', () => {
  let component: Addroom;
  let fixture: ComponentFixture<Addroom>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Addroom]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Addroom);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
