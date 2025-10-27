import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Calificaciones } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class CalificacionesService {
  private apiUrl = 'http://localhost:8080/api/calificaciones';

  constructor(private http: HttpClient) {}

  create(calificacion: Calificaciones): Observable<string> {
    return this.http.post<string>(this.apiUrl, calificacion).pipe(
      catchError(this.handleError)
    );
  }

  getById(id: string): Observable<Calificaciones> {
    return this.http.get<Calificaciones>(`${this.apiUrl}/${id}`).pipe(
      map(cal => this.transformFecha(cal)),
      catchError(this.handleError)
    );
  }

  getAll(): Observable<Calificaciones[]> {
    return this.http.get<Calificaciones[]>(this.apiUrl).pipe(
      map(calificaciones => {
        console.log('Raw calificaciones from API:', calificaciones);
        return calificaciones.map(cal => this.transformFecha(cal));
      }),
      catchError(this.handleError)
    );
  }

  update(id: string, calificacion: Calificaciones): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}`, calificacion).pipe(
      catchError(this.handleError)
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  filterByEstudiante(estudianteId: string): Observable<Calificaciones[]> {
    return this.http.get<Calificaciones[]>(`${this.apiUrl}/search/estudiante?estudianteId=${encodeURIComponent(estudianteId)}`).pipe(
      map(calificaciones => {
        console.log('Raw calificaciones for estudiante:', calificaciones);
        return calificaciones.map(cal => this.transformFecha(cal));
      }),
      catchError(this.handleError)
    );
  }

  filterByAsignatura(asignaturaId: string): Observable<Calificaciones[]> {
    return this.http.get<Calificaciones[]>(`${this.apiUrl}/search/asignatura?asignaturaId=${encodeURIComponent(asignaturaId)}`).pipe(
      map(calificaciones => {
        console.log('Raw calificaciones for asignatura:', calificaciones);
        return calificaciones.map(cal => this.transformFecha(cal));
      }),
      catchError(this.handleError)
    );
  }

  private transformFecha(cal: Calificaciones): Calificaciones {
    let fechaRegistro: string | null = null;
    let fechaRegistroDate: Date | null = null;

    if (cal.fechaRegistro && typeof cal.fechaRegistro === 'string') {
      try {
        // Asumimos formato DD/MM/YYYY
        const [day, month, year] = cal.fechaRegistro.split('/').map(Number);
        const date = new Date(year, month - 1, day);
        if (!isNaN(date.getTime())) {
          fechaRegistro = cal.fechaRegistro;
          fechaRegistroDate = date;
        } else {
          console.warn('Invalid date format:', cal.fechaRegistro);
        }
      } catch (e) {
        console.warn('Error parsing date:', cal.fechaRegistro, e);
      }
    }

    return {
      ...cal,
      fechaRegistro,
      fechaRegistroDate
    };
  }

  private handleError(error: any): Observable<never> {
    console.error('Error in CalificacionesService:', error);
    const message = error.error?.message || 'Error al interactuar con la API de calificaciones';
    return throwError(() => new Error(message));
  }
}
