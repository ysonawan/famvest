import {CommonModule} from '@angular/common';
import {Component, OnDestroy, OnInit, TemplateRef, AfterViewInit, Inject} from '@angular/core';
import { FormsModule } from "@angular/forms";
import { filter, Subscription, take } from "rxjs";
import { HoldingsService } from "../../services/holdings.service";
import { WebSocketService } from "../../services/web-socket.service";
import { HoldingsSummaryComponent } from "../shared/holdings-summary/holdings-summary.component";
import { SearchInputComponent } from "../shared/search-input/search-input.component";
import { UserFilterComponent } from "../shared/user-filter/user-filter.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {coinReportUrl, fallbackAvatarUrl, screenerInUrl, yahooFinanceUrl, tradingViewUrl} from "../../constants/constants";
import {RouterLink} from "@angular/router";
import {UtilsService} from "../../services/utils.service";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";
import {TimelineChartComponent} from "../shared/timeline-chart/timeline-chart.component";
import {UserDataStateService} from "../../services/user-data-state-service";
import {NoteComponent} from "../shared/note/note.component";
import { DOCUMENT } from '@angular/common';
import {expandCollapseAnimation} from "../shared/animations";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faMoneyBillTrendUp, faTriangleExclamation} from "@fortawesome/free-solid-svg-icons";
import {
  HoldingsTableComponent
} from "../tables/holdings-table/holdings-table.component";
import {MatDialog} from "@angular/material/dialog";
import {AddManageOrderDialogComponent} from "../dialogs/add-position-dialog/add-manage-order-dialog.component";
import {Overlay} from "@angular/cdk/overlay";

@Component({
  selector: 'app-holdings',
  imports: [CommonModule, FormsModule, SearchInputComponent, UserFilterComponent, HoldingsSummaryComponent, RouterLink, ToolBarComponent, TimelineChartComponent, NoteComponent, FaIconComponent, HoldingsTableComponent],
  templateUrl: './holdings.component.html',
  standalone: true,
  animations: [expandCollapseAnimation],
})
export class HoldingsComponent implements OnInit, OnDestroy, AfterViewInit {

  constructor(private holdingService: HoldingsService,
              private ws: WebSocketService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              private utilsService : UtilsService,
              private dialog: MatDialog,
              private overlay: Overlay,
              @Inject(DOCUMENT) private document: Document) {

  }

  ngOnInit(): void {
    this.cellTemplates = {};
    const userViewState = this.userViewStateService.getState();
    this.activeTab = userViewState.holdings.selectedHoldingType;
    this.selectedUserIds = userViewState.holdings.selectedUsersIds;
    this.searchQuery = userViewState.holdings.searchQuery;
    if(userViewState.holdings.filterSelection?.length > 0) {
      this.filterSelection = userViewState.holdings.filterSelection;
    }
    this.getCachedData();
    this.fetchHoldings();
    this.fetchHistoricalTimelineValues();
  }

  instrumentTokens: number[] = [];
  private sub?: Subscription;

  users: any[] = [];
  tabs = ['All', 'Stocks', 'Mutual Funds'];
  tabCounts: Record<string, number> = {
    'All': 0,
    'Stocks': 0,
    'Mutual Funds': 0
  };
  activeTab = 'All';
  holdings: any[] = [];
  filteredHoldings: any[] = [];
  groupedHoldings: any[] = [];
  historicalTimelineValues: any[] = [];
  errorMessage = '';
  cellTemplates!: { [key: string]: TemplateRef<any> };

  totalInvestment = 0;
  currentValue = 0;
  daysPnL = 0;
  daysPnLPercentage = 0;
  totalPnL = 0;
  totalPnLPercentage = 0;

  selectedUserIds: string[] = [];
  searchQuery: string = '';

  // Filters
  filterOptions = [
    { key: 'type', values: ['STOCKS', 'MUTUAL FUNDS'] }
  ];

