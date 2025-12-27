import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root',
})
export class HoldingsService {

  constructor(private api: ApiService) {}

  getHoldings(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/holdings');
  }

}
