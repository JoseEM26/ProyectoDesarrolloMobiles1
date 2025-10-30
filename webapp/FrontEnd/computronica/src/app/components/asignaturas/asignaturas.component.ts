// src/app/components/asignaturas/asignaturas.component.ts
import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { Observable, combineLatest, of, Subject, BehaviorSubject } from 'rxjs';
import { map, startWith, catchError, takeUntil, finalize } from 'rxjs/operators';
import { Asignatura, Usuario, TipoUsuario, Calificaciones, TipoEvaluacion, Tema } from '../../models/interfaces';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule, AsyncPipe, NgFor, NgIf, KeyValuePipe } from '@angular/common';
import Swal from 'sweetalert2';
import { AsignaturaService } from '../../services/AsignaturaService';
import { UsuarioService } from '../../services/UsuarioService';
import { AuthService } from '../../services/AuthService';
import { CalificacionesService } from '../../services/CalificacionesService';
import { Modal } from 'bootstrap';
import { TIPOS_EVALUACION } from '../../models/tipos-evaluacion';
import { RouterLink } from "@angular/router";
import { TemaService } from '../../services/TemaService';

@Component({
  selector: 'app-asignaturas',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NgFor,
    NgIf,
    ReactiveFormsModule,
    KeyValuePipe,
    RouterLink
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
// En la clase del componente
estudiantesMapa: { [email: string]: Usuario } = {};

  // Formulario de asignatura
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

  // --- NUEVO: DETALLE DE ASIGNATURA ---
  selectedAsignatura: Asignatura | null = null;
  calificaciones: Calificaciones[] = [];
  isLoadingCalificaciones = false;
 calificacionForm = new FormGroup({
  id: new FormControl<string | null>(null),
  estudianteId: new FormControl<string>('', [Validators.required]),
  evaluacion: new FormControl<TipoEvaluacion>('Tarea', [Validators.required]),
  nota: new FormControl<number>(0, [Validators.required, Validators.min(0), Validators.max(20)]),
  fechaRegistro: new FormControl<string>(
    new Date().toLocaleDateString('es-ES'), // ← ¡Formato correcto desde el inicio!
    [Validators.required]
  )
});
  editingCalificacion: Calificaciones | null = null;
  showCalificacionModal = false;
  private calificacionModal!: Modal;
  promedios: { [estudianteEmail: string]: { promedio: number; estado: string } } = {};
  TIPOS_EVALUACION = TIPOS_EVALUACION;

  constructor(
    private asignaturaService: AsignaturaService,
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private calificacionesService: CalificacionesService,
    private temaService:TemaService
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
  // === ABRIR MODAL DE NOTAS DESDE LA LISTA ===
openCalificacionModalForAsignatura(asignatura: Asignatura): void {
  this.selectedAsignatura = asignatura;
  this.loadCalificaciones(asignatura.id!);
  this.loadEstudiantesDeAsignatura(asignatura.estudiantes || []);
  this.openCalificacionModal(); // ← abre el modal
}
// === NUEVO: CREAR TEMA DESDE LISTA ===
openCreateTemaModal(asignaturaId: string): void {
  if (this.currentUser?.tipo !== TipoUsuario.administrativo) return;

  Swal.fire({
    title: 'Nuevo Tema',
    html: `
      <input id="nombre" class="swal2-input" placeholder="Nombre del tema" required>
      <textarea id="descripcion" class="swal2-textarea" placeholder="Descripción (opcional)"></textarea>
    `,
    showCancelButton: true,
    confirmButtonText: 'Crear',
    preConfirm: () => {
     const nombre = (document.getElementById('nombre') as HTMLInputElement).value.trim();
      const descripcion = (document.getElementById('descripcion') as HTMLTextAreaElement).value.trim();
      if (!nombre) {
        Swal.showValidationMessage('El nombre es obligatorio');
        return false;
      }
      return { nombre, descripcion };
    }
  }).then(result => {
    if (result.isConfirmed) {
      const tema: Omit<Tema, 'id' | 'estado' | 'fechaCreacion'> = {
        asignaturaId,
        nombre: result.value.nombre,
        descripcion: result.value.descripcion
      };
      this.temaService.create(tema).subscribe({
        next: () => {
          Swal.fire('¡Creado!', 'Tema agregado.', 'success');
          // Opcional: recargar asignaturas si necesitas ver cambios
        },
        error: () => Swal.fire('Error', 'No se pudo crear el tema', 'error')
      });
    }
  });
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
    this.searchControl.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {});
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
    return u ? `${u.nombre} ${u.apellido}` : email.split('@')[0];
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

  // --- NUEVOS MÉTODOS PARA CALIFICACIONES ---

 openAsignaturaDetail(asignatura: Asignatura): void {
  this.selectedAsignatura = asignatura;
  this.loadCalificaciones(asignatura.id!);
  this.loadEstudiantesDeAsignatura(asignatura.estudiantes || []);
  this.initCalificacionModal();
}

private loadEstudiantesDeAsignatura(emails: string[]): void {
  this.estudiantesMapa = {};
  if (emails.length === 0) return;

  this.usuarioService.getEstudiantes().subscribe({
    next: (usuarios) => {
      usuarios
        .filter(u => emails.includes(u.correoInstitucional))
        .forEach(u => {
          this.estudiantesMapa[u.correoInstitucional] = u;
        });
    },
    error: (err) => this.showError('Error al cargar nombres de estudiantes')
  });
}

  private initCalificacionModal(): void {
    const el = document.getElementById('calificacionModal');
    if (el) {
      this.calificacionModal = new Modal(el, { backdrop: 'static' });
    }
  }

  loadCalificaciones(asignaturaId: string): void {
    this.isLoadingCalificaciones = true;
    this.calificacionesService.filterByAsignatura(asignaturaId).pipe(
      finalize(() => this.isLoadingCalificaciones = false)
    ).subscribe({
      next: (califs) => {
        this.calificaciones = califs;
        this.calcularPromedios();
      },
      error: (err) => this.showError(err.message || 'Error al cargar calificaciones')
    });
  }

  calcularPromedios(): void {
    const porEstudiante: { [email: string]: number[] } = {};

    this.calificaciones.forEach(cal => {
      if (!porEstudiante[cal.estudianteId]) {
        porEstudiante[cal.estudianteId] = [];
      }
      porEstudiante[cal.estudianteId].push(cal.nota);
    });

    this.promedios = {};
    for (const [email, notas] of Object.entries(porEstudiante)) {
      const promedio = notas.reduce((a, b) => a + b, 0) / notas.length;
      const estado = promedio >= 17 ? 'Sobresaliente' : promedio >= 13 ? 'Aprobado' : 'Reprobado';
      this.promedios[email] = { promedio: +promedio.toFixed(2), estado };
    }
  }

  canEditCalificaciones(): boolean {
    return this.currentUser?.tipo === TipoUsuario.administrativo ||
           this.currentUser?.tipo === TipoUsuario.profesor;
  }

 openCalificacionModal(cal?: Calificaciones): void {
  if (!this.canEditCalificaciones()) return;

  this.editingCalificacion = cal || null;

  if (cal) {
    this.calificacionForm.patchValue({
      id: cal.id,
      estudianteId: cal.estudianteId,
      evaluacion: cal.evaluacion,
      nota: cal.nota,
      fechaRegistro: cal.fechaRegistro
    });
  } else {
    // Al crear: reset completo, id = null
    this.calificacionForm.reset({
      estudianteId: '',
      evaluacion: 'Tarea',
      nota: 0,
      fechaRegistro: new Date().toLocaleDateString('es-ES')
    });
  }

  this.calificacionModal?.show();
}
  private formatFecha(fecha: any): string {
  if (!fecha) return new Date().toLocaleDateString('es-ES');

  let date: Date;

  // Si es string como "30/10/2025"
  if (typeof fecha === 'string' && fecha.includes('/')) {
    const [d, m, y] = fecha.split('/');
    date = new Date(+y, +m - 1, +d);
  }
  // Si es Date o timestamp
  else if (fecha instanceof Date) {
    date = fecha;
  }
  else if (typeof fecha === 'number') {
    date = new Date(fecha);
  }
  else {
    date = new Date(); // fallback
  }

  return date.toLocaleDateString('es-ES'); // → "30/10/2025"
}
saveCalificacion(): void {
  if (this.calificacionForm.invalid || !this.selectedAsignatura) return;

  const fechaRaw = this.calificacionForm.get('fechaRegistro')?.value;
  const fechaFormateada = this.formatFecha(fechaRaw);

  // Extraemos los valores del formulario
  const formValue = this.calificacionForm.value;

  // Creamos el objeto base
  const data: Calificaciones = {
    estudianteId: formValue.estudianteId!,
    evaluacion: formValue.evaluacion!,
    nota: formValue.nota!,
    fechaRegistro: fechaFormateada,
    asignaturaId: this.selectedAsignatura.id!,
  } as Calificaciones;

  // Solo agregamos 'id' si estamos editando
  if (this.editingCalificacion?.id) {
    (data as any).id = this.editingCalificacion.id;
  }

  const action$ = this.editingCalificacion
    ? this.calificacionesService.update(this.editingCalificacion.id!, data)
    : this.calificacionesService.create(data);  // ← SIN id

  action$.subscribe({
    next: () => {
      this.showSuccess(this.editingCalificacion ? 'Nota actualizada' : 'Nota creada');
      this.loadCalificaciones(this.selectedAsignatura!.id!);
      this.closeCalificacionModal();
    },
    error: (err) => this.showError(err.message || 'Error al guardar nota')
  });
}

  deleteCalificacion(cal: Calificaciones): void {
    if (!this.canEditCalificaciones()) return;

    Swal.fire({
      title: '¿Eliminar nota?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true
    }).then(result => {
      if (result.isConfirmed && cal.id) {
        this.calificacionesService.delete(cal.id).subscribe({
          next: () => {
            this.showSuccess('Nota eliminada');
            this.loadCalificaciones(this.selectedAsignatura!.id!);
          },
          error: (err) => this.showError(err.message)
        });
      }
    });
  }

  closeCalificacionModal(): void {
    this.showCalificacionModal = false;
    this.calificacionModal?.hide();
    this.editingCalificacion = null;
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
