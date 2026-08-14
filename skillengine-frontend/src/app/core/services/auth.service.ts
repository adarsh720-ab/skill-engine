import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/skill.model';

const STORAGE_KEY = 'skillengine.auth';

interface StoredAuth {
  token: string;
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  // Signal-backed auth state, seeded synchronously from localStorage so a
  // page refresh never bounces the user through a flash of "logged out".
  private readonly state = signal<StoredAuth | null>(this.readStoredAuth());

  readonly token = computed(() => this.state()?.token ?? null);
  readonly email = computed(() => this.state()?.email ?? null);
  readonly role = computed(() => this.state()?.role ?? null);
  readonly isAuthenticated = computed(() => !!this.state());
  readonly isAdmin = computed(() => this.state()?.role === 'ROLE_ADMIN');

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, request)
      .pipe(tap((res) => this.persist(res)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/register`, request)
      .pipe(tap((res) => this.persist(res)));
  }

  logout(): void {
    this.state.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  private persist(res: AuthResponse): void {
    const stored: StoredAuth = { token: res.token, email: res.email, role: res.role };
    this.state.set(stored);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  }

  private readStoredAuth(): StoredAuth | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as StoredAuth) : null;
    } catch {
      return null;
    }
  }
}
