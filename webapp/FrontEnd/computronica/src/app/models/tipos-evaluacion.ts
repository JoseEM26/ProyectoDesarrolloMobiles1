export const TIPOS_EVALUACION = [
  'T1',
  'T2',
  'Proyecto',
  'T3',
  'Parcial',
  'Final'
] as const;

export type TipoEvaluacion = typeof TIPOS_EVALUACION[number];
