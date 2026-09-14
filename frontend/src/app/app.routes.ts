import { Routes } from '@angular/router';
import { orgAdminGuard } from './core/guards/org-admin.guard';
import { OrganizationMemberList } from './features/organization/organization-member-list/organization-member-list';
import { PropertyList } from './features/properties/property-list/property-list';

export const routes: Routes = [
  { path: '', redirectTo: 'properties/owned', pathMatch: 'full' },
  { path: 'properties', redirectTo: 'properties/owned', pathMatch: 'full' },
  { path: 'properties/owned', component: PropertyList, data: { mode: 'owned' } },
  { path: 'properties/deleted', component: PropertyList, data: { mode: 'deleted' } },
  { path: 'admin/members', component: OrganizationMemberList, canActivate: [orgAdminGuard] },
];
