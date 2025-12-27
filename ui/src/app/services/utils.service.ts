import { Injectable } from '@angular/core';
import {fallbackAvatarUrl} from "../constants/constants";
import {DetailInfoDialogComponent} from "../components/dialogs/detail-info-dialog/detail-info-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {HistoricalTimelineValuesService} from "./historical-timeline-values.service";
import {ToastrService} from "ngx-toastr";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class UtilsService {

  constructor(private dialog: MatDialog,
              private historicalTimelineValuesService: HistoricalTimelineValuesService,
              private toastrService: ToastrService) { }

  getAvatarUrl(users: any[], userId: string) {
    const user = users.find(u => u.userId === userId);
    return user?.profile?.avatarURL || fallbackAvatarUrl;
  }

  getUserName(users: any[], userId: string) {
    const user = users.find(u => u.userId === userId);
    return user?.name || 'Unknown User';
  }

  getUserFullName(users: any[], userId: string) {
    const user = users.find(u => u.userId === userId);
    return user?.profile?.userName || 'Unknown User';
  }

  sort(sortKey: string, sortOrder: string, rows: any[]) {
    const direction = sortOrder === 'desc' ? -1 : 1;

    rows = [...rows].sort((a, b) => {
      const aVal = this.getNestedValue(a, sortKey);
      const bVal = this.getNestedValue(b, sortKey);

      // Handle numeric values
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return (aVal - bVal) * direction;
      }

      // Handle date strings
      if (Date.parse(aVal) && Date.parse(bVal)) {
        return (new Date(aVal).getTime() - new Date(bVal).getTime()) * direction;
      }

      // Fallback to string comparison
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return aVal.localeCompare(bVal) * direction;
      }

      return 0; // If unable to compare
    });
    return rows;
  }

  filter(filterSelection: any[], rows: any[]) {
    filterSelection.forEach((filter) => {
      if (filter.selected.length > 0 && !filter.key.startsWith('custom.')) {
        rows = rows.filter((row: any) => {
          const value = this.getNestedValue(row, filter.key);
          return filter.selected.includes(value.toUpperCase());
        });
      }
    });
    return rows;
  }

  private getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((acc, part) => acc?.[part], obj);
  }

  getElapsedYMDString(dateInput: number): string {

    const inputDate = new Date(dateInput);
    const today = new Date();

    // Ensure input is valid
    if (isNaN(inputDate.getTime())) return '';

    let years = today.getFullYear() - inputDate.getFullYear();
    let months = today.getMonth() - inputDate.getMonth();
    let days = today.getDate() - inputDate.getDate();

    // Adjust months and years if needed
    if (days < 0) {
      months--;
      // Get number of days in previous month
      const prevMonth = new Date(today.getFullYear(), today.getMonth(), 0);
      days += prevMonth.getDate();
    }

    if (months < 0) {
      years--;
      months += 12;
    }

    const y = years > 0 ? `${years}y` : '';
    const m = months > 0 ? `${months}M` : '';

    return (y + m) || '0d';
  }

  showInfo(sourceData: any, title: string) {
    this.dialog.open(DetailInfoDialogComponent, {
      disableClose: false,       // Prevent closing via escape or backdrop click
      autoFocus: false,          // Focus the first form element inside the dialog
      hasBackdrop: true,        // Show a dark background overlay
      closeOnNavigation: true,  // Optional: closes the dialog if navigation occurs
      data: {
        sourceData: sourceData,
        title: title
      }
    });
  }

  formatCustomDate(date: Date): string {
    const day = date.getDate();
    const month = date.toLocaleString('en-US', { month: 'short' });
    const year = date.getFullYear();

    const getOrdinal = (n: number): string => {
      const suffixes = ['th', 'st', 'nd', 'rd'];
      const v = n % 100;
      return suffixes[(v - 20) % 10] || suffixes[v] || 'th';
    };

    return `${day}${getOrdinal(day)} ${month} ${year}`;
  }

  convertUtcTimeToLocal(utcTimeStr: string): string {
    if(!utcTimeStr) {
      return '';
    }
    const [hours, minutes, seconds] = utcTimeStr.split(':').map(Number);
    // Create a UTC Date using today's date and the given time
    const utcDate = new Date();
    utcDate.setUTCHours(hours, minutes, seconds, 0);

    // Convert to local time string (HH:mm:ss)
    const localTime = utcDate.toLocaleTimeString('en-GB', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    return localTime;
  }

  // Timeline Chart Utilities
  fetchHistoricalTimelineValues(type: string): Observable<any> {
    return this.historicalTimelineValuesService.getHistoricalTimelineValues(type);
  }

  populateHoldingsTimelineChartData(historicalTimelineValues: any[], selectedUserIds: string[] = []): {
    dateLabels: string[];
    legendLabels: string[];
    series: any[];
  } {
    // Extract dates for X-axis
    const dateLabels = historicalTimelineValues.map(entry =>
      new Date(entry.date).toISOString().split('T')[0]
    );

    // Extract timeline points
    const investedAmountSeries: number[] = [];
    const currentValueSeries: number[] = [];
    const netPnlSeries: number[] = [];

    for (const entry of historicalTimelineValues) {
      const holdings = entry.historicalHoldingsTimelines ?? [];

      // Filter by selected user IDs
      const filteredHoldings = selectedUserIds.length === 0
        ? holdings
        : holdings.filter((h: any) => selectedUserIds.includes(h.userId));

      // Sum the values for the date across all matching users
      const investedAmountSum = filteredHoldings.reduce(
        (sum: number, h: any) => sum + (h.investedAmount ?? 0),
        0
      );

      const currentValueSum = filteredHoldings.reduce(
        (sum: number, h: any) => sum + (h.currentValue ?? 0),
        0
      );

      const netPnlSum = filteredHoldings.reduce(
        (sum: number, h: any) => sum + (h.netPnl ?? 0),
        0
      );

      investedAmountSeries.push(investedAmountSum);
      currentValueSeries.push(currentValueSum);
      netPnlSeries.push(netPnlSum);
    }

    // Prepare chart series
    const series = [
      { name: 'Invested Amount', data: investedAmountSeries, smooth: true },
      { name: 'Current Value', data: currentValueSeries, smooth: true },
      { name: 'Total P&L', data: netPnlSeries, smooth: true }
    ];

    const legendLabels = series.map(s => s.name);

    return {
      dateLabels,
      legendLabels,
      series
    };
  }

  populateSipsTimelineChartData(historicalTimelineValues: any[], selectedUserIds: string[] = []): {
    dateLabels: string[];
    legendLabels: string[];
    series: any[];
  } {
    // Extract dates for X-axis
    const dateLabels = historicalTimelineValues.map(entry =>
      new Date(entry.date).toISOString().split('T')[0]
    );

    // Extract timeline points for SIPs
    const sipAmountSeries: number[] = [];
    const fundsSeries: number[] = [];

    for (const entry of historicalTimelineValues) {
      const sipAmounts = entry.historicalMfSipsTimelines ?? [];

      // Filter by selected user IDs
      const filteredSipAmounts = selectedUserIds.length === 0
        ? sipAmounts
        : sipAmounts.filter((h: any) => selectedUserIds.includes(h.userId));

      // Sum the values for the date across all matching users
      const sipAmount = filteredSipAmounts.reduce(
        (sum: number, sip: any) => sum + (sip.sipAmount ?? 0),
        0
      );

      const funds = filteredSipAmounts.reduce(
        (sum: number, sip: any) => sum + (sip.activeSips ?? 0),
        0
      );

      sipAmountSeries.push(sipAmount);
      fundsSeries.push(funds);
    }

    // Prepare chart series
    const series = [
      { name: 'Total SIP Amount', data: sipAmountSeries, smooth: true },
      { name: 'Number of SIPs', data: fundsSeries, smooth: true }
    ];

    const legendLabels = series.map(s => s.name);

    return {
      dateLabels,
      legendLabels,
      series
    };
  }

  populatePositionsTimelineChartData(historicalTimelineValues: any[], selectedUserIds: string[] = []): {
    dateLabels: string[];
    legendLabels: string[];
    series: any[];
  } {
    // Extract dates for X-axis
    const dateLabels = historicalTimelineValues.map(entry =>
      new Date(entry.date).toISOString().split('T')[0]
    );

    // Extract timeline points
    const dailyEODPnlSeries: number[] = [];
    const openPositionsSeries: number[] = [];

    for (const entry of historicalTimelineValues) {
      const positions = entry.historicalPositionsTimelines ?? [];
      // Filter by selected user IDs
      const filteredPositions = selectedUserIds.length === 0
        ? positions
        : positions.filter((p: any) => selectedUserIds.includes(p.userId));
      // Sum the dayPnl values for the date across all matching users
      const dayPnlSum = filteredPositions.reduce(
        (sum: number, p: any) => sum + (p.totalEodPnl ?? 0),
        0
      );
      dailyEODPnlSeries.push(dayPnlSum);
      // Count open derivative positions for the date
      const openPositionCount = filteredPositions.reduce(
        (sum: number, p: any) => sum + (p.openDerivativePositions ?? 0),
        0
      );
      openPositionsSeries.push(openPositionCount);
    }

    // Prepare chart series
    const series = [
      { name: 'Daily EOD PnL', data: dailyEODPnlSeries, type: 'bar' }
    ];

    const legendLabels = series.map(s => s.name);

    return {
      dateLabels,
      legendLabels,
      series
    };
  }
}
