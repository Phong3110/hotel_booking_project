import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Paymentfailure } from './paymentfailure';

describe('Paymentfailure', () => {
  let component: Paymentfailure;
  let fixture: ComponentFixture<Paymentfailure>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Paymentfailure]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Paymentfailure);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
