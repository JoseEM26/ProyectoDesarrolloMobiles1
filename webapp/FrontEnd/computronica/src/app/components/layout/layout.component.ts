import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NgbDropdownModule } from '@ng-bootstrap/ng-bootstrap';
import { Usuario, TipoUsuario } from '../../models/interfaces';
import Swal from 'sweetalert2';
import { AuthService } from '../../services/AuthService';

declare const bootstrap: any;

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NgbDropdownModule],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  isSidebarCollapsed = false;
  isDarkMode = false;
  user: Usuario | null = null;
  tipoUsuario = TipoUsuario;
  private offcanvasInstance: any = null;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authService.user$.subscribe({
      next: (user) => {
        this.user = user;
      },
      error: (err) => {
        console.error('Error fetching user:', err);
        Swal.fire({
          icon: 'error',
          title: 'Error',
          text: 'No se pudo cargar la información del usuario',
          confirmButtonColor: 'var(--primary)',
          background: 'var(--white)',
          customClass: { popup: 'swal2-popup-custom' }
        });
      }
    });

    this.isDarkMode = localStorage.getItem('darkMode') === 'true';
    this.updateDarkMode();
  }

  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;

    if (!this.isSidebarCollapsed && window.innerWidth < 992) {
      const offcanvasElement = document.getElementById('sidebarOffcanvas');
      if (offcanvasElement && !this.offcanvasInstance) {
        this.offcanvasInstance = new bootstrap.Offcanvas(offcanvasElement);
        this.offcanvasInstance.show();
      }
    } else {
      this.closeSidebar();
    }
  }

  closeSidebar() {
    if (this.offcanvasInstance) {
      this.offcanvasInstance.hide();
      setTimeout(() => {
        this.offcanvasInstance = null;
      }, 300);
    }
  }

  toggleDarkMode() {
    this.isDarkMode = !this.isDarkMode;
    localStorage.setItem('darkMode', this.isDarkMode.toString());
    this.updateDarkMode();
    Swal.fire({
      icon: 'success',
      title: this.isDarkMode ? 'Modo Oscuro Activado' : 'Modo Claro Activado',
      timer: 1500,
      showConfirmButton: false,
      background: 'var(--white)',
      customClass: { popup: 'swal2-popup-custom' }
    });
  }

  private avatarIcons = [
    'bi-person-circle', 'bi-person-hearts', 'bi-person-badge', 'bi-person-check',
    'bi-person-fill', 'bi-person-lines-fill', 'bi-person-square', 'bi-person-vcard'
  ];

  private avatarColors = [
    '#4361ee', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'
  ];

  getRandomAvatarIcon(id: string): string {
    const hash = this.hashString(id);
    return this.avatarIcons[hash % this.avatarIcons.length];
  }

  getRandomAvatarColor(id: string): string {
    const hash = this.hashString(id);
    return this.avatarColors[hash % this.avatarColors.length];
  }

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }

  private updateDarkMode() {
    document.body.classList.toggle('dark-mode', this.isDarkMode);
  }

  onNavClick() {
    if (window.innerWidth < 992) {
      this.closeSidebar();
    }
  }

  confirmLogout() {
    Swal.fire({
      title: '¿Cerrar sesión?',
      text: 'Tu sesión será cerrada y volverás al inicio',
      icon: 'question',
      iconHtml: '<i class="bi bi-box-arrow-right text-danger"></i>',
      showCancelButton: true,
      confirmButtonText: '<i class="bi bi-check-circle-fill me-2"></i>Sí, cerrar',
      cancelButtonText: '<i class="bi bi-x-circle-fill me-2"></i>Cancelar',
      reverseButtons: true,
      buttonsStyling: false,
      customClass: {
        popup: 'border-0 shadow-lg swal2-popup-custom',
        title: 'h5 fw-bold text-dark',
        htmlContainer: 'text-muted small',
        confirmButton: 'btn btn-primary btn-lg px-4 me-3',
        cancelButton: 'btn btn-outline-secondary btn-lg px-4'
      },
      background: 'var(--card-bg)',
      backdrop: `
        rgba(0,0,0,0.4)
        url('https://media.giphy.com/media/3o7abldj0b3rxrZUxW/giphy.gif')
        left top
        no-repeat
      `,
      allowOutsideClick: false,
      heightAuto: false
    }).then((result) => {
      if (result.isConfirmed) {
        this.authService.logout().subscribe({
          next: () => {
            // Clear any pending SweetAlert2 toasts
            Swal.close();
            Swal.fire({
              icon: 'success',
              title: '¡Sesión cerrada!',
              text: 'Has salido correctamente',
              iconHtml: '<i class="bi bi-check-circle-fill text-success"></i>',
              timer: 2000,
              timerProgressBar: true,
              showConfirmButton: false,
              toast: true,
              position: 'top-end',
              background: 'var(--card-bg)',
              customClass: { popup: 'shadow-lg border-0' }
            }).then(() => {
              this.router.navigate(['/login'], { replaceUrl: true });
            });
          },
          error: (err) => {
            console.error('Logout error details:', err);
            Swal.fire({
              icon: 'error',
              title: 'Error',
              text: 'No se pudo cerrar sesión. Intenta de nuevo.',
              confirmButtonText: 'OK',
              background: 'var(--card-bg)',
              customClass: { popup: 'shadow-lg border-0' }
            });
          }
        });
      }
    });
  }

  ngOnDestroy() {
    this.closeSidebar();
  }
}
