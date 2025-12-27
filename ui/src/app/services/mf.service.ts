import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class MfService {

  constructor(private api: ApiService) {
  }

  getHoldings(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/mf/holdings');
  }

  getOrders(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/mf/orders');
  }

  getSips(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/mf/sips');
  }

  updateSip(tradingAccountId: string, sipId: string, sipRequest: any,): Observable<ApiResponse> {
    return this.api.put<ApiResponse>(`/v1/mf/sips/${tradingAccountId}/${sipId}`, sipRequest);
  }


}
