import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Usuario } from '../models/interfaces';

interface Activity {
  id: string;
  type: 'course' | 'grade';
  description: string;
  date: Date;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private apiUrl = 'http://localhost:8080/api/usuarios';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

  create(usuario: Usuario): Observable<string> {
    return this.http.post<string>(this.apiUrl, usuario).pipe(
      catchError(this.handleError)
    );
  }

  getById(id: string): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

getAll(): Observable<Usuario[]> {
    return this.http.get(this.apiUrl).pipe(
      map(response => {
        console.log('Datos crudos de la API:', response); // Log 0
        if (typeof response === 'string') {
          try {
            const parsed = JSON.parse(response);
            console.log('Datos parseados:', parsed); // Log 1
            if (!Array.isArray(parsed)) {
              console.error('Error: Los datos parseados no son un array:', parsed);
              return [];
            }
            return parsed as Usuario[];
          } catch (e) {
            console.error('Error al parsear respuesta JSON:', e);
            return [];
          }
        }
        if (!Array.isArray(response)) {
          console.error('Error: La respuesta de la API no es un array:', response);
          return [];
        }
        console.log('Datos válidos:', response); // Log 1
        return response as Usuario[];
      }),
      catchError(error => {
        console.error('Error en la API:', error); // Log 2
        return of([]); // Retorna un array vacío en caso de error
      })
    );
  }


  update(id: string, usuario: Partial<Usuario>): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}`, usuario).pipe(
      catchError(this.handleError)
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  filterByEmail(email: string): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.apiUrl}/search?email=${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  getUserStats(userId: string): Observable<{ enrolledCourses: number, recentActivities: number, academicAverage: number }> {
    return this.http.get<{ enrolledCourses: number, recentActivities: number, academicAverage: number }>(
      `${this.apiUrl}/${userId}/stats`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  getUserActivities(userId: string): Observable<Activity[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${userId}/activities`, { headers: this.getHeaders() }).pipe(
      map(activities => activities.map(act => ({
        id: act.id,
        type: act.type as 'course' | 'grade',
        description: act.description,
        date: new Date(act.date)
      }))),
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('Error in UsuarioService:', error);
    const message = error.error?.message || 'Error al interactuar con la API de usuarios';
    return throwError(() => new Error(message));
  }
}
