import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { NgbOffcanvasModule } from '@ng-bootstrap/ng-bootstrap';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterOutlet, NgbOffcanvasModule],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent {
  user: any = null;
  isSidebarCollapsed = false;

  constructor(private authService: AuthService) {
    this.user = this.authService.getUser();
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        localStorage.removeItem('user');
        localStorage.removeItem('rememberMe');
        localStorage.removeItem('correo');
        window.location.href = '/login'; // Redirect to login
      },
      error: (err) => {
        console.error('Logout error:', err);
      }
    });
  }
}