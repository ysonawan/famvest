import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CurrencyPipe, DecimalPipe, NgClass, NgForOf, NgIf, PercentPipe} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {faCheckCircle, faClock} from "@fortawesome/free-solid-svg-icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {UtilsService} from "../../../services/utils.service";
import {MatTooltip} from "@angular/material/tooltip";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";

type SortableColumn =
  | 'fund'
  | 'instalmentAmount'
  | 'dayChangePercentage'
  | 'lastInstalment'
  | 'nextInstalment'
  | 'status'
  | 'created';

@Component({
  selector: 'app-mf-sip-table',
  templateUrl: './mf-sip-table.component.html',
  imports: [
    NgForOf,
    SmallChipComponent,
    NgIf,
    IstDatePipe,
    CurrencyPipe,
    DecimalPipe,
    PercentPipe,
    NgClass,
    FaIconComponent,
    ActionMenuComponent,
    MatTooltip,
    UserInfoHeaderComponent
  ],
  styleUrls: ['./mf-sip-table.component.css']
})
export class MfSipTableComponent {

  constructor(private utilsService: UtilsService) {}

  @Input() groupedSips: any[] = [];
  @Input() actions: any[] = [];
  @Input() fallbackAvatarUrl: string = '';
  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();

  sortColumn: SortableColumn | null = 'nextInstalment';
  sortDirection: 'asc' | 'desc' | null = 'asc';

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    fund: (row) => (row.mfSip?.fund ?? '').toString().toLowerCase(),
    instalmentAmount: (row) => row.mfSip?.instalmentAmount ?? 0,
    dayChangePercentage: (row) => row.dayChangePercentage ?? 0,
    lastInstalment: (row) => new Date(row.mfSip?.lastInstalment ?? 0).getTime(),
    nextInstalment: (row) => new Date(row.mfSip?.nextInstalment ?? 0).getTime(),
    status: (row) => (row.mfSip?.status ?? '').toString().toLowerCase(),
    created: (row) => row.mfSip?.created ?? 0,
  };

  get sortedGroupedSips(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedSips;
    }

    // Sort SIPs within each group
    return this.groupedSips.map(group => ({
      ...group,
      sips: [...(group.sips ?? [])].sort((a, b) => this.compareSips(a, b))
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
    const descendingByDefault: SortableColumn[] = ['nextInstalment', 'created', 'lastInstalment', 'instalmentAmount', 'lastInstalment'];
    return descendingByDefault.includes(column) ? 'desc' : 'asc';
  }

  getSortIndicator(column: SortableColumn): string {
    if (this.sortColumn !== column || !this.sortDirection) {
      return '';
    }
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  private compareSips(a: any, b: any): number {
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

  getStatusChipColor(status: string): "orange" | "red" | "green" | "gray" {
    if(status === 'ACTIVE') {
      return 'green';
    } else if(status === 'PAUSED' || status === 'CANCELLED') {
      return 'orange';
    } else {
      return 'gray';
    }
  }

  getOrderTimeStamp(timestamp: any) {
    return Number(timestamp);
  }

  trackByUser(index: number, group: any): any {
    return group.userId;
  }

  trackBySequenceNumber(index: number, item: any): any {
    return item.sequenceNumber;
  }

  isUpcomingSipInThisMonth(nextInstalment: Date | string): boolean {
    const today = new Date();
    const nextInstalmentDate = new Date(nextInstalment);
    return today.getMonth() === nextInstalmentDate.getMonth() && today.getFullYear() === nextInstalmentDate.getFullYear() && nextInstalmentDate > today ;
  }

  isSipCompletedInThisMonth(lastInstalment: Date | string): boolean {
    const today = new Date();
    const lastInstalmentDate = new Date(lastInstalment);
    return today.getMonth() === lastInstalmentDate.getMonth() && today.getFullYear() === lastInstalmentDate.getFullYear() && lastInstalmentDate < today ;
  }

  protected readonly faCheckCircle = faCheckCircle;
  protected readonly faClock = faClock;

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  getSipCreatedSince(date: number) {
    return this.utilsService.getElapsedYMDString(date)
  }

  getActionButtons(row: any): any [] {
    let actions: any[] = this.actions;
    if(row.mfSip.status === 'ACTIVE') {
      actions = actions.filter(action => action.action !== 'activate');
    } else if(row.mfSip.status === 'PAUSED') {
      actions = actions.filter(action => action.action !== 'pause');
    }
    return actions;
  }

  handleAction(event: any) {
    this.handleClickAction.emit(event);
  }

}
