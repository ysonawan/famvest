import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class UserPreferencesService {

  constructor(private api: ApiService) {
  }

  getUserPreferences(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>(`/v1/user-preferences`);
  }

  updatePreferenceValue(id: number, value: string): Observable<ApiResponse> {
    return this.api.patch<ApiResponse>(`/v1/user-preferences/${id}/value`, value);
  }
}
