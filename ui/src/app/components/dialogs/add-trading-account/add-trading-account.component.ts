import {Component, Inject} from '@angular/core';
import {NgIf} from "@angular/common";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {TradingAccountService} from "../../../services/trading-account.service";
import {ToastrService} from "ngx-toastr";
import {ToggleSecretFieldComponent} from "../../shared/forms/toggle-secret-field/toggle-secret-field.component";
import {CdkDrag, CdkDragHandle} from "@angular/cdk/drag-drop";

@Component({
  selector: 'app-add-trading-account',
  imports: [
    ReactiveFormsModule,
    NgIf,
    ToggleSecretFieldComponent,
    CdkDrag,
    CdkDragHandle
  ],
  templateUrl: './add-trading-account.component.html',
  standalone: true,
  styleUrl: './add-trading-account.component.css'
})
export class AddTradingAccountComponent {

  onboardForm: FormGroup;
  tradingAccount: any = {};

  constructor(private fb: FormBuilder,
              private tradingAccountService: TradingAccountService,
              private toastrService: ToastrService,
              private dialogRef: MatDialogRef<AddTradingAccountComponent>) {

    this.onboardForm = this.fb.group({
      name: ['', Validators.required],
      userId: ['', Validators.required],
      password: [''],
      apiKey: ['', Validators.required],
      apiSecret: ['', Validators.required],
      totpKey: [''],
    });
  }

  submit() {
    if (this.onboardForm.invalid) {
      this.onboardForm.markAllAsTouched(); // Show all validation errors
      return; // Stop submission
    }
    const onboardRequest = this.onboardForm.value;
    this.tradingAccountService.onboardTradingAccount(onboardRequest).subscribe({
      next: (response) => {
        this.toastrService.success('Trading account successfully onboarded.', 'Success');
        this.dialogRef.close('submitted');
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while adding trading account. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }

  toUpperCase(event: Event) {
    const input = event.target as HTMLInputElement;
    input.value = input.value.toUpperCase();
    this.onboardForm.get('userId')?.setValue(input.value, { emitEvent: false });
  }
}
