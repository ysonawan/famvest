import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class FundsService {

  constructor(private api: ApiService) {
  }

  getFunds(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/funds');
  }

  calculateMargin(marginCalculationRequest: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>('/v1/funds/margins/orders', marginCalculationRequest);
  }

  calculateCombinedMargin(marginCalculationRequest: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>('/v1/funds/margins/basket', marginCalculationRequest);
  }

}
