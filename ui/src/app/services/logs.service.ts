import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class LogsService {

  constructor(private api: ApiService) {
  }

  getApplicationLogs(logFileName: string): Observable<any> {
    return this.api.get<any>(`/v1/logs/${logFileName}`, { responseType: 'text' });
  }

  getAlgoLogs(logFileName: string): Observable<any> {
    return this.api.get<any>(`/v1/logs/algo/${logFileName}`, { responseType: 'text' });
  }

}
