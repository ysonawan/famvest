import {Injectable} from '@angular/core';
import {BehaviorSubject, tap} from 'rxjs';
import { ApiService } from "../api.service";
import {ApiResponse} from "../../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class AuthUserService {
  private authStatus = new BehaviorSubject<boolean>(!!localStorage.getItem('trade-manage-app-token'));
  authStatus$ = this.authStatus.asObservable();
  private loggingOut = false;

  constructor(private api: ApiService) {}

  login(credentials: { email: string, password: string, otp: string }) {
    return this.api.post<any>('/auth/login', credentials)
      .pipe(tap(response => {
        localStorage.setItem('trade-manage-app-token', response.token);
      }));
  }

  sendSignupOtp(signupRequest: any) {
    return this.api.post<any>('/auth/signup/send-otp', signupRequest);
  }

  sendLoginOtp(loginRequest: any) {
    return this.api.post<any>('/auth/login/send-otp', loginRequest);
  }

  signup(signupRequest: any) {
    return this.api.post<any>('/auth/signup', signupRequest);
  }

  logout(): void {
    this.loggingOut = true;
    localStorage.removeItem('trade-manage-app-token');
    this.setAuthenticated(false);
  }

  getToken(): string | null {
    return localStorage.getItem('trade-manage-app-token');
  }

  setAuthenticated(status: boolean): void {
    this.authStatus.next(status);
  }

  isAuthenticated(): boolean {
    return this.authStatus.getValue();
  }

  isLoggingOut(): boolean {
    return this.loggingOut;
  }

  resetLoggingOutFlag(): void {
    this.loggingOut = false;
  }

  isAdmin(): string | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.isAdmin || null;
    } catch (e) {
      return null;
    }
  }
}
