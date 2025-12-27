import { Component } from '@angular/core';
import {AuthUserService} from "../../services/auth/auth-user.service";
import {Router, RouterLink, ActivatedRoute} from "@angular/router";
import {
  FormBuilder,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators
} from "@angular/forms";
import {CommonModule, NgOptimizedImage} from "@angular/common";
import {ToastrService} from "ngx-toastr";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faEye, faEyeSlash, faSpinner} from "@fortawesome/free-solid-svg-icons";

@Component({
  selector: 'app-login',
  imports: [
    FormsModule,
    CommonModule,
    NgOptimizedImage,
    ReactiveFormsModule,
    RouterLink,
    FaIconComponent,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  loginForm: FormGroup;
  loginUsingOtp: boolean = true;
  otpSent: boolean = false;
  isSendingOtp: boolean = false;
  infoMessage: string | null = null;

  constructor(private authUserService: AuthUserService,
              private toastrService: ToastrService,
              private router: Router,
              private fb: FormBuilder,
              private route: ActivatedRoute) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: [''],
      otp: ['']
    });
    this.route.queryParams.subscribe(params => {
      if (params['reason'] === 'session-expired') {
        this.infoMessage = 'Your session has expired. Please log in again.';
      } else if (params['reason'] === 'logged-out') {
        this.infoMessage = 'You have been logged out. Please log in again.';
      } else if (params['reason'] === 'username-changed') {
        this.infoMessage = 'Your username has been changed. Please log in with your new username.';
      } else if (params['reason'] === 'password-changed') {
        this.infoMessage = 'Your password has been changed successfully. Please log in with your new password.';
      } else {
        this.infoMessage = null;
      }
    });
  }

  login(): void {
    this.authUserService.resetLoggingOutFlag();
    const email = this.loginForm.get('email')?.value;
    const password = this.loginForm.get('password')?.value;
    const otp = this.loginForm.get('otp')?.value;
    if (!email || this.loginForm.get('email')?.invalid) {
      this.loginForm.get('email')?.markAsTouched();
      return;
    }

    if (!password && !this.loginUsingOtp) {
      this.loginForm.get('password')?.markAsTouched();
      return;
    }

    if (!otp && this.loginUsingOtp) {
      this.loginForm.get('otp')?.markAsTouched();
      return;
    }

    this.authUserService.login({email: email, password: password, otp: otp}).subscribe({
      next: (response) => {
        this.authUserService.setAuthenticated(true); // Notify authentication status
        this.router.navigate(['/home']);
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while login. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  sendOtp() {
    this.isSendingOtp = true; // Start spinner
    const email = this.loginForm.get('email')?.value;
    if (!email || this.loginForm.get('email')?.invalid) {
      this.loginForm.get('email')?.markAsTouched();
      this.isSendingOtp = false;
      return;
    }
    this.authUserService.sendLoginOtp({email}).subscribe({
      next: (response) => {
        this.isSendingOtp = false;
        this.otpSent = true;
        this.toastrService.success('An OTP has been sent to your email address. Please check your inbox and spam folder.', 'Success');
      },
      error: (error) => {
        this.isSendingOtp = false;
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while sending OTP. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  protected readonly faSpinner = faSpinner;

  toLowerCase(event: Event) {
    const input = event.target as HTMLInputElement;
    input.value = input.value.toLowerCase();
    this.loginForm.get('email')?.setValue(input.value, { emitEvent: false });
  }

  showPassword = false;
  protected readonly faEye = faEye;
  protected readonly faEyeSlash = faEyeSlash;
}
