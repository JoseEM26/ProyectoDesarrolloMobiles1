import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, from, of } from 'rxjs';
import { catchError, switchMap, map } from 'rxjs/operators';
import { Auth, signInWithCustomToken, signOut } from '@angular/fire/auth';
import { AuthResponse, LoginRequest, RegisterRequest, Usuario } from '../models/interfaces';
import { signInWithEmailAndPassword } from 'firebase/auth';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private userSubject = new BehaviorSubject<Usuario | null>(null);
  user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient, private auth: Auth) {
    this.loadUserFromStorage();
  }

  // CARGAR USUARIO DESDE LOCALSTORAGE
  private loadUserFromStorage(): void {
    const data = localStorage.getItem('user');
    if (data) {
      try {
        const authRes = JSON.parse(data) as AuthResponse;
        const usuario: Usuario = {
          id: authRes.id,
          codigoInstitucional: authRes.codigoInstitucional,
          sede: authRes.sede,
          nombre: authRes.nombre,
          apellido: authRes.apellido,
          correoInstitucional: authRes.correoInstitucional,
          tipo: authRes.tipo,
          estado: authRes.estado
        };
        this.userSubject.next(usuario);
      } catch {
        localStorage.removeItem('user');
      }
    }
  }
login(request: LoginRequest): Observable<AuthResponse> {
  return from(signInWithEmailAndPassword(this.auth, request.correoInstitucional, request.contrasena)).pipe(
    switchMap(userCredential =>
      from(userCredential.user.getIdToken()).pipe(
        switchMap(idToken =>
          this.http.post<AuthResponse>(`${this.apiUrl}/login`, { idToken }).pipe(
            map(response => {
              const usuario: Usuario = {
                id: response.id,
                codigoInstitucional: response.codigoInstitucional,
                sede: response.sede,
                nombre: response.nombre,
                apellido: response.apellido,
                correoInstitucional: response.correoInstitucional,
                tipo: response.tipo,
                estado: response.estado
              };

              const authResponse = { ...response, token: idToken };
              localStorage.setItem('user', JSON.stringify(authResponse));

              // Remember Me
              if (request.rememberMe) {
                localStorage.setItem('rememberMe', 'true');
                localStorage.setItem('correo', request.correoInstitucional);
              } else {
                localStorage.removeItem('rememberMe');
                localStorage.removeItem('correo');
              }

              this.userSubject.next(usuario);
              return authResponse;
            }),
            catchError(err => this.handleError(err, 'Error al verificar con el servidor'))
          )
        ),
        catchError(err => this.handleError(err, 'Error al obtener token'))
      )
    ),
    catchError(err => this.handleError(err, 'Credenciales inválidas'))
  );
}
  // REGISTRO
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<any>(`${this.apiUrl}/registro`, request).pipe(
      switchMap(res => from(signInWithCustomToken(this.auth, res.token)).pipe(
        switchMap(cred => from(cred.user.getIdToken()).pipe(
          map(idToken => {
            const usuario: Usuario = {
              id: res.id,
              codigoInstitucional: res.codigoInstitucional,
              sede: res.sede,
              nombre: res.nombre,
              apellido: res.apellido,
              correoInstitucional: res.correoInstitucional,
              tipo: res.tipo,
              estado: res.estado
            };
            const authRes = { ...res, token: idToken };
            localStorage.setItem('user', JSON.stringify(authRes));
            this.userSubject.next(usuario);
            return authRes;
          })
        ))
      )),
      catchError(err => this.handleError(err, 'Error en registro'))
    );
  }

  // NUEVO: ACTUALIZAR USUARIO EN MEMORIA Y LOCALSTORAGE
  updateUser(updatedUser: Usuario): void {
    // Actualizar BehaviorSubject
    this.userSubject.next(updatedUser);

    // Refrescar token actual (opcional, pero recomendado)
    if (this.auth.currentUser) {
      from(this.auth.currentUser.getIdToken(true)).pipe(
        catchError(() => of(''))
      ).subscribe(newToken => {
        const authRes: AuthResponse = {
          id: updatedUser.id!,
          codigoInstitucional: updatedUser.codigoInstitucional,
          sede: updatedUser.sede,
          nombre: updatedUser.nombre,
          apellido: updatedUser.apellido,
          correoInstitucional: updatedUser.correoInstitucional,
          tipo: updatedUser.tipo,
          estado: updatedUser.estado,
          token: newToken || localStorage.getItem('user')?.match(/"token":"([^"]+)"/)?.[1] || ''
        };
        localStorage.setItem('user', JSON.stringify(authRes));
      });
    } else {
      // Si no hay sesión activa, solo actualiza datos
      const current = JSON.parse(localStorage.getItem('user') || '{}');
      const authRes: AuthResponse = {
        ...current,
        id: updatedUser.id!,
        codigoInstitucional: updatedUser.codigoInstitucional,
        sede: updatedUser.sede,
        nombre: updatedUser.nombre,
        apellido: updatedUser.apellido,
        correoInstitucional: updatedUser.correoInstitucional,
        tipo: updatedUser.tipo,
        estado: updatedUser.estado
      };
      localStorage.setItem('user', JSON.stringify(authRes));
    }
  }

  // LOGOUT
  logout(): Observable<string> {
    localStorage.removeItem('user');
    this.userSubject.next(null);
    return from(signOut(this.auth)).pipe(
      map(() => 'Sesión cerrada'),
      catchError(() => of('Sesión cerrada localmente'))
    );
  }

  // GETTER (opcional)
  getUser(): Usuario | null {
    return this.userSubject.value;
  }

  // MANEJO DE ERRORES
  private handleError(error: any, defaultMsg: string): Observable<never> {
    const msg = error.error?.message || error.message || defaultMsg;
    return throwError(() => new Error(msg));
  }
}
