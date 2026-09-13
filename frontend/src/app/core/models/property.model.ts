export type PropertyType = 'APARTMENT' | 'HOUSE' | 'COMMERCIAL' | 'LAND';

export type PropertyStatus = 'AVAILABLE' | 'RENTED' | 'UNDER_MAINTENANCE' | 'SOLD';

export interface Property {
  id?: number;
  name: string;
  address: string;
  type: PropertyType;
  status: PropertyStatus;
}
