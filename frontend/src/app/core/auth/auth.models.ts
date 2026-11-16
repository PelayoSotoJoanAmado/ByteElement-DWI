export const ROLES = [
  'ALMACEN',
  'COMPRAS',
  'SUPERVISOR',
  'CONSULTA_COMERCIAL',
  'ADMINISTRADOR',
] as const;
export type Role = (typeof ROLES)[number];
export interface User {
  id: number;
  username: string;
  rol: Role;
  activo: boolean;
  requiereCambioPassword: boolean;
  version: number;
}
export interface LoginCredentials {
  username: string;
  password: string;
}
export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  usuario: User;
}
export interface Session {
  accessToken: string;
  expiresAt: number;
  usuario: User;
}

export interface ChangePassword {
  passwordActual: string;
  passwordNueva: string;
  confirmacionPassword: string;
}
export interface UserEvent {
  id: number;
  usuarioId: number;
  actorId: number | null;
  actorUsername: string;
  usuarioUsername: string;
  tipo: string;
  fecha: string;
  detalle: string;
}
