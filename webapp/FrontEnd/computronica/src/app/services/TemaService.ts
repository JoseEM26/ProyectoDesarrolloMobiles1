// src/app/services/tema.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Tema } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class TemaService {
  private apiUrl = 'http://localhost:8080/api/temas';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });
  }

// src/app/services/tema.service.ts

// CREATE → ahora recibe { id: string }
create(tema: Omit<Tema, 'id' | 'estado' | 'fechaCreacion'>): Observable<string> {
  return this.http.post<{ id: string }>(this.apiUrl, tema, { headers: this.getHeaders() }).pipe(
    map(response => response.id), // ← extrae el ID
    catchError(this.handleError)
  );
}

  // READ BY ID
  getById(id: string): Observable<Tema> {
    return this.http.get<Tema>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() }).pipe(
      catchError(this.handleError)
    );
  }

  // READ ALL
  getAll(): Observable<Tema[]> {
    return this.http.get<Tema[]>(this.apiUrl, { headers: this.getHeaders() }).pipe(
      catchError(this.handleError)
    );
  }

  // READ BY ASIGNATURA
  getByAsignaturaId(asignaturaId: string): Observable<Tema[]> {
    return this.http.get<Tema[]>(`${this.apiUrl}/asignatura/${asignaturaId}`, { headers: this.getHeaders() }).pipe(
      catchError(this.handleError)
    );
  }

  // UPDATE (solo campos permitidos)
  update(id: string, tema: Partial<Tema>): Observable<void> {
    const payload: any = {};
    if (tema.nombre !== undefined) payload.nombre = tema.nombre;
    if (tema.descripcion !== undefined) payload.descripcion = tema.descripcion;
    if (tema.estado !== undefined) payload.estado = tema.estado;

    return this.http.put<void>(`${this.apiUrl}/${id}`, payload, { headers: this.getHeaders() }).pipe(
      catchError(this.handleError)
    );
  }

  // DELETE
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() }).pipe(
      catchError(this.handleError)
    );
  }

  // MANEJO DE ERRORES
  private handleError(error: any): Observable<never> {
    console.error('Error in TemaService:', error);
    const message = error.error?.message || error.message || 'Error en el servidor';
    return throwError(() => new Error(message));
  }
}
