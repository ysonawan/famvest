import {Component, Input, Output, EventEmitter} from '@angular/core';
import {NgForOf, NgIf, UpperCasePipe} from "@angular/common";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {MatTooltip} from "@angular/material/tooltip";
import { IposService } from '../../../services/ipos.service';
import { AlertService } from '../../../services/alert.service';
import {ToastrService} from "ngx-toastr";
import {UserInfoHeaderComponent} from "../../shared/user-info-header/user-info-header.component";

type SortableColumn =
  | 'symbol'
  | 'status'
  | 'investorType'
  | 'created_at'
  | 'updated_at'
  | 'upi_id'
  | 'category';


@Component({
  selector: 'app-ipo-applications-table',
  templateUrl: './ipo-applications-table.component.html',
  imports: [
    NgForOf,
    SmallChipComponent,
    IstDatePipe,
    MatTooltip,
    NgIf,
    UpperCasePipe,
    UserInfoHeaderComponent
  ],
  standalone: true,
  styleUrls: ['./ipo-applications-table.component.css']
})
export class IpoApplicationsTableComponent {

  @Input() groupedApplications: any[] = [];
  @Output() applicationCancelled = new EventEmitter<void>();
  @Input() fallbackAvatarUrl: string = '';

  cancellationInProgress: Set<string> = new Set();
  expandedBidDetails: Set<string> = new Set();

  sortColumn: SortableColumn | null = 'updated_at';
  sortDirection: 'asc' | 'desc' | null = 'desc';

  private readonly sortAccessors: Record<SortableColumn, (row: any) => string | number | null> = {
    symbol: (row) => row.symbol?.toString().toLowerCase() ?? '',
    status: (row) => row.status?.toString().toLowerCase() ?? '',
    investorType: (row) => row.investor_type?.toString().toLowerCase() ?? '',
    created_at: (row) => row.created_at ?? 0,
    updated_at: (row) => row.updated_at?.toString().toLowerCase() ?? '',
    category: (row) => row.updated_at?.toString().toLowerCase() ?? '',
    upi_id: (row) => row.updated_at?.toString().toLowerCase() ?? '',
  };

  get sortedGroupedApplications(): any[] {
    if (!this.sortColumn || !this.sortDirection) {
      return this.groupedApplications;
    }

    // Sort applications within each group
    return this.groupedApplications.map(group => ({
      ...group,
      applications: [...(group.applications ?? [])].sort((a, b) => this.compareRows(a, b))
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
    const descendingByDefault: SortableColumn[] = ['updated_at'];
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
    private iposService: IposService,
    private alertService: AlertService,
    private toastrService: ToastrService,
  ) {}

  getStatusChipColor(status: string): "blue" | "red" | "green" | "gray" | "orange" {
    switch(status?.toUpperCase()) {
      case "SUBMITTED":
      case "APPLIED":
        return "blue";
      case "ACCEPTED":
      case "ALLOTTED":
        return "green";
      case "REJECTED":
      case "NOT ALLOTTED":
        return "red";
      case "PENDING":
        return "orange";
      default:
        return "gray";
    }
  }

  toggleBidDetails(appId: string): void {
    if (this.expandedBidDetails.has(appId)) {
      this.expandedBidDetails.delete(appId);
    } else {
      this.expandedBidDetails.add(appId);
    }
  }

  isBidDetailsExpanded(appId: string): boolean {
    return this.expandedBidDetails.has(appId);
  }

  getIpoType(type: string): "SME" | "MAINBOARD" {
    if(type === 'ipo') {
      return "MAINBOARD";
    } else {
      return "SME";
    }
  }

  getInvestorType(type: string) {
    switch(type?.toUpperCase()) {
      case "IND":
        return "INDIVIDUAL";
      case "HNI":
        return "HNI";
      case "NRI":
        return "NRI";
      case "RETAIL":
        return "RETAIL";
      default:
        return "gray";
    }
  }

  trackByApplicationId(index: number, item: any): any {
    return item.id;
  }

  trackByUserId(index: number, group: any): any {
    return group.userId;
  }

  cancelApplication(application: any): void {
    this.alertService.confirm(
      'Cancel IPO Application',
      `Are you sure you want to cancel the IPO application for ${application.symbol}?`,
      () => {
        this.performCancellation(application);
      }
    );
  }

  private performCancellation(application: any): void {
    this.cancellationInProgress.add(application.id);
    this.iposService.cancelIpoApplication(application.user_id, application.id).subscribe({
      next: (response: any) => {
        this.cancellationInProgress.delete(application.id);
        this.toastrService.success(response?.data?.message || 'IPO application cancelled successfully', 'Success');
        this.applicationCancelled.emit();
      },
      error: (error) => {
        this.cancellationInProgress.delete(application.id);
        const errorMessage = error?.error?.message || 'Failed to cancel IPO application';
        this.toastrService.error(errorMessage, 'Error');
      }
    });
  }
}

