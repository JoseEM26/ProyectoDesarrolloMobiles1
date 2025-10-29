// src/app/components/asignaturas/asignaturas.component.ts
import { Component, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { Observable, combineLatest, of, Subject } from 'rxjs';
import { map, startWith, catchError, takeUntil } from 'rxjs/operators';
import { Asignatura, Usuario, TipoUsuario } from '../../models/interfaces';
import { FormControl, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AsyncPipe, NgFor, NgIf, CommonModule } from '@angular/common';
import Swal from 'sweetalert2';
import { AsignaturaService } from '../../services/AsignaturaService';
import { UsuarioService } from '../../services/UsuarioService';
import { AuthService } from '../../services/AuthService';
import { Modal } from 'bootstrap';

@Component({
  selector: 'app-asignaturas',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NgFor,
    NgIf,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './asignaturas.component.html',
  styleUrls: ['./asignaturas.component.scss']
})
export class AsignaturasComponent implements OnInit, OnDestroy, AfterViewInit {
  asignaturas$!: Observable<Asignatura[]>;
  filteredAsignaturas$!: Observable<Asignatura[]>;
  pageSize = 10;
  currentPage = 1;
  totalPages = 1;
  totalItems = 0;
  pages: number[] = [];
  searchControl = new FormControl('');
  isLoading = true;
  isSaving = false;
  estudiantes$!: Observable<Usuario[]>;
  profesores$!: Observable<Usuario[]>;
  filteredProfesores: Usuario[] = [];
  filteredEstudiantes: Usuario[] = [];
  showProfesorSuggestions = false;
  showEstudianteSuggestions = false;
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
        this.showError('Error al cargar el usuario autenticado');
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
      console.log('Modal inicializado');
    } else {
      console.error('Modal #asignaturaModal no encontrado');
    }
  }

  loadAsignaturas(): void {
    if (!this.currentUser) {
      this.showError('Usuario no autenticado');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;

    this.asignaturas$ = this.asignaturaService.getAll().pipe(
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
      })
    );

    this.filteredAsignaturas$ = combineLatest([
      this.asignaturas$,
      this.searchControl.valueChanges.pipe(startWith(''))
    ]).pipe(
      map(([asignaturas, search]) => {
        let filtered = asignaturas;
        if (search) {
          const term = search.toLowerCase();
          filtered = asignaturas.filter(asig =>
            (asig.nombre?.toLowerCase().includes(term)) ||
            (asig.codigoAsignatura?.toLowerCase().includes(term))
          );
        }

        this.totalItems = filtered.length;
        this.totalPages = Math.max(1, Math.ceil(filtered.length / this.pageSize));
        if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
        if (this.currentPage < 1) this.currentPage = 1;
        this.pages = this.generatePages(this.totalPages, this.currentPage);

        const start = (this.currentPage - 1) * this.pageSize;
        return filtered.slice(start, start + this.pageSize);
      }),
      map(asignaturas => {
        this.isLoading = false;
        return asignaturas;
      })
    );
  }

  private generatePages(total: number, current: number): number[] {
    const max = 5;
    let start = Math.max(1, current - Math.floor(max / 2));
    let end = Math.min(total, start + max - 1);
    if (end - start < max - 1) start = Math.max(1, end - max + 1);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  setupSearch(): void {
    this.searchControl.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.currentPage = 1;
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

  getEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
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
    if (this.currentUser?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo administrativos pueden crear');
      return;
    }
    this.isEditMode = false;
    this.asignaturaForm.reset({
      nombre: '',
      codigoAsignatura: '',
      descripcion: '',
      creditos: 1,
      profesores: [],
      estudiantes: []
    });
    this.loadUsuarios();
    this.resetSuggestions();
    this.modal.show();
  }

  openEditModal(asignatura: Asignatura): void {
    if (this.currentUser?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo administrativos pueden editar');
      return;
    }
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

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.currentPage = page;
    }
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
