import {Component, EventEmitter, Input, Output} from '@angular/core';
import {DecimalPipe, NgClass, NgForOf, NgIf, PercentPipe} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {faClock, faTag} from "@fortawesome/free-solid-svg-icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {MarketDepthInvokerComponent} from "../../shared/market-depth-invoker/market-depth-invoker.component";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {MatTooltip} from "@angular/material/tooltip";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";
import {FormsModule} from "@angular/forms";

type SortableColumn =
  | 'instrument'
  | 'quantity'
  | 'product'
  | 'price'
  | 'change'
  | 'status'
  | 'timestamp';

@Component({
  selector: 'app-orders-table',
  templateUrl: './orders-table.component.html',
  imports: [
    NgClass,
    DecimalPipe,
    NgForOf,
    PercentPipe,
    SmallChipComponent,
    NgIf,
    IstDatePipe,
    FaIconComponent,
    MarketDepthInvokerComponent,
    ActionMenuComponent,
    MatTooltip,
    UserInfoHeaderComponent,
    FormsModule
  ],
  standalone: true,
})
export class OrdersTableComponent {

  @Input() groupedOrders: any[] = [];
  @Input() actions: any[] = [];
  @Input() fallbackAvatarUrl: string = '';
  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();
  @Output() selectedOrdersChange = new EventEmitter<any[]>();

  sortColumn: SortableColumn | null = 'timestamp';
  sortDirection: 'asc' | 'desc' | null = 'desc';
  selectedOrders: Set<any> = new Set();

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    instrument: (row) => (row.displayName ?? '').toString().toLowerCase(),
    quantity: (row) => row.order?.quantity ?? 0,
    price: (row) => {
      if (row.order?.orderType === 'MARKET') {
        return row.order?.averagePrice ?? 0;
      } else if (row.order?.orderType === 'SL') {
        return row.order?.triggerPrice ?? 0;
      }
      return row.order?.price ?? 0;
    },
    change: (row) => row.change ?? 0,
    product: (row) => row.order?.product ?? 0,
    status: (row) => (row.order?.status ?? '').toString().toLowerCase(),
    timestamp: (row) => row.order?.orderTimestamp ?? 0,
  };

  get sortedGroupedOrders(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedOrders;
    }

    // Sort orders within each group
    return this.groupedOrders.map(group => ({
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
    const descendingByDefault: SortableColumn[] = ['timestamp', 'change'];
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

  getActionButtons(row: any): any [] {
    let actions: any[] = this.actions;
    if(!this.isOrderPending(row.order.status)) {
      actions = actions.filter(action => action.action !== 'modify' && action.action !== 'cancel');
    }
    return actions;
  }

  pendingStatus = ['TRIGGER PENDING', 'AMO REQ RECEIVED', 'MODIFY AMO REQ RECEIVED', 'OPEN', 'OPEN PENDING'];

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  getStatusChipColor(status: string): "blue" | "red" | "green" | "gray" {
    if(this.pendingStatus.indexOf(status) > -1) {
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

  isOrderPending(status: string): boolean {
    return this.pendingStatus.indexOf(status) > -1;
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }

  // Checkbox selection methods
  get allPendingOrders(): any[] {
    return this.groupedOrders.flatMap(group =>
      group.orders.filter((order: any) => this.isOrderPending(order.order.status))
    );
  }

  get isAllSelected(): boolean {
    const pendingOrders = this.allPendingOrders;
    return pendingOrders.length > 0 && pendingOrders.every(order => this.selectedOrders.has(order));
  }

  get isSomeSelected(): boolean {
    const pendingOrders = this.allPendingOrders;
    return pendingOrders.some(order => this.selectedOrders.has(order)) && !this.isAllSelected;
  }

  toggleSelectAll(): void {
    if (this.isAllSelected) {
      // Unselect all
      this.allPendingOrders.forEach(order => this.selectedOrders.delete(order));
    } else {
      // Select all pending orders
      this.allPendingOrders.forEach(order => this.selectedOrders.add(order));
    }
    this.emitSelectedOrders();
  }

  toggleOrderSelection(order: any): void {
    if (this.selectedOrders.has(order)) {
      this.selectedOrders.delete(order);
    } else {
      this.selectedOrders.add(order);
    }
    this.emitSelectedOrders();
  }

  isOrderSelected(order: any): boolean {
    return this.selectedOrders.has(order);
  }

  private emitSelectedOrders(): void {
    this.selectedOrdersChange.emit(Array.from(this.selectedOrders));
  }

  clearSelection(): void {
    this.selectedOrders.clear();
    this.emitSelectedOrders();
  }

  protected readonly faClock = faClock;
  protected readonly faTag = faTag;
}

