export type PropertyType = 'APARTMENT' | 'HOUSE' | 'COMMERCIAL' | 'BUILDING' | 'LAND';

export type PropertyStatus = 'AVAILABLE' | 'RENTED' | 'UNDER_MAINTENANCE' | 'SOLD';

export interface Address {
  id?: string;
  street: string;
  houseNumber: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface Property {
  id?: string;
  name: string;
  address: Address;
  type: PropertyType;
  status: PropertyStatus;
}
