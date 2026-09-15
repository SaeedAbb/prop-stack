import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PropertyDetail } from './property-detail';
import { Property } from '../../../core/models/property.model';
import { PropertyService } from '../../../core/services/property.service';

describe('PropertyDetail', () => {
  const PROPERTY_ID = '00000000-0000-0000-0000-000000000001';
  const property: Property = {
    id: PROPERTY_ID,
    name: 'Sunset Apartments',
    address: { street: 'Main St', houseNumber: '12', city: 'Springfield', state: 'IL', postalCode: '62701', country: 'USA' },
    type: 'APARTMENT',
    status: 'AVAILABLE',
  };

  let propertyService: { getById: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  function text(fixture: { nativeElement: HTMLElement }): string {
    return fixture.nativeElement.textContent ?? '';
  }

  async function createFixture() {
    await TestBed.configureTestingModule({
      imports: [PropertyDetail],
      providers: [
        provideRouter([]),
        { provide: PropertyService, useValue: propertyService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: PROPERTY_ID }) } } },
      ],
    }).compileComponents();

    return TestBed.createComponent(PropertyDetail);
  }

  function clickButtonByText(fixture: { nativeElement: HTMLElement }, label: string): void {
    const button = Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) => b.textContent?.trim().includes(label));
    (button as HTMLButtonElement).click();
  }

  beforeEach(() => {
    propertyService = { getById: vi.fn(), update: vi.fn() };
  });

  it('renders the read-only address once loaded', async () => {
    propertyService.getById.mockReturnValue(of(property));
    const fixture = await createFixture();
    fixture.detectChanges();

    const content = text(fixture);
    expect(content).toContain('Sunset Apartments');
    expect(content).toContain('Main St');
    expect(content).toContain('Springfield');
  });

  it('shows an error message and retries on a server error', async () => {
    propertyService.getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(text(fixture)).toContain('Could not load this property');

    propertyService.getById.mockReturnValue(of(property));
    clickButtonByText(fixture, 'Retry');
    fixture.detectChanges();

    expect(propertyService.getById).toHaveBeenCalledTimes(2);
    expect(text(fixture)).toContain('Sunset Apartments');
  });

  it('shows a not-found message without a retry option on a 404', async () => {
    propertyService.getById.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const fixture = await createFixture();
    fixture.detectChanges();

    expect(text(fixture)).toContain('Property not found');
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'));
    expect(buttons.some((button) => button.textContent?.includes('Retry'))).toBe(false);
  });

  it('reverts unsaved edits and keeps the panel open when Cancel is clicked', async () => {
    propertyService.getById.mockReturnValue(of(property));
    const fixture = await createFixture();
    fixture.detectChanges();

    clickButtonByText(fixture, 'Edit');
    fixture.detectChanges();

    const streetInput = fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement;
    streetInput.value = 'Changed St';

    clickButtonByText(fixture, 'Cancel');
    fixture.detectChanges();

    expect(text(fixture)).toContain('Main St');
    expect(text(fixture)).not.toContain('Changed St');

    clickButtonByText(fixture, 'Edit');
    fixture.detectChanges();

    const reseededStreetInput = fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement;
    expect(reseededStreetInput.value).toBe('Main St');
  });

  it('saves the edited address and returns to the read-only view', async () => {
    propertyService.getById.mockReturnValue(of(property));
    const fixture = await createFixture();
    fixture.detectChanges();

    clickButtonByText(fixture, 'Edit');
    fixture.detectChanges();

    const streetInput = fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement;
    streetInput.value = 'New Street';

    const updated: Property = { ...property, address: { ...property.address, street: 'New Street' } };
    propertyService.update.mockReturnValue(of(updated));

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(propertyService.update).toHaveBeenCalledWith(
      PROPERTY_ID,
      expect.objectContaining({
        name: property.name,
        type: property.type,
        status: property.status,
        address: expect.objectContaining({ street: 'New Street' }),
      }),
    );
    expect(text(fixture)).toContain('New Street');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });

  it('shows a validation error and does not save when a required field is blank', async () => {
    propertyService.getById.mockReturnValue(of(property));
    const fixture = await createFixture();
    fixture.detectChanges();

    clickButtonByText(fixture, 'Edit');
    fixture.detectChanges();

    const streetInput = fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement;
    streetInput.value = '   ';

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(propertyService.update).not.toHaveBeenCalled();
    expect(text(fixture)).toContain('Please fill in all address fields.');
  });

  it('shows an inline error and preserves edits when saving fails', async () => {
    propertyService.getById.mockReturnValue(of(property));
    const fixture = await createFixture();
    fixture.detectChanges();

    clickButtonByText(fixture, 'Edit');
    fixture.detectChanges();

    const streetInput = fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement;
    streetInput.value = 'New Street';

    propertyService.update.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(text(fixture)).toContain('Could not save the address');
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
    expect((fixture.nativeElement.querySelector('input[name="street"]') as HTMLInputElement).value).toBe('New Street');
  });
});
