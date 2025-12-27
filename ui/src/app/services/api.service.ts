import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {ApplicationPropertiesService} from "../application-properties.service";

@Injectable({
  providedIn: 'root',
})
export class ApiService {

  private baseUrl: string = '';

  constructor(private http: HttpClient,
              private appProperties: ApplicationPropertiesService) {
    this.baseUrl = this.appProperties.getConfig().baseUrl;

  }

  private formatErrors(error: HttpErrorResponse) {
    return throwError(() => error);
  }

  get<T>(path: string, options = {}): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, options).pipe(
      catchError(this.formatErrors)
    );
  }

  post<T>(path: string, body: any = {}, options = {}): Observable<T> {
    console.log(`API POST: ${this.baseUrl}${path}`, body);
    return this.http.post<T>(`${this.baseUrl}${path}`, body, options).pipe(
      catchError(this.formatErrors)
    );
  }

  put<T>(path: string, body: any = {}, options = {}): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body, options).pipe(
      catchError(this.formatErrors)
    );
  }

  delete<T>(path: string, options = {}): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`, options).pipe(
      catchError(this.formatErrors)
    );
  }

  patch<T>(path: string, body: any = {}, options = {}): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}${path}`, body, options).pipe(
      catchError(this.formatErrors)
    );
  }
}
