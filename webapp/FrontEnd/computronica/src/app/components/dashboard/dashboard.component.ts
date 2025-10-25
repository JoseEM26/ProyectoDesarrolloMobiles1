import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Firestore, collection, collectionData } from '@angular/fire/firestore';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  studentsCount$!: Observable<number>;
  gradesCount$!: Observable<number>;
  subjectsCount$!: Observable<number>;
  recentGrades$!: Observable<any[]>;
  averageBySubject$!: Observable<any[]>;

  constructor(private firestore: Firestore, private authService: AuthService) {}

  ngOnInit() {
    this.studentsCount$ = collectionData(collection(this.firestore, 'users')).pipe(
      map(users => users.filter((user: any) => user.tipo === 'estudiante').length)
    );
    this.gradesCount$ = collectionData(collection(this.firestore, 'grades')).pipe(
      map(grades => grades.length)
    );
    this.subjectsCount$ = collectionData(collection(this.firestore, 'subjects')).pipe(
      map(subjects => subjects.length)
    );
    this.recentGrades$ = collectionData(collection(this.firestore, 'grades')).pipe(
      map(grades => grades.slice(0, 5))
    );
    this.averageBySubject$ = collectionData(collection(this.firestore, 'grades')).pipe(
      map(grades => {
        const averages = grades.reduce((acc: any, grade: any) => {
          if (!acc[grade.subjectId]) {
            acc[grade.subjectId] = { sum: 0, count: 0 };
          }
          acc[grade.subjectId].sum += grade.nota;
          acc[grade.subjectId].count += 1;
          return acc;
        }, {});
        return Object.keys(averages).map(subjectId => ({
          subjectId,
          average: (averages[subjectId].sum / averages[subjectId].count).toFixed(2)
        }));
      })
    );
  }
}