import { Component, Input } from '@angular/core';
import {IndexQuote} from "../../../models/index-quote";
import {DecimalPipe, NgClass, NgForOf, NgIf, PercentPipe} from "@angular/common";

@Component({
  selector: 'app-index-ticker',
  templateUrl: './index-ticker.component.html',
  imports: [
    NgClass,
    NgForOf,
    DecimalPipe,
    NgIf,
    PercentPipe,
  ],
  standalone: true,
  styleUrls: ['./index-ticker.component.css']
})
export class IndexTickerComponent {
  /** Array of quotes to render */
  @Input() quotes: IndexQuote[] = [];
  @Input() showTickers!: boolean;

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }
}
