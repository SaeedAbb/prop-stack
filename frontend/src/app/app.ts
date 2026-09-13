import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { OrgRoleService } from './core/services/org-role.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly orgRole = inject(OrgRoleService);
  protected readonly isOrgAdmin = this.orgRole.isOrgAdmin;
}
