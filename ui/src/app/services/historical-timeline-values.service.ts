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

  getGainersAndLosers(timeframe: string, userIds?: string[]): Observable<ApiResponse> {
    let url = `/v1/holdings/gainers-losers?timeframe=${timeframe}`;
    if (userIds && userIds.length > 0) {
      // Add each userId as a separate query parameter
      const userIdsParam = userIds.map(id => `userIds=${id}`).join('&');
      url += `&${userIdsParam}`;
    }
    return this.api.get<ApiResponse>(url);
  }

}
