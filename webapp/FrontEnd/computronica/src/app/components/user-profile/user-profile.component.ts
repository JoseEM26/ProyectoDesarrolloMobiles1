// src/app/components/user-profile/user-profile.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, Subscription, of, BehaviorSubject, combineLatest } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { Usuario, TipoUsuario, Sede, Asignatura, Calificaciones } from '../../models/interfaces';
import Swal from 'sweetalert2';
import { AuthService } from '../../services/AuthService';
import { UsuarioService } from '../../services/UsuarioService';
import { AsignaturaService } from '../../services/AsignaturaService';
import { CalificacionesService } from '../../services/CalificacionesService';

interface Activity {
  id: string;
  type: 'course' | 'grade' | 'profile' | 'assignment';
  description: string;
  date: Date;
  details?: { [key: string]: any };
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
  activityFilter: 'all' | 'recent' | 'courses' | 'grades' | 'profile' | 'assignments' = 'all';
  private loggingOut = false;
  private subscriptions = new Subscription();
  tipoUsuario = TipoUsuario;

  // Fixed sede options
  sedes: Sede[] = ['Campus Central', 'Campus Norte', 'Campus Sur', 'Campus Este'];

  // Observables for stats
  enrolledCoursesCount$ = new BehaviorSubject<number>(0);
  recentActivitiesCount$ = new BehaviorSubject<number>(0);
  academicAverage$ = new BehaviorSubject<number>(0);
  filteredActivities$ = new BehaviorSubject<Activity[]>([]);
  profileCompletion$ = new BehaviorSubject<number>(0);

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
    private usuarioService: UsuarioService,
    private asignaturaService: AsignaturaService,
    private calificacionesService: CalificacionesService
  ) {}

  ngOnInit() {
    this.subscriptions.add(
      this.authService.user$.subscribe({
        next: (user) => {
          console.log('DEPURACIÓN: Usuario autenticado:', user);
          this.user = user;
          if (user) {
            if (!user.id || !user.correoInstitucional || !user.tipo || user.estado === undefined) {
              console.warn('Usuario incompleto:', user);
              this.showError('El usuario no tiene todos los datos requeridos');
              return;
            }
            this.editedUser = { ...user };
            this.loadUserStats(user);
            this.calculateProfileCompletion(user);
          } else {
            this.showError('No se pudo cargar la información del usuario');
          }
        },
        error: (err) => {
          console.error('DEPURACIÓN: Error fetching user:', err);
          this.showError('Error al cargar el perfil');
        }
      })
    );
  }

  private calculateProfileCompletion(user: Usuario) {
    let completion = 0;
    if (user.nombre) completion += 25;
    if (user.apellido) completion += 25;
    if (user.codigoInstitucional) completion += 25;
    if (user.sede) completion += 25;
    console.log('DEPURACIÓN: Completitud del perfil:', completion);
    this.profileCompletion$.next(completion);
  }

  private loadUserStats(user: Usuario) {
    this.isLoadingActivities = true;
    const userId = user.id!;
    const userType = user.tipo;

    // Mock activities (for fallback)
    const mockActivities: Activity[] = [
      { id: '1', type: 'course', description: 'Te matriculaste en Matemáticas I', date: new Date('2025-10-20'), details: { courseId: 'mat1', courseName: 'Matemáticas I' } },
      { id: '2', type: 'grade', description: 'Recibiste una nota de 18 en Examen Final', date: new Date('2025-10-22'), details: { grade: 18, evaluation: 'Examen Final' } },
      { id: '3', type: 'profile', description: 'Actualizaste tu perfil', date: new Date('2025-10-23'), details: {} },
      { id: '4', type: 'assignment', description: 'Enviaste la tarea de Física II', date: new Date('2025-10-24'), details: { assignmentId: 'fis2-t1', courseName: 'Física II' } }
    ];

    // Enrolled Courses Count
    if (userType === TipoUsuario.estudiante) {
      this.subscriptions.add(
        this.asignaturaService.getAll().pipe(
          map(asignaturas => asignaturas.filter(a => a.estudiantes?.includes(userId)).length),
          tap(count => {
            console.log('DEPURACIÓN: Cursos matriculados (estudiante):', count);
            this.enrolledCoursesCount$.next(count);
          }),
          catchError(() => {
            this.showError('Error al cargar cursos matriculados');
            return of(0);
          })
        ).subscribe()
      );
    } else if (userType === TipoUsuario.profesor) {
      this.subscriptions.add(
        this.asignaturaService.getAll().pipe(
          map(asignaturas => asignaturas.filter(a => a.profesores?.includes(user.correoInstitucional)).length),
          tap(count => {
            console.log('DEPURACIÓN: Cursos asignados (profesor):', count);
            this.enrolledCoursesCount$.next(count);
          }),
          catchError(() => {
            this.showError('Error al cargar cursos asignados');
            return of(0);
          })
        ).subscribe()
      );
    } else {
      this.subscriptions.add(
        this.asignaturaService.getAll().pipe(
          map(asignaturas => asignaturas.length),
          tap(count => {
            console.log('DEPURACIÓN: Total de cursos (admin):', count);
            this.enrolledCoursesCount$.next(count);
          }),
          catchError(() => {
            this.showError('Error al cargar total de cursos');
            return of(0);
          })
        ).subscribe()
      );
    }

    // Academic Average
    if (userType === TipoUsuario.estudiante) {
      this.subscriptions.add(
        this.calificacionesService.filterByEstudiante(userId).pipe(
          map(grades => {
            const validGrades = grades.filter(g => !isNaN(Number(g.nota)));
            const avg = validGrades.length
              ? Number((validGrades.reduce((sum, g) => sum + Number(g.nota), 0) / validGrades.length).toFixed(2))
              : 0;
            console.log('DEPURACIÓN: Promedio académico (estudiante):', avg);
            return avg;
          }),
          tap(avg => this.academicAverage$.next(avg)),
          catchError(() => {
            this.showError('Error al cargar promedio académico');
            return of(0);
          })
        ).subscribe()
      );
    } else if (userType === TipoUsuario.profesor) {
      this.subscriptions.add(
        combineLatest([
          this.calificacionesService.getAll(),
          this.asignaturaService.getAll()
        ]).pipe(
          map(([grades, asignaturas]) => {
            const assignedAsignaturas = asignaturas.filter(a => a.profesores?.includes(user.correoInstitucional));
            const assignedIds = assignedAsignaturas.map(a => a.id).filter((id): id is string => !!id);
            const relevantGrades = grades.filter(g => assignedIds.includes(g.asignaturaId));
            const validGrades = relevantGrades.filter(g => !isNaN(Number(g.nota)));
            const avg = validGrades.length
              ? Number((validGrades.reduce((sum, g) => sum + Number(g.nota), 0) / validGrades.length).toFixed(2))
              : 0;
            console.log('DEPURACIÓN: Promedio académico (profesor):', avg);
            return avg;
          }),
          tap(avg => this.academicAverage$.next(avg)),
          catchError(() => {
            this.showError('Error al cargar promedio de asignaturas');
            return of(0);
          })
        ).subscribe()
      );
    } else {
      // Admin: Academic average not relevant
      this.academicAverage$.next(0);
    }

    // Activities
    this.subscriptions.add(
      this.getFilteredActivities(user).pipe(
        map(activities => activities.length ? activities : mockActivities),
        tap(activities => {
          console.log('DEPURACIÓN: Actividades filtradas:', activities);
          this.filterActivities(activities);
          this.isLoadingActivities = false;
        }),
        catchError(() => {
          console.warn('DEPURACIÓN: Usando actividades mock por error');
          this.filterActivities(mockActivities);
          this.isLoadingActivities = false;
          return of(mockActivities);
        })
      ).subscribe()
    );
  }

