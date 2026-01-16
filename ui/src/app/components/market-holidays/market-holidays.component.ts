import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MarketInformationService } from '../../services/market-information.service';
import { MarketHoliday, MarketHolidaysResponse } from '../../models/market-holidays';
import { ToastrService } from 'ngx-toastr';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCheckCircle, faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { HttpErrorResponse } from '@angular/common/http';
import {IstDatePipe} from "../shared/pipes/ist-date.pipe";
import {SmallChipComponent} from "../shared/small-chip/small-chip.component";
import {MatTooltip} from "@angular/material/tooltip";

type SortableColumn = 'date' | 'description' | 'holiday_type';

@Component({
  selector: 'app-market-holidays',
  imports: [CommonModule, FaIconComponent, IstDatePipe, SmallChipComponent, MatTooltip],
  templateUrl: './market-holidays.component.html',
  styleUrl: './market-holidays.component.css'
})
export class MarketHolidaysComponent implements OnInit {

  holidays: MarketHoliday[] = [];
  errorMessage = '';
  isLoading = false;
  expandedExchanges: Set<MarketHoliday> = new Set();

  sortColumn: SortableColumn | null = 'date';
  sortDirection: 'asc' | 'desc' | null = 'asc';

  faCheckCircle = faCheckCircle;
  faTimesCircle = faTimesCircle;

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    date: (row) => row.date ?? 0,
    description: (row) => row.description?.toString().toLowerCase() ?? '',
    holiday_type: (row) => row.holiday_type?.toString().toLowerCase() ?? '',
  };

  get sortedHolidays(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.holidays;
    }

    return [...this.holidays].sort((a, b) => this.compareRows(a, b));
  }

  constructor(
    private marketInformationService: MarketInformationService,
    private toastrService: ToastrService
  ) { }

  ngOnInit(): void {
    this.fetchMarketHolidays();
  }

  fetchMarketHolidays(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.marketInformationService.getMarketHolidays().subscribe({
      next: (response: MarketHolidaysResponse) => {
        this.holidays = response.data;
        this.isLoading = false;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        if (error.error?.message) {
          this.toastrService.error(error.error.message, 'Error');
          this.errorMessage = error.error.message;
        } else {
          this.toastrService.error('An unexpected error occurred while fetching market holidays.', 'Error');
          this.errorMessage = 'Failed to load market holidays';
        }
      }
    });
  }

  setSort(column: SortableColumn): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'desc' ? 'asc' : 'desc';
      return;
    }
    this.sortColumn = column;
    this.sortDirection = this.getDefaultDirection(column);
  }

  private getDefaultDirection(column: SortableColumn): 'asc' | 'desc' {
    const descendingByDefault: SortableColumn[] = ['date'];
    return descendingByDefault.includes(column) ? 'desc' : 'asc';
  }

  getSortIndicator(column: SortableColumn): string {
    if (this.sortColumn !== column || !this.sortDirection) {
      return '';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private compareRows(a: any, b: any): number {
    const accessor = this.sortColumn ? this.sortAccessors[this.sortColumn] : null;
    if (!accessor || !this.sortDirection) {
      return 0;
    }

    const valueA = accessor(a);
    const valueB = accessor(b);

    if (valueA == null && valueB == null) { return 0; }
    if (valueA == null) { return this.sortDirection === 'asc' ? -1 : 1; }
    if (valueB == null) { return this.sortDirection === 'asc' ? 1 : -1; }

    let comparison: number;
    if (typeof valueA === 'string' && typeof valueB === 'string') {
      comparison = valueA.localeCompare(valueB);
    } else {
      comparison = (valueA as number) - (valueB as number);
    }

    return this.sortDirection === 'asc' ? comparison : -comparison;
  }

  toggleExchanges(holiday: MarketHoliday): void {
    if (this.expandedExchanges.has(holiday)) {
      this.expandedExchanges.delete(holiday);
    } else {
      this.expandedExchanges.add(holiday);
    }
  }

  isExchangesExpanded(holiday: MarketHoliday): boolean {
    return this.expandedExchanges.has(holiday);
  }

  getExchangeTimingTooltip(startTime: number, endTime: number): string {
    const startDate = new Date(startTime);
    const endDate = new Date(endTime);

    const startFormatted = startDate.toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });

    const endFormatted = endDate.toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });

    return `${startFormatted} - ${endFormatted}`;
  }

  trackById(index: number, item: any): any {
    return item.id;
  }

  isPastHoliday(dateString: string): boolean {
    const holidayDate = new Date(dateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    holidayDate.setHours(0, 0, 0, 0);
    return holidayDate < today;
  }

}

