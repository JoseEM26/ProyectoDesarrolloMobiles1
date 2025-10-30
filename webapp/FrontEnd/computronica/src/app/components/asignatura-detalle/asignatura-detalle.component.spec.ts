import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AsignaturaDetalleComponent } from './asignatura-detalle.component';

describe('AsignaturaDetalleComponent', () => {
  let component: AsignaturaDetalleComponent;
  let fixture: ComponentFixture<AsignaturaDetalleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AsignaturaDetalleComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AsignaturaDetalleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