private getFilteredActivities(user: Usuario): Observable<Activity[]> {
    if (!user.id) {
      console.warn('DEPURACIÓN: No user ID provided');
      return of([]);
    }

    if (user.tipo === TipoUsuario.estudiante) {
      return this.usuarioService.getUserActivities(user.id).pipe(
        map(activities => activities.filter(a =>
          ['course', 'grade', 'profile', 'assignment'].includes(a.type)
        )),
        tap(activities => console.log('DEPURACIÓN: Actividades filtradas para estudiante:', activities)),
        catchError(err => {
          console.error('DEPURACIÓN: Error al cargar actividades de estudiante:', err);
          return of([]);
        })
      );
    } else if (user.tipo === TipoUsuario.profesor) {
      return combineLatest([
        this.usuarioService.getUserActivities(user.id),
        this.asignaturaService.getAll()
      ]).pipe(
        map(([activities, asignaturas]) => {
          const assignedAsignaturas = asignaturas.filter(a => a.profesores?.includes(user.correoInstitucional));
          const assignedIds = assignedAsignaturas
            .map(a => a.id)
            .filter((id): id is string => !!id);

          return activities.filter((a: Activity) => {
            switch (a.type) {
              case 'profile':
                return true;
              case 'grade':
              case 'assignment':
                // Debug the details object
                console.log('DEPURACIÓN: Activity details:', a.details);
                const asignaturaId = a.details?.['asignaturaId'];
                return asignaturaId ? assignedIds.includes(asignaturaId) : false;
              default:
                return false;
            }
          });
        }),
        tap(activities => console.log('DEPURACIÓN: Actividades filtradas para profesor:', activities)),
        catchError(err => {
          console.error('DEPURACIÓN: Error al cargar actividades de profesor:', err);
          return of([]);
        })
      );
    } else {
      return this.usuarioService.getUserActivities(user.id).pipe(
        tap(activities => console.log('DEPURACIÓN: Todas las actividades (admin):', activities)),
        catchError(err => {
          console.error('DEPURACIÓN: Error al cargar actividades de admin:', err);
          return of([]);
        })
      );
    }
  }

  filterActivities(activities: Activity[] = []) {
    if (!this.user?.id) {
      console.warn('No user ID for filtering activities');
      this.showError('No se puede filtrar: usuario no cargado');
      this.isLoadingActivities = false;
      return;
    }

    this.isLoadingActivities = true;
    let filtered = [...activities];
    switch (this.activityFilter) {
      case 'recent':
        filtered = filtered.sort((a, b) => b.date.getTime() - a.date.getTime()).slice(0, 5);
        break;
      case 'courses':
        filtered = filtered.filter(a => a.type === 'course');
        break;
      case 'grades':
        filtered = filtered.filter(a => a.type === 'grade');
        break;
      case 'profile':
        filtered = filtered.filter(a => a.type === 'profile');
        break;
      case 'assignments':
        filtered = filtered.filter(a => a.type === 'assignment');
        break;
      default:
        filtered = filtered;
    }
    console.log('DEPURACIÓN: Actividades filtradas por tipo:', filtered);
    this.filteredActivities$.next(filtered);
    this.recentActivitiesCount$.next(filtered.length);
    this.isLoadingActivities = false;
  }

  getRandomAvatarIcon(id: string | undefined, fallback: string): string {
    const key = id || fallback;
    if (!key) return this.avatarIcons[0];
    const hash = this.hashString(key);
    return this.avatarIcons[hash % this.avatarIcons.length];
  }

  getRandomAvatarColor(id: string | undefined, fallback: string): string {
    const key = id || fallback;
    if (!key) return this.avatarColors[0];
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
        !this.editedUser.codigoInstitucional || !this.editedUser.sede) {
      this.showError('No se puede guardar: datos de usuario incompletos');
      return;
    }

    this.isLoading = true;
    const updatedUser: Partial<Usuario> = {
      id: this.editedUser.id,
      nombre: this.editedUser.nombre,
      apellido: this.editedUser.apellido,
      codigoInstitucional: this.editedUser.codigoInstitucional,
      sede: currentUser.tipo === TipoUsuario.administrativo ? this.editedUser.sede : currentUser.sede // Restrict sede changes
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
          this.calculateProfileCompletion(fullUser);
          const profileActivity: Activity = {
            id: `profile-${Date.now()}`,
            type: 'profile',
            description: 'Actualizaste tu información personal',
            date: new Date()
          };
          this.filteredActivities$.next([profileActivity, ...this.filteredActivities$.value]);
          if (!this.loggingOut) {
            this.showSuccess('Perfil actualizado correctamente');
          }
        }),
        catchError((error) => {
          console.error('DEPURACIÓN: Error al actualizar perfil:', error);
          this.isLoading = false;
          this.showError('Error al actualizar el perfil');
          return of(null);
        })
      ).subscribe()
    );
  }

  getActivityIcon(type: string): string {
    switch (type) {
      case 'course': return 'bi-book-fill';
      case 'grade': return 'bi-award-fill';
      case 'profile': return 'bi-person-fill';
      case 'assignment': return 'bi-file-earmark-text-fill';
      default: return 'bi-activity';
    }
  }

  getActivityIconClass(type: string): string {
    switch (type) {
      case 'course': return 'course';
      case 'grade': return 'grade';
      case 'profile': return 'profile';
      case 'assignment': return 'assignment';
      default: return 'default';
    }
  }

  private showSuccess(msg: string) {
    if (this.loggingOut) return;
    console.log('DEPURACIÓN: Mostrando mensaje de éxito:', msg);
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
    console.error('DEPURACIÓN: Mostrando mensaje de error:', msg);
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
    console.log('DEPURACIÓN: Preparando para cerrar sesión');
    this.loggingOut = true;
    this.subscriptions.unsubscribe();
  }

  ngOnDestroy() {
    this.prepareForLogout();
  }
}
