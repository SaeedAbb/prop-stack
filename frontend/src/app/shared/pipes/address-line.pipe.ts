import { Pipe, PipeTransform } from '@angular/core';
import { Address } from '../../core/models/property.model';

@Pipe({ name: 'addressLine' })
export class AddressLinePipe implements PipeTransform {
  transform(address: Address): string {
    return [address.street, address.city, address.state, address.postalCode, address.country]
      .filter((part): part is string => !!part?.trim())
      .join(', ');
  }
}
