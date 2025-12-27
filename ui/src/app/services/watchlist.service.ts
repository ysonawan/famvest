import { Injectable } from '@angular/core';
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";
import {ApiService} from "./api.service";
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class WatchlistService {

  constructor(private api: ApiService, private http: HttpClient) {
  }

  getWatchlist(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/watchlist`);
  }

  searchInstruments(searchTerm: string): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/watchlist/instruments?search=${searchTerm}`);
  }

  saveWatchListInstrument(watchlistId: number, watchlistInstrument: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/watchlist/${watchlistId}/watchlistInstruments`, watchlistInstrument);
  }

  deleteWatchListInstrument(watchlistId: number, watchlistInstrumentId: number): Observable<ApiResponse> {
    return this.api.delete<ApiResponse>(`/v1/watchlist/${watchlistId}/watchlistInstruments/${watchlistInstrumentId}`);
  }

  updateWatchlistName(watchlistId: number, newName: string) {
    return this.api.patch<ApiResponse>(`/v1/watchlist/${watchlistId}`, { name: newName });
  }

  reorderWatchlistInstruments(watchlistId: number, watchlistInstrumentIds: number[]): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/watchlist/${watchlistId}/reorder`, watchlistInstrumentIds);
  }
}
