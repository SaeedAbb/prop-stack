import { inject } from '@angular/core';
import type { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Router } from '@angular/router';
import { createAuthGuard, type AuthGuardData } from 'keycloak-angular';
import { ORG_ADMIN_REALM_ROLE } from '../constants/roles.constants';

const isOrgAdmin = async (
  _route: ActivatedRouteSnapshot,
  _state: RouterStateSnapshot,
  authData: AuthGuardData,
): Promise<boolean | UrlTree> => {
  const { authenticated, grantedRoles } = authData;
  if (authenticated && grantedRoles.realmRoles.includes(ORG_ADMIN_REALM_ROLE)) {
    return true;
  }
  return inject(Router).createUrlTree(['/properties']);
};

export const orgAdminGuard: CanActivateFn = createAuthGuard(isOrgAdmin);
