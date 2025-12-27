import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from "@angular/common";
import {MarketDepthInvokerComponent} from "../../shared/market-depth-invoker/market-depth-invoker.component";
import {MatTooltip} from "@angular/material/tooltip";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faTriangleExclamation} from "@fortawesome/free-solid-svg-icons";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";

@Component({
  selector: 'app-holdings-table',
  templateUrl: './holdings-table.component.html',
  standalone: true,
  imports: [
    CommonModule,
    MarketDepthInvokerComponent,
    MatTooltip,
    FaIconComponent,
    ActionMenuComponent,
    UserInfoHeaderComponent,
  ]
})
export class HoldingsTableComponent {

  @Input() groupedHoldings: any[] = [];
  @Input() actions: any[] = [];

  @Input() fallbackAvatarUrl: string = '';

  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();

  getActionButtons(row: any): any [] {
    let actions: any[] = this.actions;
    if(row.type === 'Mutual Funds') {
      actions =ls
      actions.filter(action => action.action !== 'screenerIn' && action.action !== 'yahooFinance' &&
        action.action !== 'chart' && action.action !== 'exit');
    }
    if(row.type === 'Stocks') {
      actions = actions.filter(action => action.action !== 'coinReport');
    }
    return actions;
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }

  expandedInstrument: any = null;

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  toggleInstrument(holding: any) {
    //this.expandedInstrument = this.expandedInstrument === holding ? null : holding;
  }

  trackByUser(index: number, item: any): any {
    return item.userId;
  }

  trackBySequenceNumber(index: number, item: any): any {
    return item.sequenceNumber;
  }

  isScopeToPledge(holding: any): boolean {
    let availableForPledge = holding.quantity - holding.collateralQuantity;
    return availableForPledge * holding.lastPrice > 20000;
  }

  protected readonly faTriangleExclamation = faTriangleExclamation;

  sortColumn: SortableColumn | null = 'dayChangePercentage';
  sortDirection: 'asc' | 'desc' | null = 'desc';

  private readonly sortAccessors: Record<SortableColumn, (holding: any) => string | number | null> = {
    instrument: (holding) => (holding.instrument ?? '').toString().toLowerCase(),
    quantity: (holding) => holding.quantity ?? 0,
    averagePrice: (holding) => holding.averagePrice ?? 0,
    dayChangePercentage: (holding) => holding.dayChangePercentage ?? 0,
    investedAmount: (holding) => holding.investedAmount ?? 0,
    currentValue: (holding) => holding.currentValue ?? 0,
    dayPnl: (holding) => holding.dayPnl ?? 0,
    netChangePercentage: (holding) => holding.netChangePercentage ?? 0,
  };

  get sortedGroupedHoldings(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedHoldings;
    }

    return this.groupedHoldings.map(group => ({
      ...group,
      // Sorting holdings without mutating the original array
      holdings: [...(group.holdings ?? [])].sort((a, b) => this.compareHoldings(a, b)),
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
    const descendingByDefault: SortableColumn[] = ['dayChangePercentage', 'netChangePercentage'];
    return descendingByDefault.includes(column) ? 'desc' : 'asc';
  }

  getSortIndicator(column: SortableColumn): string {
    if (this.sortColumn !== column || !this.sortDirection) {
      return '';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private compareHoldings(a: any, b: any): number {
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
}

type SortableColumn =
  | 'instrument'
  | 'quantity'
  | 'averagePrice'
  | 'dayChangePercentage'
  | 'investedAmount'
  | 'currentValue'
  | 'dayPnl'
  | 'netChangePercentage';

