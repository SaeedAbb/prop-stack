import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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

  private readonly refresh$ = new Subject<void>();

  private readonly state$: Observable<PropertyListState> = this.refresh$.pipe(
    startWith(undefined),
    switchMap(() =>
      this.propertyService.getAll().pipe(
        map((properties): PropertyListState => ({ status: 'success', properties })),
        catchError((error: HttpErrorResponse) => {
          console.error('Failed to load properties', error);
          return of<PropertyListState>({
            status: 'error',
            message: 'Could not load properties. Is the backend running on http://localhost:8080?',
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

  protected readonly properties = computed(() => {
    const current = this.state();
    return current.status === 'success' ? current.properties : [];
  });

  protected onAddProperty(): void {
    this.dialog
      .open(AddPropertyDialog)
      .afterClosed()
      .subscribe((created?: Property) => {
        if (created) {
          this.refresh$.next();
        }
      });
  }
}
