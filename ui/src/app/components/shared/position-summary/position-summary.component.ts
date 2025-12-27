import { Component, Input } from '@angular/core';
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-position-summary',
  templateUrl: './position-summary.component.html',
  imports: [
    CommonModule,
  ],
  standalone: true,
  styleUrls: ['./position-summary.component.css']
})
export class PositionSummaryComponent {
  @Input() maxProfit!: number;
  @Input() profitLeft!: number;
  @Input() daysPnL!: number;
  @Input() totalPnL!: number;
}
