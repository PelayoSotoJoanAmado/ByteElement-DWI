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
