// src/app/components/asignaturas/asignaturas.component.ts
import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { Observable, combineLatest, of, Subject, BehaviorSubject } from 'rxjs';
import { map, startWith, catchError, takeUntil, finalize } from 'rxjs/operators';
import { Asignatura, Usuario, TipoUsuario } from '../../models/interfaces';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, AsyncPipe, NgFor, NgIf } from '@angular/common';
import Swal from 'sweetalert2';
import { AsignaturaService } from '../../services/AsignaturaService';
import { UsuarioService } from '../../services/UsuarioService';
import { AuthService } from '../../services/AuthService';
import { Modal } from 'bootstrap';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-asignaturas',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NgFor,
    NgIf,
    RouterLink,
    ReactiveFormsModule
  ],
  templateUrl: './asignaturas.component.html',
  styleUrls: ['./asignaturas.component.scss']
})
export class AsignaturasComponent implements OnInit, OnDestroy, AfterViewInit {
  // Datos reactivos
  asignaturasFiltradas: Asignatura[] = [];
  private asignaturasSubject = new BehaviorSubject<Asignatura[]>([]);

  // Estados
  isLoading = true;
  isSaving = false;
  searchControl = new FormControl('');
  currentUser: Usuario | null = null;
  tipoUsuario = TipoUsuario;

  // Formulario
  asignaturaForm = new FormGroup({
    id: new FormControl<string | null>(null),
    nombre: new FormControl('', [Validators.required, Validators.minLength(3)]),
    codigoAsignatura: new FormControl('', [Validators.required]),
    descripcion: new FormControl(''),
    creditos: new FormControl(1, [Validators.required, Validators.min(1)]),
    profesores: new FormControl<string[]>([]),
    estudiantes: new FormControl<string[]>([])
  });

  isEditMode = false;
  private destroy$ = new Subject<void>();
  private modal!: Modal;

  // Modal
  estudiantes$!: Observable<Usuario[]>;
  profesores$!: Observable<Usuario[]>;
  filteredProfesores: Usuario[] = [];
  filteredEstudiantes: Usuario[] = [];
  showProfesorSuggestions = false;
  showEstudianteSuggestions = false;

