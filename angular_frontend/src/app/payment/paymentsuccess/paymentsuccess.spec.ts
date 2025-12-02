import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Paymentsuccess } from './paymentsuccess';

describe('Paymentsuccess', () => {
  let component: Paymentsuccess;
  let fixture: ComponentFixture<Paymentsuccess>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Paymentsuccess]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Paymentsuccess);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
