import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CurrencyPipe, DecimalPipe, NgClass, NgForOf, NgIf, PercentPipe} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {faClock} from "@fortawesome/free-solid-svg-icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {MatTooltip} from "@angular/material/tooltip";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";

type SortableColumn =
  | 'fund'
  | 'amount'
  | 'quantity'
  | 'price'
  | 'nav'
  | 'status'
  | 'timestamp';

@Component({
  selector: 'app-mf-order-table',
  templateUrl: './mf-order-table.component.html',
  imports: [
    NgClass,
    DecimalPipe,
    NgForOf,
    PercentPipe,
    SmallChipComponent,
    NgIf,
    IstDatePipe,
    CurrencyPipe,
    ActionMenuComponent,
    MatTooltip,
    UserInfoHeaderComponent
  ],
  styleUrls: ['./mf-order-table.component.css']
})
export class MfOrderTableComponent {

  @Input() groupedRows: any[] = [];
  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();
  @Input() actions: any[] = [];
  @Input() fallbackAvatarUrl: string = '';

  mfPendingStatus = ['PROCESSING', 'OPEN'];
  sortColumn: SortableColumn | null = 'timestamp';
  sortDirection: 'asc' | 'desc' | null = 'desc';

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    fund: (row) => (row.mfOrder?.fund ?? '').toString().toLowerCase(),
    amount: (row) => row.mfOrder?.amount ?? 0,
    quantity: (row) => row.mfOrder?.quantity ?? 0,
    price: (row) => row.mfOrder?.averagePrice ?? 0,
    nav: (row) => row.lastPrice ?? 0,
    status: (row) => (row.mfOrder?.status ?? '').toString().toLowerCase(),
    timestamp: (row) => row.mfOrder?.orderTimestamp ?? 0,
  };

  get sortedGroupedRows(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedRows;
    }

    // Sort orders within each group
    return this.groupedRows.map(group => ({
      ...group,
      orders: [...(group.orders ?? [])].sort((a, b) => this.compareOrders(a, b))
    }));
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
    const descendingByDefault: SortableColumn[] = ['timestamp', 'nav', 'amount'];
    return descendingByDefault.includes(column) ? 'desc' : 'asc';
  }

  getSortIndicator(column: SortableColumn): string {
    if (this.sortColumn !== column || !this.sortDirection) {
      return '';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private compareOrders(a: any, b: any): number {
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

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  getActionButtons(row: any): any [] {
    return this.actions;
  }

  getStatusChipColor(status: string): "blue" | "red" | "green" | "gray" {
    if(this.mfPendingStatus.indexOf(status) > -1) {
      return "blue";
    } else if(status === 'COMPLETE') {
      return 'green';
    } else if(status === 'FAILED' || status === 'REJECTED') {
      return 'red';
    } else {
      return 'gray';
    }
  }

  getOrderTimeStamp(timestamp: any) {
    return Number(timestamp);
  }

  getTransactionTypeChipColor(status: string): "blue" | "red" | "green" | "gray" {
    switch (status) {
      case "BUY":
        return "blue";
      case "SELL":
        return "red";
      default:
        return "gray";
    }
  }

  trackBySequenceNumber(index: number, item: any): any {
    return item.sequenceNumber;
  }

  trackByUser(index: number, group: any): any {
    return group.userId;
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }

  getVariety(inputVariety: string): string {
    return inputVariety === 'regular' ? 'ONE TIME' : inputVariety
  }

  protected readonly faClock = faClock;

}
