import {Component, Inject} from '@angular/core';
import {NgIf} from "@angular/common";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {TradingAccountService} from "../../../services/trading-account.service";
import {ToastrService} from "ngx-toastr";
import {ToggleSecretFieldComponent} from "../../shared/forms/toggle-secret-field/toggle-secret-field.component";
import {NoteComponent} from "../../shared/note/note.component";
import {CdkDrag, CdkDragHandle} from "@angular/cdk/drag-drop";

@Component({
  selector: 'app-update-trading-account',
    imports: [
        ReactiveFormsModule,
        NgIf,
        ToggleSecretFieldComponent,
        NoteComponent,
        CdkDrag,
        CdkDragHandle
    ],
  templateUrl: './update-trading-account.component.html',
  styleUrl: './update-trading-account.component.css'
})
export class UpdateTradingAccountComponent {

  updateUserForm: FormGroup;
  tradingAccount: any = {};

  constructor(private fb: FormBuilder,
              private tradingAccountService: TradingAccountService,
              private toastrService: ToastrService,
              private dialogRef: MatDialogRef<UpdateTradingAccountComponent>,
              @Inject(MAT_DIALOG_DATA) public data: { tradingAccount: any}) {

    this.updateUserForm = this.fb.group({
      name: ['', Validators.required],
      userId: ['', Validators.required],
      password: [''],
      apiKey: [''],
      apiSecret: [''],
      totpKey: [''],
    });
    if(data.tradingAccount) {
      this.updateUserForm.get('userId')?.disable();
      this.tradingAccount = data.tradingAccount;
      this.updateUserForm.patchValue({
        name: this.tradingAccount.name,
        userId: this.tradingAccount.userId
      });
    }
  }

  update() {
    if (this.updateUserForm.invalid) {
      this.updateUserForm.markAllAsTouched(); // Show all validation errors
      return; // Stop submission
    }
    const onboardRequest = this.updateUserForm.value;
    if(this.tradingAccount.id) {
      onboardRequest.id=this.tradingAccount.id;
      onboardRequest.userId=this.tradingAccount.userId;
    }
    this.tradingAccountService.updateTradingAccount(this.tradingAccount.userId, onboardRequest).subscribe({
      next: (response) => {
        this.toastrService.success('Trading account updated successfully.', 'Success');
        this.dialogRef.close('updated');
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while updating trading account. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
