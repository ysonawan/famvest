import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class HistoricalTimelineValuesService {

  constructor(private api: ApiService) {
  }

  getHistoricalTimelineValues(type: string): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/history/timelines/${type}`);
  }

}
