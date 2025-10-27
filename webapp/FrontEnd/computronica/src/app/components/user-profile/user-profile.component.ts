import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, Subscription, of, BehaviorSubject } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { Usuario, TipoUsuario } from '../../models/interfaces';
import Swal from 'sweetalert2';
import { AuthService } from '../../services/AuthService';
import { UsuarioService } from '../../services/UsuarioService';

interface Activity {
  id: string;
  type: 'course' | 'grade';
  description: string;
  date: Date;
}

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss']
})
export class UserProfileComponent implements OnInit, OnDestroy {
  user: Usuario | null = null;
  isEditing = false;
  editedUser: Partial<Usuario> = {};
  isLoading = false;
  isLoadingActivities = false;
  activityFilter: 'all' | 'recent' | 'courses' | 'grades' = 'all';
  private loggingOut = false;
  private subscriptions = new Subscription();
  tipoUsuario = TipoUsuario;

  // Observables for stats
  enrolledCoursesCount$ = new BehaviorSubject<number>(0);
  recentActivitiesCount$ = new BehaviorSubject<number>(0);
  academicAverage$ = new BehaviorSubject<number>(0);
  filteredActivities$ = new BehaviorSubject<Activity[]>([]);

  private avatarIcons = [
    'bi-person-circle', 'bi-person-hearts', 'bi-person-badge', 'bi-person-check',
    'bi-person-fill', 'bi-person-lines-fill', 'bi-person-square', 'bi-person-vcard',
    'bi-person-workspace', 'bi-person-x', 'bi-person-rolodex', 'bi-person-video3'
  ];

  private avatarColors = [
    '#2563eb', '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#06b6d4'
  ];

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit() {
    this.subscriptions.add(
      this.authService.user$.subscribe({
        next: (user) => {
          this.user = user;
          if (user) {
            if (!user.id || !user.correoInstitucional || !user.tipo || user.estado === undefined) {
              console.warn('Usuario incompleto:', { id: user.id, correo: user.correoInstitucional, tipo: user.tipo, estado: user.estado });
              this.showError('El usuario no tiene todos los datos requeridos');
              return;
            }
            this.editedUser = { ...user };
            this.loadUserStats(user.id);
          } else {
            this.showError('No se pudo cargar la información del usuario');
          }
        },
        error: (err) => {
          console.error('Error fetching user:', err);
          this.showError('Error al cargar el perfil');
        }
      })
    );
  }

  private loadUserStats(userId: string) {
    this.isLoadingActivities = true;
    this.subscriptions.add(
      this.usuarioService.getUserStats(userId).pipe(
        tap((stats) => {
          this.enrolledCoursesCount$.next(stats.enrolledCourses || 0);
          this.recentActivitiesCount$.next(stats.recentActivities || 0);
          this.academicAverage$.next(stats.academicAverage || 0);
        }),
        catchError(() => {
          this.showError('Error al cargar estadísticas');
          return of({ enrolledCourses: 0, recentActivities: 0, academicAverage: 0 });
        })
      ).subscribe()
    );

    this.subscriptions.add(
      this.usuarioService.getUserActivities(userId).pipe(
        tap((activities) => {
          this.filteredActivities$.next(activities);
          this.isLoadingActivities = false;
        }),
        catchError(() => {
          this.showError('Error al cargar actividades');
          this.isLoadingActivities = false;
          return of([]);
        })
      ).subscribe()
    );
  }

  filterActivities() {
    if (!this.user?.id) {
      console.warn('No user ID for filtering activities');
      this.showError('No se puede filtrar: usuario no cargado');
      this.isLoadingActivities = false;
      return;
    }

    this.isLoadingActivities = true;
    this.subscriptions.add(
      this.usuarioService.getUserActivities(this.user.id).pipe(
        map((activities: Activity[]) => {
          let filtered: Activity[] = [...activities];
          if (this.activityFilter === 'recent') {
            filtered = [...activities]
              .sort((a, b) => b.date.getTime() - a.date.getTime())
              .slice(0, 5);
          } else if (this.activityFilter === 'courses') {
            filtered = activities.filter(a => a.type === 'course');
          } else if (this.activityFilter === 'grades') {
            filtered = activities.filter(a => a.type === 'grade');
          }
          return filtered;
        }),
        tap((filtered: Activity[]) => {
          this.filteredActivities$.next(filtered);
          this.recentActivitiesCount$.next(filtered.length);
          this.isLoadingActivities = false;
        }),
        catchError(() => {
          this.showError('Error al filtrar actividades');
          this.isLoadingActivities = false;
          this.filteredActivities$.next([]);
          return of([]);
        })
      ).subscribe()
    );
  }

