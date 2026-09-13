import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { PROPERTY_STATUS_DISPLAY, PROPERTY_TYPE_DISPLAY } from '../../../core/constants/property-display.constants';
import { Property, PropertyStatus, PropertyType } from '../../../core/models/property.model';
import { PropertyService } from '../../../core/services/property.service';

@Component({
  selector: 'app-add-property-dialog',
  imports: [
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatExpansionModule,
  ],
  templateUrl: './add-property-dialog.html',
  styleUrl: './add-property-dialog.scss',
})
export class AddPropertyDialog {
  private readonly propertyService = inject(PropertyService);
  private readonly dialogRef = inject(MatDialogRef<AddPropertyDialog>);

  protected readonly typeOptions = Object.entries(PROPERTY_TYPE_DISPLAY) as [PropertyType, { label: string }][];
  protected readonly statusOptions = Object.entries(PROPERTY_STATUS_DISPLAY) as [PropertyStatus, { label: string }][];

  protected readonly type = signal<PropertyType | null>(null);
  protected readonly status = signal<PropertyStatus | null>(null);
  protected readonly addressExpanded = signal(true);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected onSubmit(
    event: SubmitEvent,
    name: string,
    street: string,
    city: string,
    state: string,
    postalCode: string,
    country: string,
  ): void {
    event.preventDefault();

    const trimmedName = name.trim();
    const trimmedStreet = street.trim();
    const trimmedCity = city.trim();
    const selectedType = this.type();
    const selectedStatus = this.status();

    if (!trimmedName || !trimmedStreet || !trimmedCity || !selectedType || !selectedStatus) {
      this.errorMessage.set('Please fill in name, street, city, type and status.');
      this.addressExpanded.set(true);
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);

    const property: Property = {
      name: trimmedName,
      address: {
        street: trimmedStreet,
        city: trimmedCity,
        state: state.trim() || undefined,
        postalCode: postalCode.trim() || undefined,
        country: country.trim() || undefined,
      },
      type: selectedType,
      status: selectedStatus,
    };

    this.propertyService.create(property).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.dialogRef.close(created);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Failed to create property', error);
        this.submitting.set(false);
        this.errorMessage.set('Could not add this property. Please try again.');
      },
    });
  }

  protected onCancel(): void {
    this.dialogRef.close();
  }
}
