import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class AlgoService {

  constructor(private api: ApiService) {
  }

  getStraddles(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/algo/straddles`);
  }

  getStraddleExecutions(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/algo/straddles/executions`);
  }

  getStraddleExecutionsByStraddleId(straddleId: number): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/algo/straddles/executions/${straddleId}`);
  }

  getStraddleExecutionPnlForToday(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/algo/straddles/executions/pnl`);
  }

  updateStraddleStatus(id: number, status: any): Observable<ApiResponse> {
    return this.api.patch<ApiResponse>(`/v1/algo/straddles/${id}/status`, status);
  }

  executeStraddle(id: number): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/algo/straddles/${id}/execute`);
  }

  saveStraddleStrategy(straddleStrategy: any): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/algo/straddles`, straddleStrategy);
  }

  updateStraddleStrategy(id: number, straddleStrategy: any): Observable<ApiResponse> {
    return this.api.put<ApiResponse>(`/v1/algo/straddles/${id}`, straddleStrategy);
  }

  deleteStraddleStrategy(id: number): Observable<ApiResponse> {
    return this.api.delete<ApiResponse>(`/v1/algo/straddles/${id}`);
  }

}
