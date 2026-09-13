import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import Keycloak from 'keycloak-js';
import { Observable, Subject, catchError, map, of, startWith, switchMap } from 'rxjs';
import { OrganizationMember } from '../../../core/models/organization-member.model';
import { OrganizationMemberService } from '../../../core/services/organization-member.service';
import { MemberInitialsPipe } from '../../../shared/pipes/member-initials.pipe';

type MembersState =
  | { readonly status: 'loading' }
  | { readonly status: 'success'; readonly members: OrganizationMember[] }
  | { readonly status: 'error'; readonly message: string };

const LOADING_STATE: MembersState = { status: 'loading' };

const DISPLAYED_COLUMNS: string[] = ['member', 'actions'];

@Component({
  selector: 'app-organization-member-list',
  imports: [
    MatTableModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MemberInitialsPipe,
  ],
  templateUrl: './organization-member-list.html',
  styleUrl: './organization-member-list.scss',
})
export class OrganizationMemberList {
  private readonly memberService = inject(OrganizationMemberService);
  private readonly keycloak = inject(Keycloak);
  private readonly destroyRef = inject(DestroyRef);

  private readonly refresh$ = new Subject<void>();

  private readonly state$: Observable<MembersState> = this.refresh$.pipe(
    startWith(undefined),
    switchMap(() =>
      this.memberService.getAll().pipe(
        map((members): MembersState => ({ status: 'success', members })),
        catchError((error: HttpErrorResponse) => {
          console.error('Failed to load organization members', error);
          return of<MembersState>({
            status: 'error',
            message: 'Could not load organization members. Is the backend running on http://localhost:8080?',
          });
        }),
        startWith(LOADING_STATE),
      ),
    ),
  );

  private readonly state = toSignal(this.state$, { initialValue: LOADING_STATE });

  protected readonly loading = computed(() => this.state().status === 'loading');

  protected readonly errorMessage = computed(() => {
    const current = this.state();
    return current.status === 'error' ? current.message : null;
  });

  protected readonly members = computed(() => {
    const current = this.state();
    return current.status === 'success'
      ? [...current.members].sort((a, b) => a.email.localeCompare(b.email))
      : [];
  });

  protected readonly currentUserId = computed(() => this.keycloak.subject ?? null);

  protected readonly displayedColumns = DISPLAYED_COLUMNS;

  protected readonly addPending = signal(false);
  protected readonly addErrorMessage = signal<string | null>(null);
  protected readonly removingMemberId = signal<string | null>(null);
  protected readonly removeErrorMessage = signal<string | null>(null);

  protected onAddMember(
    event: SubmitEvent,
    email: string,
    firstName: string,
    lastName: string,
  ): void {
    event.preventDefault();
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      return;
    }

    this.addPending.set(true);
    this.addErrorMessage.set(null);

    this.memberService
      .add({
        email: trimmedEmail,
        firstName: firstName.trim() || undefined,
        lastName: lastName.trim() || undefined,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.addPending.set(false);
          (event.target as HTMLFormElement).reset();
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          this.addPending.set(false);
          this.addErrorMessage.set(
            error.status === 400
              ? 'This looks like a brand-new email - please also fill in first and last name.'
              : 'Could not add this member. Check the details and try again.',
          );
        },
      });
  }

  protected onRemoveMember(member: OrganizationMember): void {
    this.removingMemberId.set(member.id);
    this.removeErrorMessage.set(null);

    this.memberService
      .remove(member.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.removingMemberId.set(null);
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to remove organization member', error);
          this.removingMemberId.set(null);
          this.removeErrorMessage.set('Could not remove this member. Please try again.');
        },
      });
  }
}
