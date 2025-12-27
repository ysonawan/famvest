import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ApplicationPropertiesService {
  private config: any = {};
  private environment: string;

  constructor() {
    // Dynamically set the environment based on the hostname
    const hostname = window.location.hostname;
    this.environment = hostname === 'localhost' ? 'dev' : 'prod';
  }

  public loadConfig(): void {
    this.config = {
      dev: {
        baseUrl: 'http://localhost:8090/rest',
        wsUrl: 'http://localhost:8090/ws',
      },
      prod: {
        baseUrl: '/rest',
        wsUrl: '/ws',
      }
    };
  }

  getConfig(): any {
    return this.config[this.environment];
  }

  getEnvironment(): any {
    return this.environment;
  }
}
