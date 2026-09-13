import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import Keycloak from 'keycloak-js';
import { map } from 'rxjs';
import { OrgRoleService } from './core/services/org-role.service';

// TEST COMMIT for /release verification - safe to remove after testing.
interface KeycloakUserClaims {
  readonly name?: string;
  readonly preferred_username?: string;
  readonly email?: string;
}

@Component({
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
  ],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly orgRole = inject(OrgRoleService);
  private readonly keycloak = inject(Keycloak);
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly isOrgAdmin = this.orgRole.isOrgAdmin;

  private readonly isHandset = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly sidenavMode = computed<'over' | 'side'>(() => (this.isHandset() ? 'over' : 'side'));
  protected readonly sidenavOpened = signal(true);

  private readonly userClaims = computed(() => this.keycloak.tokenParsed as KeycloakUserClaims | undefined);
  protected readonly displayName = computed(() => this.userClaims()?.name ?? this.userClaims()?.preferred_username ?? '');
  protected readonly email = computed(() => this.userClaims()?.email ?? '');

  constructor() {
    inject(MatIconRegistry).setDefaultFontSetClass('material-symbols-outlined', 'mat-ligature-font');

    effect(() => {
      this.sidenavOpened.set(!this.isHandset());
    });
  }

  protected onNavLinkClick(): void {
    if (this.isHandset()) {
      this.sidenavOpened.set(false);
    }
  }

  protected onLogout(): void {
    void this.keycloak.logout();
  }
}
