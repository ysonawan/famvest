import { Component, Input } from '@angular/core';
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-mf-order-summary',
  templateUrl: './mf-order-summary.component.html',
  imports: [
    CommonModule,
  ],
  standalone: true,
  styleUrls: ['./mf-order-summary.component.css']
})
export class MfOrderSummaryComponent {
  @Input() totalBuyOrders!: number;
  @Input() totalBuyAmount!: number;
  @Input() totalSellOrders!: number;
  @Input() totalSellAmount!: number;
}
