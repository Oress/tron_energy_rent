import { EnvironmentProviders } from '@angular/core';

import { provideApi } from '../api';
import { environment } from '../../../environments/environment';

export function provideDashboardApi(): EnvironmentProviders {
  return provideApi(environment.apiBasePath);
}
