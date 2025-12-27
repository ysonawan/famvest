import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CommonModule} from "@angular/common";
import {ActionMenuComponent} from "../../shared/action-menu/action-menu.component";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-scheduled-tasks-table',
  imports: [
    CommonModule,
    ActionMenuComponent,
    SmallChipComponent,
    IstDatePipe,
    MatTooltip
  ],
  templateUrl: './scheduled-tasks-table.component.html',
  standalone: true,
  styleUrl: './scheduled-tasks-table.component.css'
})
export class ScheduledTasksTableComponent {

  @Input() rows: any[] = [];
  @Output() handleClickAction = new EventEmitter<{ action: string, row: any }>();

  @Input() actions: any[] = [];

  // Sorting state
  sortColumn: string | null = 'executionStartTime';
  sortDirection: 'asc' | 'desc' | null = 'desc';

  private readonly sortAccessors: Record<string, (row: any) => string | number | null> = {
    schedulerName: (row) => (row.schedulerName ?? '').toString().toLowerCase(),
    cronExpression: (row) => (row.cronExpression ?? '').toString().toLowerCase(),
    lastExecutionDate: (row) => row.lastExecutionDate ?? 0,
    executionStartTime: (row) => row.executionStartTime ?? 0,
    executionEndTime: (row) => row.executionEndTime ?? 0,
    status: (row) => (row.status ?? '').toString().toLowerCase(),
  };

  get sortedRows(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.rows;
    }

    return [...this.rows].sort((a, b) => this.compareRows(a, b));
  }

  setSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'desc' ? 'asc' : 'desc';
      return;
    }

    this.sortColumn = column;
    this.sortDirection = 'asc';
  }

  getSortIndicator(column: string): string {
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

  getActionButtons(row: any): any [] {
    if(row.isActive) {
      return this.actions.filter(action => action.action !== 'activate');
    } else {
      return this.actions.filter(action => action.action !== 'pause');
    }
  }

  handleAction(event: { action: string, row: any }) {
    this.handleClickAction.emit(event);
  }

  getStatusColor(status: string): "blue" | "red" | "green" | "gray" | "orange" {
    switch (status?.toUpperCase()) {
      case 'COMPLETED':
      case 'ACTIVE':
        return 'green';
      case 'IN_PROGRESS':
        return 'blue';
      case 'FAILED':
        return 'red';
      case 'PAUSED':
        return 'orange';
      default:
        return 'gray';
    }
  }
}
