import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CurrencyPipe, NgForOf, NgIf} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";

type SortableColumn =
  | 'instrument'
  | 'side'
  | 'target'
  | 'stopLoss'
  | 'expiryScope'
  | 'lots'
  | 'status'
  | 'tradeType'
  | 'entryTime'
  | 'exitTime'
  | 'underlyingStrikeSelector';

@Component({
  selector: 'app-algo-straddle-strategy-table',
  templateUrl: './algo-straddle-strategy-table.component.html',
  imports: [
    NgForOf,
    SmallChipComponent,
    CurrencyPipe,
    ActionMenuComponent,
    IstDatePipe,
    NgIf,
    UserInfoHeaderComponent
  ],
  standalone : true,
  styleUrls: ['./algo-straddle-strategy-table.component.css']
})
export class AlgoStraddleStrategyTableComponent {

  @Input() groupedStraddles: any[] = [];
  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();
  @Input() actions: any[] = [];
  @Input() fallbackAvatarUrl: string = '';

  sortColumn: SortableColumn | null = 'status';
  sortDirection: 'asc' | 'desc' | null = 'asc';

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    instrument: (row) => row.instrument?.toString().toLowerCase() ?? '',
    side: (row) => row.side?.toString().toLowerCase() ?? '',
    target: (row) => row.target ?? 0,
    stopLoss: (row) => row.stopLoss ?? 0,
    expiryScope: (row) => row.expiryScope?.toString().toLowerCase() ?? '',
    lots: (row) => row.lots ?? 0,
    status: (row) => row.isActive ? 'active' : 'paused',
    tradeType: (row) => row.paperTrade ? 'paper' : 'live',
    entryTime: (row) => row.entryTime ?? 0,
    exitTime: (row) => row.exitTime ?? 0,
    underlyingStrikeSelector: (row) => row.underlyingStrikeSelector?.toString().toLowerCase() ?? '',
  };

  get sortedGroupedStraddles(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedStraddles;
    }

    // Sort straddles within each group
    return this.groupedStraddles.map(group => ({
      ...group,
      straddles: [...(group.straddles ?? [])].sort((a, b) => this.compareRows(a, b))
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
    const descendingByDefault: SortableColumn[] = ['status', 'entryTime'];
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

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  getActionButtons(row: any): any [] {
    if(row.isActive) {
      return this.actions.filter(action => action.action !== 'activate');
    } else {
      return this.actions.filter(action => action.action !== 'pause');
    }
  }

  getStatusChipColor(row: any): "green" | "orange"  {
    if(row.isActive) {
      return 'green';
    } else {
      return 'orange';
    }
  }

  getTradeTypeChipColor(row: any): "green" | "orange"  {
    if(row.paperTrade) {
      return 'orange';
    } else {
      return 'green';
    }
  }

  getStraddleSideChipColor(status: string): "blue" | "red" | "gray" {
    switch (status) {
      case "LONG":
        return "blue";
      case "SHORT":
        return "red";
      default:
        return "gray";
    }
  }

  trackById(index: number, item: any): any {
    return item.id;
  }

  trackByUser(index: number, group: any): any {
    return group.userId;
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }


  getTradeType(row: any): string {
    return row.paperTrade ? 'PAPER' : 'LIVE';
  }

}
