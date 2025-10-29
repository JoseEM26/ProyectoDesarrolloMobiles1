import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { Observable, combineLatest, of } from 'rxjs';
import { map, startWith, catchError, finalize } from 'rxjs/operators';
import { Usuario, TipoUsuario, RegisterRequest } from '../../models/interfaces';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AsyncPipe, NgFor, NgIf, DatePipe, CommonModule } from '@angular/common';
import { Timestamp } from 'firebase/firestore';
import { UsuarioService } from '../../services/UsuarioService';
import { AuthService } from '../../services/AuthService';
import Swal from 'sweetalert2';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule, AsyncPipe, NgFor, NgIf, DatePipe,
     ReactiveFormsModule, FormsModule
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  @ViewChild('userModal') userModal!: TemplateRef<any>;

  users$!: Observable<Usuario[]>;
  filteredUsers$!: Observable<Usuario[]>;
  pageSize = 10;
  currentPage = 1;
  totalPages = 1;
  pages: number[] = [];
  searchControl = new FormControl('');
  tipoFilter = new FormControl('all');
  tipoUsuario = TipoUsuario;
  isLoading = true;
  isSaving = false;
  isEditMode = false;
  modalRef!: NgbModalRef;
  userForm!: FormGroup;
  currentUserId: string | null = null;
totalFilteredUsers = 0; // Añade esta propiedad

  sedes = ['Lima Centro', 'San Juan de Lurigancho', 'Miraflores'];

  constructor(
    private usuarioService: UsuarioService,
    private authService: AuthService,
    private fb: FormBuilder,
    private modalService: NgbModal
  ) {
    this.createForm();
  }

  ngOnInit(): void {
    this.loadUsers();
    this.setupFilters();
  }

  createForm(): void {
    this.userForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellido: ['', [Validators.required, Validators.minLength(2)]],
      codigoInstitucional: ['', [Validators.required, Validators.minLength(3)]],
      sede: ['', Validators.required],
      correoInstitucional: ['', [Validators.required, Validators.email]],
      contrasena: ['', [Validators.minLength(6)]],
      tipo: [TipoUsuario.estudiante]
    });
  }
getEndIndex(): number {
  return Math.min(this.currentPage * this.pageSize, this.totalFilteredUsers);
}
  openCreateModal(): void {
    this.isEditMode = false;
    this.currentUserId = null;
    this.userForm.reset({ sede: '', tipo: TipoUsuario.estudiante });
    this.userForm.get('contrasena')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.get('contrasena')?.updateValueAndValidity();
    this.modalRef = this.modalService.open(this.userModal, { size: 'lg', backdrop: 'static' });
  }

  openEditModal(user: Usuario): void {
    this.isEditMode = true;
    this.currentUserId = user.id || null;
    this.userForm.patchValue({
      nombre: user.nombre,
      apellido: user.apellido,
      codigoInstitucional: user.codigoInstitucional,
      sede: user.sede,
      correoInstitucional: user.correoInstitucional,
      tipo: user.tipo
    });
    this.userForm.get('contrasena')?.clearValidators();
    this.userForm.get('contrasena')?.updateValueAndValidity();
    this.modalRef = this.modalService.open(this.userModal, { size: 'lg', backdrop: 'static' });
  }
