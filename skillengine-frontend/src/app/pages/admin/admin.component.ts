import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SkillService } from '../../core/services/skill.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  private readonly fb = inject(FormBuilder);
  private readonly skillService = inject(SkillService);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    category: ['', Validators.required],
    prerequisiteFor: [''], // comma-separated skill names, split on submit
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const prerequisiteFor = raw.prerequisiteFor
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s.length > 0);

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.skillService
      .createSkill({ name: raw.name, category: raw.category, prerequisiteFor })
      .subscribe({
        next: (created) => {
          this.loading.set(false);
          this.successMessage.set(`Created "${created.name}" (${created.category}).`);
          this.form.reset({ name: '', category: '', prerequisiteFor: '' });
        },
        error: (err: HttpErrorResponse) => {
  this.loading.set(false);

  if (err.status === 403) {
    this.errorMessage.set('Your account does not have admin access.');
    return;
  }

  const backendMessage =
    typeof err.error === 'string'
      ? err.error
      : err.error?.message;

  this.errorMessage.set(
    backendMessage || `Request failed with status ${err.status}.`
  );

  console.error('Create skill failed:', err);
},
      });
  }
}
