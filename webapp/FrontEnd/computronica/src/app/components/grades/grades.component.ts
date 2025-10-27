import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/AuthService';
import { Calificaciones, TipoUsuario, Usuario } from '../../models/interfaces';
import Swal from 'sweetalert2';
import { CalificacionesService } from '../../services/CalificacionesService';

@Component({
  selector: 'app-grades',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './grades.component.html',
  styleUrls: ['./grades.component.scss']
})
export class GradesComponent implements OnInit {
  calificaciones: Calificaciones[] = [];
  filteredCalificaciones: Calificaciones[] = [];
  paginatedCalificaciones: Calificaciones[] = [];
  loading = false;
  estudianteFilter = '';
  asignaturaFilter = '';
  currentPage = 1;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;
  user: Usuario | null = null;
  tipoUsuario = TipoUsuario;
  Math = Math; // Add this line to expose Math to the template

  constructor(
    private calificacionesService: CalificacionesService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.authService.user$.subscribe(user => {
      this.user = user;
      this.loadCalificaciones();
    });
  }

  loadCalificaciones() {
    this.loading = true;
    this.calificacionesService.getAll().subscribe({
      next: (calificaciones) => {
        this.calificaciones = calificaciones;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: error.message || 'No se pudieron cargar las calificaciones',
          confirmButtonText: 'OK',
          background: 'var(--card-bg)',
          customClass: { popup: 'swal2-popup' }
        });
      }
    });
  }

  applyFilters() {
    let filtered = this.calificaciones;

    if (this.estudianteFilter) {
      filtered = filtered.filter(cal =>
        (cal.estudianteNombre || cal.estudianteId)
          .toLowerCase()
          .includes(this.estudianteFilter.toLowerCase())
      );
    }

    if (this.asignaturaFilter) {
      filtered = filtered.filter(cal =>
        (cal.asignaturaNombre || cal.asignaturaId)
          .toLowerCase()
          .includes(this.asignaturaFilter.toLowerCase())
      );
    }

    this.filteredCalificaciones = filtered;
    this.totalItems = filtered.length;
    this.totalPages = Math.ceil(this.totalItems / this.pageSize);
    this.updatePaginatedCalificaciones();
  }

  updatePaginatedCalificaciones() {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedCalificaciones = this.filteredCalificaciones.slice(start, end);
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePaginatedCalificaciones();
    }
  }

  getPages(): number[] {
    const pages = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  editCalificacion(calificacion: Calificaciones) {
    // Implementar lógica de edición (por ejemplo, abrir un modal)
    Swal.fire({
      icon: 'info',
      title: 'Funcionalidad en desarrollo',
      text: 'Edición de calificaciones aún no implementada.',
      confirmButtonText: 'OK',
      background: 'var(--card-bg)',
      customClass: { popup: 'swal2-popup' }
    });
  }

  confirmDelete(id: string) {
    Swal.fire({
      title: '¿Eliminar calificación?',
      text: 'Esta acción no se puede deshacer.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '<i class="bi bi-check-circle-fill me-2"></i>Eliminar',
      cancelButtonText: '<i class="bi bi-x-circle-fill me-2"></i>Cancelar',
      reverseButtons: true,
      buttonsStyling: false,
      customClass: {
        popup: 'swal2-popup',
        title: 'h5 fw-bold',
        confirmButton: 'btn btn-danger btn-lg px-4 me-3',
        cancelButton: 'btn btn-outline-secondary btn-lg px-4'
      },
      background: 'var(--card-bg)'
    }).then((result) => {
      if (result.isConfirmed) {
        this.calificacionesService.delete(id).subscribe({
          next: () => {
            Swal.fire({
              icon: 'success',
              title: 'Calificación eliminada',
              timer: 1500,
              showConfirmButton: false,
              toast: true,
              position: 'top-end',
              background: 'var(--card-bg)',
              customClass: { popup: 'swal2-popup' }
            });
            this.loadCalificaciones();
          },
          error: (error) => {
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: error.message || 'No se pudo eliminar la calificación',
              confirmButtonText: 'OK',
              background: 'var(--card-bg)',
              customClass: { popup: 'swal2-popup' }
            });
          }
        });
      }
    });
  }
}
