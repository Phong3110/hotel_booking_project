import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Managerooms } from './managerooms';

describe('Managerooms', () => {
  let component: Managerooms;
  let fixture: ComponentFixture<Managerooms>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Managerooms]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Managerooms);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