  constructor(
    private asignaturaService: AsignaturaService,
    private usuarioService: UsuarioService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (user) => {
        this.currentUser = user;
        this.loadAsignaturas();
        this.setupSearch();
      },
      error: () => {
        this.showError('Error al cargar el usuario');
        this.isLoading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    this.initModal();
  }

  private initModal(): void {
    const el = document.getElementById('asignaturaModal');
    if (el) {
      this.modal = new Modal(el, { backdrop: 'static', keyboard: false });
    }
  }
// Añade estos métodos en la clase

getIniciales(nombre: string): string {
  if (!nombre) return '??';
  return nombre
    .split(' ')
    .map(word => word[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

getColor(nombre: string): string {
  const colors = [
    '#1D4E89', '#4A90E2', '#00A6A6', '#2E8B57', '#8B5A2B',
    '#6A5ACD', '#C71585', '#DC143C', '#FF8C00', '#32CD32'
  ];
  let hash = 0;
  for (let i = 0; i < nombre.length; i++) {
    hash = nombre.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash % colors.length)];
}

getGradient(nombre: string): string {
  const base = this.getColor(nombre);
  return `linear-gradient(135deg, ${base}, ${this.lighten(base, 20)})`;
}

lighten(color: string, percent: number): string {
  const num = parseInt(color.replace('#', ''), 16);
  const amt = Math.round(2.55 * percent);
  const R = (num >> 16) + amt;
  const G = (num >> 8 & 0x00FF) + amt;
  const B = (num & 0x0000FF) + amt;
  return '#' + (0x1000000 + (R < 255 ? R : 255) * 0x10000 + (G < 255 ? G : 255) * 0x100 + (B < 255 ? B : 255)).toString(16).slice(1);
}
  loadAsignaturas(): void {
    if (!this.currentUser) {
      this.showError('Usuario no autenticado');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;

    this.asignaturaService.getAll().pipe(
      map(asignaturas => {
        if (!Array.isArray(asignaturas)) return [];
        return asignaturas
          .filter(asig => {
            if (!asig || typeof asig !== 'object') return false;
            if (this.currentUser!.tipo === TipoUsuario.administrativo) return true;
            if (this.currentUser!.tipo === TipoUsuario.estudiante) {
              return asig.estudiantes?.includes(this.currentUser!.correoInstitucional);
            }
            if (this.currentUser!.tipo === TipoUsuario.profesor) {
              return asig.profesores?.includes(this.currentUser!.correoInstitucional);
            }
            return false;
          })
          .sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''));
      }),
      catchError(err => {
        this.showError(err.message || 'Error al cargar asignaturas');
        return of([]);
      }),
      finalize(() => this.isLoading = false)
    ).subscribe(filtered => {
      this.asignaturasSubject.next(filtered);
    });

    // Filtrado reactivo
    combineLatest([
      this.asignaturasSubject.asObservable(),
      this.searchControl.valueChanges.pipe(startWith(''))
    ]).pipe(
      map(([asignaturas, search]) => {
        if (!search) return asignaturas;
        const term = search.toLowerCase();
        return asignaturas.filter(asig =>
          (asig.nombre?.toLowerCase().includes(term)) ||
          (asig.codigoAsignatura?.toLowerCase().includes(term))
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe(filtered => {
      this.asignaturasFiltradas = filtered;
    });
  }

  setupSearch(): void {
    this.searchControl.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      // Reactivo
    });
  }

  filter(event: any, usuarios: Usuario[], tipo: 'profesor' | 'estudiante'): void {
    const query = event.target.value.toLowerCase();
    const filtered = usuarios.filter(u =>
      `${u.nombre} ${u.apellido}`.toLowerCase().includes(query) ||
      u.correoInstitucional.toLowerCase().includes(query)
    );
    if (tipo === 'profesor') this.filteredProfesores = filtered;
    else this.filteredEstudiantes = filtered;
  }

  selectProfesor(email: string): void {
    const current = this.asignaturaForm.get('profesores')?.value || [];
    if (!current.includes(email)) {
      this.asignaturaForm.get('profesores')?.setValue([...current, email]);
    }
    this.showProfesorSuggestions = false;
  }

  selectEstudiante(email: string): void {
    const current = this.asignaturaForm.get('estudiantes')?.value || [];
    if (!current.includes(email)) {
      this.asignaturaForm.get('estudiantes')?.setValue([...current, email]);
    }
    this.showEstudianteSuggestions = false;
  }

  removeProfesor(email: string): void {
    const current = this.asignaturaForm.get('profesores')?.value || [];
    this.asignaturaForm.get('profesores')?.setValue(current.filter(e => e !== email));
  }

  removeEstudiante(email: string): void {
    const current = this.asignaturaForm.get('estudiantes')?.value || [];
    this.asignaturaForm.get('estudiantes')?.setValue(current.filter(e => e !== email));
  }

  getNombreUsuario(email: string, usuarios: Usuario[]): string {
    const u = usuarios.find(u => u.correoInstitucional === email);
    return u ? `${u.nombre} ${u.apellido}` : email;
  }

  private resetSuggestions(): void {
    this.filteredProfesores = [];
    this.filteredEstudiantes = [];
    this.showProfesorSuggestions = false;
    this.showEstudianteSuggestions = false;
  }

  openCreateModal(): void {
    if (this.currentUser?.tipo !== TipoUsuario.administrativo) return;
    this.isEditMode = false;
    this.asignaturaForm.reset({ nombre: '', codigoAsignatura: '', descripcion: '', creditos: 1, profesores: [], estudiantes: [] });
    this.loadUsuarios();
    this.resetSuggestions();
    this.modal.show();
  }

  openEditModal(asignatura: Asignatura): void {
    if (this.currentUser?.tipo !== TipoUsuario.administrativo) return;
    this.isEditMode = true;
    this.asignaturaForm.patchValue({
      id: asignatura.id,
      nombre: asignatura.nombre,
      codigoAsignatura: asignatura.codigoAsignatura,
      descripcion: asignatura.descripcion || '',
      creditos: asignatura.creditos,
      profesores: asignatura.profesores || [],
      estudiantes: asignatura.estudiantes || []
    });
    this.loadUsuarios();
    this.resetSuggestions();
    this.modal.show();
  }

  saveAsignatura(): void {
    if (this.asignaturaForm.invalid) {
      this.asignaturaForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const data = this.asignaturaForm.value as Asignatura;

    const action$ = this.isEditMode && data.id
      ? this.asignaturaService.update(data.id, data)
      : this.asignaturaService.create(data);

    action$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving = false;
        this.modal.hide();
        this.showSuccess(this.isEditMode ? 'Asignatura actualizada' : 'Asignatura creada');
        this.loadAsignaturas();
      },
      error: (err) => {
        this.isSaving = false;
        this.showError(err.message || 'Error al guardar');
      }
    });
  }

  private loadUsuarios(): void {
    this.estudiantes$ = this.usuarioService.getEstudiantes();
    this.profesores$ = this.usuarioService.getProfesores();
  }

  deleteAsignatura(asignatura: Asignatura): void {
    if (this.currentUser?.tipo !== TipoUsuario.administrativo || !asignatura.id) return;

    Swal.fire({
      title: '¿Eliminar?',
      text: `"${asignatura.nombre}" será eliminada`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.asignaturaService.delete(asignatura.id!).subscribe({
          next: () => {
            this.showSuccess('Asignatura eliminada');
            this.loadAsignaturas();
          },
          error: (err) => this.showError(err.message || 'Error al eliminar')
        });
      }
    });
  }

  private showError(msg: string) {
    Swal.fire({ icon: 'error', title: 'Error', text: msg });
  }

  private showSuccess(msg: string) {
    Swal.fire({ icon: 'success', title: 'Éxito', text: msg, timer: 2000, showConfirmButton: false });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
