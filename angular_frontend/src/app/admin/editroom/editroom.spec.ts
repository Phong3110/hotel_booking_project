import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Editroom } from './editroom';

describe('Editroom', () => {
  let component: Editroom;
  let fixture: ComponentFixture<Editroom>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Editroom]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Editroom);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
