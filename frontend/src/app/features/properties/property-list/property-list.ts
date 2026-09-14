import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, map, Observable, of, startWith, Subject, switchMap } from 'rxjs';
import { PropertyService } from '../../../core/services/property.service';
import { Property } from '../../../core/models/property.model';
import { AddressLinePipe } from '../../../shared/pipes/address-line.pipe';
import { PropertyStatusClassPipe } from '../../../shared/pipes/property-status-class.pipe';
import { PropertyStatusLabelPipe } from '../../../shared/pipes/property-status-label.pipe';
import { PropertyTypeIconPipe } from '../../../shared/pipes/property-type-icon.pipe';
import { PropertyTypeLabelPipe } from '../../../shared/pipes/property-type-label.pipe';
import { AddPropertyDialog } from '../add-property-dialog/add-property-dialog';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog';

export type PropertyListMode = 'owned' | 'deleted';

type PropertyListState =
  | { readonly status: 'loading' }
  | { readonly status: 'success'; readonly properties: Property[] }
  | { readonly status: 'error'; readonly message: string };

const LOADING_STATE: PropertyListState = { status: 'loading' };

@Component({
  selector: 'app-property-list',
  imports: [
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    PropertyStatusLabelPipe,
    PropertyStatusClassPipe,
    PropertyTypeLabelPipe,
    PropertyTypeIconPipe,
    AddressLinePipe,
  ],
  templateUrl: './property-list.html',
  styleUrl: './property-list.scss',
})
export class PropertyList {
  private readonly propertyService = inject(PropertyService);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  protected readonly mode: PropertyListMode = (this.route.snapshot.data['mode'] as PropertyListMode | undefined) ?? 'owned';

  private readonly refresh$ = new Subject<void>();

  private readonly state$: Observable<PropertyListState> = this.refresh$.pipe(
    startWith(undefined),
    switchMap(() => {
      const properties$ = this.mode === 'deleted' ? this.propertyService.getAllDeleted() : this.propertyService.getAll();
      return properties$.pipe(
        map((properties): PropertyListState => ({ status: 'success', properties })),
        catchError((error: HttpErrorResponse) => {
          console.error('Failed to load properties', error);
          return of<PropertyListState>({
            status: 'error',
            message: 'Could not load properties. Is the backend running on http://localhost:8080?',
          });
        }),
        startWith(LOADING_STATE),
      );
    }),
  );

  private readonly state = toSignal(this.state$, { initialValue: LOADING_STATE });

  protected readonly loading = computed(() => this.state().status === 'loading');

  protected readonly errorMessage = computed(() => {
    const current = this.state();
    return current.status === 'error' ? current.message : null;
  });

  protected readonly properties = computed(() => {
    const current = this.state();
    return current.status === 'success' ? current.properties : [];
  });

  protected readonly deletingPropertyId = signal<number | null>(null);
  protected readonly deleteErrorMessage = signal<string | null>(null);
  protected readonly restoringPropertyId = signal<number | null>(null);
  protected readonly restoreErrorMessage = signal<string | null>(null);

  protected onAddProperty(): void {
    this.dialog
      .open(AddPropertyDialog, { maxWidth: '95vw', maxHeight: '90vh' })
      .afterClosed()
      .subscribe((created?: Property) => {
        if (created) {
          this.refresh$.next();
        }
      });
  }

  protected onDeleteProperty(property: Property): void {
    if (property.id == null) {
      return;
    }
    const id = property.id;

    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: 'Delete property',
          message: `Delete "${property.name}"? You can restore it later from Deleted.`,
          confirmLabel: 'Delete',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.deleteConfirmed(id);
        }
      });
  }

  private deleteConfirmed(id: number): void {
    this.deletingPropertyId.set(id);
    this.deleteErrorMessage.set(null);

    this.propertyService
      .delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingPropertyId.set(null);
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to delete property', error);
          this.deletingPropertyId.set(null);
          this.deleteErrorMessage.set('Could not delete this property. Please try again.');
        },
      });
  }

  protected onRestoreProperty(property: Property): void {
    if (property.id == null) {
      return;
    }
    const id = property.id;

    this.dialog
      .open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
        data: {
          title: 'Restore property',
          message: `Restore "${property.name}"? It will show up again under Owned.`,
          confirmLabel: 'Restore',
          confirmColor: 'primary',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.restoreConfirmed(id);
        }
      });
  }

  private restoreConfirmed(id: number): void {
    this.restoringPropertyId.set(id);
    this.restoreErrorMessage.set(null);

    this.propertyService
      .restore(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.restoringPropertyId.set(null);
          this.refresh$.next();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to restore property', error);
          this.restoringPropertyId.set(null);
          this.restoreErrorMessage.set('Could not restore this property. Please try again.');
        },
      });
  }
}
