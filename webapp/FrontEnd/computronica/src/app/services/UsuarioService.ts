// src/app/services/UsuarioService.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Usuario, Activity, ActivityDetails } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private apiUrl = 'http://localhost:8080/api/usuarios';

  constructor( private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  // CREAR
  create(usuario: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>(this.apiUrl, usuario).pipe(
      catchError(this.handleError)
    );
  }

  getAll(): Observable<Usuario[]> {
    return this.http.get<any>(this.apiUrl).pipe(
      map(res => {
        const users = res?.data || res;
        return Array.isArray(users) ? users : [];
      }),
      catchError(() => of([]))
    );
  }
// NUEVO: TOGGLE ESTADO
  toggleEstado(id: string): Observable<Usuario> {
    return this.http.patch<Usuario>(
      `${this.apiUrl}/${id}/toggle-estado`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  // NUEVO: INHABILITAR
  inhabilitar(id: string): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/inhabilitar`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  // NUEVO: HABILITAR
  habilitar(id: string): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${id}/habilitar`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }
  // OBTENER POR ID
  getById(id: string): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  update(id: string, usuario: Partial<Usuario>): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.apiUrl}/${id}`, usuario, { headers: this.getHeaders() }).pipe(
      catchError(err => { throw err.error?.message || 'Error al actualizar'; })
    );
  }

  getEstudiantes(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/tipo/estudiante`);
  }

  getProfesores(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/tipo/profesor`);
  }

  // ELIMINAR (opcional)
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  // BUSCAR POR EMAIL
  filterByEmail(email: string): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/search?email=${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  // ESTADÍSTICAS
  getUserStats(userId: string): Observable<{ enrolledCourses: number, recentActivities: number, academicAverage: number }> {
    return this.http.get<any>(`${this.apiUrl}/${userId}/stats`, { headers: this.getHeaders() }).pipe(
      map(res => res?.data || res),
      catchError(this.handleError)
    );
  }

  getUserActivities(userId: string): Observable<Activity[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${userId}/activities`, { headers: this.getHeaders() }).pipe(
      map(activities => activities.map(act => ({
        id: act.id || '',
        type: ['course', 'grade', 'profile', 'assignment'].includes(act.type)
          ? act.type as 'course' | 'grade' | 'profile' | 'assignment'
          : 'profile',
        description: act.description || '',
        date: act.date ? new Date(act.date) : new Date(),
        details: act.details as ActivityDetails | undefined
      }))),
      catchError(this.handleError)
    );
  }

  // MANEJO DE ERRORES
  private handleError(error: any): Observable<never> {
    console.error('Error in UsuarioService:', error);
    const message = error.error?.message || error.message || 'Error en el servidor';
    return throwError(() => new Error(message));
  }
}
