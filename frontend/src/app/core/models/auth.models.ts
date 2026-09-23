/**
 * Modelos alineados con el contrato OpenAPI de ms-auth-users
 * (ms-auth-users/openapi.yaml).
 */

export interface RegisterRequest {
  nombre: string;
  email: string;
  password: string;
}

/**
 * Respuesta de POST /auth/register. Corresponde a la entidad Users
 * devuelta por el backend; el campo `password` contiene el hash BCrypt,
 * nunca la contraseña original, y no debe usarse ni mostrarse en el UI.
 */
export interface RegisterResponse {
  id: number;
  nombre: string;
  email: string;
  password: string;
  estado: 'ACTIVO' | 'INACTIVO' | 'BLOQUEADO';
  creadoEn: string;
}

/**
 * Errores de validacion de /auth/register: mapa campo -> mensaje.
 */
export type ValidationErrorResponse = Record<string, string>;

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  message: string;
}

/**
 * Error devuelto por /auth/login (credenciales invalidas) y por el
 * filtro de validacion de JWT en rutas protegidas.
 */
export interface AuthErrorResponse {
  message: string;
  error?: string;
}
