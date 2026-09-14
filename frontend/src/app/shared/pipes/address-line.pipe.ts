import { Pipe, PipeTransform } from '@angular/core';
import { Address } from '../../core/models/property.model';

@Pipe({ name: 'addressLine' })
export class AddressLinePipe implements PipeTransform {
  transform(address: Address): string {
    const streetLine = [address.street, address.houseNumber].filter((part) => !!part?.trim()).join(' ');
    return [streetLine, address.city, address.state, address.postalCode, address.country]
      .filter((part): part is string => !!part?.trim())
      .join(', ');
  }
}
