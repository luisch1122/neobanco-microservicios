import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/services/auth.service';
import { GenericErrorResponse, UserProfileResponse } from '../../core/models/auth.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatToolbarModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loadingProfile = signal(true);
  readonly loadError = signal<string | null>(null);

  readonly profileForm = this.formBuilder.group({
    nombre: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]]
  });

  readonly profileSubmitting = signal(false);
  readonly profileSuccess = signal<string | null>(null);
  readonly profileError = signal<string | null>(null);

  readonly passwordForm = this.formBuilder.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  readonly passwordSubmitting = signal(false);
  readonly passwordSuccess = signal<string | null>(null);
  readonly passwordError = signal<string | null>(null);

  ngOnInit(): void {
    this.authService.getMe().subscribe({
      next: (profile) => {
        this.loadingProfile.set(false);
        this.patchProfileForm(profile);
      },
      error: () => {
        this.loadingProfile.set(false);
        this.loadError.set('No se pudo cargar tu perfil. Intenta nuevamente más tarde.');
      }
    });
  }

  submitProfile(): void {
    this.profileSuccess.set(null);
    this.profileError.set(null);

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const request = this.profileForm.getRawValue() as { nombre: string; email: string };
    this.profileSubmitting.set(true);

    this.authService.updateMe(request).subscribe({
      next: (profile) => {
        this.profileSubmitting.set(false);
        this.patchProfileForm(profile);
        this.profileSuccess.set('Datos actualizados correctamente.');
      },
      error: (error: HttpErrorResponse) => {
        this.profileSubmitting.set(false);
        this.profileError.set(this.extractBusinessError(error, 'No se pudo actualizar tu perfil.'));
      }
    });
  }

  submitPassword(): void {
    this.passwordSuccess.set(null);
    this.passwordError.set(null);

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const request = this.passwordForm.getRawValue() as { currentPassword: string; newPassword: string };
    this.passwordSubmitting.set(true);

    this.authService.updatePassword(request).subscribe({
      next: () => {
        this.passwordSubmitting.set(false);
        this.passwordSuccess.set('Contraseña actualizada correctamente.');
        this.passwordForm.reset();
      },
      error: (error: HttpErrorResponse) => {
        this.passwordSubmitting.set(false);
        this.passwordError.set(this.extractBusinessError(error, 'No se pudo actualizar tu contraseña.'));
      }
    });
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }

  private patchProfileForm(profile: UserProfileResponse): void {
    this.profileForm.patchValue({ nombre: profile.nombre, email: profile.email });
  }

  /**
   * Los errores de negocio de /auth/me y /auth/me/password llegan como
   * 400 con el body { error: "mensaje" } (ver GenericErrorResponse en
   * el contrato OpenAPI de ms-auth-users).
   */
  private extractBusinessError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 400 && error.error && typeof error.error === 'object') {
      const body = error.error as GenericErrorResponse;
      if (body.error) {
        return body.error;
      }
    }
    return fallback;
  }
}
