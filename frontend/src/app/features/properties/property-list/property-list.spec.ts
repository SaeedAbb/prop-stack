import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router, UrlTree, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PropertyList, PropertyListMode } from './property-list';
import { Property } from '../../../core/models/property.model';
import { PropertyService } from '../../../core/services/property.service';

describe('PropertyList', () => {
  const PROPERTY_ID = '00000000-0000-0000-0000-000000000001';
  const property: Property = {
    id: PROPERTY_ID,
    name: 'Sunset Apartments',
    address: { street: 'Main St', houseNumber: '12', city: 'Springfield', state: 'IL', postalCode: '62701', country: 'USA' },
    type: 'APARTMENT',
    status: 'AVAILABLE',
  };

  let propertyService: { getAll: ReturnType<typeof vi.fn>; getAllDeleted: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let router: Router;

  function setup(mode: PropertyListMode) {
    propertyService = {
      getAll: vi.fn().mockReturnValue(of([property])),
      getAllDeleted: vi.fn().mockReturnValue(of([property])),
    };
    dialog = { open: vi.fn().mockReturnValue({ afterClosed: () => of(false) }) };

    return TestBed.configureTestingModule({
      imports: [PropertyList],
      providers: [
        provideRouter([]),
        { provide: PropertyService, useValue: propertyService },
        { provide: MatDialog, useValue: dialog },
        { provide: ActivatedRoute, useValue: { snapshot: { data: { mode } } } },
      ],
    }).compileComponents();
  }

  it('navigates to the property detail page when an owned card is clicked', async () => {
    await setup('owned');
    router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const fixture = TestBed.createComponent(PropertyList);
    fixture.detectChanges();

    const card = (fixture.nativeElement as HTMLElement).querySelector('.property-card') as HTMLElement;
    card.click();

    expect(navigateByUrl).toHaveBeenCalled();
    const navigatedUrl = router.serializeUrl(navigateByUrl.mock.calls[0][0] as UrlTree);
    expect(navigatedUrl).toBe(`/properties/${PROPERTY_ID}`);
  });

  it('does not navigate when a deleted card is clicked', async () => {
    await setup('deleted');
    router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const fixture = TestBed.createComponent(PropertyList);
    fixture.detectChanges();

    const card = (fixture.nativeElement as HTMLElement).querySelector('.property-card') as HTMLElement;
    card.click();

    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('opens the delete confirmation without navigating when the delete button is clicked', async () => {
    await setup('owned');
    router = TestBed.inject(Router);
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    const fixture = TestBed.createComponent(PropertyList);
    fixture.detectChanges();

    const deleteButton = (fixture.nativeElement as HTMLElement).querySelector(
      'button[aria-label="Delete property"]',
    ) as HTMLButtonElement;
    deleteButton.click();

    expect(dialog.open).toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });
});
