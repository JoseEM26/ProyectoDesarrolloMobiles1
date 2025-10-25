import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, throwError } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../models/auth-response';
import { Auth, signInWithEmailAndPassword, signOut } from '@angular/fire/auth';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private auth: Auth) {}

  login(correo: string, contrasena: string): Observable<AuthResponse> {
    return from(signInWithEmailAndPassword(this.auth, correo, contrasena)).pipe(
      switchMap((userCredential) =>
        from(userCredential.user.getIdToken()).pipe(
          switchMap((token) =>
            this.http.post<AuthResponse>(`${this.apiUrl}/login`, {
              correoInstitucional: correo,
              contrasena
            }).pipe(
              map((response) => {
                response.token = token;
                return response;
              }),
              catchError((error) => {
                console.error('Backend login error:', error);
                return throwError(() => new Error('Error en la autenticación con el servidor: ' + error.message));
              })
            )
          )
        )
      ),
      catchError((error) => {
        console.error('Firebase login error:', error);
        return throwError(() => new Error('Error en la autenticación de Firebase: ' + error.message));
      })
    );
  }

  register(user: {
    nombre: string;
    apellido: string;
    correoInstitucional: string;
    contrasena: string;
    codigoInstitucional: string;
    sede: string;
    tipo?: 'estudiante' | 'profesor' | 'administrativo';
  }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/registro`, user).pipe(
      catchError((error) => {
        console.error('Backend register error:', error);
        return throwError(() => new Error('Error en el registro: ' + error.message));
      })
    );
  }

  logout(): Observable<any> {
    return from(signOut(this.auth)).pipe(
      switchMap(() => this.http.post(`${this.apiUrl}/logout`, {})),
      catchError((error) => {
        console.error('Logout error:', error);
        return throwError(() => new Error('Error al cerrar sesión: ' + error.message));
      })
    );
  }

  saveUser(authResponse: AuthResponse): void {
    localStorage.setItem('user', JSON.stringify(authResponse));
  }

  getUser(): AuthResponse | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getUser();
  }
}