import { Pipe, PipeTransform } from '@angular/core';
import { PROPERTY_TYPE_DISPLAY } from '../../core/constants/property-display.constants';
import { PropertyType } from '../../core/models/property.model';

@Pipe({ name: 'propertyTypeIcon' })
export class PropertyTypeIconPipe implements PipeTransform {
  transform(type: PropertyType): string {
    return PROPERTY_TYPE_DISPLAY[type].icon;
  }
}
