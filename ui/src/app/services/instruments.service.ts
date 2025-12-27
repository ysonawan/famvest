import { Injectable } from '@angular/core';
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";
import {ApiService} from "./api.service";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class InstrumentsService {

  constructor(private api: ApiService, private http: HttpClient) {
  }

  getInstrumentByToken(instrumentToken: number): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/instruments?instrumentToken=${instrumentToken}`);
  }

  getInstrumentBySymbol(tradingSymbol: string, exchange: string): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/instruments?tradingSymbol=${tradingSymbol}&exchange=${exchange}`);
  }

}
