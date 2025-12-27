import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class HistoricalCandleDataService {

  constructor(private api: ApiService) {
  }

  getHistoricalCandleData(request: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>('/v1/history/candles', request);
  }

}
