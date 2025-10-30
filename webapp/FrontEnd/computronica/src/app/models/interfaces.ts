// src/app/models/interfaces.ts
import { Timestamp } from "firebase/firestore";

export interface Calificaciones {
  id?: string;
  estudianteId: string;
  asignaturaId: string;
  evaluacion: TipoEvaluacion;
  nota: number;
  fechaRegistro: string | null;
  estudianteNombre?: string;
  asignaturaNombre?: string;
  fechaRegistroDate?: Date | null; // Mantenido como en tu código
}
// src/app/models/tipos-evaluacion.ts
export const TIPOS_EVALUACION = ['Examen', 'Tarea', 'Proyecto', 'Parcial', 'Final'] as const;
export type TipoEvaluacion = typeof TIPOS_EVALUACION[number];
export type Sede = 'Campus Central' | 'Campus Norte' | 'Campus Sur' | 'Campus Este';
export interface Asignatura {
  id?: string;
  nombre: string;
  codigoAsignatura: string;
  descripcion: string;
  creditos: number;
  profesores: string[];
  estudiantes: string[];
}
export interface Tema {
  id?: string;
  asignaturaId: string;
  nombre: string;
  descripcion?: string;
  estado?: boolean;
  fechaCreacion?: Date | any;
}


export enum TipoUsuario {
  estudiante = 'estudiante',
  profesor = 'profesor',
  administrativo = 'administrativo'
}
export interface ActivityDetails {
  courseId?: string;
  courseName?: string;
  grade?: number;
  evaluation?: string;
  assignmentId?: string;
  asignaturaId?: string;
}

 export interface Activity {
  id: string;
  type: 'course' | 'grade' | 'profile' | 'assignment';
  description: string;
  date: Date;
  details?: ActivityDetails;
}

export interface Usuario {
  id?: string;
  codigoInstitucional: string;
  sede: string;
  nombre: string;
  apellido: string;
  correoInstitucional: string;
  contrasena?: string;
  tipo: TipoUsuario; // Cambiar de 'TipoUsuario | string' a 'TipoUsuario'
  estado: boolean;
  createdAt?: string | Date | Timestamp | null;
  updatedAt?: string | Date | Timestamp | null;
  avatar?: string;
}
export interface RegisterRequest {
  codigoInstitucional: string;
  sede: string;
  nombre: string;
  apellido: string;
  correoInstitucional: string;
  contrasena: string;
  tipo?: TipoUsuario;
}

export interface LoginRequest {
  correoInstitucional: string;
  contrasena: string;
  rememberMe?: boolean;
}

export interface AuthResponse {
  id: string;
  codigoInstitucional: string;
  sede: string;
  nombre: string;
  apellido: string;
  correoInstitucional: string;
  tipo: TipoUsuario;
  estado: boolean;
  token: string;
}
