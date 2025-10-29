// src/app/components/grades/grades.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/AuthService';
import { CalificacionesService } from '../../services/CalificacionesService';
import { UsuarioService } from '../../services/UsuarioService';
import { AsignaturaService } from '../../services/AsignaturaService';
import { Calificaciones, Usuario, Asignatura, TipoUsuario } from '../../models/interfaces';
import Swal from 'sweetalert2';
import { Subject, combineLatest, of } from 'rxjs';
import { takeUntil, map, catchError } from 'rxjs/operators';
import { Modal } from 'bootstrap';
import { TipoEvaluacion, TIPOS_EVALUACION } from '../../models/tipos-evaluacion';

@Component({
  selector: 'app-grades',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './grades.component.html',
  styleUrls: ['./grades.component.scss']
})
export class GradesComponent implements OnInit, OnDestroy {
  calificaciones: Calificaciones[] = [];
  filteredCalificaciones: Calificaciones[] = [];
  paginatedCalificaciones: Calificaciones[] = [];
  loading = false;
  isSaving = false;
  estudianteFilter = '';
  asignaturaFilter = '';
  currentPage = 1;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;
  user: Usuario | null = null;
  tipoUsuario = TipoUsuario;
  Math = Math;

  // Modal
  calificacionForm = new FormGroup({
    id: new FormControl<string | null>(null),
    estudianteId: new FormControl('', Validators.required),
    asignaturaId: new FormControl('', Validators.required),
    evaluacion: new FormControl<TipoEvaluacion | null>(null, Validators.required),
    nota: new FormControl(0, [
      Validators.required,
      Validators.min(1),
      Validators.max(20)
    ])
  });
  isEditMode = false;
  private modal!: Modal;
  private destroy$ = new Subject<void>();

  // Listas
  estudiantes: Usuario[] = [];
  asignaturas: Asignatura[] = [];
  tiposEvaluacion = TIPOS_EVALUACION;

  constructor(
    private calificacionesService: CalificacionesService,
    private authService: AuthService,
    private usuarioService: UsuarioService,
    private asignaturaService: AsignaturaService
  ) {}

