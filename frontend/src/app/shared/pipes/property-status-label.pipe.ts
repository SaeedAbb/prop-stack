import { Pipe, PipeTransform } from '@angular/core';
import { PROPERTY_STATUS_DISPLAY } from '../../core/constants/property-display.constants';
import { PropertyStatus } from '../../core/models/property.model';

@Pipe({ name: 'propertyStatusLabel' })
export class PropertyStatusLabelPipe implements PipeTransform {
  transform(status: PropertyStatus): string {
    return PROPERTY_STATUS_DISPLAY[status].label;
  }
}
