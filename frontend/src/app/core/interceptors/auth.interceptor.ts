import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../services/auth.service';

/**
 * Adjunta el header Authorization: Bearer <token> a las peticiones
 * salientes cuando existe una sesion activa. No se aplica a
 * /auth/register ni /auth/login, que no requieren token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.token();

  const isAuthEndpoint = req.url.includes('/auth/register') || req.url.includes('/auth/login');

  if (!token || isAuthEndpoint) {
    return next(req);
  }

  const authorizedRequest = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });

  return next(authorizedRequest);
};
