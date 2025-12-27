import {Component, EventEmitter, Input, OnInit, OnChanges, SimpleChanges, Output} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import {FormsModule} from "@angular/forms";
import {fallbackAvatarUrl} from "../../../constants/constants";
import {CdkDrag, CdkDragHandle} from "@angular/cdk/drag-drop";
import {UserFilterComponent} from "../user-filter/user-filter.component";

export interface UserDetailsData {
  userId: string;
  userName: string;
  avatarUrl?: string;
  [key: string]: any; // Allow dynamic properties
}

export interface ColumnConfig {
  key: string;
  label: string;
  type?: 'text' | 'currency' | 'number' | 'percentage' | 'currency-with-percent';
  colorize?: boolean; // Whether to apply green/red colors based on positive/negative values
  align?: 'left' | 'right' | 'center';
}

@Component({
  selector: 'app-pnl-details-popup',
  imports: [CommonModule, NgOptimizedImage, FormsModule, CdkDrag, CdkDragHandle, UserFilterComponent],
  templateUrl: './pnl-details-popup.component.html',
  standalone: true,
  styleUrls: ['./pnl-details-popup.component.css']
})
export class PnlDetailsPopupComponent implements OnInit, OnChanges {

  @Input() userData: UserDetailsData[] = [];
  @Input() columns: ColumnConfig[] = [];
  @Input() title: string = 'User Details';
  @Input() sortByColumn?: string; // Column key to sort by (descending)
  @Output() close = new EventEmitter<void>();

  filteredData: UserDetailsData[] = [];
  selectedUserIds: string[] = [];

  constructor() {}

  ngOnInit(): void {
    this.filteredData = [...this.userData];
    this.applySorting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['userData']) {
      // Reapply the filter instead of resetting it
      this.applyFilter();
    }
  }

  onUserSelectionChange(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.applyFilter();
  }

  onSelectAllUsers(): void {
    this.selectedUserIds = [];
    this.applyFilter();
  }

  private applyFilter(): void {
    if (this.selectedUserIds.length === 0) {
      // Show all users when no filter is selected
      this.filteredData = [...this.userData];
    } else {
      // Filter by selected user IDs
      this.filteredData = this.userData.filter(user =>
        this.selectedUserIds.includes(user.userId)
      );
    }
    this.applySorting();
  }

  private applySorting(): void {
    if (this.sortByColumn) {
      this.filteredData.sort((a, b) => {
        const aVal = a[this.sortByColumn!] || 0;
        const bVal = b[this.sortByColumn!] || 0;
        return bVal - aVal; // Descending order
      });
    }
  }

  closePopup(): void {
    this.close.emit();
  }

  getCellValue(row: UserDetailsData, column: ColumnConfig): string {
    const value = row[column.key];

    if (value === null || value === undefined) {
      return '-';
    }

    switch (column.type) {
      case 'currency':
        return `₹${this.formatNumber(value)}`;
      case 'number':
        return this.formatNumber(value);
      case 'percentage':
        return `${this.formatNumber(value)}%`;
      case 'currency-with-percent':
        // For currency-with-percent, we expect the value to be the amount
        // and the percentage to be in a field with the same key + 'Percent'
        const percentKey = column.key + 'Percent';
        const percentValue = row[percentKey];
        if (percentValue !== null && percentValue !== undefined) {
          return `₹${this.formatNumber(value)} (${this.formatNumber(percentValue)}%)`;
        }
        return `₹${this.formatNumber(value)}`;
      default:
        return String(value);
    }
  }

  private formatNumber(value: number): string {
    if (typeof value !== 'number') {
      return String(value);
    }
    // Return full number with 2 decimal places and comma separators
    return value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  getCellClass(row: UserDetailsData, column: ColumnConfig): string {
    const classes = ['font-medium'];

    if (column.colorize) {
      const value = row[column.key];
      if (typeof value === 'number') {
        if (value > 0) {
          classes.push('text-green-600');
        } else if (value < 0) {
          classes.push('text-red-600');
        } else {
          classes.push('text-gray-700');
        }
      }
    } else {
      classes.push('text-gray-700');
    }

    return classes.join(' ');
  }

  getAlignment(column: ColumnConfig): string {
    return column.align || 'right';
  }

  getSummaryValue(column: ColumnConfig): string {
    const sum = this.filteredData.reduce((total, user) => {
      const value = user[column.key];
      if (typeof value === 'number') {
        return total + value;
      }
      return total;
    }, 0);

    switch (column.type) {
      case 'currency':
        return `₹${this.formatNumber(sum)}`;
      case 'number':
        return this.formatNumber(sum);
      case 'percentage':
        // For percentage, we might want average instead of sum
        const count = this.filteredData.filter(user => typeof user[column.key] === 'number').length;
        const avg = count > 0 ? sum / count : 0;
        return `${this.formatNumber(avg)}%`;
      case 'currency-with-percent':
        const percentKey = column.key + 'Percent';
        const percentSum = this.filteredData.reduce((total, user) => {
          const value = user[percentKey];
          if (typeof value === 'number') {
            return total + value;
          }
          return total;
        }, 0);
        const percentCount = this.filteredData.filter(user => typeof user[percentKey] === 'number').length;
        return `₹${this.formatNumber(sum)}`;
      default:
        return '-';
    }
  }

  getSummaryClass(column: ColumnConfig): string {
    const classes = ['font-bold'];

    if (column.colorize) {
      const sum = this.filteredData.reduce((total, user) => {
        const value = user[column.key];
        if (typeof value === 'number') {
          return total + value;
        }
        return total;
      }, 0);

      if (sum > 0) {
        classes.push('text-green-600');
      } else if (sum < 0) {
        classes.push('text-red-600');
      } else {
        classes.push('text-gray-700');
      }
    } else {
      classes.push('text-gray-700');
    }

    return classes.join(' ');
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
}
