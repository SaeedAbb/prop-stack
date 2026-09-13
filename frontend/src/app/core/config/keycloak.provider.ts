import { provideKeycloak } from 'keycloak-angular';
import { environment } from '../../../environments/environment';

export function providePropStackKeycloak() {
  return provideKeycloak({
    config: {
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    },
    initOptions: {
      onLoad: 'login-required',
      pkceMethod: 'S256',
    },
  });
}