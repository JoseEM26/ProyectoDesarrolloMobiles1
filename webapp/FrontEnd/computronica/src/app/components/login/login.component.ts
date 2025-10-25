import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { NgbAlertModule } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, NgbAlertModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  correo = '';
  contrasena = '';
  errorMessage = '';
  loading = false;
  showPassword = false;
  rememberMe = false;

  constructor(private authService: AuthService, private router: Router) {}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  login() {
    this.loading = true;
    this.errorMessage = '';
    if (!this.correo || !this.contrasena) {
      this.errorMessage = 'Todos los campos son obligatorios';
      this.loading = false;
      return;
    }
    if (this.contrasena.length < 6) {
      this.errorMessage = 'La contraseña debe tener al menos 6 caracteres';
      this.loading = false;
      return;
    }
    this.authService.login(this.correo, this.contrasena).subscribe({
      next: (response) => {
        this.authService.saveUser(response);
        if (this.rememberMe) {
          localStorage.setItem('rememberMe', 'true');
          localStorage.setItem('correo', this.correo);
        } else {
          localStorage.removeItem('rememberMe');
          localStorage.removeItem('correo');
        }
        this.router.navigate(['/dashboard']);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'Credenciales inválidas';
        this.loading = false;
        console.error('Login error:', err);
      }
    });
  }

  ngOnInit() {
    const rememberMe = localStorage.getItem('rememberMe');
    if (rememberMe === 'true') {
      this.correo = localStorage.getItem('correo') || '';
      this.rememberMe = true;
    }
  }
}