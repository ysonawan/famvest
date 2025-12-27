import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class IposService {

  constructor(private api: ApiService) {
  }

  getIPOs(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/ipos`);
  }

  getIPOApplications(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/ipos/applications`);
  }

  getVPA(tradingAccountId: string): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/ipos/${tradingAccountId}/vpa`);
  }

  submitIpoApplication(tradingAccountId: string, ipoBidRequest: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/ipos/${tradingAccountId}/applications`, ipoBidRequest);
  }

  cancelIpoApplication(tradingAccountId: string, applicationId: string): Observable<ApiResponse> {
    return this.api.delete<ApiResponse>(`/v1/ipos/${tradingAccountId}/applications/${applicationId}`);
  }

}
