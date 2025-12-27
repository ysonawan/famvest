import { Injectable } from '@angular/core';
import {
  HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, EMPTY } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import {AuthUserService} from "../services/auth/auth-user.service";

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authUserService: AuthUserService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // If user is already being logged out, don't make the API call
    if (this.authUserService.isLoggingOut()) {
      console.warn('HTTP request blocked - user is logging out:', req.url);
      return EMPTY;
    }

    const token = this.authUserService.getToken();
    let request = req;

    if (token) {
      request = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
    }

    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          // Prevent multiple logout attempts
          if (!this.authUserService.isLoggingOut()) {
            this.authUserService.logout();
            this.router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
          }
          // Return EMPTY to prevent error from propagating to component handlers
          // This prevents error messages from showing on the screen
          return EMPTY;
        }
        return throwError(() => error);
      })
    );
  }
}
