import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateSkillRequest,
  SkillNodeDto,
  SkillStackResponse,
} from '../models/skill.model';

@Injectable({ providedIn: 'root' })
export class SkillService {
  private readonly http = inject(HttpClient);
  private readonly skillsUrl = `${environment.apiBaseUrl}/skills`;
  private readonly adminUrl = `${environment.apiBaseUrl}/admin`;

  /** GET /api/v1/skills/related?query=&maxHops= */
  findRelated(query: string, maxHops = 3): Observable<SkillStackResponse[]> {
    const params = new HttpParams()
      .set('query', query)
      .set('maxHops', maxHops);

    return this.http.get<SkillStackResponse[]>(
      `${this.skillsUrl}/related`,
      { params }
    );
  }

  /** POST /api/v1/admin — ROLE_ADMIN only */
  createSkill(request: CreateSkillRequest): Observable<SkillNodeDto> {
    return this.http.post<SkillNodeDto>(this.adminUrl, request);
  }

  /** GET /api/v1/skills/search?query=&limit= — lightweight autocomplete */
  searchSkills(query: string, limit = 8): Observable<SkillNodeDto[]> {
    const params = new HttpParams()
      .set('query', query)
      .set('limit', limit);

    return this.http.get<SkillNodeDto[]>(
      `${this.skillsUrl}/search`,
      { params }
    );
  }
}