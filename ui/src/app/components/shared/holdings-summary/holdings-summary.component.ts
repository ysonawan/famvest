import { Component, Input } from '@angular/core';
import {CommonModule, NgOptimizedImage} from "@angular/common";
import {fallbackAvatarUrl} from "../../../constants/constants";
import {UtilsService} from "../../../services/utils.service";

@Component({
  selector: 'app-holdings-summary',
  templateUrl: './holdings-summary.component.html',
  imports: [
    CommonModule,
  ],
  standalone: true,
  styleUrls: ['./holdings-summary.component.css']
})
export class HoldingsSummaryComponent {

  constructor(private utilsService: UtilsService) {

  }

  @Input() totalInvestment!: number;
  @Input() currentValue!: number;
  @Input() daysPnL!: number;
  @Input() daysPnLPercentage!: number;
  @Input() totalPnL!: number;
  @Input() totalPnLPercentage!: number;

}
