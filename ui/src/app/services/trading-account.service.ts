import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root',
})
export class TradingAccountService {

  constructor(private api: ApiService) {}

  onboardTradingAccount(tradingAccountRequest: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/accounts`, tradingAccountRequest);
  }

  updateTradingAccount(tradingAccountId: string, tradingAccountRequest: any): Observable<ApiResponse> {
    return this.api.put<ApiResponse>(`/v1/accounts/${tradingAccountId}`, tradingAccountRequest);
  }

  deleteTradingAccount(tradingAccountId: string): Observable<ApiResponse> {
    return this.api.delete<ApiResponse>(`/v1/accounts/${tradingAccountId}`);
  }

  unmapTradingAccount(tradingAccountId: string): Observable<ApiResponse> {
    return this.api.patch<ApiResponse>(`/v1/accounts/${tradingAccountId}/unmap`);
  }

  mapTradingAccount(tradingAccountId: string): Observable<ApiResponse> {
    return this.api.patch<ApiResponse>(`/v1/accounts/${tradingAccountId}/map`);
  }

  getTradingAccount(userId: any): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/accounts/${userId}`);
  }

  getTradingAccounts(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/accounts/profiles');
  }

  renewTokensForAll(): Observable<ApiResponse> {
    return this.api.post<ApiResponse>('/v1/accounts/renew-request-tokens');
  }

  getTotp(tradingAccountUserId: string): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/accounts/${tradingAccountUserId}/totp`);
  }
}
