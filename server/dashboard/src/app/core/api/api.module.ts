import { NgModule, ModuleWithProviders, SkipSelf, Optional } from '@angular/core';
import { NrgyrentDashboardConfiguration } from './configuration';
import { HttpClient } from '@angular/common/http';


@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: []
})
export class NrgyrentDashboardApiModule {
    public static forRoot(configurationFactory: () => NrgyrentDashboardConfiguration): ModuleWithProviders<NrgyrentDashboardApiModule> {
        return {
            ngModule: NrgyrentDashboardApiModule,
            providers: [ { provide: NrgyrentDashboardConfiguration, useFactory: configurationFactory } ]
        };
    }

    constructor( @Optional() @SkipSelf() parentModule: NrgyrentDashboardApiModule,
                 @Optional() http: HttpClient) {
        if (parentModule) {
            throw new Error('NrgyrentDashboardApiModule is already loaded. Import in your base AppModule only.');
        }
        if (!http) {
            throw new Error('You need to import the HttpClientModule in your AppModule! \n' +
            'See also https://github.com/angular/angular/issues/20575');
        }
    }
}
