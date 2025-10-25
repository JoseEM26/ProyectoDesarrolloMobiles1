export interface AuthResponse {
  id: string;
  codigoInstitucional: string;
  sede: string;
  nombre: string;
  apellido: string;
  correoInstitucional: string;
  tipo: 'estudiante' | 'profesor' | 'administrativo';
  estado: boolean;
  token: string;
}