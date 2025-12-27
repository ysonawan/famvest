import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class ScheduledTasksService {

  constructor(private api: ApiService) {
  }

  getScheduledTasks(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/admin/schedulers`);
  }

  executeScheduledTask(id: number): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/admin/schedulers/${id}/execute`);
  }

  updateScheduledTaskStatus(id: number, status: any): Observable<ApiResponse> {
    return this.api.patch<ApiResponse>(`/v1/admin/schedulers/${id}/status`, status);
  }


}