  getRandomAvatarIcon(id: string | undefined, fallback: string): string {
    const key = id || fallback;
    if (!key) {
      console.warn('No ID or fallback provided for avatar icon');
      return this.avatarIcons[0];
    }
    const hash = this.hashString(key);
    return this.avatarIcons[hash % this.avatarIcons.length];
  }

  getRandomAvatarColor(id: string | undefined, fallback: string): string {
    const key = id || fallback;
    if (!key) {
      console.warn('No ID or fallback provided for avatar color');
      return this.avatarColors[0];
    }
    const hash = this.hashString(key);
    return this.avatarColors[hash % this.avatarColors.length];
  }

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }

  toggleEdit() {
    this.isEditing = !this.isEditing;
    if (!this.isEditing && this.user) {
      this.editedUser = { ...this.user };
    } else if (this.isEditing) {
      setTimeout(() => {
        const nombreInput = document.getElementById('nombre') as HTMLInputElement;
        if (nombreInput) nombreInput.focus();
      }, 0);
    }
  }

  saveProfile() {
    const currentUser = this.user;
    if (!currentUser || !currentUser.id || !this.editedUser.id ||
        !this.editedUser.nombre || !this.editedUser.apellido ||
        !this.editedUser.codigoInstitucional || !this.editedUser.sede ||
        !currentUser.correoInstitucional || !currentUser.tipo || currentUser.estado === undefined) {
      console.warn('Incomplete user data for save:', { currentUser, editedUser: this.editedUser });
      this.showError('No se puede guardar: datos de usuario incompletos');
      return;
    }

    this.isLoading = true;
    const updatedUser: Partial<Usuario> = {
      id: this.editedUser.id,
      nombre: this.editedUser.nombre,
      apellido: this.editedUser.apellido,
      codigoInstitucional: this.editedUser.codigoInstitucional,
      sede: this.editedUser.sede
    };

    this.subscriptions.add(
      this.usuarioService.update(currentUser.id, updatedUser).pipe(
        tap(() => {
          const fullUser: Usuario = {
            id: currentUser.id,
            codigoInstitucional: updatedUser.codigoInstitucional || currentUser.codigoInstitucional,
            sede: updatedUser.sede || currentUser.sede,
            nombre: updatedUser.nombre || currentUser.nombre,
            apellido: updatedUser.apellido || currentUser.apellido,
            correoInstitucional: currentUser.correoInstitucional,
            tipo: currentUser.tipo,
            estado: currentUser.estado,
            createdAt: currentUser.createdAt || new Date().toISOString(),
            updatedAt: new Date().toISOString(),
            avatar: currentUser.avatar
          };
          this.user = fullUser;
          this.authService.updateUser(fullUser);
          this.isEditing = false;
          this.isLoading = false;
          if (!this.loggingOut) {
            this.showSuccess('Perfil actualizado correctamente');
          }
        }),
        catchError((error) => {
          this.isLoading = false;
          this.showError('Error al actualizar el perfil');
          return of(null);
        })
      ).subscribe()
    );
  }

  getActivityIcon(type: string): string {
    return type === 'course' ? 'bi-book-fill' : 'bi-award-fill';
  }

  getActivityIconClass(type: string): string {
    return type === 'course' ? 'course' : 'grade';
  }

  private showSuccess(msg: string) {
    if (this.loggingOut) return;
    Swal.fire({
      icon: 'success',
      title: 'Éxito',
      text: msg,
      timer: 1500,
      showConfirmButton: false,
      toast: true,
      position: 'top-end',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }

  private showError(msg: string) {
    if (this.loggingOut) return;
    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: msg,
      confirmButtonText: 'OK',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }

  prepareForLogout() {
    this.loggingOut = true;
    this.subscriptions.unsubscribe();
  }

  ngOnDestroy() {
    this.prepareForLogout();
  }
}
