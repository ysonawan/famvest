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
  selectedHoliday: MarketHoliday | null = null;

  faCheckCircle = faCheckCircle;
  faTimesCircle = faTimesCircle;

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

  toggleHolidayDetails(holiday: MarketHoliday): void {
    this.selectedHoliday = this.selectedHoliday === holiday ? null : holiday;
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

}

