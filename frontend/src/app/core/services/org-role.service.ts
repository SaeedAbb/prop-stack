import { Injectable, computed, inject } from '@angular/core';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import { ORG_ADMIN_REALM_ROLE } from '../constants/roles.constants';

@Injectable({ providedIn: 'root' })
export class OrgRoleService {
  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  readonly isOrgAdmin = computed(() => {
    this.keycloakEvent(); // re-evaluate when Keycloak emits (e.g. after token refresh)
    return (this.keycloak.realmAccess?.roles ?? []).includes(ORG_ADMIN_REALM_ROLE);
  });
}
