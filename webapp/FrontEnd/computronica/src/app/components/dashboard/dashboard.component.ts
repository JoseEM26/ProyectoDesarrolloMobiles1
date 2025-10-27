import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, BehaviorSubject, of, combineLatest, Subscription } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';
import { Asignatura, Calificaciones, Usuario, TipoUsuario } from '../../models/interfaces';
import Swal from 'sweetalert2';
import ChartDataLabels from 'chartjs-plugin-datalabels';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { AsignaturaService } from '../../services/AsignaturaService';
import { UsuarioService } from '../../services/UsuarioService';
import { CalificacionesService } from '../../services/CalificacionesService';
import { AuthService } from '../../services/AuthService';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('averagesChart') averagesChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('gradesDistributionChart') gradesDistributionChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('monthlyGradesChart') monthlyGradesChartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('hardestSubjectsChart') hardestSubjectsChartCanvas!: ElementRef<HTMLCanvasElement>;

  studentsCount$!: Observable<number>;
  subjectsCount$!: Observable<number>;
  gradesCount$!: Observable<number>;
  overallAverage$!: Observable<number>;
  filteredGrades$!: Observable<Calificaciones[]>;
  averageBySubject$!: Observable<{ asignaturaNombre: string; average: number }[]>;
  gradesDistribution$!: Observable<{ range: string; count: number }[]>;
  topStudents$!: Observable<{ estudianteNombre: string; average: number }[]>;
  monthlyGrades$!: Observable<{ month: string; count: number }[]>;
  hardestSubjects$!: Observable<{ asignaturaNombre: string; average: number }[]>;

  isLoadingGrades = true;
  isLoadingAverages = true;
  gradeFilter = 'all';
  user: Usuario | null = null;
  private loggingOut = false; // Flag to prevent notifications during logout

  private gradesSubject = new BehaviorSubject<Calificaciones[]>([]);
  private subscriptions = new Subscription();

  private averagesChart: Chart | null = null;
  private gradesDistributionChart: Chart | null = null;
  private monthlyGradesChart: Chart | null = null;
  private hardestSubjectsChart: Chart | null = null;

  constructor(
    private asignaturaService: AsignaturaService,
    private usuarioService: UsuarioService,
    private calificacionesService: CalificacionesService,
    private authService: AuthService
  ) {
    Chart.register(...registerables, ChartDataLabels);
  }

  private avatarIcons = [
    'bi-person-circle', 'bi-person-hearts', 'bi-person-badge', 'bi-person-check',
    'bi-person-fill', 'bi-person-lines-fill', 'bi-person-square', 'bi-person-vcard',
    'bi-person-workspace', 'bi-person-x', 'bi-person-rolodex', 'bi-person-video3'
  ];

  private avatarColors = [
    '#4361ee', '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#06b6d4'
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

  ngOnInit() {
    this.subscriptions.add(
      this.authService.user$.subscribe({
        next: (user) => {
          this.user = user;
          if (user && !this.loggingOut) {
            this.loadAllData();
            this.setupObservables();
          }
        },
        error: () => this.showError('Error al cargar usuario')
      })
    );
  }

  private setupObservables() {
    this.studentsCount$ = this.usuarioService.getAll().pipe(
      map(users => users.filter(u => u.tipo === TipoUsuario.estudiante).length),
      catchError(() => of(0))
    );

    this.subjectsCount$ = this.asignaturaService.getAll().pipe(
      map(subjects => subjects.length),
      catchError(() => of(0))
    );

    this.gradesCount$ = this.calificacionesService.getAll().pipe(
      map(grades => grades.length),
      catchError(() => of(0))
    );

    this.overallAverage$ = this.calificacionesService.getAll().pipe(
      map(grades => {
        const valid = grades.filter(g => typeof g.nota === 'number' && !isNaN(g.nota));
        return valid.length ? Number((valid.reduce((sum, g) => sum + g.nota, 0) / valid.length).toFixed(2)) : 0;
      }),
      catchError(() => of(0))
    );

    this.averageBySubject$ = combineLatest([
      this.calificacionesService.getAll(),
      this.asignaturaService.getAll()
    ]).pipe(
      tap(() => (this.isLoadingAverages = false)),
      map(([grades, asignaturas]) => {
        const map = new Map(asignaturas.map(a => [a.id!, a.nombre]));
        const averages: Record<string, { sum: number; count: number }> = {};

        grades.forEach(g => {
          if (!g.asignaturaId || typeof g.nota !== 'number') return;
          if (!averages[g.asignaturaId]) averages[g.asignaturaId] = { sum: 0, count: 0 };
          averages[g.asignaturaId].sum += g.nota;
          averages[g.asignaturaId].count += 1;
        });

        return Object.entries(averages)
          .map(([id, { sum, count }]) => ({
            asignaturaNombre: map.get(id) || id,
            average: Number((sum / count).toFixed(2))
          }))
          .sort((a, b) => b.average - a.average);
      }),
      catchError(() => of([]))
    );

    this.gradesDistribution$ = this.calificacionesService.getAll().pipe(
      map(grades => {
        const ranges = [
          { range: '0-10', min: 0, max: 10, count: 0 },
          { range: '11-15', min: 11, max: 15, count: 0 },
          { range: '16-20', min: 16, max: 20, count: 0 }
        ];
        grades.forEach(g => {
          if (typeof g.nota !== 'number') return;
          const range = ranges.find(r => g.nota >= r.min && g.nota <= r.max);
          if (range) range.count++;
        });
        return ranges.filter(r => r.count > 0);
      }),
      catchError(() => of([]))
    );

    this.topStudents$ = combineLatest([
      this.calificacionesService.getAll(),
      this.usuarioService.getAll()
    ]).pipe(
      map(([grades, usuarios]) => {
        const map = new Map(usuarios.map(u => [u.id!, `${u.nombre} ${u.apellido}`]));
        const averages: Record<string, { sum: number; count: number }> = {};

        grades.forEach(g => {
          if (!g.estudianteId || typeof g.nota !== 'number') return;
          if (!averages[g.estudianteId]) averages[g.estudianteId] = { sum: 0, count: 0 };
          averages[g.estudianteId].sum += g.nota;
          averages[g.estudianteId].count += 1;
        });

        return Object.entries(averages)
          .map(([id, { sum, count }]) => ({
            estudianteNombre: map.get(id) || id,
            average: Number((sum / count).toFixed(2))
          }))
          .sort((a, b) => b.average - a.average)
          .slice(0, 5);
      }),
      catchError(() => of([]))
    );

    this.monthlyGrades$ = this.calificacionesService.getAll().pipe(
      map(grades => {
        const map = new Map<string, number>();
        grades.forEach(g => {
          if (!g.fechaRegistroDate) return;
          const month = g.fechaRegistroDate.toLocaleDateString('es-ES', { month: 'short', year: 'numeric' });
          map.set(month, (map.get(month) || 0) + 1);
        });
        return Array.from(map.entries())
          .map(([month, count]) => ({ month, count }))
          .sort((a, b) => new Date(a.month + ' 1').getTime() - new Date(b.month + ' 1').getTime())
          .slice(-6);
      }),
      catchError(() => of([]))
    );

    this.hardestSubjects$ = this.averageBySubject$.pipe(
      map(subjects => subjects.sort((a, b) => a.average - b.average).slice(0, 5)),
      catchError(() => of([]))
    );

    this.filteredGrades$ = this.gradesSubject.asObservable();
  }

  private loadAllData() {
    if (this.loggingOut) return; // Skip if logging out
    this.isLoadingGrades = true;
    const grades$ = this.user?.tipo === TipoUsuario.estudiante
      ? this.calificacionesService.filterByEstudiante(this.user.id!)
      : this.calificacionesService.getAll();

    this.subscriptions.add(
      combineLatest([grades$, this.usuarioService.getAll(), this.asignaturaService.getAll()])
        .pipe(
          map(([grades, usuarios, asignaturas]) => {
            const usuarioMap = new Map(usuarios.map(u => [u.id!, `${u.nombre} ${u.apellido}`]));
            const asignaturaMap = new Map(asignaturas.map(a => [a.id!, a.nombre]));
            return this.mapGrades(grades, usuarioMap, asignaturaMap);
          }),
          map(grades => this.filterGradesByType(grades)),
          catchError(() => of([]))
        )
        .subscribe(grades => {
          this.gradesSubject.next(grades);
          this.isLoadingGrades = false;
          if (!this.loggingOut) {
            this.showSuccess(`Mostrando ${grades.length} calificaciones`);
          }
        })
    );
  }

  private filterGradesByType(grades: Calificaciones[]): Calificaciones[] {
    let filtered = grades.slice(0, 5);
    switch (this.gradeFilter) {
      case 'high':
        filtered = grades.sort((a, b) => b.nota - a.nota).slice(0, 5);
        break;
      case 'low':
        filtered = grades.sort((a, b) => a.nota - b.nota).slice(0, 5);
        break;
      case 'recent':
      default:
        filtered = grades.slice(0, 5);
        break;
    }
    return filtered;
  }

  filterGrades() {
    this.loadAllData();
  }

  refreshGrades() {
    this.gradeFilter = 'all';
    this.loadAllData();
  }

  getGradeClass(nota: number): string {
    if (nota >= 16) return 'excellent';
    if (nota >= 11) return 'good';
    if (nota >= 6) return 'average';
    return 'poor';
  }

  getGradeIcon(nota: number): string {
    if (nota >= 16) return 'bi bi-trophy-fill';
    if (nota >= 11) return 'bi bi-star-fill';
    if (nota >= 6) return 'bi bi-check-circle-fill';
    return 'bi bi-x-circle-fill';
  }

  getMedalIcon(index: number): string {
    return index === 0 ? 'bi bi-trophy-fill text-warning' :
           index === 1 ? 'bi bi-award-fill text-secondary' :
           index === 2 ? 'bi bi-medal-fill text-bronze' : 'bi bi-circle-fill text-muted';
  }

  private parseDate(dateStr: string | null): Date | null {
    if (!dateStr?.trim()) return null;
    const parts = dateStr.split(/[/\-]/);
    if (parts.length !== 3) return null;
    const [d, m, y] = parts.map(Number);
    if (isNaN(d) || isNaN(m) || isNaN(y)) return null;
    const date = new Date(y, m - 1, d);
    return isNaN(date.getTime()) ? null : date;
  }

  private mapGrades(
    grades: Calificaciones[],
    usuarioMap: Map<string, string>,
    asignaturaMap: Map<string, string>
  ): Calificaciones[] {
    return grades
      .filter(g => typeof g.nota === 'number' && !isNaN(g.nota))
      .map(g => ({
        ...g,
        estudianteNombre: usuarioMap.get(g.estudianteId) || g.estudianteId || 'Desconocido',
        asignaturaNombre: asignaturaMap.get(g.asignaturaId) || g.asignaturaId || 'N/A',
        fechaRegistroDate: this.parseDate(g.fechaRegistro)
      }))
      .sort((a, b) =>
        (b.fechaRegistroDate?.getTime() || 0) - (a.fechaRegistroDate?.getTime() || 0)
      );
  }

  ngAfterViewInit() {
    const safeSubscribe = <T>(
      obs$: Observable<T>,
      renderFn: (data: T) => void,
      canvas: ElementRef<HTMLCanvasElement> | undefined,
      chartInstance: Chart | null
    ) => {
      this.subscriptions.add(
        obs$.subscribe(data => {
          if (this.loggingOut) return; // Skip if logging out
          if (data && Array.isArray(data) && data.length > 0 && canvas?.nativeElement) {
            if (chartInstance) chartInstance.destroy();
            setTimeout(() => renderFn(data), 100);
          }
        })
      );
    };

    safeSubscribe(this.averageBySubject$, this.renderAveragesChart.bind(this), this.averagesChartCanvas, this.averagesChart);
    safeSubscribe(this.gradesDistribution$, this.renderGradesDistributionChart.bind(this), this.gradesDistributionChartCanvas, this.gradesDistributionChart);
    safeSubscribe(this.monthlyGrades$, this.renderMonthlyGradesChart.bind(this), this.monthlyGradesChartCanvas, this.monthlyGradesChart);
    safeSubscribe(this.hardestSubjects$, this.renderHardestSubjectsChart.bind(this), this.hardestSubjectsChartCanvas, this.hardestSubjectsChart);
  }

  renderAveragesChart(data: { asignaturaNombre: string; average: number }[]) {
    if (this.averagesChart) this.averagesChart.destroy();
    const ctx = this.averagesChartCanvas.nativeElement.getContext('2d')!;
    this.averagesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.asignaturaNombre),
        datasets: [{
          label: 'Promedio',
          data: data.map(d => d.average),
          backgroundColor: 'rgba(67, 97, 238, 0.6)',
          borderColor: '#4361ee',
          borderWidth: 2,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          datalabels: {
            anchor: 'end',
            align: 'top',
            color: 'var(--text)',
            font: { weight: 'bold' },
            formatter: v => v.toFixed(1)
          }
        },
        scales: { y: { beginAtZero: true, max: 20, ticks: { stepSize: 5 } } }
      }
    });
  }

  renderGradesDistributionChart(data: { range: string; count: number }[]) {
    if (this.gradesDistributionChart) this.gradesDistributionChart.destroy();
    const ctx = this.gradesDistributionChartCanvas.nativeElement.getContext('2d')!;
    this.gradesDistributionChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(d => d.range),
        datasets: [{
          data: data.map(d => d.count),
          backgroundColor: ['#ef4444', '#f59e0b', '#10b981'],
          borderWidth: 3,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right' },
          datalabels: {
            color: '#fff',
            formatter: (v, ctx: any) => {
              const total = ctx.dataset.data.reduce((a: number, b: number) => a + b, 0);
              return ((v / total) * 100).toFixed(0) + '%';
            },
            font: { weight: 'bold', size: 14 }
          }
        }
      }
    });
  }

  renderMonthlyGradesChart(data: { month: string; count: number }[]) {
    if (this.monthlyGradesChart) this.monthlyGradesChart.destroy();
    const ctx = this.monthlyGradesChartCanvas.nativeElement.getContext('2d')!;
    this.monthlyGradesChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: data.map(d => d.month),
        datasets: [{
          label: 'Calificaciones',
          data: data.map(d => d.count),
          borderColor: '#4361ee',
          backgroundColor: 'rgba(67, 97, 238, 0.1)',
          tension: 0.4,
          fill: true,
          pointBackgroundColor: '#4361ee',
          pointRadius: 6
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false },
          datalabels: { display: false }
        }
      }
    });
  }

  renderHardestSubjectsChart(data: { asignaturaNombre: string; average: number }[]) {
    if (this.hardestSubjectsChart) this.hardestSubjectsChart.destroy();
    const ctx = this.hardestSubjectsChartCanvas.nativeElement.getContext('2d')!;
    this.hardestSubjectsChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map(d => d.asignaturaNombre),
        datasets: [{
          label: 'Promedio',
          data: data.map(d => d.average),
          backgroundColor: 'rgba(239, 68, 68, 0.7)',
          borderColor: '#dc2626',
          borderWidth: 2
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        plugins: { legend: { display: false } },
        scales: { x: { max: 20 } }
      }
    });
  }

  async exportReport() {
    if (this.loggingOut) return; // Skip if logging out
    const doc = new jsPDF();
    const now = new Date();

    doc.setFontSize(20);
    doc.text('Reporte Académico', 20, 25);
    doc.setFontSize(12);
    doc.text(`Generado: ${now.toLocaleString()}`, 20, 35);
    doc.text(`Usuario: ${this.user?.nombre} ${this.user?.apellido}`, 20, 43);

    let y = 55;

    const [students, subjects, gradesCount, overallAvg] = await Promise.all([
      this.getValue(this.studentsCount$),
      this.getValue(this.subjectsCount$),
      this.getValue(this.gradesCount$),
      this.getValue(this.overallAverage$)
    ]);

    doc.setFontSize(14);
    doc.text('Estadísticas', 20, y);
    y += 10;

    autoTable(doc, {
      head: [['Métrica', 'Valor']],
      body: [
        ['Estudiantes', students?.toString() || '0'],
        ['Asignaturas', subjects?.toString() || '0'],
        ['Calificaciones', gradesCount?.toString() || '0'],
        ['Promedio General', `${overallAvg?.toFixed(2) || '0.00'}/20`]
      ],
      startY: y,
      theme: 'striped'
    });
    y = (doc as any).lastAutoTable.finalY + 15;

    const top = await this.getValue(this.topStudents$);
    if (top?.length) {
      doc.setFontSize(14);
      doc.text('Top 5 Estudiantes', 20, y);
      y += 10;
      autoTable(doc, {
        head: [['Estudiante', 'Promedio']],
        body: top.map(s => [s.estudianteNombre, s.average.toFixed(2)]),
        startY: y,
        theme: 'striped'
      });
      y = (doc as any).lastAutoTable.finalY + 15;
    }

    const grades = await this.getValue(this.filteredGrades$);
    if (grades?.length) {
      doc.addPage();
      doc.setFontSize(14);
      doc.text('Calificaciones Recientes', 20, 20);
      autoTable(doc, {
        head: [['Estudiante', 'Asignatura', 'Evaluación', 'Nota', 'Fecha']],
        body: grades.slice(0, 10).map(g => [
          g.estudianteNombre || 'N/A',
          g.asignaturaNombre || 'N/A',
          g.evaluacion || 'N/A',
          g.nota.toFixed(1),
          g.fechaRegistroDate?.toLocaleDateString('es-ES') || 'N/A'
        ]),
        startY: 30,
        theme: 'striped'
      });
    }

    doc.save(`reporte-${now.toISOString().split('T')[0]}.pdf`);
    if (!this.loggingOut) {
      this.showSuccess('Reporte exportado');
    }
  }

  private async getValue<T>(obs: Observable<T>): Promise<T | null> {
    return new Promise(resolve => {
      const sub = obs.subscribe({
        next: v => { resolve(v); sub.unsubscribe(); },
        error: () => { resolve(null); sub.unsubscribe(); }
      });
    });
  }

  private showSuccess(msg: string) {
    if (this.loggingOut) return; // Skip notifications during logout
    Swal.fire({
      icon: 'success',
      title: 'Éxito',
      text: msg,
      timer: 1500,
      showConfirmButton: false,
      toast: true,
      position: 'top-end',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }

  private showError(msg: string) {
    if (this.loggingOut) return; // Skip notifications during logout
    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: msg,
      confirmButtonText: 'OK',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }

  // Called by layout.component.ts before logout
  prepareForLogout() {
    this.loggingOut = true;
    this.subscriptions.unsubscribe(); // Cancel all data loading
    [this.averagesChart, this.gradesDistributionChart, this.monthlyGradesChart, this.hardestSubjectsChart]
      .forEach(chart => chart?.destroy());
  }

  ngOnDestroy() {
    this.prepareForLogout();
  }
}
