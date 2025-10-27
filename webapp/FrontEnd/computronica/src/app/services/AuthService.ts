import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, from } from 'rxjs';
import { catchError, tap, switchMap, map } from 'rxjs/operators';
import { Auth, signInWithEmailAndPassword, signInWithCustomToken, signOut } from '@angular/fire/auth';
import { AuthResponse, LoginRequest, RegisterRequest, Usuario } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private userSubject = new BehaviorSubject<Usuario | null>(null);
  user$ = this.userSubject.asObservable();

 constructor(private http: HttpClient, private auth: Auth) {
    const userData = localStorage.getItem('user');
    if (userData) {
      try {
        const parsedUser = JSON.parse(userData) as AuthResponse;
        const usuario: Usuario = {
          id: parsedUser.id,
          codigoInstitucional: parsedUser.codigoInstitucional,
          sede: parsedUser.sede,
          nombre: parsedUser.nombre,
          apellido: parsedUser.apellido,
          correoInstitucional: parsedUser.correoInstitucional,
          tipo: parsedUser.tipo,
          estado: parsedUser.estado
        };
        this.userSubject.next(usuario);
      } catch (error) {
        console.error('Error parsing user from localStorage:', error);
        localStorage.removeItem('user');
      }
    }

    this.auth.onAuthStateChanged(async (firebaseUser) => {
      if (firebaseUser && this.userSubject.value) {
        try {
          const idToken = await firebaseUser.getIdToken(true);
          await this.verifyUserWithBackend(idToken).toPromise();
        } catch (error) {
          console.error('Error verifying user with backend:', error);
          this.logout().subscribe({
            error: (err) => console.error('Auto-logout failed:', err)
          });
        }
      } else if (!firebaseUser) {
        this.userSubject.next(null);
        localStorage.removeItem('user');
      }
    });
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/registro`, request).pipe(
      switchMap((response) =>
        from(signInWithCustomToken(this.auth, response.token)).pipe(
          switchMap((userCredential) =>
            from(userCredential.user.getIdToken()).pipe(
              map((idToken) => {
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
                this.userSubject.next(usuario);
                return authResponse;
              })
            )
          )
        )
      ),
      catchError((error) => this.handleError(error, 'Error en registro'))
    );
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return from(signInWithEmailAndPassword(this.auth, request.correoInstitucional, request.contrasena)).pipe(
      switchMap((userCredential) =>
        from(userCredential.user.getIdToken()).pipe(
          switchMap((idToken) =>
            this.http.post<AuthResponse>(`${this.apiUrl}/login`, { idToken }).pipe(
              map((response) => {
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
                if (request.rememberMe) {
                  localStorage.setItem('rememberMe', 'true');
                  localStorage.setItem('correo', request.correoInstitucional);
                }
                this.userSubject.next(usuario);
                return authResponse;
              }),
              catchError((error) => this.handleError(error, 'Error al verificar usuario con el servidor'))
            )
          ),
          catchError((error) => this.handleError(error, 'Error al obtener el token de Firebase'))
        )
      ),
      catchError((error) => this.handleError(error, 'Error en autenticación con Firebase'))
    );
  }

  logout(): Observable<string> {
    const userData = localStorage.getItem('user');
    const clearLocalState = () => {
      localStorage.removeItem('user');
      localStorage.removeItem('rememberMe');
      localStorage.removeItem('correo');
      this.userSubject.next(null);
    };

    // If no user data or no Firebase user, just sign out from Firebase
    if (!userData || !this.auth.currentUser) {
      clearLocalState();
      return from(signOut(this.auth)).pipe(
        map(() => 'Sesión cerrada exitosamente'),
        catchError((error) => this.handleError(error, 'Error al cerrar sesión en Firebase'))
      );
    }

    // Try to refresh the token before logout
    return from(this.auth.currentUser.getIdToken(true)).pipe(
      switchMap((freshToken) => {
        const headers = new HttpHeaders().set('Authorization', `Bearer ${freshToken}`);
        return this.http.post(`${this.apiUrl}/logout`, {}, { headers, responseType: 'text' }).pipe(
          tap(clearLocalState),
          switchMap(() => from(signOut(this.auth)).pipe(map(() => 'Sesión cerrada exitosamente'))),
          catchError((error) => {
            console.warn('Backend logout failed:', error);
            clearLocalState();
            return from(signOut(this.auth)).pipe(
              map(() => 'Sesión cerrada exitosamente'),
              catchError((fbError) => this.handleError(fbError, 'Error al cerrar sesión en Firebase'))
            );
          })
        );
      }),
      catchError((error) => {
        console.warn('Token refresh failed:', error);
        clearLocalState();
        return from(signOut(this.auth)).pipe(
          map(() => 'Sesión cerrada exitosamente'),
          catchError((fbError) => this.handleError(fbError, 'Error al cerrar sesión en Firebase'))
        );
      })
    );
  }

  getUser(): Usuario | null {
    return this.userSubject.value;
  }

  private verifyUserWithBackend(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { idToken }).pipe(
      catchError((error) => this.handleError(error, 'Error al verificar usuario con el servidor'))
    );
  }

  private handleError(error: any, defaultMessage: string): Observable<never> {
    let message = defaultMessage;
    if (error.error?.message) {
      message = error.error.message;
    } else if (error.code) {
      switch (error.code) {
        case 'auth/invalid-email':
          message = 'Correo electrónico inválido';
          break;
        case 'auth/user-disabled':
          message = 'Cuenta desactivada';
          break;
        case 'auth/user-not-found':
        case 'auth/wrong-password':
          message = 'Credenciales inválidas';
          break;
        case 'auth/invalid-credential':
          message = 'Token inválido o expirado';
          break;
        default:
          message = `${defaultMessage}: ${error.message}`;
      }
    } else if (error.status) {
      switch (error.status) {
        case 400:
          message = 'Solicitud inválida';
          break;
        case 401:
          message = 'No autorizado: token inválido';
          break;
        case 403:
          message = 'Acceso denegado: cuenta desactivada';
          break;
        case 404:
          message = 'Usuario no encontrado';
          break;
        default:
          message = `${defaultMessage}: ${error.message || 'Error desconocido'}`;
      }
    }
    console.error('Error in AuthService:', error);
    return throwError(() => new Error(message));
  }
  updateUser(user: Usuario): void {
    this.userSubject.next(user);
    localStorage.setItem('user', JSON.stringify({
      id: user.id,
      codigoInstitucional: user.codigoInstitucional,
      sede: user.sede,
      nombre: user.nombre,
      apellido: user.apellido,
      correoInstitucional: user.correoInstitucional,
      tipo: user.tipo,
      estado: user.estado,
      token: this.auth.currentUser ? this.auth.currentUser.getIdToken() : ''
    }));
  }
}