// users.component.ts
getInitials(user: any): string {
  return `${user.nombre.charAt(0)}${user.apellido.charAt(0)}`.toUpperCase();
}


  saveUser(): void {
    if (this.userForm.invalid || (this.isEditMode && !this.currentUserId)) return;

    this.isSaving = true;
    const formValue = this.userForm.value;

   if (this.isEditMode) {
  const payload: Partial<Usuario> = {
    nombre: formValue.nombre,
    apellido: formValue.apellido,
    codigoInstitucional: formValue.codigoInstitucional,
    sede: formValue.sede,
    tipo: formValue.tipo
    // NO envíes correoInstitucional ni contrasena → Firebase Auth no lo permite
  };

  this.usuarioService.update(this.currentUserId!, payload).pipe(
    finalize(() => { this.isSaving = false; this.currentUserId = null; })
  ).subscribe({
    next: (updatedUser) => {
      this.showToast('success', 'Usuario actualizado');

      // ACTUALIZAR USUARIO ACTUAL SI ES ÉL MISMO
      const currentUser = this.authService.getUser();
      if (currentUser?.id === updatedUser.id) {
        this.authService.updateUser(updatedUser);
      }

      this.modalRef.close();
      this.loadUsers();
    },
    error: (err) => this.showToast('error', err.message || 'Error al actualizar')
  });
}else {
      // CREAR
      const request: RegisterRequest = {
        codigoInstitucional: formValue.codigoInstitucional,
        sede: formValue.sede,
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        correoInstitucional: formValue.correoInstitucional,
        contrasena: formValue.contrasena,
        tipo: formValue.tipo
      };

      this.authService.register(request).pipe(
        finalize(() => { this.isSaving = false; })
      ).subscribe({
        next: () => {
          this.showToast('success', 'Usuario creado y sesión iniciada');
          this.modalRef.close();
          this.loadUsers();
        },
        error: (err) => this.showToast('error', err.message || 'Error al crear')
      });
    }
  }

  toggleUserStatus(user: Usuario): void {
    Swal.fire({
      title: user.estado ? 'Desactivar usuario' : 'Activar usuario',
      text: `¿${user.estado ? 'desactivar' : 'activar'} a ${user.nombre}?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: user.estado ? 'Desactivar' : 'Activar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed && user.id) {
        this.usuarioService.update(user.id, { estado: !user.estado }).pipe(
          finalize(() => this.loadUsers())
        ).subscribe({
          next: () => this.showToast('success', `Usuario ${!user.estado ? 'activado' : 'desactivado'}`),
          error: () => this.showToast('error', 'Error al cambiar estado')
        });
      }
    });
  }

  loadUsers(): void {
    this.isLoading = true;
    this.users$ = this.usuarioService.getAll().pipe(
      map(users => users.map(u => ({
        ...u,
        createdAt: this.convertToDate(u.createdAt),
        updatedAt: this.convertToDate(u.updatedAt)
      }))),
      catchError(() => of([]))
    );

    this.filteredUsers$ = combineLatest([
      this.users$,
      this.searchControl.valueChanges.pipe(startWith('')),
      this.tipoFilter.valueChanges.pipe(startWith('all'))
    ]).pipe(
      map(([users, search, tipo]) => {
        let filtered = users;
        if (search) {
          const term = search.toLowerCase();
          filtered = filtered.filter(u =>
            [u.nombre, u.apellido, u.correoInstitucional, u.codigoInstitucional]
              .some(f => f?.toLowerCase().includes(term))
          );
        }
        if (tipo !== 'all') filtered = filtered.filter(u => u.tipo === tipo);
this.totalFilteredUsers = filtered.length; // AÑADE ESTO
        this.totalPages = Math.max(1, Math.ceil(filtered.length / this.pageSize));
        this.currentPage = Math.min(this.currentPage, this.totalPages);
        this.pages = this.generatePages(this.totalPages, this.currentPage);
        const start = (this.currentPage - 1) * this.pageSize;
        return filtered.slice(start, start + this.pageSize);
      }),
      map(users => { this.isLoading = false; return users; })
    );
  }

  setupFilters(): void {
    this.searchControl.valueChanges.subscribe(() => this.currentPage = 1);
    this.tipoFilter.valueChanges.subscribe(() => this.currentPage = 1);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  private generatePages(total: number, current: number): number[] {
    const pages: number[] = [];
    const max = 5;
    let start = Math.max(1, current - Math.floor(max / 2));
    let end = Math.min(total, start + max - 1);
    if (end - start < max - 1) start = Math.max(1, end - max + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  private convertToDate(value: any): any {
    if (value instanceof Timestamp) return value.toDate();
    if (typeof value === 'string') {
      const d = new Date(value);
      return isNaN(d.getTime()) ? value : d;
    }
    return value;
  }

  getCreatedAtDate(user: Usuario): Date | null {
    const d = this.convertToDate(user.createdAt);
    return d instanceof Date ? d : null;
  }

  getAvatarIcon(id: string): string {
    const icons = ['bi-person-fill', 'bi-person-circle', 'bi-person-badge'];
    return icons[Math.abs(this.hash(id)) % icons.length];
  }

  getAvatarColor(id: string): string {
    const colors = ['#4361ee', '#7209b7', '#f72585'];
    return colors[Math.abs(this.hash(id)) % colors.length];
  }

  private hash(str: string): number {
    let h = 0; for (let i = 0; i < str.length; i++) h = (h << 5) - h + str.charCodeAt(i);
    return h & h;
  }

  getStatusClass(estado: boolean): string {
    return estado ? 'text-success' : 'text-danger';
  }

  private showToast(icon: 'success' | 'error', title: string): void {
    Swal.fire({ icon, title, toast: true, position: 'top-end', timer: 3000, showConfirmButton: false });
  }
}
