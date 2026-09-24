import { Injectable, computed, signal } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  LoginRequest,
  LoginResponse,
  PasswordUpdateRequest,
  RegisterRequest,
  RegisterResponse,
  UserProfileResponse,
  UserUpdateRequest
} from '../models/auth.models';

const TOKEN_STORAGE_KEY = 'neobanco.auth.token';
const EMAIL_STORAGE_KEY = 'neobanco.auth.email';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = environment.apiUrl;

  // Estado de sesion reactivo. Se inicializa leyendo lo que haya
  // persistido en localStorage (sobrevive a recargas de pagina).
  private readonly tokenSignal = signal<string | null>(this.readStoredToken());
  private readonly emailSignal = signal<string | null>(localStorage.getItem(EMAIL_STORAGE_KEY));

  readonly token = this.tokenSignal.asReadonly();
  readonly email = this.emailSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);

  constructor(private readonly http: HttpClient) {}

  /**
   * Registra un nuevo usuario. No inicia sesion automaticamente: el
   * backend no emite token en /auth/register, solo en /auth/login.
   */
  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/auth/register`, request);
  }

  /**
   * Inicia sesion contra /auth/login. El backend devuelve el JWT tanto
   * en el body como en la cabecera Authorization; usamos el body por
   * simplicidad y persistimos el token/email para las siguientes
   * peticiones y para sobrevivir recargas de pagina.
   */
  login(request: LoginRequest): Observable<HttpResponse<LoginResponse>> {
    return this.http
      .post<LoginResponse>(`${this.apiUrl}/auth/login`, request, { observe: 'response' })
      .pipe(
        tap((response) => {
          const body = response.body;
          if (body?.token) {
            this.setSession(body.token, body.email);
          }
        })
      );
  }

  /**
   * Obtiene el perfil del usuario autenticado (deriva la identidad del
   * token, nunca de un parametro del cliente).
   */
  getMe(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.apiUrl}/auth/me`);
  }

  /**
   * Actualiza nombre/email del usuario autenticado. Si el email cambia,
   * se sincroniza en el estado local para que siga usandose en el header
   * Authorization y en la UI.
   */
  updateMe(request: UserUpdateRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}/auth/me`, request).pipe(
      tap((profile) => {
        if (this.emailSignal() !== null) {
          localStorage.setItem(EMAIL_STORAGE_KEY, profile.email);
          this.emailSignal.set(profile.email);
        }
      })
    );
  }

  /**
   * Cambia la contraseña del usuario autenticado. No cierra la sesion:
   * el token actual sigue siendo valido hasta que expire.
   */
  updatePassword(request: PasswordUpdateRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.apiUrl}/auth/me/password`, request);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(EMAIL_STORAGE_KEY);
    this.tokenSignal.set(null);
    this.emailSignal.set(null);
  }

  private setSession(token: string, email: string): void {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    localStorage.setItem(EMAIL_STORAGE_KEY, email);
    this.tokenSignal.set(token);
    this.emailSignal.set(email);
  }

  private readStoredToken(): string | null {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  }
}
