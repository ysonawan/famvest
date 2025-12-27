import { Component } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {AuthUserService} from "../../services/auth/auth-user.service";
import {ToastrService} from "ngx-toastr";
import {NgIf, NgOptimizedImage} from "@angular/common";
import {Router, RouterLink} from "@angular/router";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faEye, faEyeSlash, faSpinner} from "@fortawesome/free-solid-svg-icons";

@Component({
  selector: 'app-signup',
  standalone: true,
  templateUrl: './signup.component.html',
  imports: [
    ReactiveFormsModule,
    NgIf,
    NgOptimizedImage,
    FaIconComponent,
    RouterLink
  ]
})
export class SignUpComponent {
  signUpForm: FormGroup;
  otpForm: FormGroup;
  otpSent = false;
  isSendingOtp: boolean = false;
  isVerifyingOtp: boolean = false;

  constructor(private fb: FormBuilder,
              private authUserService: AuthUserService,
              private toastrService: ToastrService,
              private router: Router) {

    // Clear any existing token and reset logout flag to ensure HTTP calls work
    localStorage.removeItem('trade-manage-app-token');
    this.authUserService.resetLoggingOutFlag();

    this.signUpForm = this.fb.group({
      fullName: ['', Validators.required],
      password: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
    });

    this.otpForm = this.fb.group({
      otp: ['', Validators.required],
    });
  }

  sendOtp() {
    console.log('sendOtp() called');
    this.isSendingOtp = true;
    if (this.signUpForm.invalid) {
      this.signUpForm.markAllAsTouched(); // Show all validation errors
      this.isSendingOtp = false;
      console.log('Form is invalid, stopping submission');
      return; // Stop submission
    }
    const signupRequest = this.signUpForm.value;
    console.log('Signup request:', signupRequest);
    console.log('Calling authUserService.sendSignupOtp()...');
    this.authUserService.sendSignupOtp(signupRequest).subscribe({
      next: (response) => {
        console.log('OTP sent successfully:', response);
        this.isSendingOtp = false;
        this.toastrService.success('An OTP has been sent to your email address. Please check your inbox and spam folder.', 'Success');
        this.otpSent = true
      },
      error: (error) => {
        console.error('Error sending OTP:', error);
        this.isSendingOtp = false;
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while sending OTP. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  verifyOtp() {
    this.isVerifyingOtp = true;
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched(); // Show all validation errors
      this.isVerifyingOtp = false;
      return; // Stop submission
    }
    const payload = {
      ...this.signUpForm.value,
      otp: this.otpForm.value.otp,
    };

    this.authUserService.signup(payload).subscribe({
      next: (response) => {
        this.isVerifyingOtp = false;
        this.router.navigate(['/login']);
        this.toastrService.success('Account created! You can now log in.', 'Success');
      },
      error: (error) => {
        this.isVerifyingOtp = false;
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while registering user. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  protected readonly faSpinner = faSpinner;

  toLowerCase(event: Event) {
    const input = event.target as HTMLInputElement;
    input.value = input.value.toLowerCase();
    this.signUpForm.get('email')?.setValue(input.value, { emitEvent: false });
  }

  showPassword = false;
  protected readonly faEye = faEye;
  protected readonly faEyeSlash = faEyeSlash;
}
