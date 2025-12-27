import {Component, Inject, OnInit} from '@angular/core';
import {NgClass, NgForOf, NgIf} from "@angular/common";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {DragDropModule} from '@angular/cdk/drag-drop';
import {AlgoService} from "../../../services/algo.service";
import {ToastrService} from "ngx-toastr";
import {UtilsService} from "../../../services/utils.service";
import {FormsModule} from "@angular/forms";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";

@Component({
  selector: 'app-straddle-executions-dialog',
  imports: [
    NgForOf,
    NgIf,
    NgClass,
    DragDropModule,
    FormsModule,
    IstDatePipe,
    SmallChipComponent
  ],
  templateUrl: './straddle-executions-dialog.component.html',
  standalone: true,
  styleUrl: './straddle-executions-dialog.component.css'
})
export class StraddleExecutionsDialogComponent implements OnInit {

  straddleId: number;
  strategyName: string;
  executions: any[] = [];
  filteredExecutions: any[] = [];
  isLoading: boolean = true;

  // Filters
  profitLossFilter: string = 'all'; // 'all', 'profit', 'loss'
  typeFilter: string = 'all'; // 'all', 'live', 'paper'
  selectedMonth: string = 'all'; // 'all', 'YYYY-MM'
  availableMonths: string[] = [];

  // Summary
  summary = {
    totalExecutions: 0,
    profitableExecutions: 0,
    lossExecutions: 0,
    totalPnl: 0,
    totalProfit: 0,
    totalLoss: 0,
    avgPnl: 0,
    winRate: 0
  };

  constructor(
    private algoService: AlgoService,
    private toastrService: ToastrService,
    private utilsService: UtilsService,
    private dialogRef: MatDialogRef<StraddleExecutionsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { straddleId: number, strategyName: string }
  ) {
    this.straddleId = data.straddleId;
    this.strategyName = data.strategyName || `Straddle #${this.straddleId}`;
  }

  ngOnInit() {
    this.fetchExecutions();
  }

  fetchExecutions() {
    this.isLoading = true;
    this.algoService.getStraddleExecutionsByStraddleId(this.straddleId).subscribe({
      next: (response) => {
        // Filter out executions without exitPnl (incomplete/active trades)
        this.executions = (response.data || []).filter(exec => exec.exitPnl !== null && exec.exitPnl !== undefined);
        this.extractAvailableMonths();
        // Set the latest month as default filter
        if (this.availableMonths.length > 0) {
          this.selectedMonth = this.availableMonths[0]; // First element is the latest month (sorted DESC)
        }
        this.calculateSummary();
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        this.toastrService.error(error?.error?.message || 'Failed to fetch executions', 'Error');
        this.isLoading = false;
      }
    });
  }

  extractAvailableMonths() {
    const monthsSet = new Set<string>();
    this.executions.forEach(exec => {
      if (exec.executionDate) {
        const date = new Date(exec.executionDate);
        const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        monthsSet.add(monthKey);
      }
    });
    this.availableMonths = Array.from(monthsSet).sort().reverse();
  }

  getTradeType(row: any): string {
    return row.paperTrade ? 'PAPER' : 'LIVE';
  }

  getTradeTypeChipColor(row: any): "green" | "orange"  {
    if(row.paperTrade) {
      return 'orange';
    } else {
      return 'green';
    }
  }

  calculateSummary() {
    // Calculate summary based on filtered executions instead of all executions
    this.summary.totalExecutions = this.filteredExecutions.length;
    this.summary.profitableExecutions = 0;
    this.summary.lossExecutions = 0;
    this.summary.totalPnl = 0;
    this.summary.totalProfit = 0;
    this.summary.totalLoss = 0;

    this.filteredExecutions.forEach(exec => {
      const pnl = exec.exitPnl || 0;
      this.summary.totalPnl += pnl;

      if (pnl > 0) {
        this.summary.profitableExecutions++;
        this.summary.totalProfit += pnl;
      } else if (pnl < 0) {
        this.summary.lossExecutions++;
        this.summary.totalLoss += pnl;
      }
    });

    this.summary.avgPnl = this.summary.totalExecutions > 0
      ? this.summary.totalPnl / this.summary.totalExecutions
      : 0;

    this.summary.winRate = this.summary.totalExecutions > 0
      ? (this.summary.profitableExecutions / this.summary.totalExecutions) * 100
      : 0;
  }

  applyFilters() {
    let filtered = [...this.executions];

    // Apply profit/loss filter
    if (this.profitLossFilter === 'profit') {
      filtered = filtered.filter(exec => (exec.exitPnl || 0) > 0);
    } else if (this.profitLossFilter === 'loss') {
      filtered = filtered.filter(exec => (exec.exitPnl || 0) < 0);
    }

    // Apply type filter
    if (this.typeFilter === 'live') {
      filtered = filtered.filter(exec => !exec.paperTrade);
    } else if (this.typeFilter === 'paper') {
      filtered = filtered.filter(exec => exec.paperTrade);
    }

    // Apply month filter
    if (this.selectedMonth !== 'all') {
      filtered = filtered.filter(exec => {
        if (exec.executionDate) {
          const date = new Date(exec.executionDate);
          const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
          return monthKey === this.selectedMonth;
        }
        return false;
      });
    }

    this.filteredExecutions = filtered;
    // Recalculate summary based on filtered data
    this.calculateSummary();
  }

  onFilterChange() {
    this.applyFilters();
  }

  formatDate(date: any): string {
    if (!date) return '-';
    return this.utilsService.formatCustomDate(new Date(date));
  }

  formatCurrency(value: number): string {
    if (value === null || value === undefined) return '-';
    return '₹' + value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  getPnlClass(pnl: number): string {
    if (pnl > 0) return 'text-green-600 font-semibold';
    if (pnl < 0) return 'text-red-600 font-semibold';
    return 'text-gray-600';
  }

  getMonthLabel(monthKey: string): string {
    if (monthKey === 'all') return 'All Months';
    const [year, month] = monthKey.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' });
  }

  onClose(): void {
    this.dialogRef.close();
  }
}
