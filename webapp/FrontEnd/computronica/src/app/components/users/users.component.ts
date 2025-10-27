// src/app/components/users/users.component.ts
import { Component, OnInit } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { map, startWith, catchError } from 'rxjs/operators';
import { Usuario, TipoUsuario } from '../../models/interfaces';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AsyncPipe, NgFor, NgIf, DatePipe, CommonModule } from '@angular/common';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { Timestamp } from 'firebase/firestore';
import { UsuarioService } from '../../services/UsuarioService';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    AsyncPipe,
    NgFor,
    NgIf,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    NgbDropdownModule
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users$!: Observable<Usuario[]>;
  filteredUsers$!: Observable<Usuario[]>;
  pageSize: number = 10;
  currentPage: number = 1;
  totalPages: number = 1;
  pages: number[] = []; // Propiedad para almacenar las páginas
  searchControl = new FormControl('');
  tipoFilter = new FormControl('all');
  tipoUsuario = TipoUsuario;
  isLoading = true;

  constructor(private usuarioService: UsuarioService) {}

  ngOnInit(): void {
    this.loadUsers();
    this.setupFilters();
  }

  loadUsers(): void {
    this.users$ = this.usuarioService.getAll().pipe(
      catchError(error => {
        console.error('Error al obtener usuarios:', error); // Log 3
        this.isLoading = false;
        return of([]);
      }),
      map(users => {
        console.log('Usuarios recibidos del servicio:', users); // Log 4
        if (!Array.isArray(users)) {
          console.error('Error: Los datos recibidos no son un array:', users); // Log 5
          return [];
        }
        const validUsers = users.filter(user => typeof user === 'object' && user !== null && 'nombre' in user && 'codigoInstitucional' in user);
        console.log('Usuarios válidos:', validUsers); // Log 6
        return validUsers
          .sort((a, b) => (a.nombre || '').localeCompare(b.nombre || ''))
          .map(user => {
            const convertedUser = {
              ...user,
              createdAt: this.convertToDate(user.createdAt),
              updatedAt: this.convertToDate(user.updatedAt)
            };
            console.log('Usuario convertido:', convertedUser); // Log 7
            return convertedUser;
          });
      })
    );

    this.filteredUsers$ = combineLatest([
      this.users$,
      this.searchControl.valueChanges.pipe(startWith('')),
      this.tipoFilter.valueChanges.pipe(startWith('all'))
    ]).pipe(
      map(([users, search, tipo]) => {
        console.log('Datos para filtrar:', { usersCount: users.length, search, tipo }); // Log 8
        let filtered = users;

        if (search) {
          const searchLower = search.toLowerCase();
          filtered = users.filter(user =>
            (user.nombre?.toLowerCase()?.includes(searchLower) || false) ||
            (user.apellido?.toLowerCase()?.includes(searchLower) || false) ||
            (user.correoInstitucional?.toLowerCase()?.includes(searchLower) || false) ||
            (user.codigoInstitucional?.toLowerCase()?.includes(searchLower) || false)
          );
          console.log('Usuarios después de búsqueda:', filtered); // Log 9
        }

        if (tipo !== 'all') {
          filtered = filtered.filter(user => user.tipo?.toLowerCase() === tipo?.toLowerCase());
          console.log('Usuarios después de filtro por tipo:', filtered); // Log 10
        }

        this.totalPages = Math.max(1, Math.ceil(filtered.length / this.pageSize));
        console.log('Total páginas calculadas:', this.totalPages); // Log 11
        console.log('currentPage:', this.currentPage); // Log 12

        if (this.currentPage > this.totalPages) {
          this.currentPage = this.totalPages;
          console.log('currentPage ajustado a:', this.currentPage); // Log 13
        } else if (this.currentPage < 1) {
          this.currentPage = 1;
          console.log('currentPage ajustado a:', this.currentPage); // Log 13
        }

        // Generar páginas directamente
        this.pages = this.generatePages(this.totalPages, this.currentPage);
        console.log('Páginas generadas (componente):', this.pages); // Log 14

        const startIndex = (this.currentPage - 1) * this.pageSize;
        const paginatedUsers = filtered.slice(startIndex, startIndex + this.pageSize);
        console.log('Usuarios paginados:', paginatedUsers); // Log 15
        return paginatedUsers;
      }),
      map(users => {
        this.isLoading = false;
        console.log('Usuarios finales para la tabla:', users); // Log 16
        return users;
      })
    );
  }

  // Nueva función para generar páginas
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

    console.log('Páginas generadas:', pages); // Log 20
    return pages;
  }

  private convertToDate(value: string | Date | Timestamp | null | undefined): string | Date | Timestamp | null | undefined {
    if (value == null) return null;
    if (value instanceof Timestamp) return value;
    if (typeof value === 'string') {
      const parsedDate = new Date(value);
      return isNaN(parsedDate.getTime()) ? value : parsedDate;
    }
    return value;
  }

  getCreatedAtDate(user: Usuario): Date | null {
    const value = user.createdAt;
    if (!value) return null;
    if (value instanceof Timestamp) return value.toDate();
    if (typeof value === 'string') {
      const parsedDate = new Date(value);
      return isNaN(parsedDate.getTime()) ? null : parsedDate;
    }
    return value instanceof Date ? value : null;
  }

  setupFilters(): void {
    this.searchControl.valueChanges.subscribe(() => {
      this.currentPage = 1;
      console.log('Filtro de búsqueda cambiado:', this.searchControl.value); // Log 17
    });
    this.tipoFilter.valueChanges.subscribe(() => {
      this.currentPage = 1;
      console.log('Filtro de tipo cambiado:', this.tipoFilter.value); // Log 18
    });
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      console.log('Página cambiada a:', page); // Log 19
    }
  }

  getAvatarIcon(userId: string): string {
    const icons = ['bi-person-fill', 'bi-person-circle', 'bi-person-badge'];
    const index = userId.length % icons.length;
    return icons[index];
  }

  getAvatarColor(userId: string): string {
    const colors = ['var(--primary)', 'var(--secondary)', 'var(--accent)'];
    const index = userId.length % colors.length;
    return colors[index];
  }

  getStatusClass(estado: boolean): string {
    return estado ? 'text-success' : 'text-danger';
  }
}
