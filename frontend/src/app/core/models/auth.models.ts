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

/**
 * Perfil del usuario autenticado devuelto por GET/PUT /auth/me y por
 * PUT /auth/me/password. Nunca incluye la contraseña.
 */
export interface UserProfileResponse {
  id: number;
  nombre: string;
  email: string;
  estado: 'ACTIVO' | 'INACTIVO' | 'BLOQUEADO';
  creadoEn: string;
}

export interface UserUpdateRequest {
  nombre: string;
  email: string;
}

export interface PasswordUpdateRequest {
  currentPassword: string;
  newPassword: string;
}

/**
 * Error de negocio devuelto por PUT /auth/me y PUT /auth/me/password
 * (400): email duplicado, contraseña actual incorrecta, etc. El backend
 * usa la clave `error` en minúscula para estos casos.
 */
export interface GenericErrorResponse {
  error: string;
}
