import {Component, Input} from '@angular/core';
import {CurrencyPipe, DecimalPipe, NgIf} from "@angular/common";

@Component({
  selector: 'app-sip-summary',
  imports: [
    CurrencyPipe,
    NgIf,
    DecimalPipe
  ],
  templateUrl: './sip-summary.component.html',
  styleUrl: './sip-summary.component.css'
})
export class SipSummaryComponent {

  @Input() totalSipAmount!: number;
  @Input() contributionThisMonth!: number;
  @Input() upcomingSips!: number;
  @Input() activeSips!: number;
  @Input() pausedSips!: number;

}
