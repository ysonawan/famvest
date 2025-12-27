import {Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, OnChanges, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf} from "@angular/common";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {MatTooltip} from "@angular/material/tooltip";
import {
  faArrowDown,
  faArrowUp,
  faChartPie, faClipboardList, faFile,
  faFileInvoiceDollar,
  faRefresh, faRightFromBracket,
  faSliders,
  faSpinner, faXmark
} from "@fortawesome/free-solid-svg-icons";
import {expandCollapseAnimation} from "../animations";

@Component({
  selector: 'app-tool-bar',
  templateUrl: './tool-bar.component.html',
  standalone: true,
  imports: [
    NgClass,
    NgForOf,
    FaIconComponent,
    NgIf,
    MatTooltip
  ],
  animations: [expandCollapseAnimation]
})
export class ToolBarComponent implements OnInit, OnChanges {

  constructor(private eRef: ElementRef) {}

  ngOnInit(): void {
    this.initFilterOptions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // If isLoadingData changes from parent, update isRefreshing
    if (changes['isLoadingData']) {
      this.isRefreshing = changes['isLoadingData'].currentValue;
    }
  }

  private initFilterOptions() {
    this.filterSelection = this.filterOptions.map(option => {
      // Try to find existing selection by key
      const existing = this.filterSelection?.find(f => f.key === option.key);
      return {
        key: option.key,
        selected: existing ? existing.selected : []
      };
    });
  }

  showFilterSort = false;

  @Input() sortOptions: any[] = [];
  @Input() sortSelection: { key: string; direction: 'asc' | 'desc' } = { key: '', direction: 'asc' };

  @Input() filterOptions: any[] = [];
  @Input() filterSelection: any[] = [];
  @Input() showChartButton: boolean = false;
  @Input() showChargesButton: boolean = false;
  @Input() showSummaryButton: boolean = false;
  @Input() showLogsButton: boolean = false;
  @Input() showExitButton: boolean = false;
  @Input() showCancelButton: boolean = false;
  @Input() exitButtonLabel: string = 'Exit Selected';
  @Input() cancelButtonLabel: string = 'Cancel Selected';
  @Input() isLoadingData: boolean = false;

  // Output Events
  @Output() filtersChanged = new EventEmitter<any[]>();

  @Output() sortChanged = new EventEmitter<any>();

  @Output() handleRefresh = new EventEmitter<() => void>();

  @Output() toggleChart = new EventEmitter<() => void>();

  @Output() charges = new EventEmitter<() => void>();

  @Output() summary = new EventEmitter<() => void>();

  @Output() exit = new EventEmitter<() => void>();

  @Output() logs = new EventEmitter<() => void>();

  onFilterChange(filter: any, selection: string): void {
    let selectionFilter = this.filterSelection.find(f => f.key === filter.key);
    let index = selectionFilter.selected.indexOf(selection);
    if(index > -1) {
      selectionFilter.selected.splice(index, 1);
    } else {
      selectionFilter.selected.push(selection);
    }
    this.emitFilterChange();
  }

   isFilterOptionSelected(filter: any, selection: string): boolean {
    let isFilterSelected: boolean = false;
     let selectionFilter = this.filterSelection.find(f => f.key === filter.key);
     if(selectionFilter) {
       isFilterSelected = selectionFilter.selected.indexOf(selection) > -1 ;
     }
     return isFilterSelected;
   }

  // Sort selection
  toggleSort(sort: any): void {
    if (this.sortSelection.key === sort.key) {
      // Toggle direction
      this.sortSelection.direction = this.sortSelection.direction === 'asc' ? 'desc' : 'asc';
    } else {
      // Set new key and default to ascending
      this.sortSelection = { key: sort.key, direction: 'asc' };
    }
    this.sortChanged.emit(this.sortSelection);
  }

  // Clear all filters
  clearFilters(): void {
    this.filterSelection = this.filterOptions.map(option => ({
      key: option.key,
      selected: []
    }));
    this.emitFilterChange();
  }

  // Clear all filters
  clearSort(): void {
    this.sortSelection = { key: '', direction: 'asc' };
    this.emitSortChange();
  }

  private emitFilterChange(): void {
    this.filtersChanged.emit(this.filterSelection);
  }

  private emitSortChange(): void {
    this.sortChanged.emit(this.sortSelection);
  }

  togglePopover() {
    this.showFilterSort = !this.showFilterSort;
  }

  toggleChartView() {
    this.toggleChart.emit();
  }

  showCharges() {
    this.charges.emit();
  }

  exitSelected() {
    this.exit.emit();
  }

  showSummary() {
    this.summary.emit();
  }

  showLogs() {
    this.logs.emit();
  }

  isRefreshing: boolean = false;

  refresh() {
    this.isRefreshing = true;
    this.handleRefresh.emit(() => {
      this.isRefreshing = false; // set by parent
    });
  }

  getFilterSortIconColor() {
    return (this.isAnyFilterSelected() || this.sortSelection?.key) ? 'text-white' : 'text-gray-600';
  }

  getFilterSortButtonColor() {
    return (this.isAnyFilterSelected() || this.sortSelection?.key) ? 'bg-amber-500 hover:bg-amber-600 text-white' : 'bg-white text-gray-700';
  }

  private isAnyFilterSelected() {
    let isFilterSelected = false;
    for (const filter of this.filterSelection) {
      if (filter.selected.length > 0) {
        //break out of the loop
        isFilterSelected = true;
        break;
      }
    }
    return isFilterSelected;
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.showFilterSort = false;
    }
  }

  protected readonly faSliders = faSliders;
  protected readonly faRefresh = faRefresh;
  protected readonly faSpinner = faSpinner;


  protected readonly faArrowUp = faArrowUp;
  protected readonly faArrowDown = faArrowDown;
  protected readonly faChartPie = faChartPie;
  protected readonly faFileInvoiceDollar = faFileInvoiceDollar;
  protected readonly faClipboardList = faClipboardList;
  protected readonly faFile = faFile;
  protected readonly faRightFromBracket = faRightFromBracket;
  protected readonly faXmark = faXmark;
}
