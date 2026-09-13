import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: Keycloak,
          useValue: {
            realmAccess: { roles: [] },
            tokenParsed: { name: 'Test User', email: 'test@example.com' },
            logout: () => Promise.resolve(),
          },
        },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: signal({ type: 'Ready', args: undefined }) },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
