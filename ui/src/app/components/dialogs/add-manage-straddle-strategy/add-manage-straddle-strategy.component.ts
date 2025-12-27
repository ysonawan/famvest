import {Component, Inject, OnInit} from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from "@angular/forms";
import { MatDialogRef, MAT_DIALOG_DATA } from "@angular/material/dialog";
import { ToastrService } from "ngx-toastr";
import { CdkDrag, CdkDragHandle } from "@angular/cdk/drag-drop";
import {AlgoService} from "../../../services/algo.service";
import {NgForOf, NgIf} from "@angular/common";
import {TradingAccountService} from "../../../services/trading-account.service";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {InstrumentsService} from "../../../services/instruments.service";
import {UtilsService} from "../../../services/utils.service";

@Component({
  selector: 'app-add-manage-straddle-strategy',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CdkDrag,
    CdkDragHandle,
    NgForOf,
    SmallChipComponent,
    NgIf
  ],
  templateUrl: './add-manage-straddle-strategy.component.html',
  styleUrl: './add-manage-straddle-strategy.component.css'
})
export class AddManageStraddleStrategyComponent implements OnInit {

  straddleForm: FormGroup;
  isEditMode: boolean = false;
  users: any = [];
  id: number = 0;

  constructor(
    private fb: FormBuilder,
    private toastrService: ToastrService,
    private dialogRef: MatDialogRef<AddManageStraddleStrategyComponent>,
    private algoService: AlgoService,
    private userService: TradingAccountService,
    private utilService: UtilsService,
    @Inject(MAT_DIALOG_DATA) public data: any  // pass in existing strategy if editing
  ) {
    if(data) {
      this.isEditMode = true;
    }
    this.id = data?.id;
    this.straddleForm = this.fb.group({
      userId: [data?.userId || '', Validators.required],
      side: [data?.side || 'SHORT', Validators.required],
      instrument: [{ value: data?.instrument || '', disabled: this.isEditMode }, Validators.required],
      underlyingStrikeSelector: [data?.underlyingStrikeSelector || 'INDEX', Validators.required],
      underlyingSegment: [data?.underlyingSegment || '', Validators.required],
      exchange: [data?.exchange || '', Validators.required],
      tradingSegment: [data?.tradingSegment || '', Validators.required],
      index: [data?.index || '', Validators.required],
      strikeStep: [data?.strikeStep || '50', Validators.required],
      lots: [data?.lots || 1, Validators.required],
      orderType: [data?.marketOrder === false ? 'LIMIT' : 'MARKET', Validators.required],
      entryTime: [this.utilService.convertUtcTimeToLocal(data?.entryTime) || '09:30:00', Validators.required],
      exitTime: [this.utilService.convertUtcTimeToLocal(data?.exitTime) || '15:15:00', Validators.required],
      tradeType: [data?.paperTrade === false ? 'LIVE' : 'PAPER', Validators.required],
      stopLoss: [data?.stopLoss || '', Validators.required],
      trailingSl: [data?.trailingSl === true ? 'Yes' : 'No', Validators.required],
      target: [data?.target || '', Validators.required],
      expiryScope: [data?.expiryScope || 'CURRENT', Validators.required],
    });
    if (this.isEditMode) {
      this.straddleForm.get('userId')?.disable();
      this.straddleForm.get('instrument')?.disable();
    }

    this.straddleForm.get('instrument')?.valueChanges.subscribe(val => {
      let exchange, tradingSegment, index, underlyingSegment;
      let underlyingStrikeSelector = this.straddleForm.get('underlyingStrikeSelector')?.value;

      if((val === 'NIFTY 50' ||  val === 'NIFTY BANK') && underlyingStrikeSelector === 'INDEX') {
        underlyingSegment = 'NSE';
        exchange = 'NSE';
        tradingSegment = 'NFO-OPT';
        index = val === 'NIFTY BANK' ? 'BANKNIFTY' : 'NIFTY';
      } else if((val === 'NIFTY 50' ||  val === 'NIFTY BANK') && underlyingStrikeSelector === 'FUTURE') {
        underlyingSegment = 'NFO-FUT';
        exchange = 'NSE';
        tradingSegment = 'NFO-OPT';
        index = val === 'NIFTY BANK' ? 'BANKNIFTY' : 'NIFTY';
      } else if(val === 'SENSEX' && underlyingStrikeSelector === 'INDEX') {
        underlyingSegment = 'BSE';
        exchange = 'BSE';
        tradingSegment = 'BFO-OPT';
        index = 'SENSEX';
      } else if(val === 'SENSEX' && underlyingStrikeSelector === 'FUTURE') {
        underlyingSegment = 'BFO-FUT';
        exchange = 'BSE';
        tradingSegment = 'BFO-OPT';
        index = 'SENSEX';
      }
      this.straddleForm.patchValue({
        underlyingSegment: underlyingSegment,
        exchange: exchange,
        tradingSegment: tradingSegment,
        index: index
      });
    });

    this.straddleForm.get('underlyingStrikeSelector')?.valueChanges.subscribe(val => {
      let instrument = this.straddleForm.get('instrument')?.value;
      let underlyingSegment;
      if((instrument === 'NIFTY 50' ||  instrument === 'NIFTY BANK') && val === 'INDEX') {
        underlyingSegment = 'NSE';
      } else if((instrument === 'NIFTY 50' ||  instrument === 'NIFTY BANK') && val === 'FUTURE') {
        underlyingSegment = 'NFO-FUT';
      } else if(instrument === 'SENSEX' && val === 'INDEX') {
        underlyingSegment = 'BSE';
      } else if(instrument === 'SENSEX' && val === 'FUTURE') {
        underlyingSegment = 'BFO-FUT';
      }
      this.straddleForm.patchValue({
        underlyingSegment: underlyingSegment
      });
    });
  }

  fetchUsers(): void {
    this.userService.getTradingAccounts().subscribe({
      next: (response) => {
        this.users = response.data.filter(user => user.active);
      },
      error: (error) => {
        this.toastrService.error(error.error.message, 'Error');
        console.error(error);      }
    });
  }

  submit() {
    if (this.straddleForm.invalid) {
      this.straddleForm.markAllAsTouched();
      return; // Stop submission
    }
    const payload = this.straddleForm.getRawValue();

    const request$ = this.isEditMode
       ? this.algoService.updateStraddleStrategy(this.id, payload)
       : this.algoService.saveStraddleStrategy(payload);

    request$.subscribe({
      next: () => {
        this.toastrService.success(`Straddle strategy ${this.isEditMode ? 'updated' : 'created'} successfully.`, 'Success');
        this.dialogRef.close('submitted');
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while adding straddle strategy. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }

  ngOnInit(): void {
    this.fetchUsers();
  }
}
