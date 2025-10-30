// src/app/components/dashboard/dashboard.component.ts
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
  monthlyGrades$!: Observable<{ month: string; count: number }[]>;
  hardestSubjects$!: Observable<{ asignaturaNombre: string; average: number }[]>;
  topEliteStudents$!: Observable<{ estudianteId: string; estudianteNombre: string; average: number }[]>;
  topStudents$!: Observable<{ estudianteId: string; estudianteNombre: string; average: number }[]>;
  isExporting = false;
  isLoadingGrades = true;
  isLoadingAverages = true;
  gradeFilter = 'all';
  user: Usuario | null = null;
  tipoUsuario = TipoUsuario; // Expose TipoUsuario for template
  private loggingOut = false;
  private gradesSubject = new BehaviorSubject<Calificaciones[]>([]);
  private subscriptions = new Subscription();

  private averagesChart: Chart | null = null;
  private gradesDistributionChart: Chart | null = null;
  private monthlyGradesChart: Chart | null = null;
  private hardestSubjectsChart: Chart | null = null;

  private avatarIcons = [
    'bi-person-circle', 'bi-person-hearts', 'bi-person-badge', 'bi-person-check',
    'bi-person-fill', 'bi-person-lines-fill', 'bi-person-square', 'bi-person-vcard',
    'bi-person-workspace', 'bi-person-x', 'bi-person-rolodex', 'bi-person-video3'
  ];

  private avatarColors = [
    '#4361ee', '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
    '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#06b6d4'
  ];

  constructor(
    private asignaturaService: AsignaturaService,
    private usuarioService: UsuarioService,
    private calificacionesService: CalificacionesService,
    private authService: AuthService
  ) {
    Chart.register(...registerables, ChartDataLabels);
  }

  ngOnInit() {
    this.subscriptions.add(
      this.authService.user$.subscribe({
        next: (user) => {
          console.log('DEPURACIÓN: Usuario autenticado:', user);
          this.user = user;
          if (user && !this.loggingOut) {
            console.log('DEPURACIÓN: Iniciando carga de datos para usuario:', user.id, user.tipo);
            this.loadAllData();
            this.setupObservables();
          } else {
            console.warn('DEPURACIÓN: No hay usuario o se está cerrando sesión');
            this.showError('Usuario no autenticado');
          }
        },
        error: (err) => {
          console.error('DEPURACIÓN: Error al cargar usuario:', err);
          this.showError('Error al cargar usuario');
        }
      })
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
          if (this.loggingOut) return;
          if (data && Array.isArray(data) && data.length > 0 && canvas?.nativeElement) {
            if (chartInstance) chartInstance.destroy();
            setTimeout(() => renderFn(data), 100);
          } else {
            console.warn('DEPURACIÓN: Datos vacíos o inválidos para gráfico:', data);
          }
        })
      );
    };

    safeSubscribe(this.averageBySubject$, this.renderAveragesChart.bind(this), this.averagesChartCanvas, this.averagesChart);
    safeSubscribe(this.gradesDistribution$, this.renderGradesDistributionChart.bind(this), this.gradesDistributionChartCanvas, this.gradesDistributionChart);
    safeSubscribe(this.monthlyGrades$, this.renderMonthlyGradesChart.bind(this), this.monthlyGradesChartCanvas, this.monthlyGradesChart);
    safeSubscribe(this.hardestSubjects$, this.renderHardestSubjectsChart.bind(this), this.hardestSubjectsChartCanvas, this.hardestSubjectsChart);
  }

  ngOnDestroy() {
    console.log('DEPURACIÓN: Destruyendo componente');
    this.prepareForLogout();
  }

  private setupObservables() {
    // 1. CONTADORES SIMPLES
    this.studentsCount$ = this.usuarioService.getAll().pipe(
      map(users => {
        if (this.user?.tipo !== TipoUsuario.administrativo) return 0; // Hide for non-admins
        const count = users.filter(u => u.tipo === TipoUsuario.estudiante).length;
        console.log('DEPURACIÓN: Cantidad de estudiantes:', count);
        return count;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al cargar estudiantes:', err);
        return of(0);
      })
    );

    this.subjectsCount$ = combineLatest([
      this.asignaturaService.getAll(),
      this.user ? of(this.user) : this.authService.user$
    ]).pipe(
      map(([subjects, user]) => {
        if (!user) return 0;
        const filteredSubjects = user.tipo === TipoUsuario.profesor
          ? subjects.filter(s => s.profesores?.includes(user.correoInstitucional))
          : subjects;
        console.log('DEPURACIÓN: Asignaturas filtradas:', filteredSubjects);
        return filteredSubjects.length;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al cargar asignaturas:', err);
        return of(0);
      })
    );

    this.gradesCount$ = this.getFilteredGrades().pipe(
      map(grades => {
        console.log('DEPURACIÓN: Calificaciones filtradas para conteo:', grades);
        return grades.length;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al cargar calificaciones:', err);
        return of(0);
      })
    );

    // 2. PROMEDIO GENERAL
    this.overallAverage$ = this.getFilteredGrades().pipe(
      map(grades => {
        const valid = grades.filter(g => typeof Number(g.nota) === 'number' && !isNaN(Number(g.nota)));
        console.log('DEPURACIÓN: Calificaciones válidas para promedio:', valid);
        const avg = valid.length
          ? Number((valid.reduce((sum, g) => sum + Number(g.nota), 0) / valid.length).toFixed(2))
          : 0;
        console.log('DEPURACIÓN: Promedio calculado:', avg);
        return avg;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular promedio:', err);
        return of(0);
      })
    );

    // 3. PROMEDIO POR ASIGNATURA
    this.averageBySubject$ = combineLatest([
      this.getFilteredGrades(),
      this.asignaturaService.getAll()
    ]).pipe(
      tap(() => (this.isLoadingAverages = false)),
      map(([grades, asignaturas]) => {
        console.log('DEPURACIÓN: Calificaciones para promedio por asignatura:', grades);
        console.log('DEPURACIÓN: Asignaturas:', asignaturas);
        const asignaturaMap = new Map(asignaturas.map(a => [a.id!, a.nombre]));
        const averages: Record<string, { sum: number; count: number }> = {};

        grades.forEach(g => {
          const nota = Number(g.nota);
          if (!g.asignaturaId || isNaN(nota)) {
            console.warn('DEPURACIÓN: Calificación inválida:', g);
            return;
          }
          if (!averages[g.asignaturaId]) averages[g.asignaturaId] = { sum: 0, count: 0 };
          averages[g.asignaturaId].sum += nota;
          averages[g.asignaturaId].count += 1;
        });

        const result = Object.entries(averages)
          .map(([id, { sum, count }]) => ({
            asignaturaNombre: asignaturaMap.get(id) || id,
            average: count > 0 ? Number((sum / count).toFixed(2)) : 0
          }))
          .sort((a, b) => b.average - a.average);
        console.log('DEPURACIÓN: Promedio por asignatura:', result);
        return result;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular promedio por asignatura:', err);
        return of([]);
      })
    );

    // 4. DISTRIBUCIÓN DE NOTAS
    this.gradesDistribution$ = this.getFilteredGrades().pipe(
      map(grades => {
        const ranges = [
          { range: '0-10', min: 0, max: 10, count: 0 },
          { range: '11-15', min: 11, max: 15, count: 0 },
          { range: '16-20', min: 16, max: 20, count: 0 }
        ];
        grades.forEach(g => {
          const nota = Number(g.nota);
          if (isNaN(nota)) {
            console.warn('DEPURACIÓN: Nota inválida:', g);
            return;
          }
          const range = ranges.find(r => nota >= r.min && nota <= r.max);
          if (range) range.count++;
        });
        const result = ranges.filter(r => r.count > 0);
        console.log('DEPURACIÓN: Distribución de notas:', result);
        return result;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular distribución de notas:', err);
        return of([]);
      })
    );

    // 5. TOP 5 ESTUDIANTES
    this.topStudents$ = combineLatest([
      this.getFilteredGrades(),
      this.usuarioService.getAll()
    ]).pipe(
      map(([grades, usuarios]) => {
        console.log('DEPURACIÓN: Calificaciones para top estudiantes:', grades);
        console.log('DEPURACIÓN: Usuarios:', usuarios);
        const usuarioMap = new Map(usuarios.map(u => [u.id!, `${u.nombre || ''} ${u.apellido || ''}`.trim() || 'Desconocido']));
        const averages: Record<string, { sum: number; count: number }> = {};

        grades.forEach(g => {
          const nota = Number(g.nota);
          if (!g.estudianteId || isNaN(nota)) {
            console.warn('DEPURACIÓN: Calificación inválida:', g);
            return;
          }
          if (!averages[g.estudianteId]) averages[g.estudianteId] = { sum: 0, count: 0 };
          averages[g.estudianteId].sum += nota;
          averages[g.estudianteId].count += 1;
        });

        const result = Object.entries(averages)
          .map(([id, { sum, count }]) => ({
            estudianteId: id,
            estudianteNombre: usuarioMap.get(id) || 'Estudiante Desconocido',
            average: count > 0 ? Number((sum / count).toFixed(2)) : 0
          }))
          .filter(s => s.average > 0)
          .sort((a, b) => b.average - a.average)
          .slice(0, 5);
        console.log('DEPURACIÓN: Top 5 estudiantes:', result);
        return result;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular top estudiantes:', err);
        return of([]);
      })
    );

    // 6. TOP ÉLITE: SOLO ≥17.0
    this.topEliteStudents$ = this.topStudents$.pipe(
      map(students => {
        let elite = students.filter(s => s.average >= 17.0);
        if (this.user?.tipo === TipoUsuario.estudiante) {
          // Only show the student's own position if they qualify
          elite = elite.filter(s => s.estudianteId === this.user!.id);
        }
        console.log('DEPURACIÓN: Top Elite estudiantes:', elite);
        if (elite.length === 0) {
          console.warn('DEPURACIÓN: No hay estudiantes con promedio ≥ 17.0');
        }
        return elite;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular top elite estudiantes:', err);
        return of([]);
      })
    );

    // 7. NOTAS POR MES
    this.monthlyGrades$ = this.getFilteredGrades().pipe(
      map(grades => {
        const map = new Map<string, number>();
        grades.forEach(g => {
          if (!g.fechaRegistroDate) {
            console.warn('DEPURACIÓN: Fecha de registro inválida:', g);
            return;
          }
          const month = g.fechaRegistroDate.toLocaleDateString('es-ES', { month: 'short', year: 'numeric' });
          map.set(month, (map.get(month) || 0) + 1);
        });
        const result = Array.from(map.entries())
          .map(([month, count]) => ({ month, count }))
          .sort((a, b) => new Date(a.month + ' 1').getTime() - new Date(b.month + ' 1').getTime())
          .slice(-6);
        console.log('DEPURACIÓN: Notas por mes:', result);
        return result;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular notas por mes:', err);
        return of([]);
      })
    );

    // 8. ASIGNATURAS MÁS DIFÍCILES
    this.hardestSubjects$ = this.averageBySubject$.pipe(
      map(subjects => {
        if (this.user?.tipo === TipoUsuario.estudiante) return []; // Hide for students
        const result = subjects.sort((a, b) => a.average - b.average).slice(0, 5);
        console.log('DEPURACIÓN: Asignaturas más difíciles:', result);
        return result;
      }),
      catchError((err) => {
        console.error('DEPURACIÓN: Error al calcular asignaturas más difíciles:', err);
        return of([]);
      })
    );

    // 9. CALIFICACIONES FILTRADAS
    this.filteredGrades$ = this.gradesSubject.asObservable().pipe(
      tap(grades => console.log('DEPURACIÓN: Calificaciones filtradas:', grades))
    );
  }

  private getFilteredGrades(): Observable<Calificaciones[]> {
    if (!this.user) return of([]);
    if (this.user.tipo === TipoUsuario.estudiante) {
      return this.calificacionesService.filterByEstudiante(this.user.id!).pipe(
        tap(grades => console.log('DEPURACIÓN: Calificaciones filtradas por estudiante:', grades)),
        catchError((err) => {
          console.error('DEPURACIÓN: Error al filtrar calificaciones por estudiante:', err);
          return of([]);
        })
      );
    } else if (this.user.tipo === TipoUsuario.profesor) {
      return combineLatest([
        this.calificacionesService.getAll(),
        this.asignaturaService.getAll()
      ]).pipe(
        map(([grades, asignaturas]) => {
          const assignedAsignaturas = asignaturas.filter(asig =>
            asig.profesores?.includes(this.user!.correoInstitucional)
          );
          const assignedAsignaturaIds = assignedAsignaturas.map(asig => asig.id).filter((id): id is string => !!id);
          const filteredGrades = grades.filter(grade => assignedAsignaturaIds.includes(grade.asignaturaId));
          console.log('DEPURACIÓN: Calificaciones filtradas por profesor:', filteredGrades);
          return filteredGrades;
        }),
        catchError((err) => {
          console.error('DEPURACIÓN: Error al filtrar calificaciones por profesor:', err);
          return of([]);
        })
      );
    } else {
      return this.calificacionesService.getAll().pipe(
        tap(grades => console.log('DEPURACIÓN: Todas las calificaciones:', grades)),
        catchError((err) => {
          console.error('DEPURACIÓN: Error al cargar todas las calificaciones:', err);
          return of([]);
        })
      );
    }
  }

  private loadAllData() {
    if (this.loggingOut || !this.user) {
      console.warn('DEPURACIÓN: No se carga datos porque loggingOut es true o no hay usuario');
      return;
    }
    this.isLoadingGrades = true;
    console.log('DEPURACIÓN: Tipo de usuario:', this.user.tipo, 'ID:', this.user.id);

    this.subscriptions.add(
      combineLatest([this.getFilteredGrades(), this.usuarioService.getAll(), this.asignaturaService.getAll()])
        .pipe(
          map(([grades, usuarios, asignaturas]) => {
            console.log('DEPURACIÓN: Datos combinados:', { grades, usuarios, asignaturas });
            const usuarioMap = new Map(usuarios.map(u => [u.id!, `${u.nombre || ''} ${u.apellido || ''}`.trim() || 'Desconocido']));
            const asignaturaMap = new Map(asignaturas.map(a => [a.id!, a.nombre]));
            return this.mapGrades(grades, usuarioMap, asignaturaMap);
          }),
          map(grades => this.filterGradesByType(grades)),
          catchError((err) => {
            console.error('DEPURACIÓN: Error al procesar datos combinados:', err);
            return of([]);
          })
        )
        .subscribe(grades => {
          console.log('DEPURACIÓN: Calificaciones mapeadas y filtradas:', grades);
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
        filtered = grades.sort((a, b) => Number(b.nota) - Number(a.nota)).slice(0, 5);
        break;
      case 'low':
        filtered = grades.sort((a, b) => Number(a.nota) - Number(a.nota)).slice(0, 5);
        break;
      case 'recent':
      default:
        filtered = grades.slice(0, 5);
        break;
    }
    console.log('DEPURACIÓN: Calificaciones filtradas por tipo:', filtered);
    return filtered;
  }

  filterGrades() {
    console.log('DEPURACIÓN: Filtrando calificaciones con filtro:', this.gradeFilter);
    this.loadAllData();
  }

  refreshGrades() {
    console.log('DEPURACIÓN: Refrescando calificaciones');
    this.gradeFilter = 'all';
    this.loadAllData();
  }

  getRandomAvatarIcon(id: string): string {
    const hash = this.hashString(id);
    return this.avatarIcons[hash % this.avatarIcons.length];
  }

  getRandomAvatarColor(id: string): string {
    const hash = this.hashString(id);
    return this.avatarColors[hash % this.avatarColors.length];
  }

  getNotaBadgeClass(nota: number): string {
    if (nota <= 12) return 'bg-danger';
    if (nota <= 16) return 'bg-warning';
    return 'bg-success';
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

  private hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash);
  }

  private parseDate(dateStr: string | null): Date | null {
    if (!dateStr?.trim()) {
      console.warn('DEPURACIÓN: Fecha de registro vacía o inválida:', dateStr);
      return null;
    }
    const parts = dateStr.split(/[/\-]/);
    if (parts.length !== 3) {
      console.warn('DEPURACIÓN: Formato de fecha inválido:', dateStr);
      return null;
    }
    const [d, m, y] = parts.map(Number);
    if (isNaN(d) || isNaN(m) || isNaN(y)) {
      console.warn('DEPURACIÓN: Componentes de fecha inválidos:', dateStr);
      return null;
    }
    const date = new Date(y, m - 1, d);
    if (isNaN(date.getTime())) {
      console.warn('DEPURACIÓN: Fecha inválida:', dateStr);
      return null;
    }
    return date;
  }

  private mapGrades(
    grades: Calificaciones[],
    usuarioMap: Map<string, string>,
    asignaturaMap: Map<string, string>
  ): Calificaciones[] {
    const mappedGrades = grades
      .filter(g => {
        const nota = Number(g.nota);
        const isValid = typeof nota === 'number' && !isNaN(nota) && g.estudianteId;
        if (!isValid) {
          console.warn('DEPURACIÓN: Calificación inválida en mapGrades:', g);
        }
        return isValid;
      })
      .map(g => ({
        ...g,
        nota: Number(g.nota),
        estudianteNombre: usuarioMap.get(g.estudianteId) || g.estudianteId || 'Desconocido',
        asignaturaNombre: asignaturaMap.get(g.asignaturaId) || g.asignaturaId || 'N/A',
        fechaRegistroDate: this.parseDate(g.fechaRegistro)
      }))
      .sort((a, b) =>
        (b.fechaRegistroDate?.getTime() || 0) - (a.fechaRegistroDate?.getTime() || 0)
      );
    console.log('DEPURACIÓN: Calificaciones mapeadas:', mappedGrades);
    return mappedGrades;
  }

  async exportReport() {
    if (this.user?.tipo !== TipoUsuario.administrativo) {
      this.showError('Solo los administrativos pueden exportar reportes');
      return;
    }
    if (this.loggingOut) return;
    console.log('DEPURACIÓN: Exportando reporte');
    this.isExporting = true;
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

    console.log('DEPURACIÓN: Estadísticas para reporte:', { students, subjects, gradesCount, overallAvg });

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
    console.log('DEPURACIÓN: Top estudiantes para reporte:', top);
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
    console.log('DEPURACIÓN: Calificaciones para reporte:', grades);
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
          Number(g.nota).toFixed(1),
          g.fechaRegistroDate?.toLocaleDateString('es-ES') || 'N/A'
        ]),
        startY: 30,
        theme: 'striped'
      });
    }

    doc.save(`reporte-${now.toISOString().split('T')[0]}.pdf`);
    this.isExporting = false;
    if (!this.loggingOut) {
      this.showSuccess('Reporte exportado');
    }
  }

  private async getValue<T>(obs: Observable<T>): Promise<T | null> {
    return new Promise(resolve => {
      const sub = obs.subscribe({
        next: v => {
          console.log('DEPURACIÓN: Valor obtenido de observable:', v);
          resolve(v);
          sub.unsubscribe();
        },
        error: (err) => {
          console.error('DEPURACIÓN: Error al obtener valor de observable:', err);
          resolve(null);
          sub.unsubscribe();
        }
      });
    });
  }

  private showSuccess(msg: string) {
    if (this.loggingOut) return;
    console.log('DEPURACIÓN: Mostrando mensaje de éxito:', msg);
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
    if (this.loggingOut) return;
    console.error('DEPURACIÓN: Mostrando mensaje de error:', msg);
    Swal.fire({
      icon: 'error',
      title: 'Error',
      text: msg,
      confirmButtonText: 'OK',
      background: 'var(--card-bg)',
      customClass: { popup: 'shadow-lg border-0' }
    });
  }

  prepareForLogout() {
    console.log('DEPURACIÓN: Preparando para cerrar sesión');
    this.loggingOut = true;
    this.subscriptions.unsubscribe();
    [this.averagesChart, this.gradesDistributionChart, this.monthlyGradesChart, this.hardestSubjectsChart]
      .forEach(chart => chart?.destroy());
  }

  renderAveragesChart(data: { asignaturaNombre: string; average: number }[]) {
    console.log('DEPURACIÓN: Renderizando gráfico de promedios:', data);
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
    console.log('DEPURACIÓN: Renderizando gráfico de distribución de notas:', data);
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
    console.log('DEPURACIÓN: Renderizando gráfico de notas por mes:', data);
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
getAvatarColor(id: string): string {
  const colors = ['#4361ee', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];
  let hash = 0;
  for (let i = 0; i < id.length; i++) hash = id.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash % colors.length)];
}

getAvatarIcon(id: string): string {
  const icons = ['bi-person', 'bi-person-fill', 'bi-person-check', 'bi-person-plus', 'bi-person-x'];
  let hash = 0;
  for (let i = 0; i < id.length; i++) hash = id.charCodeAt(i) + ((hash << 5) - hash);
  return icons[Math.abs(hash % icons.length)];
}




  renderHardestSubjectsChart(data: { asignaturaNombre: string; average: number }[]) {
    console.log('DEPURACIÓN: Renderizando gráfico de asignaturas más difíciles:', data);
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
}
