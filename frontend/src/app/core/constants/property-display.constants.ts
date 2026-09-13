import { PropertyStatus, PropertyType } from '../models/property.model';

export interface PropertyStatusDisplay {
  readonly label: string;
  readonly modifierClass: string;
}

export const PROPERTY_STATUS_DISPLAY: Record<PropertyStatus, PropertyStatusDisplay> = {
  AVAILABLE: { label: 'Available', modifierClass: 'property-card__status--available' },
  RENTED: { label: 'Rented', modifierClass: 'property-card__status--rented' },
  UNDER_MAINTENANCE: { label: 'Under maintenance', modifierClass: 'property-card__status--maintenance' },
  SOLD: { label: 'Sold', modifierClass: 'property-card__status--sold' },
};

export interface PropertyTypeDisplay {
  readonly label: string;
  readonly icon: string;
}

export const PROPERTY_TYPE_DISPLAY: Record<PropertyType, PropertyTypeDisplay> = {
  APARTMENT: { label: 'Apartment', icon: 'apartment' },
  HOUSE: { label: 'House', icon: 'home' },
  COMMERCIAL: { label: 'Commercial', icon: 'storefront' },
  LAND: { label: 'Land', icon: 'terrain' },
};