  ngOnInit() {
    this.initModal();
    this.authService.user$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (user) => {
        this.user = user;
        if (user) {
          this.loadData();
        } else {
          this.showError('Usuario no autenticado');
          this.loading = false;
        }
      },
      error: (err) => {
        this.showError('Error al cargar el usuario autenticado');
        this.loading = false;
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initModal() {
    const el = document.getElementById('calificacionModal');
    if (el) this.modal = new Modal(el);
  }

  private loadData() {
    if (!this.user) return;

    this.loading = true;
    combineLatest([
      this.calificacionesService.getAll(),
      this.usuarioService.getAll(),
      this.asignaturaService.getAll()
    ]).pipe(
      map(([califs, usuarios, asignaturas]: [Calificaciones[], Usuario[], Asignatura[]]) => {
        let filteredCalifs = califs;

        // Filter calificaciones based on user role
        if (this.user!.tipo === TipoUsuario.administrativo) {
          // Admin sees all calificaciones
          filteredCalifs = califs;
        } else if (this.user!.tipo === TipoUsuario.estudiante) {
          // Student sees only their own calificaciones
          filteredCalifs = califs.filter(cal => cal.estudianteId === this.user!.id);
        } else if (this.user!.tipo === TipoUsuario.profesor) {
          // Professor sees calificaciones for their assigned asignaturas
          const assignedAsignaturas = asignaturas.filter(asig =>
            asig.profesores?.includes(this.user!.correoInstitucional)
          );
          const assignedAsignaturaIds = assignedAsignaturas.map(asig => asig.id).filter((id): id is string => !!id);
          filteredCalifs = califs.filter(cal => assignedAsignaturaIds.includes(cal.asignaturaId));
        }

        return [filteredCalifs, usuarios, asignaturas] as [Calificaciones[], Usuario[], Asignatura[]];
      }),
      catchError(err => {
        this.loading = false;
        this.showError(err.message || 'Error al cargar datos');
        return of([[], [], []] as [Calificaciones[], Usuario[], Asignatura[]]);
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: ([califs, usuarios, asignaturas]) => {
        this.calificaciones = califs; // Fixed: Assign to calificaciones, not calificacionForm
        this.estudiantes = usuarios.filter(u => u.tipo === TipoUsuario.estudiante);
        this.asignaturas = asignaturas;
        this.enrichCalificaciones();
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.showError(err.message || 'Error al cargar datos');
      }
    });
  }

  private enrichCalificaciones() {
    this.calificaciones = this.calificaciones.map(cal => ({
      ...cal,
      estudianteNombre: this.estudiantes.find(e => e.id === cal.estudianteId)
        ? `${this.estudiantes.find(e => e.id === cal.estudianteId)?.nombre} ${this.estudiantes.find(e => e.id === cal.estudianteId)?.apellido}`
        : cal.estudianteId,
      asignaturaNombre: this.asignaturas.find(a => a.id === cal.asignaturaId)?.nombre || cal.asignaturaId
    }));
  }

  applyFilters() {
    let filtered = this.calificaciones;
    if (this.estudianteFilter && this.user?.tipo !== TipoUsuario.estudiante) {
      filtered = filtered.filter(c =>
        c.estudianteNombre?.toLowerCase().includes(this.estudianteFilter.toLowerCase()) ||
        c.estudianteId.toLowerCase().includes(this.estudianteFilter.toLowerCase())
      );
    }
    if (this.asignaturaFilter && this.user?.tipo !== TipoUsuario.profesor) {
      filtered = filtered.filter(c =>
        c.asignaturaNombre?.toLowerCase().includes(this.asignaturaFilter.toLowerCase()) ||
        c.asignaturaId.toLowerCase().includes(this.asignaturaFilter.toLowerCase())
      );
    }
    this.filteredCalificaciones = filtered;
    this.totalItems = filtered.length;
    this.totalPages = Math.ceil(this.totalItems / this.pageSize);
    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
    if (this.currentPage < 1) this.currentPage = 1;
    this.updatePaginatedCalificaciones();
  }

  updatePaginatedCalificaciones() {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedCalificaciones = this.filteredCalificaciones.slice(start, start + this.pageSize);
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedCalificaciones();
    }
  }

  getPages(): number[] {
    const maxPages = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let end = Math.min(this.totalPages, start + maxPages - 1);
    if (end - start < maxPages - 1) start = Math.max(1, end - maxPages + 1);
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  getNotaBadgeClass(nota: number): string {
    if (nota <= 12) return 'bg-danger';
    if (nota <= 16) return 'bg-warning';
    return 'bg-success';
  }

  getEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  openCreateModal() {
    if (this.user?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo los administrativos pueden crear calificaciones');
      return;
    }
    this.isEditMode = false;
    this.calificacionForm.reset({
      estudianteId: '',
      asignaturaId: '',
      evaluacion: null,
      nota: 0
    });
    this.modal.show();
  }

  openEditModal(cal: Calificaciones) {
    if (this.user?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo los administrativos pueden editar calificaciones');
      return;
    }
    this.isEditMode = true;
    this.calificacionForm.patchValue({
      id: cal.id,
      estudianteId: cal.estudianteId,
      asignaturaId: cal.asignaturaId,
      evaluacion: cal.evaluacion,
      nota: cal.nota
    });
    this.modal.show();
  }

  saveCalificacion() {
    if (this.calificacionForm.invalid) return;

    this.isSaving = true;
    const data = this.calificacionForm.value as Calificaciones;
    const action$ = this.isEditMode && data.id
      ? this.calificacionesService.update(data.id, data)
      : this.calificacionesService.create(data);

    action$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.isSaving = false;
        this.modal.hide();
        this.showSuccess(this.isEditMode ? 'Calificación actualizada' : 'Calificación creada');
        this.loadData();
      },
      error: (err) => {
        this.isSaving = false;
        this.showError(err.message || 'Error al guardar');
      }
    });
  }

  confirmDelete(id: string) {
    if (this.user?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo los administrativos pueden eliminar calificaciones');
      return;
    }

    Swal.fire({
      title: '¿Eliminar?',
      text: 'No se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Eliminar',
      cancelButtonText: 'Cancelar',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg' }
    }).then(result => {
      if (result.isConfirmed) {
        this.calificacionesService.delete(id).subscribe({
          next: () => {
            this.showSuccess('Eliminada');
            this.loadData();
          },
          error: (err) => this.showError(err.message || 'Error al eliminar')
        });
      }
    });
  }

  private showError(msg: string) {
    Swal.fire({ icon: 'error', title: 'Error', text: msg, background: 'var(--card-bg)' });
  }

  private showSuccess(msg: string) {
    Swal.fire({ icon: 'success', title: 'Éxito', text: msg, timer: 2000, showConfirmButton: false, background: 'var(--card-bg)' });
  }
}
