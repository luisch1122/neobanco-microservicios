import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest, ValidationErrorResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.formBuilder.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  readonly submitting = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly generalError = signal<string | null>(null);

  submit(): void {
    this.successMessage.set(null);
    this.generalError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.form.getRawValue() as RegisterRequest;
    this.submitting.set(true);

    this.authService.register(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/login'], { queryParams: { registrado: 'ok' } });
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.applyServerErrors(error);
      }
    });
  }

  /**
   * Vuelca los errores de validacion devueltos por el backend (mapa
   * campo -> mensaje, ver ValidationErrorResponse) sobre los controles
   * correspondientes del formulario, o muestra un error general si la
   * respuesta no tiene ese formato.
   */
  private applyServerErrors(error: HttpErrorResponse): void {
    if (error.status === 400 && error.error && typeof error.error === 'object') {
      const validationErrors = error.error as ValidationErrorResponse;
      let matchedAny = false;

      Object.entries(validationErrors).forEach(([field, message]) => {
        const control = this.form.get(field);
        if (control) {
          control.setErrors({ server: message });
          matchedAny = true;
        }
      });

      if (matchedAny) {
        return;
      }
    }

    this.generalError.set('No se pudo completar el registro. Intenta nuevamente.');
  }
}
