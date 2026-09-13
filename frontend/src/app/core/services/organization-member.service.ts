import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AddOrganizationMemberRequest, OrganizationMember } from '../models/organization-member.model';

@Injectable({ providedIn: 'root' })
export class OrganizationMemberService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organization/members`;

  getAll(): Observable<OrganizationMember[]> {
    return this.http.get<OrganizationMember[]>(this.baseUrl);
  }

  add(request: AddOrganizationMemberRequest): Observable<void> {
    return this.http.post<void>(this.baseUrl, request);
  }

  remove(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}`);
  }
}
