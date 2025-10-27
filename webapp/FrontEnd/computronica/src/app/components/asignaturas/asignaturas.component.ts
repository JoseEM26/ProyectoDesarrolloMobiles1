// src/app/components/asignaturas/asignaturas.component.ts
import { Component, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { map, startWith, catchError } from 'rxjs/operators';
import { Asignatura } from '../../models/interfaces';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AsyncPipe, NgFor, NgIf, CommonModule } from '@angular/common';
import Swal from 'sweetalert2';
import { AsignaturaService } from '../../services/AsignaturaService';

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
export class AsignaturasComponent implements OnInit {
  asignaturas$!: Observable<Asignatura[]>;
  filteredAsignaturas$!: Observable<Asignatura[]>;
  pageSize: number = 10;
  currentPage: number = 1;
  totalPages: number = 1;
  pages: number[] = [];
  searchControl = new FormControl('');
  isLoading = true;

  constructor(private asignaturaService: AsignaturaService) {}

  ngOnInit(): void {
    this.loadAsignaturas();
    this.setupSearch();
  }

  loadAsignaturas(): void {
    this.asignaturas$ = this.asignaturaService.getAll().pipe(
      catchError(error => {
        console.error('Error al obtener asignaturas:', error); // Log 1
        this.isLoading = false;
        this.showError('Error al cargar las asignaturas');
        return of([]);
      }),
      map(asignaturas => {
        console.log('Asignaturas recibidas del servicio:', asignaturas); // Log 2
        if (!Array.isArray(asignaturas)) {
          console.error('Error: Los datos recibidos no son un array:', asignaturas); // Log 3
          return [];
        }
        const validAsignaturas = asignaturas.filter(asig =>
          typeof asig === 'object' &&
          asig !== null &&
          'nombre' in asig &&
          'codigoAsignatura' in asig
        );
        console.log('Asignaturas válidas:', validAsignaturas); // Log 4
        return validAsignaturas.sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''));
      })
    );

    this.filteredAsignaturas$ = combineLatest([
      this.asignaturas$,
      this.searchControl.valueChanges.pipe(startWith(''))
    ]).pipe(
      map(([asignaturas, search]) => {
        console.log('Datos para filtrar:', { asignaturasCount: asignaturas.length, search }); // Log 5
        let filtered = asignaturas;

        if (search) {
          const searchLower = search.toLowerCase();
          filtered = asignaturas.filter(asig =>
            (asig.nombre?.toLowerCase()?.includes(searchLower) || false) ||
            (asig.codigoAsignatura?.toLowerCase()?.includes(searchLower) || false)
          );
          console.log('Asignaturas después de búsqueda:', filtered); // Log 6
        }

        this.totalPages = Math.max(1, Math.ceil(filtered.length / this.pageSize));
        console.log('Total páginas calculadas:', this.totalPages); // Log 7
        console.log('currentPage:', this.currentPage); // Log 8

        if (this.currentPage > this.totalPages) {
          this.currentPage = this.totalPages;
          console.log('currentPage ajustado a:', this.currentPage); // Log 9
        } else if (this.currentPage < 1) {
          this.currentPage = 1;
          console.log('currentPage ajustado a:', this.currentPage); // Log 9
        }

        this.pages = this.generatePages(this.totalPages, this.currentPage);
        console.log('Páginas generadas:', this.pages); // Log 10

        const startIndex = (this.currentPage - 1) * this.pageSize;
        const paginatedAsignaturas = filtered.slice(startIndex, startIndex + this.pageSize);
        console.log('Asignaturas paginadas:', paginatedAsignaturas); // Log 11
        return paginatedAsignaturas;
      }),
      map(asignaturas => {
        this.isLoading = false;
        console.log('Asignaturas finales para la tabla:', asignaturas); // Log 12
        return asignaturas;
      })
    );
  }

  private generatePages(totalPages: number, currentPage: number | null): number[] {
    const pages: number[] = [];
    const maxPagesToShow = 5;
    const validTotalPages = totalPages > 0 ? totalPages : 1;
    const validCurrentPage = currentPage && currentPage > 0 ? currentPage : 1;

    let startPage: number, endPage: number;

    if (validTotalPages <= maxPagesToShow) {
      startPage = 1;
      endPage = validTotalPages;
    } else {
      const maxPagesBeforeCurrent = Math.floor(maxPagesToShow / 2);
      const maxPagesAfterCurrent = Math.ceil(maxPagesToShow / 2) - 1;

      startPage = Math.max(1, validCurrentPage - maxPagesBeforeCurrent);
      endPage = Math.min(validTotalPages, validCurrentPage + maxPagesAfterCurrent);

      if (endPage - startPage < maxPagesToShow - 1) {
        if (startPage === 1) {
          endPage = startPage + maxPagesToShow - 1;
        } else if (endPage === validTotalPages) {
          startPage = endPage - maxPagesToShow + 1;
        }
      }
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    console.log('Páginas generadas:', pages); // Log 13
    return pages;
  }

  setupSearch(): void {
    this.searchControl.valueChanges.subscribe(() => {
      this.currentPage = 1;
      console.log('Filtro de búsqueda cambiado:', this.searchControl.value); // Log 14
    });
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      console.log('Página cambiada a:', page); // Log 15
    }
  }

  private showError(msg: string) {
    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: msg,
      confirmButtonText: 'OK',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }
}
