import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, includeBearerTokenInterceptor } from 'keycloak-angular';
import { routes } from './app.routes';
import { providePropStackKeycloak } from './core/config/keycloak.provider';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
    {
      provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
      useValue: [
        {
          urlPattern: new RegExp(`^${environment.apiBaseUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(/.*)?$`),
        },
      ],
    },
    providePropStackKeycloak(),
  ],
};