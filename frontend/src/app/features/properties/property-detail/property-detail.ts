import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Property } from '../../../core/models/property.model';
import { PropertyService } from '../../../core/services/property.service';

type PropertyDetailState =
  | { readonly status: 'loading' }
  | { readonly status: 'success'; readonly property: Property }
  | { readonly status: 'not-found' }
  | { readonly status: 'error'; readonly message: string };

@Component({
  selector: 'app-property-detail',
  imports: [
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './property-detail.html',
  styleUrl: './property-detail.scss',
})
export class PropertyDetail {
  private readonly propertyService = inject(PropertyService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  private readonly id = this.route.snapshot.paramMap.get('id')!;

  protected readonly state = signal<PropertyDetailState>({ status: 'loading' });

  protected readonly property = computed(() => {
    const current = this.state();
    return current.status === 'success' ? current.property : null;
  });

  protected readonly errorMessage = computed(() => {
    const current = this.state();
    return current.status === 'error' ? current.message : null;
  });

  protected readonly addressExpanded = signal(true);
  protected readonly addressEditing = signal(false);
  protected readonly addressSaving = signal(false);
  protected readonly addressErrorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set({ status: 'loading' });

    this.propertyService
      .getById(this.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (property) => this.state.set({ status: 'success', property }),
        error: (error: HttpErrorResponse) => {
          console.error('Failed to load property', error);
          this.state.set(
            error.status === 404
              ? { status: 'not-found' }
              : { status: 'error', message: 'Could not load this property. Is the backend running on http://localhost:8080?' },
          );
        },
      });
  }

  protected onEditAddress(): void {
    this.addressEditing.set(true);
    this.addressErrorMessage.set(null);
  }

  protected onCancelAddress(): void {
    this.addressEditing.set(false);
    this.addressErrorMessage.set(null);
  }

  protected onSaveAddress(
    event: SubmitEvent,
    street: string,
    houseNumber: string,
    city: string,
    state: string,
    postalCode: string,
    country: string,
  ): void {
    event.preventDefault();

    const current = this.property();
    if (!current) {
      return;
    }

    const trimmedStreet = street.trim();
    const trimmedHouseNumber = houseNumber.trim();
    const trimmedCity = city.trim();
    const trimmedState = state.trim();
    const trimmedPostalCode = postalCode.trim();
    const trimmedCountry = country.trim();

    if (!trimmedStreet || !trimmedHouseNumber || !trimmedCity || !trimmedState || !trimmedPostalCode || !trimmedCountry) {
      this.addressErrorMessage.set('Please fill in all address fields.');
      return;
    }

    this.addressSaving.set(true);
    this.addressErrorMessage.set(null);

    const updated: Property = {
      ...current,
      address: {
        ...current.address,
        street: trimmedStreet,
        houseNumber: trimmedHouseNumber,
        city: trimmedCity,
        state: trimmedState,
        postalCode: trimmedPostalCode,
        country: trimmedCountry,
      },
    };

    this.propertyService
      .update(this.id, updated)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (saved) => {
          this.addressSaving.set(false);
          this.addressEditing.set(false);
          this.state.set({ status: 'success', property: saved });
        },
        error: (error: HttpErrorResponse) => {
          console.error('Failed to update address', error);
          this.addressSaving.set(false);
          this.addressErrorMessage.set('Could not save the address. Please try again.');
        },
      });
  }
}
