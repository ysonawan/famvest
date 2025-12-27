import {APP_INITIALIZER, ApplicationConfig, importProvidersFrom, LOCALE_ID} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {BrowserAnimationsModule, provideAnimations} from "@angular/platform-browser/animations";
import {
  HTTP_INTERCEPTORS,
  HttpClientModule,
} from "@angular/common/http";

import { ApplicationPropertiesService } from './application-properties.service';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import {provideToastr} from "ngx-toastr";
import { NgxEchartsModule } from 'ngx-echarts';

export function initConfig(config: ApplicationPropertiesService) {
  return () => config.loadConfig();
}
export const appConfig: ApplicationConfig = {
  providers: [
    BrowserAnimationsModule,
    provideRouter(routes),
    provideAnimations(),     // required for animations
    provideToastr({
      timeOut: 10000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    importProvidersFrom(HttpClientModule),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initConfig,
      deps: [ApplicationPropertiesService],
      multi: true
    },
    { provide: LOCALE_ID, useValue: 'en-IN' },
    importProvidersFrom(
      NgxEchartsModule.forRoot({
        echarts: () => import('echarts')
      })
    )
  ]
};
