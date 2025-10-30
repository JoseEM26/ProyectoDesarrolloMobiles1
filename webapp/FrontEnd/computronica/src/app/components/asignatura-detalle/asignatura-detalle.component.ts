// src/app/components/asignaturas/asignatura-detalle.component.ts
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, combineLatest, of } from 'rxjs';
import { map, switchMap, catchError, take } from 'rxjs/operators';
import { Asignatura, Usuario, TipoUsuario, Tema } from '../../models/interfaces';
import { AsignaturaService } from '../../services/AsignaturaService';
import { UsuarioService } from '../../services/UsuarioService';
import { AuthService } from '../../services/AuthService';
import Swal from 'sweetalert2';
import { TemaService } from '../../services/TemaService';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-asignatura-detalle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './asignatura-detalle.component.html',
  styleUrls: ['./asignatura-detalle.component.scss']
})
export class AsignaturaDetalleComponent implements OnInit {
  asignatura$!: Observable<Asignatura | null>;
  profesores$!: Observable<Usuario[]>;
  estudiantes$!: Observable<Usuario[]>;
  temas$!: Observable<Tema[]>;
  currentUser: Usuario | null = null;
  tipoUsuario = TipoUsuario;
  isAdmin = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private asignaturaService: AsignaturaService,
    private usuarioService: UsuarioService,
    private temaService: TemaService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.pipe(take(1)).subscribe(user => {
      this.currentUser = user;
      this.isAdmin = user?.tipo === TipoUsuario.administrativo;
    });

    const id$ = this.route.paramMap.pipe(
      map(params => params.get('id')),
      catchError(() => of(null))
    );

    this.asignatura$ = id$.pipe(
      switchMap(id => id ? this.asignaturaService.getById(id) : of(null)),
      catchError(() => of(null))
    );

    this.profesores$ = this.asignatura$.pipe(
      switchMap(asig => {
        if (!asig?.profesores?.length) return of([]);
        return combineLatest(
          asig.profesores.map(email => this.usuarioService.filterByEmail(email))
        ).pipe(map(arrays => arrays.flat()));
      })
    );

    this.estudiantes$ = this.asignatura$.pipe(
      switchMap(asig => {
        if (!asig?.estudiantes?.length) return of([]);
        return combineLatest(
          asig.estudiantes.map(email => this.usuarioService.filterByEmail(email))
        ).pipe(map(arrays => arrays.flat()));
      })
    );

    this.temas$ = id$.pipe(
      switchMap(id => id ? this.temaService.getByAsignaturaId(id) : of([])),
      map(temas => temas.sort((a, b) => (a.fechaCreacion || 0) > (b.fechaCreacion || 0) ? 1 : -1))
    );
  }

  volver(): void {
    this.router.navigate(['/subjects']);
  }

  // === CRUD TEMAS (solo admin) ===
  openCreateTemaModal(): void {
    Swal.fire({
      title: 'Nuevo Tema',
      html: `
        <input id="nombre" class="swal2-input" placeholder="Nombre del tema" required>
        <textarea id="descripcion" class="swal2-textarea" placeholder="Descripción (opcional)"></textarea>
      `,
      showCancelButton: true,
      confirmButtonText: 'Crear',
      preConfirm: () => {
        const nombre = (document.getElementById('nombre') as HTMLInputElement).value;
        const descripcion = (document.getElementById('descripcion') as HTMLTextAreaElement).value;
        if (!nombre) {
          Swal.showValidationMessage('El nombre es obligatorio');
          return false;
        }
        return { nombre, descripcion };
      }
    }).then(result => {
      if (result.isConfirmed && this.route.snapshot.paramMap.get('id')) {
        const tema: Omit<Tema, 'id' | 'estado' | 'fechaCreacion'> = {
          asignaturaId: this.route.snapshot.paramMap.get('id')!,
          nombre: result.value.nombre,
          descripcion: result.value.descripcion
        };
        this.temaService.create(tema).subscribe({
          next: () => {
            Swal.fire('¡Creado!', 'Tema agregado.', 'success');
            this.ngOnInit(); // recargar
          },
          error: () => Swal.fire('Error', 'No se pudo crear el tema', 'error')
        });
      }
    });
  }

  editarTema(tema: Tema): void {
    console.log(tema.asignaturaId + "    " + tema.asignaturaId +"DEPURAR")
    Swal.fire({
      title: 'Editar Tema',
      html: `
        <input id="nombre" class="swal2-input" value="${tema.nombre}" required>
        <textarea id="descripcion" class="swal2-textarea">${tema.descripcion || ''}</textarea>
      `,
      showCancelButton: true,
      confirmButtonText: 'Guardar',
      preConfirm: () => {
        const nombre = (document.getElementById('nombre') as HTMLInputElement).value;
        if (!nombre) {
          Swal.showValidationMessage('El nombre es obligatorio');
          return false;
        }
        return { nombre, descripcion: (document.getElementById('descripcion') as HTMLTextAreaElement).value };
      }
    }).then(result => {
      if (result.isConfirmed && tema.id) {
        this.temaService.update(tema.id, {
          nombre: result.value.nombre,
          descripcion: result.value.descripcion
        }).subscribe({
          next: () => {
            Swal.fire('¡Actualizado!', '', 'success');
            this.ngOnInit();
          },
          error: () => Swal.fire('Error', 'No se pudo actualizar', 'error')
        });
      }
    });
  }

  eliminarTema(tema: Tema): void {
    Swal.fire({
      title: '¿Eliminar tema?',
      text: `"${tema.nombre}" será eliminado`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Eliminar'
    }).then(result => {
      if (result.isConfirmed && tema.id) {
        this.temaService.delete(tema.id).subscribe({
          next: () => {
            Swal.fire('Eliminado', '', 'success');
            this.ngOnInit();
          },
          error: () => Swal.fire('Error', 'No se pudo eliminar', 'error')
        });
      }
    });
  }
}
