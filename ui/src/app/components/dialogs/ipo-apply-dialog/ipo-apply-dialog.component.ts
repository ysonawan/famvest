import { Component, Inject, OnInit, Output, EventEmitter } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CdkDrag, CdkDragHandle } from '@angular/cdk/drag-drop';
import { ToastrService } from 'ngx-toastr';
import { IstDatePipe } from '../../shared/pipes/ist-date.pipe';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTrash, faPlus } from '@fortawesome/free-solid-svg-icons';
import {MatTooltip} from "@angular/material/tooltip";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {UserDropdownComponent} from "../../shared/user-dropdown/user-dropdown.component";
import { IposService } from '../../../services/ipos.service';

interface Bid {
  quantity: number;
  price: number;
  auto_cutoff: boolean;
}

@Component({
  selector: 'app-ipo-apply-dialog',
  templateUrl: './ipo-apply-dialog.component.html',
  styleUrls: ['./ipo-apply-dialog.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    CdkDrag,
    CdkDragHandle,
    IstDatePipe,
    FaIconComponent,
    MatTooltip,
    SmallChipComponent,
    UserDropdownComponent,
    UserDropdownComponent
  ]
})
export class IpoApplyDialogComponent implements OnInit {
  @Output() applicationSubmitted = new EventEmitter<void>();

  applyForm!: FormGroup;
  ipo: any;
  vpaName: string = '';
  selectedTradingAccountId: string = '';
  bids: Bid[] = [
    { quantity: 0, price: 0, auto_cutoff: false }
  ];

  totalAmountPayable = 0;
  protected readonly faTrash = faTrash;
  protected readonly faPlus = faPlus;
  readonly maxBids = 3;
  isLoadingVPA = false;
  isSubmitting = false;
  vpaData: any = null;

  constructor(
    private fb: FormBuilder,
    private toastrService: ToastrService,
    private dialogRef: MatDialogRef<IpoApplyDialogComponent>,
    private iposService: IposService,
    @Inject(MAT_DIALOG_DATA) public data: { ipo: any }
  ) {
    this.ipo = data.ipo;
    this.bids[0] = { quantity: this.ipo.min_qty || 1, price: this.ipo.investor_types[0].max_price, auto_cutoff: true };
    this.applyForm = this.fb.group({
      upiId: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9]+@[a-zA-Z0-9]+$/)]]
    });
  }

  ngOnInit(): void {
    this.calculateAmountPayable();
  }

  calculateAmountPayable(): void {
    this.totalAmountPayable = 0;
    this.bids.forEach((bid) => {
      if (bid.quantity > 0) {
        const bidPrice = bid.auto_cutoff ? this.ipo.investor_types[0].max_price : bid.price;
        if (bidPrice > 0) {
          this.totalAmountPayable += bid.quantity * bidPrice;
        } else if (bid.auto_cutoff) {
          this.totalAmountPayable += bid.quantity * this.ipo.investor_types[0].max_price;
        }
      }
    });
  }

  onCutoffPriceToggle(index: number): void {
    this.bids[index].price = this.getMaxPrice();
    this.calculateAmountPayable();
  }

  getMinPrice(): number {
    return this.ipo.investor_types?.[0]?.min_price || 0;
  }

  getMaxPrice(): number {
    return this.ipo.investor_types?.[0]?.max_price || 0;
  }

  getLotsMinQty(): number {
    return this.ipo.min_qty || 1;
  }

  isValidBid(bid: Bid): boolean {
    if (bid.quantity === 0) return true; // Empty bid is valid (optional)
    if (bid.quantity < this.getLotsMinQty()) return false;
    return bid.auto_cutoff || (bid.price >= this.getMinPrice() && bid.price <= this.getMaxPrice());
  }

  addBid(): void {
    if (this.bids.length < this.maxBids) {
      this.bids.push({ quantity: 0, price: 0, auto_cutoff: false });
    }
  }

  removeBid(index: number): void {
    if (this.bids.length > 1) {
      this.bids.splice(index, 1);
      this.calculateAmountPayable();
    }
  }

  canAddBid(): boolean {
    return this.bids.length < this.maxBids;
  }

  fetchVPAId(tradingAccountId: string): void {
    if (tradingAccountId) {
      this.isLoadingVPA = true;
      this.iposService.getVPA(tradingAccountId).subscribe({
        next: (response: any) => {
          this.vpaData = response.data;
          // Populate UPI ID if available
          if (response.data?.vpa) {
            this.applyForm.get('upiId')?.setValue(response.data.vpa);
            this.vpaName = response.data.name || '';
          } else {
            this.applyForm.get('upiId')?.setValue('');
            this.vpaName = '';
          }
          this.isLoadingVPA = false;
        },
        error: (error) => {
          this.isLoadingVPA = false;
          this.applyForm.get('upiId')?.setValue('');
          this.vpaName = '';
          console.error('Error fetching VPA:', error);
          this.toastrService.error('Failed to fetch VPA details', 'Error');
        }
      });
    }
  }

  submit(): void {
    if (!this.selectedTradingAccountId) {
      this.toastrService.error('Please select a trading account.', 'Validation Error');
      return;
    }

    if (this.applyForm.invalid) {
      this.applyForm.markAllAsTouched();
      this.toastrService.error('Please fill in the required fields correctly.', 'Validation Error');
      return;
    }

    // Validate at least one bid
    const hasValidBid = this.bids.some(bid => bid.quantity > 0);
    if (!hasValidBid) {
      this.toastrService.error('Please enter at least one bid.', 'Validation Error');
      return;
    }

    // Validate all bids
    for (let i = 0; i < this.bids.length; i++) {
      if (!this.isValidBid(this.bids[i])) {
        this.toastrService.error(`Bid ${i + 1} is invalid. Check quantity and price range.`, 'Validation Error');
        return;
      }
    }

    const ipoBidRequest = {
      upi_id: this.applyForm.get('upiId')?.value,
      instrument_id: this.ipo.id,
      investor_type: 'IND',
      bids: this.bids.filter(bid => bid.quantity > 0)
    };

    this.isSubmitting = true;
    this.iposService.submitIpoApplication(this.selectedTradingAccountId, ipoBidRequest).subscribe({
      next: (response: any) => {
        this.isSubmitting = false;
        this.toastrService.success(response?.data?.message, 'Success');
        this.applicationSubmitted.emit();
        this.dialogRef.close({ success: true, data: response.data });
      },
      error: (error) => {
        this.isSubmitting = false;
        console.error('Error submitting IPO application:', error);
        const errorMessage = error?.error?.message || 'Failed to submit IPO application';
        this.toastrService.error(errorMessage, 'Error');
      }
    });
  }

  getIpoStatusChipColor(status: string): "green" | "orange" | "gray" {
    switch (status) {
      case "ongoing":
      case "preapply":
        return "green";
      case "upcoming":
        return "orange";
      default:
        return "gray";
    }
  }

  getIpoType(type: string): "SME" | "MAINBOARD" {
    if(type === 'IPO') {
      return "MAINBOARD";
    } else {
      return "SME";
    }
  }

  onClose(): void {
    this.dialogRef.close();
  }
}

