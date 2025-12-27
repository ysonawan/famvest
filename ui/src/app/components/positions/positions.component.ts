import {Component, Inject, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {PositionsService} from "../../services/positions.service";
import {SearchInputComponent} from "../shared/search-input/search-input.component";
import {CommonModule, DOCUMENT} from "@angular/common";
import { WebSocketService } from '../../services/web-socket.service';
import {filter, Subscription, take, of} from 'rxjs';
import {UserFilterComponent} from "../shared/user-filter/user-filter.component";
import {PositionsTableComponent} from "../tables/positions-table/positions-table.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {AddManageOrderDialogComponent} from "../dialogs/add-position-dialog/add-manage-order-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {fallbackAvatarUrl} from "../../constants/constants";
import {PositionSummaryComponent} from "../shared/position-summary/position-summary.component";
import {FundsService} from "../../services/funds.service";
import {UtilsService} from "../../services/utils.service";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";
import {Overlay} from "@angular/cdk/overlay";
import {UserDataStateService} from "../../services/user-data-state-service";
import {NoteComponent} from "../shared/note/note.component";
import {expandCollapseAnimation} from "../shared/animations";
import {AlertService} from "../../services/alert.service";
import { BarChartComponent } from '../shared/bar-chart/bar-chart.component';
import { ChargesSummaryComponent } from '../shared/charges-summary/charges-summary.component';
import { OrdersService } from '../../services/orders.service';
import {catchError} from "rxjs/operators";
import {ApiErrorResponse} from "../../models/api-error-response.model";

@Component({
  selector: 'app-positions',
  imports: [SearchInputComponent, CommonModule, UserFilterComponent, PositionsTableComponent, PositionSummaryComponent, ToolBarComponent, NoteComponent, BarChartComponent, ChargesSummaryComponent],
  templateUrl: './positions.component.html',
  standalone: true,
  styleUrl: './positions.component.css',
  animations: [expandCollapseAnimation],
})
export class PositionsComponent implements OnInit, OnDestroy {

  constructor(private positionsService: PositionsService,
              private ws: WebSocketService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              private dialog: MatDialog,
              private fundsService: FundsService,
              private ordersService: OrdersService,
              private utilsService : UtilsService,
              private overlay: Overlay,
              private alertService: AlertService,
              @Inject(DOCUMENT) private document: Document) {

  }

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    this.selectedUserIds = userViewState.positions.selectedUsersIds;
    this.searchQuery = userViewState.positions.searchQuery;
    if(userViewState.positions.filterSelection?.length > 0) {
      this.filterSelection = userViewState.positions.filterSelection;
    }
    this.getCachedData();
    this.fetchPositions();
    this.fetchFunds();
    this.fetchCharges();
    this.fetchHistoricalTimelineValues();
  }

  @ViewChild(ChargesSummaryComponent) chargesSummaryComponent?: ChargesSummaryComponent;
  @ViewChild(PositionsTableComponent) positionTableComponent?: PositionsTableComponent;

  users: any[] = [];
  instrumentTokens: number[] = [];
  funds: any[] = [];
  charges: any[] = [];
  filterCharges: any[] = [];
  private sub?: Subscription;

  positions: any[] = [];
  filteredPositions: any[] = [];
  groupedPositions: any[] = [];
  selectedPositions: any[] = [];
  errorMessage = '';

  maxProfit = 0;
  profitLeft = 0;
  totalPnL = 0;
  totalDayPnL = 0;

  selectedUserIds: string[] = [];
  searchQuery: string = '';
  fallbackAvatarUrl = fallbackAvatarUrl;
  filterSelection: any[] = [];

  historicalTimelineValues: any[] = [];

  // Filters
  filterOptions = [
    { key: 'position.product', values: ['NRML', 'CNC', 'MIS'] },
    { key: 'position.exchange', values: ['NFO', 'BFO', 'NSE', 'BSE'] },
    { key: 'custom.type', values: ['BUY', 'SELL', 'EXITED'] }
  ];

  // Bar chart data
  barChartXAxisLabels: string[] = [];
  barChartSeries: any[] = [];
  barChartLegendLabels: string[] = ['Daily EOD PnL'];
  barChartTitle: string = 'Daily EOD PnL';

  // Helper to calculate group summary
  private calculateGroupSummary(positions: any[]) {
    positions = positions.filter(p => p.position.product === 'NRML'); // consider only NRML positions for summary
    const daysPnL = positions.reduce((sum: number, positionDetails: any) => sum + (positionDetails.dayPnl || 0), 0);
    const totalPnL = positions.reduce((sum: number, positionDetails: any) => sum + (positionDetails.position.pnl || 0), 0);
    const maxProfit = positions.reduce(
      (sum: number, positionDetails: any) =>
        sum + ((positionDetails.position.sellValue || 0) - (positionDetails.position.buyValue || 0)),
      0
    );
    const profitLeft = maxProfit - totalPnL;
    return { daysPnL, totalPnL, maxProfit, profitLeft };
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

  getUtilizedFunds(userId: string) {
    return this.funds.find(fund => fund.userId === userId)?.margin?.utilised?.debits || 0;
  }

  getAvailableFunds(userId: string) {
    return this.funds.find(fund => fund.userId === userId)?.margin?.net || 0;
  }

  disconnect(): void {
    this.ws.unsubscribe(this.instrumentTokens);
  }

  ngOnDestroy(): void {
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.positions) {
        this.positions = userDataState.positions;
        this.renderPositions();
      }
      if(userDataState.funds) {
        this.funds = userDataState.funds;
      }
    }
  }
  setCachedData(): void {
    this.userDataStateService.setState({
      positions: this.positions,
      funds: this.funds
    });
  }
  renderPositions(): void {
      this.updateFilteredAndGroupedPositions();
  }

  isLoadingData = false;
  fetchPositions(done?: () => void): void {
    this.isLoadingData = true;
    this.positionsService.getPositions().subscribe({
      next: (response) => {
        this.positions = response.data;
        this.renderPositions();
        //get instrument tokens from positions
        this.instrumentTokens = this.positions.filter(position => position.instrumentToken).map(position => position.instrumentToken);
        this.subscribeToWebSocket();
        this.setCachedData();
        // Clear selection in the table component
        if (this.positionTableComponent) {
          this.positionTableComponent.clearSelection();
        }
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching positions. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        this.isLoadingData = false;
        if (done) done();
      }
    });
  }

  fetchFunds(done?: () => void): void {
    this.fundsService.getFunds().subscribe({
      next: (response) => {
        this.funds = response.data;
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching funds. Verify that the backend service is operational.', 'Error');
        }
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  fetchCharges(done?: () => void): void {
    this.ordersService.fetchCharges().subscribe({
      next: (response) => {
        this.charges = response.data;
        this.filterChargesBySelectedUsers();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching charges. Verify that the backend service is operational.', 'Error');
        }
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  fetchHistoricalTimelineValues(): void {
    this.utilsService.fetchHistoricalTimelineValues('positions').subscribe({
      next: (response) => {
        this.historicalTimelineValues = response.data || [];
        this.prepareBarChartData();
      },
      error: (error) => {
        this.toastrService.error('Failed to fetch historical positions timeline data', 'Error');
        this.historicalTimelineValues = [];
        this.prepareBarChartData();
      }
    });
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
        this.updatePositionsOnUpdate(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.updateFilteredAndGroupedPositions();
    this.filterChargesBySelectedUsers();
    this.saveUserViewState();
    this.prepareBarChartData();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.updateFilteredAndGroupedPositions();
    this.filterChargesBySelectedUsers();
    this.saveUserViewState();
    this.prepareBarChartData();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  onSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedPositions();
    this.saveUserViewState();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      positions: {
        selectedUsersIds: this.selectedUserIds,
        searchQuery: this.searchQuery,
        filterSelection: this.filterSelection,
      }
    });
  }

  filterChargesBySelectedUsers() {
    if(this.selectedUserIds.length !== 0) {
      this.filterCharges = this.charges.filter(charge => this.selectedUserIds.includes(charge.userId));
    } else {
      this.filterCharges = this.charges;
    }
    if (this.chargesSummaryComponent) {
      this.chargesSummaryComponent.showChargesDetails(this.filterCharges );
    }
  }

  updateFilteredAndGroupedPositions(): void {
    // Filtering
    let filtered = this.positions;
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter(p => this.selectedUserIds.includes(p.userId));
    }
    if (this.searchQuery) {
      filtered = filtered.filter(p =>
        p.displayName.toLowerCase().includes(this.searchQuery) ||
        p.userId.toLowerCase().includes(this.searchQuery)
      );
    }
    //filter
    if (this.filterSelection.length > 0) {
      filtered = this.utilsService.filter(this.filterSelection, filtered);
      filtered = this.filterCustomType(filtered);
    }
    this.filteredPositions = filtered;

    // Grouping
    const grouped: { [key: string]: any[] } = {};
    this.filteredPositions.forEach(position => {
      if (!grouped[position.userId]) {
        grouped[position.userId] = [];
      }
      grouped[position.userId].push(position);
      position.position.change = 0;
      position.position.value = position.position.value < 0 ? (-1 * position.position.value) : position.position.value
    });
    this.groupedPositions = Object.entries(grouped).map(([userId, positions]) => {

      return { userId, positions, ...this.calculateGroupSummary(positions) };
    });
    this.calculateSummary();
  }

  private filterCustomType(filtered: any[]) {
    const sideFilter = this.filterSelection.find(filter => filter.key === 'custom.type' && filter.selected.length > 0 );
    if (sideFilter) {
      filtered = filtered.filter(p => {
        const netQty = p.position.netQuantity;
        const selected = sideFilter.selected;

        // Keep if ANY selected condition matches
        return selected.some((side: string) => {
          if (side === 'BUY') return netQty > 0;
          if (side === 'SELL') return netQty < 0;
          if (side === 'EXITED') return netQty === 0;
          return false;
        });
      });
    }
    return filtered;
  }

  calculateSummary(): void {
    let pnl = 0;
    let dayPnl = 0;
    let maxProfit = 0;
    let profitLeft = 0;
    let positions = this.filteredPositions.filter(p => p.position.product === 'NRML'); // consider only NRML positions for summary
    positions.forEach(positionDetails => {
      maxProfit += positionDetails.position.sellValue - positionDetails.position.buyValue
      pnl += positionDetails.position.pnl || 0;
      dayPnl += positionDetails.dayPnl || 0;
      profitLeft = maxProfit - pnl;
    });
    this.totalPnL = pnl;
    this.totalDayPnL = dayPnl;
    this.maxProfit = maxProfit;
    this.profitLeft = profitLeft;
  }

  prepareBarChartData(): void {
    const chartData = this.utilsService.populatePositionsTimelineChartData(
      this.historicalTimelineValues,
      this.selectedUserIds
    );
    this.barChartXAxisLabels = chartData.dateLabels;
    this.barChartSeries = chartData.series;
    this.barChartLegendLabels = chartData.legendLabels;
  }

  updatePositionsOnUpdate(ticks: any[]): void {
    // Update the LTP of the position based on the tick received
    ticks.forEach(tick => {
      const matchedPositions: any[] = this.positions.filter(p => p.instrumentToken === tick.instrumentToken);
      matchedPositions.forEach(positionDetails => {
        positionDetails.position.change = tick.change;
        let change = tick.change;
        let closingPrice = tick.closePrice;
        if((positionDetails.position.daySellQuantity * -1) === positionDetails.position.netQuantity) {
          closingPrice = positionDetails.position.sellPrice;
          change = ((tick.lastTradedPrice- closingPrice)/closingPrice) * 100;
        } else if (positionDetails.position.dayBuyQuantity === positionDetails.position.netQuantity) {
          closingPrice = positionDetails.position.buyPrice;
          change = ((tick.lastTradedPrice - closingPrice)/closingPrice) * 100;
        }
        positionDetails.dayPnl = ((change * (positionDetails.position.netQuantity*closingPrice)) / 100);
        if (positionDetails && positionDetails.position.lastPrice !== tick.lastTradedPrice) {
          positionDetails.position.lastPrice = tick.lastTradedPrice;
          if(positionDetails.position.netQuantity < 0) {
            const realisedPnl= (positionDetails.position.sellPrice - positionDetails.position.buyPrice) * positionDetails.position.buyQuantity;
            const unrealisedPnl = (positionDetails.position.sellPrice - tick.lastTradedPrice) * positionDetails.position.netQuantity * -1;
            positionDetails.position.pnl = realisedPnl + unrealisedPnl;
          } else if (positionDetails.position.netQuantity > 0) {
            const realisedPnl= (positionDetails.position.sellPrice - positionDetails.position.buyPrice) * positionDetails.position.sellQuantity;
            const unrealisedPnl = (tick.lastTradedPrice - positionDetails.position.buyPrice) * positionDetails.position.netQuantity;
            positionDetails.position.pnl = realisedPnl + unrealisedPnl;
          }
        }
      });
    });
    this.updateGroupSummaries();
    this.calculateSummary();
  }

  // Helper to update group summaries in-place
  updateGroupSummaries(): void {
    this.groupedPositions.forEach(group => {
      const summary = this.calculateGroupSummary(group.positions);
      Object.assign(group, summary);
    });
  }

  // Getter to add user info to grouped positions for the table component
  get groupedPositionsWithUserInfo(): any[] {
    return this.groupedPositions.map(group => ({
      ...group,
      userName: this.getUserName(group.userId),
      fullName: this.getUserFullName(group.userId),
      utilizedFunds: this.getUtilizedFunds(group.userId),
      availableFunds: this.getAvailableFunds(group.userId),
      avatarUrl: this.getAvatarUrl(group.userId),
    }));
  }

  actions: any[] = [
    {action: 'copy', color:'blue', label: 'Copy Position'},
    {action: 'exit', color:'red', label: 'Exit Position'},
    {action: 'info', color:'gray', label: 'Position Details'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'exit') {
      this.openAddOrExitOrderDialog(event.row, event.action);
    } else if(event.action === 'copy') {
      this.openAddOrExitOrderDialog(event.row, event.action);
    } else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, `Position Details - ${event.row.displayName}`);
    }
  }

  openAddOrExitOrderDialog(positionDetails: any, action: string): void {
    const instrument = {
      exchange: positionDetails.position.exchange,
      tradingSymbol: positionDetails.position.tradingSymbol,
      instrumentToken: positionDetails.instrumentToken,
      lastPrice: positionDetails.position.lastPrice,
      product: positionDetails.position.product,
      quantity: positionDetails.position.netQuantity < 0 ? -1 * positionDetails.position.netQuantity : positionDetails.position.netQuantity,
      displayName : positionDetails.displayName,
      segment: positionDetails.position.segment || positionDetails.position.exchange, // Use segment or exchange as needed
      tradingAccountId: positionDetails.userId
    }
    let sourceData = {
      isBuyMode: false,
      isCopy: false,
      isExitPosition:  false,
      product: positionDetails.position.product,
    }

    if(action === 'copy') {
      sourceData.isBuyMode = positionDetails.position.netQuantity > 0;
      sourceData.isCopy = true;
    } else if(action === 'exit') {
      sourceData.isBuyMode = positionDetails.position.netQuantity < 0;
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

      }
    });
  }

  applyFilters(event: any) {
    this.filterSelection = event;
    this.updateFilteredAndGroupedPositions();
    this.saveUserViewState();
  }

  handleRefresh(done: () => void) {
    this.fetchPositions(done);
    this.fetchFunds(done);
    this.fetchCharges(done);
    this.fetchHistoricalTimelineValues();
  }


  showChart = false;
  showCharges = false;
  legendLabels: string[] = [];
  series: any[] = [];

  toggleChart() {
    this.showChart = !this.showChart;
  }

  toggleCharges() {
    this.showCharges = !this.showCharges;
  }

  viewCharges() {
    this.toggleCharges();
  }

  positionsForExit: any[] = [];
  storeSelectedPositions(positions: any[]) {
    this.positionsForExit = positions;
  }

  onSelectedPositionsChange(selectedPositions: any[]): void {
    this.selectedPositions = selectedPositions;
  }

  exitSelectedPositions(): void {
    if (this.selectedPositions.length === 0) {
      this.toastrService.warning('No positions selected', 'Warning');
      return;
    }

    const count = this.selectedPositions.length;
    this.alertService.confirm(
      `Exit ${count} Position${count > 1 ? 's' : ''}?`,
      `Are you sure you want to exit ${count} selected position${count > 1 ? 's' : ''} at market price? This action cannot be undone.`,
      () => {
        this.processBulkExit();
      }
    );
  }

  private processBulkExit(): void {
    const totalPositions = this.selectedPositions.length;
    let processed = 0;
    let succeeded = 0;
    let failed = 0;
    const responses: any[] = [];

    this.selectedPositions.forEach((positionDetails) => {
      this.exitPosition(positionDetails).pipe(
        catchError(err => {
          return of({
            error: true,
            id: positionDetails.userId,
            displayName: positionDetails.displayName,
            message: err?.error?.message || 'Unknown error'
          } as any);
        })
      ).subscribe({
        next: (result) => {
          processed++;
          responses.push({
            positionDetails,
            result,
            error: 'error' in result && result.error
          });

          if ('error' in result && result.error) {
            failed++;
            console.error(`❌ Position exit failed for ${positionDetails.displayName}:`, result);
            // Show individual error immediately
            this.toastrService.error(
              `Position exit failed for ${positionDetails.displayName}: ${result.message}`,
              'Position Exit Error'
            );
          } else {
            succeeded++;
            console.log(`✅ Position exit successful for ${positionDetails.displayName}. Order ID: ${result.data.orderId}`);
            // Show individual success immediately
            this.toastrService.success(
              `Position ${positionDetails.displayName} exited successfully. Order ID: ${result.data.orderId}`,
              'Position Exit Success'
            );
          }

          // Show summary after all processed
          if (processed === totalPositions) {
            this.showBulkExitSummary(responses, succeeded, failed);
          }
        },
        error: (err) => {
          processed++;
          failed++;
          console.error(`Fatal error for position ${positionDetails.displayName}:`, err);
          responses.push({
            positionDetails,
            result: {
              error: true,
              id: positionDetails.userId,
              displayName: positionDetails.displayName,
              message: 'Stream error - Something went wrong in the processing pipeline'
            },
            error: true
          });

          // Show individual fatal error
          this.toastrService.error(
            `Fatal error while exiting ${positionDetails.displayName}`,
            'Fatal Error'
          );

          if (processed === totalPositions) {
            this.showBulkExitSummary(responses, succeeded, failed);
          }
        }
      });
    });
  }

  private showBulkExitSummary(responses: any[], succeeded: number, failed: number): void {
    // Refresh positions
    this.fetchPositions();
    this.selectedPositions = [];

    // Clear selection in the table component
    if (this.positionTableComponent) {
      this.positionTableComponent.clearSelection();
    }

    // Show detailed summary
    const totalProcessed = succeeded + failed;
    let summaryMessage = `Bulk Exit Summary:\n`;
    summaryMessage += `✅ Succeeded: ${succeeded}\n`;
    summaryMessage += `❌ Failed: ${failed}\n`;
    summaryMessage += `Total: ${totalProcessed}`;

    if (succeeded > 0 && failed > 0) {
      // Mixed results
      const failedDetails = responses
        .filter(r => r.error)
        .map(r => `• ${r.positionDetails.displayName}: ${r.result.message}`)
        .join('\n');

      this.alertService.info(
        'Bulk Exit Completed',
        `${summaryMessage}\n\nFailed Positions:\n${failedDetails}`,
        () => { console.log('Bulk exit summary acknowledged'); }
      );
    } else if (failed > 0) {
      // All failed
      const failedDetails = responses
        .map(r => `• ${r.positionDetails.displayName}: ${r.result.message}`)
        .join('\n');

      this.alertService.info(
        'Bulk Exit Failed',
        `${summaryMessage}\n\nFailed Positions:\n${failedDetails}`,
        () => { console.log('Bulk exit summary acknowledged'); }
      );
    } else {
      // All succeeded
      const successDetails = responses
        .map(r => `• ${r.positionDetails.displayName} - Order ID: ${r.result.data.orderId}`)
        .join('\n');

      this.alertService.info(
        'Bulk Exit Successful',
        `${summaryMessage}\n\nExited Positions:\n${successDetails}`,
        () => { console.log('Bulk exit summary acknowledged'); }
      );
    }
  }

  private exitPosition(positionDetails: any) {
    // Create exit order with opposite transaction type and MARKET order type
    const transactionType = positionDetails.position.netQuantity > 0 ? 'SELL' : 'BUY';
    const quantity = Math.abs(positionDetails.position.netQuantity);

    const orderParams = {
      quantity: quantity,
      orderType: 'MARKET',
      tradingsymbol: positionDetails.position.tradingSymbol,
      product: positionDetails.position.product,
      exchange: positionDetails.position.exchange,
      transactionType: transactionType,
      validity: 'DAY',
      price: 0,
      triggerPrice: 0,
      tag: 'famvest-bulk-exit'
    };

    const orderRequest = {
      tradingAccountId: positionDetails.userId,
      orderParams: orderParams
    };

    return this.ordersService.placeOrder(orderRequest, 'regular');
  }
}
