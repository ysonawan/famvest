import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {ApiService} from "./api.service";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class MarketInformationService {

  constructor(private api: ApiService) {

  }

  getMarketHolidays(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/market/holidays');
  }

}

