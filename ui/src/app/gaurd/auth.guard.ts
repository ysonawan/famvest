import { Injectable } from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {AuthUserService} from "../services/auth/auth-user.service";

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authUserService: AuthUserService, private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {

    const isAuthenticated = this.authUserService.isAuthenticated();

    if (isAuthenticated) {
      if (state.url === '/login') {
        // If the user is authenticated and trying to access the login page, redirect to dashboard
        this.router.navigate(['/home']);
        return false;
      }
      return true;
    } else if (state.url !== '/login') {
      // Redirect unauthenticated users to login if not already on the login page
      this.router.navigate(['/login']);
    } else {
      // If the user is not authenticated and trying to access the login page, allow access
      return true;
    }
    return false;
  }
}
