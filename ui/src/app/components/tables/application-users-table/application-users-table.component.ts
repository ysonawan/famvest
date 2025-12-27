import {Component, Input} from '@angular/core';
import {CommonModule} from "@angular/common";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";

@Component({
  selector: 'app-application-users-table',
  imports: [
    CommonModule,
    IstDatePipe
  ],
  templateUrl: './application-users-table.component.html',
  standalone: true,
  styleUrl: './application-users-table.component.css'
})
export class ApplicationUsersTableComponent {

  @Input() rows: any[] = [];

  // Sorting state
  sortColumn: string | null = 'createdAt';
  sortDirection: 'asc' | 'desc' | null = 'asc';

  private readonly sortAccessors: Record<string, (row: any) => string | number | null> = {
    id: (row) => (row.id ?? '').toString().toLowerCase(),
    userName: (row) => (row.userName ?? '').toString().toLowerCase(),
    fullName: (row) => (row.fullName ?? '').toString().toLowerCase(),
    role: (row) => (row.role ?? '').toString().toLowerCase(),
    createdAt: (row) => row.createdAt ?? 0,
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

}