  actions: any[] = [
    {action: 'exit', color:'red', label: 'Exit'},
    {action: 'chart', color:'gray', label: 'View Chart'},
    {action: 'screenerIn', color:'gray', label: 'Screener.in'},
    {action: 'yahooFinance', color:'gray', label: 'Yahoo Finance'},
    {action: 'coinReport', color:'gray', label: 'Coin Fund Report'},
    {action: 'info', color:'gray', label: 'Holding Details'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'exit') {
      this.openAddOrExitOrderDialog(event.row, 'exit');
    } else if(event.action === 'chart') {
      window.open(tradingViewUrl(event.row.exchange, event.row.instrument), '_blank', 'noopener,noreferrer');
    } else if(event.action === 'screenerIn') {
      window.open(screenerInUrl(event.row.instrument), '_blank', 'noopener,noreferrer');
    } else if(event.action === 'yahooFinance') {
      let exchange = event.row.exchange === 'NSE' ? 'NS' : event.row.exchange === 'BSE' ? 'BO' : 'NS';
      window.open(yahooFinanceUrl(event.row.instrument, exchange), '_blank', 'noopener,noreferrer');
    } else if(event.action === 'coinReport') {
      window.open(coinReportUrl(event.row.tradingSymbol), '_blank', 'noopener,noreferrer');
    } else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, `Holding Details - ${event.row.instrument}`);
    }
  }

  getAvatarUrl(userId: string) {
    return this.utilsService.getAvatarUrl(this.users, userId);
  }

  getUserName(userId: string) {
    return this.utilsService.getUserName(this.users, userId);
  }

  getUserFullName(userId: string) {
    return this.utilsService.getUserFullName(this.users, userId);
  }

  disconnect(): void {
    this.ws.unsubscribe(this.instrumentTokens);
  }

  ngOnDestroy(): void {
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
  }

  ngAfterViewInit(): void {
  }

  fetchHistoricalTimelineValues(done?: () => void): void {
    this.utilsService.fetchHistoricalTimelineValues('holdings').subscribe({
      next: (response) => {
        this.historicalTimelineValues = response.data;
        this.populateTimelineChartData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching holdings time line values. Verify that the backend service is operational.', 'Error');
        }
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.holdings) {
        this.holdings = userDataState.holdings;
        this.renderHoldings();
      }
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      holdings: this.holdings
    });
  }

  renderHoldings(): void {
    this.updateCounts();
    this.updateFilteredAndGroupedHoldings();
  }

  isLoadingData = false;
  fetchHoldings(done?: () => void): void {
    this.isLoadingData = true;
    this.holdingService.getHoldings().subscribe({
      next: (response) => {
        this.holdings = response.data;
        this.renderHoldings();
        this.instrumentTokens = this.holdings.filter((holding: any) => holding.instrumentToken).map((holding: any) => holding.instrumentToken);
        this.subscribeToWebSocket();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching holdings. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        this.isLoadingData = false;
        if (done) done();
      }
    });
  }

  updateCounts() {
    let all = 0;
    this.tabs.forEach(tab => {
      const count = this.holdings.filter(h => h.type === tab).length;
      all += count;
      this.tabCounts[tab] = count;
    });
    this.tabCounts['All'] = all;
  }

  subscribeToWebSocket(): void {
    if (this.instrumentTokens.length > 0) {
      this.ws.connectionState().pipe(
        filter(c => c), // only when connected = true
        take(1)
      ).subscribe(() => {
        this.ws.subscribe(this.instrumentTokens);
      });
      this.sub = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updateHoldingsOnUpdate(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  updateHoldingsOnUpdate(ticks: any[]): void {
    // Update the LTP of the position based on the tick received
    ticks.forEach(tick => {
      const matchedHoldings: any[] = this.holdings.filter((h: any) => h.instrumentToken === tick.instrumentToken);
      matchedHoldings.forEach(holding => {
        if (holding && holding.lastPrice !== tick.lastTradedPrice) {
          holding.lastPrice = tick.lastTradedPrice;
          holding.currentValue = holding.quantity * tick.lastTradedPrice;
          holding.netPnl = (holding.currentValue - holding.investedAmount);
          holding.netChangePercentage = ((holding.netPnl / holding.investedAmount) * 100);
          holding.dayChangePercentage = ((tick.lastTradedPrice - tick.closePrice) / tick.closePrice) * 100;
          holding.dayPnl = ((holding.dayChangePercentage * (holding.quantity * tick.closePrice)) / 100);
        }
      });
    });
    this.calculateSummary();
    this.updateGroupSummaries();
  }

  // Helper to calculate group summary
  private calculateGroupSummary(holdings: any[]) {
    const totalInvestment = holdings.reduce((sum: number, h: any) => sum + (h.investedAmount || 0), 0);
    const currentValue = holdings.reduce((sum: number, h: any) => sum + (h.currentValue || 0), 0);
    const daysPnL = holdings.reduce((sum: number, h: any) => sum + (h.dayPnl || 0), 0);
    const totalPnL = holdings.reduce((sum: number, h: any) => sum + (h.netPnl || 0), 0);
    const daysPnLPercentage = (daysPnL / totalInvestment) || 0;
    const totalPnLPercentage = (totalPnL / totalInvestment) || 0;
    return { totalInvestment, currentValue, daysPnL, totalPnL, daysPnLPercentage, totalPnLPercentage };
  }

  // Helper to update group summaries in-place
  updateGroupSummaries(): void {
    this.groupedHoldings.forEach(group => {
      const summary = this.calculateGroupSummary(group.holdings);
      Object.assign(group, summary);
    });
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.updateFilteredAndGroupedHoldings();
    this.saveUserViewState();
    this.populateTimelineChartData();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.updateFilteredAndGroupedHoldings();
    this.saveUserViewState();
    this.populateTimelineChartData();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  onSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedHoldings();
    this.saveUserViewState();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      holdings: {
        selectedUsersIds: this.selectedUserIds,
        selectedHoldingType: this.activeTab,
        searchQuery: this.searchQuery,
        filterSelection: this.filterSelection,
      }
    });
  }

  calculateSummary(): void {
    let totalInvested = 0;
    let totalCurrent = 0;
    let totalDayPnL = 0;
    let totalNetPnL = 0;

    this.filteredHoldings.forEach((holding: any) => {
      totalInvested += holding.investedAmount || 0;
      totalCurrent += holding.currentValue || 0;
      totalDayPnL += holding.dayPnl || 0;
      totalNetPnL += holding.netPnl || 0;
    });
    this.totalInvestment = totalInvested;
    this.currentValue = totalCurrent;
    this.daysPnL = totalDayPnL;
    this.daysPnLPercentage = (totalDayPnL / totalInvested) || 0;
    this.totalPnL = totalNetPnL;
    this.totalPnLPercentage = (totalNetPnL / totalInvested) || 0;

  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.updateFilteredAndGroupedHoldings();
    this.saveUserViewState();
  }

  hideChildSummary(group: any) : boolean {
    return (this.selectedUserIds.length === 1 || this.users.length === 1 ||
      this.holdings.filter((h: any) => h.userId === group.userId).length === 1);
  }

  updateFilteredAndGroupedHoldings(): void {
    // Filtering
    let filtered = this.holdings;
    if (this.activeTab !== 'All') {
      filtered = filtered.filter((h: any) => h.type === this.activeTab);
    }
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter((h: any) => this.selectedUserIds.includes(h.userId));
    }
    if (this.searchQuery) {
      filtered = filtered.filter((h: any) =>
        h.instrument.toLowerCase().includes(this.searchQuery) ||
        h.userId.toLowerCase().includes(this.searchQuery)
      );
    }
    //filter
    if (this.filterSelection.length > 0) {
      filtered = this.utilsService.filter(this.filterSelection, filtered);
    }
    this.filteredHoldings = filtered;

    // Grouping
    const grouped: { [key: string]: any[] } = {};
    this.filteredHoldings.forEach((holding: any) => {
      if (!grouped[holding.userId]) {
        grouped[holding.userId] = [];
      }
      grouped[holding.userId].push(holding);
    });
    this.groupedHoldings = Object.entries(grouped).map(([userId, holdings]) => {

      return {
        userId,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId),
        holdings,
        ...this.calculateGroupSummary(holdings)
      };
    });

    this.calculateSummary();
  }

  filterSelection: any[] = [];
  applyFilters(event: any) {
    this.filterSelection = event;
    this.updateFilteredAndGroupedHoldings();
    this.saveUserViewState();
  }


  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
  protected readonly faMoneyBillTrendUp = faMoneyBillTrendUp;
  protected readonly faTriangleExclamation = faTriangleExclamation;

  handleRefresh(done: () => void) {
    this.fetchHoldings(done);
  }

  showChart = false;
  dateLabels: string[] = [];
  legendLabels: string[] = [];
  series: any[] = [];
  toggleChart() {
    this.showChart = !this.showChart;
  }

  populateTimelineChartData() {
    const chartData = this.utilsService.populateHoldingsTimelineChartData(this.historicalTimelineValues, this.selectedUserIds);
    this.dateLabels = chartData.dateLabels;
    this.legendLabels = chartData.legendLabels;
    this.series = chartData.series;
  }

  openAddOrExitOrderDialog(holding: any, action: string): void {
    const instrument = {
      exchange: holding.exchange,
      tradingSymbol: holding.tradingSymbol,
      instrumentToken: holding.instrumentToken,
      lastPrice: holding.lastPrice,
      quantity: holding.quantity,
      displayName: holding.instrument,
      segment: holding.exchange, // Use exchange as segment for holdings
      tradingAccountId: holding.userId
    }
    let sourceData = {
      isBuyMode: false,
      isCopy: false,
      isExitPosition: false,
      product: 'CNC', // Holdings are typically CNC (Cash and Carry)
    }

    if(action === 'exit') {
      // For holdings, we always sell (opposite of BUY)
      sourceData.isBuyMode = false;
      sourceData.isExitPosition = true;
    }

    const dialogRef = this.dialog.open(AddManageOrderDialogComponent, {
      disableClose: true,
      autoFocus: true,
      hasBackdrop: false,
      closeOnNavigation: false,
      scrollStrategy: this.overlay.scrollStrategies.noop(),
      data: { sourceData: sourceData, instrument: { ...instrument } }
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result) {
        // Refresh holdings after successful order
        this.fetchHoldings();
      }
    });
  }

}
