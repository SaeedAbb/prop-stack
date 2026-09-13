import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, Observable, of, startWith } from 'rxjs';
import { PropertyService } from '../../../core/services/property.service';
import { Property } from '../../../core/models/property.model';

type PropertyListState =
  | { readonly status: 'loading' }
  | { readonly status: 'success'; readonly properties: Property[] }
  | { readonly status: 'error'; readonly message: string };

const LOADING_STATE: PropertyListState = { status: 'loading' };

@Component({
  selector: 'app-property-list',
  imports: [],
  templateUrl: './property-list.html',
  styleUrl: './property-list.scss',
})
export class PropertyList {
  private readonly propertyService = inject(PropertyService);

  private readonly state$: Observable<PropertyListState> = this.propertyService.getAll().pipe(
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
}
