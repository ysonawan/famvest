 import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {

  constructor(private apiService: ApiService) { }

  updateProfile(request: any): Observable<any> {
    return this.apiService.put('/v1/users/profile/update', request);
  }

  changePassword(request: any): Observable<any> {
    return this.apiService.put('/v1/users/profile/change-password', request);
  }

  getTradingAccounts(): Observable<any> {
    return this.apiService.get('/v1/users/profile/trading-accounts');
  }
}

