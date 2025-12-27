import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from "@angular/common";
import {MarketDepthInvokerComponent} from "../../shared/market-depth-invoker/market-depth-invoker.component";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {FormsModule} from "@angular/forms";

type SortableColumn =
  | 'instrument'
  | 'quantity'
  | 'averagePrice'
  | 'change'
  | 'value'
  | 'dayPnl'
  | 'netPnl';

@Component({
  selector: 'app-positions-table',
  templateUrl: './positions-tables.component.html',
  standalone: true,
  imports: [
    CommonModule,
    MarketDepthInvokerComponent,
    ActionMenuComponent,
    UserInfoHeaderComponent,
    SmallChipComponent,
    FormsModule,
  ]
})
export class PositionsTableComponent {

  @Input() groupedPositions: any[] = [];
  @Input() actions: any[] = [];
  @Input() fallbackAvatarUrl: string = '';

  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();
  @Output() selectedPositionsChange = new EventEmitter<any[]>();

  sortColumn: SortableColumn | null = 'netPnl';
  sortDirection: 'asc' | 'desc' | null = 'desc';
  selectedPositions: Set<any> = new Set();

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    instrument: (row) => (row.displayName ?? '').toString().toLowerCase(),
    quantity: (row) => row.position?.netQuantity ?? 0,
    averagePrice: (row) => row.position?.averagePrice ?? 0,
    change: (row) => row.position?.change ?? 0,
    value: (row) => row.position?.value ?? 0,
    dayPnl: (row) => row.dayPnl ?? 0,
    netPnl: (row) => row.position?.pnl ?? 0,
  };

  get sortedGroupedPositions(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedPositions;
    }

    return this.groupedPositions.map(group => ({
      ...group,
      positions: [...(group.positions ?? [])].sort((a, b) => this.comparePositions(a, b)),
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
    const descendingByDefault: SortableColumn[] = ['dayPnl', 'netPnl', 'change'];
    return descendingByDefault.includes(column) ? 'desc' : 'asc';
  }

  getSortIndicator(column: SortableColumn): string {
    if (this.sortColumn !== column || !this.sortDirection) {
      return '';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private comparePositions(a: any, b: any): number {
    const accessor = this.sortColumn ? this.sortAccessors[this.sortColumn] : null;
    if (!accessor || !this.sortDirection) {
      return 0;
    }

    const valueA = accessor(a);
    const valueB = accessor(b);

    if (valueA == null && valueB == null) { return 0; }
    if (valueA == null) { return this.sortDirection === 'asc' ? -1 : 1; }
    if (valueB == null) { return this.sortDirection === 'asc' ? 1 : -1; }

    let comparison = 0;
    if (typeof valueA === 'string' && typeof valueB === 'string') {
      comparison = valueA.localeCompare(valueB);
    } else {
      comparison = (valueA as number) - (valueB as number);
    }

    return this.sortDirection === 'asc' ? comparison : -comparison;
  }

  getActionButtons(row: any): any[] {
    let actions: any[] = this.actions;
    if (row.position.netQuantity === 0) {
      actions = actions.filter(action => action.action !== 'exit');
    }
    return actions;
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  quantityColor(value: number): string {
    return value > 0 ? 'text-blue-600' : value < 0 ? 'text-red-600' : '';
  }

  trackByUser(index: number, item: any): any {
    return item.userId;
  }

  trackBySequenceNumber(index: number, item: any): any {
    return item.sequenceNumber;
  }

  getTransactionType(quantity: number) {
    if (quantity > 0) {
      return "BUY";
    } else if (quantity < 0) {
      return "SELL";
    } else {
      return "EXITED";
    }
  }

  getTransactionTypeChipColor(quantity: number): "blue" | "red" | "green" | "gray" {
    if (quantity > 0) {
      return "blue";
    } else if (quantity < 0) {
      return "red";
    } else {
      return "gray";
    }
  }

  // Checkbox selection methods
  get allExitablePositions(): any[] {
    return this.groupedPositions.flatMap(group =>
      group.positions.filter((position: any) => position.position.netQuantity !== 0)
    );
  }

  get isAllSelected(): boolean {
    const exitablePositions = this.allExitablePositions;
    return exitablePositions.length > 0 && exitablePositions.every(position => this.selectedPositions.has(position));
  }

  get isSomeSelected(): boolean {
    const exitablePositions = this.allExitablePositions;
    return exitablePositions.some(position => this.selectedPositions.has(position)) && !this.isAllSelected;
  }

  toggleSelectAll(): void {
    if (this.isAllSelected) {
      // Unselect all
      this.allExitablePositions.forEach(position => this.selectedPositions.delete(position));
    } else {
      // Select all exitable positions
      this.allExitablePositions.forEach(position => this.selectedPositions.add(position));
    }
    this.emitSelectedPositions();
  }

  togglePositionSelection(position: any): void {
    if (this.selectedPositions.has(position)) {
      this.selectedPositions.delete(position);
    } else {
      this.selectedPositions.add(position);
    }
    this.emitSelectedPositions();
  }

  isPositionSelected(position: any): boolean {
    return this.selectedPositions.has(position);
  }

  private emitSelectedPositions(): void {
    this.selectedPositionsChange.emit(Array.from(this.selectedPositions));
  }

  clearSelection(): void {
    this.selectedPositions.clear();
    this.emitSelectedPositions();
  }
}

