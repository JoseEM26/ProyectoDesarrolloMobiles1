import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { LayoutComponent } from './components/layout/layout.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { UserProfileComponent } from './components/user-profile/user-profile.component';
import { GradesComponent } from './components/grades/grades.component';
import { CoursesComponent } from './components/courses/courses.component';
import { PresentationComponentComponent } from './components/presentation-component/presentation-component.component';
import { ChatComponentComponent } from './components/chat-component/chat-component.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'courses', component: CoursesComponent },
      { path: 'grades', component: GradesComponent },
      { path: 'profile', component: UserProfileComponent },
      { path: 'chat', component: ChatComponentComponent },
      { path: 'presentation', component: PresentationComponentComponent }
    ]
  }
];