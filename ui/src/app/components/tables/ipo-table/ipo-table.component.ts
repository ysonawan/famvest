import {Component, Input, Output, EventEmitter} from '@angular/core';
import {NgForOf, NgIf} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {MatDialog} from "@angular/material/dialog";
import {IpoApplyDialogComponent} from "../../dialogs/ipo-apply-dialog/ipo-apply-dialog.component";

type SortableColumn =
  | 'symbol'
  | 'name'
  | 'status'
  | 'type'
  | 'priceRange'
  | 'startDate'
  | 'endDate'
  | 'listingDate';

@Component({
  selector: 'app-ipo-table',
  templateUrl: './ipo-table.component.html',
  imports: [
    NgForOf,
    SmallChipComponent,
    IstDatePipe,
    NgIf
  ],
  standalone : true,
  styleUrls: ['./ipo-table.component.css']
})
export class IpoTableComponent {

  @Input() rows: any[] = [];
  @Output() applicationSubmitted = new EventEmitter<void>();
  expandedTimelines: Set<string> = new Set();

  sortColumn: SortableColumn | null = 'startDate';
  sortDirection: 'asc' | 'desc' | null = 'desc';

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    symbol: (row) => row.symbol?.toString().toLowerCase() ?? '',
    name: (row) => row.name?.toString().toLowerCase() ?? '',
    status: (row) => row.status?.toString().toLowerCase() ?? '',
    type: (row) => row.sub_type?.toString().toLowerCase() ?? '',
    priceRange: (row) => row.investor_types?.[0]?.min_price ?? 0,
    startDate: (row) => row.start_at ?? 0,
    endDate: (row) => row.end_at ?? 0,
    listingDate: (row) => row.listing_date ?? 0,
  };

  get sortedRows(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.rows;
    }

    return [...this.rows].sort((a, b) => this.compareRows(a, b));
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
    const descendingByDefault: SortableColumn[] = ['startDate', 'endDate', 'listingDate'];
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

  constructor(
    private dialog: MatDialog
  ) {}

  getIpoStatusChipColor(status: string): "green" | "orange" | "gray" {
    switch (status) {
      case "ongoing":
      case "preapply":
        return "green";
      case "upcoming":
        return "orange";
      default:
        return "gray";
    }
  }

  getIpoType(type: string): "SME" | "MAINBOARD" {
    if(type === 'IPO') {
      return "MAINBOARD";
    } else {
      return "SME";
    }
  }

  trackById(index: number, item: any): any {
    return item.id;
  }

  toggleTimeline(ipoId: string): void {
    if (this.expandedTimelines.has(ipoId)) {
      this.expandedTimelines.delete(ipoId);
    } else {
      this.expandedTimelines.add(ipoId);
    }
  }

  isTimelineExpanded(ipoId: string): boolean {
    return this.expandedTimelines.has(ipoId);
  }

  openApplyDialog(ipo: any): void {
    const dialogRef = this.dialog.open(IpoApplyDialogComponent, {
      disableClose: true,       // Prevent closing via escape or backdrop click
      autoFocus: true,          // Focus the first form element inside the dialog
      hasBackdrop: true,        // Show a dark background overlay
      closeOnNavigation: false,  // Optional: closes the dialog if navigation occurs
      data: {ipo: ipo}
    });
    dialogRef.componentInstance.applicationSubmitted.subscribe(() => {
      this.applicationSubmitted.emit();
    });
    dialogRef.afterClosed().subscribe((result: any) => {
    });
  }
}
