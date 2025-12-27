import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import {AuthUserService} from "../services/auth/auth-user.service";


@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(private auth: AuthUserService, private router: Router) {}

  canActivate(): boolean {
    if (this.auth.isAdmin()) {
      return true;
    }
    return false;
  }
}
