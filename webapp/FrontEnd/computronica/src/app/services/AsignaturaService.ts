// src/app/services/asignatura.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Asignatura } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class AsignaturaService {
  private apiUrl = 'http://localhost:8080/api/asignaturas';

  constructor(private http: HttpClient) {}

// src/app/services/asignatura.service.ts

create(asignatura: Asignatura): Observable<Asignatura> {  // ← Asignatura
  return this.http.post<Asignatura>(this.apiUrl, asignatura).pipe(
    catchError(this.handleError)
  );
}

update(id: string, asignatura: Asignatura): Observable<Asignatura> {  // ← También Asignatura
  return this.http.put<Asignatura>(`${this.apiUrl}/${id}`, asignatura).pipe(
    catchError(this.handleError)
  );
}

  getById(id: string): Observable<Asignatura> {
    return this.http.get<Asignatura>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAll(): Observable<Asignatura[]> {
    return this.http.get<Asignatura[]>(this.apiUrl).pipe(
      catchError(this.handleError)
    );
  }


  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  filterByNombre(nombre: string): Observable<Asignatura[]> {
    return this.http.get<Asignatura[]>(`${this.apiUrl}/search?nombre=${encodeURIComponent(nombre)}`).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('Error in AsignaturaService:', error);
    const message = error.error?.message || 'Error al interactuar con la API de asignaturas';
    return throwError(() => new Error(message));
  }
}
