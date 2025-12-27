import {AfterViewInit, Component, Inject, OnInit} from '@angular/core';
import {DOCUMENT, NgClass, NgForOf, NgIf} from "@angular/common";
import {SearchInputComponent} from "../shared/search-input/search-input.component";
import {UserFilterComponent} from "../shared/user-filter/user-filter.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {coinReportUrl, fallbackAvatarUrl} from "../../constants/constants";
import {MfService} from "../../services/mf.service";
import {MfSipTableComponent} from "../tables/mf-sip-table/mf-sip-table.component";
import {SipSummaryComponent} from "../shared/sip-summary/sip-summary.component";
import {MfOrderTableComponent} from "../tables/mf-order-table/mf-order-table.component";
import {catchError} from "rxjs/operators";
import {of} from "rxjs";
import {ApiErrorResponse} from "../../models/api-error-response.model";
import {UtilsService} from "../../services/utils.service";
import {MatDialog} from "@angular/material/dialog";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";
import {UserDataStateService} from "../../services/user-data-state-service";
import {NoteComponent} from "../shared/note/note.component";
import {expandCollapseAnimation} from "../shared/animations";
import {MfOrderSummaryComponent} from "../shared/mf-order-summary/mf-order-summary.component";
import {TimelineChartComponent} from "../shared/timeline-chart/timeline-chart.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faInfoCircle} from "@fortawesome/free-solid-svg-icons";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-mf-sips',
  imports: [NgForOf, NgIf, SearchInputComponent, UserFilterComponent, MfSipTableComponent, SipSummaryComponent, MfOrderTableComponent, ToolBarComponent, NgClass, NoteComponent, MfOrderSummaryComponent, TimelineChartComponent, FaIconComponent, MatTooltip],
  templateUrl: './mf-sips.component.html',
  styleUrl: './mf-sips.component.css',
  animations: [expandCollapseAnimation],
})
export class MfSipsComponent implements  OnInit, AfterViewInit {

  // FontAwesome icons
  faInfoCircle = faInfoCircle;

  constructor(private mfService: MfService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              private utilsService: UtilsService,
              private dialog: MatDialog,
              @Inject(DOCUMENT) private document: Document) { }

  users: any[] = [];
  sips: any[] = [];
  mfOrders: any[] = [];
  filteredSips: any[] = [];
  filteredMfOrders: any[] = [];
  groupedSips: any[] = [];
  mfGroupedOrders: any[] = [];
  historicalTimelineValues: any[] = [];
  errorMessage = '';
  parentTabs = ['SIPs', 'Orders'];
  activeParentTab = 'SIPs';
  parentTabCounts: Record<string, number> = {
    'SIPs': 0,
    'Orders': 0,
  };

  totalSipAmount = 0;
  contributionThisMonth = 0;
  upcomingSips = 0;
  activeSips = 0;
  pausedSips = 0;

  selectedUserIds: string[] = [];
  searchQuery: string = '';
  mfSearchQuery: string = '';

  totalBuyOrders = 0;
  totalBuyAmount = 0;
  totalSellOrders = 0;
  totalSellAmount = 0;

  // Filters
  filterOptions = [
    { key: 'mfSip.status', values: ['ACTIVE', 'PAUSED'] }
  ];

  // Filters
  mfOrderFilterOptions = [
    { key: 'custom.dayRange', values: ['This Week', 'This Month', 'Last Month']},
    { key: 'mfOrder.status', values: ['PROCESSING', 'COMPLETE', 'REJECTED'] },
    { key: 'mfOrder.variety', values: ['SIP', 'REGULAR']},
    { key: 'mfOrder.purchaseType', values: ['ADDITIONAL', 'FRESH']},
    { key: 'mfOrder.transactionType', values: ['BUY', 'SELL']}
  ];

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    if(userViewState && Object.keys(userViewState).length > 0) {
      this.activeParentTab = userViewState.mfSips.selectedSectionType;
      this.selectedUserIds = userViewState.mfSips.selectedUsersIds;
      this.searchQuery = userViewState.mfSips.searchQuery;
      this.mfSearchQuery = userViewState.mfSips.mfSearchQuery;
      if(userViewState.mfSips.filterSelection?.length > 0) {
        this.filterSelection = userViewState.mfSips.filterSelection;
      }

      if(userViewState.mfSips.mfOrderFilterSelection?.length > 0) {
        this.mfOrderFilterSelection = userViewState.mfSips.mfOrderFilterSelection;
      }
    }
    this.getCachedData();
    this.fetchSips();
    this.fetchMutualFundOrders();
    this.fetchHistoricalTimelineValues();
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

  setActiveParentTab(tab: string): void {
    this.activeParentTab = tab;
    this.saveUserViewState();
  }

  onMutualFundsSearch(query: string): void {
    this.mfSearchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedMutualFundOrders();
    this.saveUserViewState();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.sips) {
        this.sips = userDataState.sips;
        this.renderSips();
      }
      if(userDataState.mfOrders) {
        this.mfOrders = userDataState.mfOrders;
        this.renderMfOrders();
      }
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      sips: this.sips,
      mfOrders: this.mfOrders
    });
  }

  renderSips(): void {
    this.updateSipCount();
    this.updateFilteredAndGroupedSips();
  }

  isLoadingData = false;
  fetchSips(done?: () => void): void {
    this.isLoadingData = true;
    this.mfService.getSips().subscribe({
      next: (response) => {
        this.sips = response.data;
        this.renderSips();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching sips. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        this.isLoadingData = false;
        if (done) done();
      }
    });
  }

  updateSipCount() {
    this.parentTabCounts['SIPs'] = this.sips.length;
  }

  renderMfOrders(): void {
    this.updateMfOrderCount();
    this.updateFilteredAndGroupedMutualFundOrders();
  }

  fetchMutualFundOrders(done?: () => void): void {
    this.isLoadingData = true;
    this.mfService.getOrders().subscribe({
      next: (response) => {
        this.mfOrders = response.data;
        this.renderMfOrders();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching mutual fund orders. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        this.isLoadingData = false;
        if (done) done();
      }
    });
  }

  fetchHistoricalTimelineValues(done?: () => void): void {
    this.utilsService.fetchHistoricalTimelineValues('sips').subscribe({
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

  showChart = false;
  dateLabels: string[] = [];
  legendLabels: string[] = [];
  series: any[] = [];
  toggleChart() {
    this.showChart = !this.showChart;
  }

  populateTimelineChartData() {
    const chartData = this.utilsService.populateSipsTimelineChartData(this.historicalTimelineValues, this.selectedUserIds);
    this.dateLabels = chartData.dateLabels;
    this.legendLabels = chartData.legendLabels;
    this.series = chartData.series;
  }

  updateMfOrderCount() {
    const open = this.mfOrders.filter(o => (this.mfPendingStatus.indexOf(o.mfOrder.status) > -1)).length;
    const executed = this.mfOrders.filter(o => (this.mfPendingStatus.indexOf(o.mfOrder.status) < 0)).length;
    this.parentTabCounts['Orders'] = open+executed;
  }

  private calculateGroupSummary(sips: any[]) {
    let totalSipAmount = 0;
    let contributionThisMonth = 0;
    let upcomingSips = 0;
    let activeSips = 0;
    let pausedSips = 0;

    const today = new Date();
    const currentMonth =today.getMonth();
    const currentYear = today.getFullYear();

    for (const sip of sips) {
      const sipData = sip?.mfSip;
      if (!sipData) continue;

      if(sipData.status === 'PAUSED') {
        pausedSips += 1;
        continue;
      }
      const amount = sipData.instalmentAmount || 0;
      totalSipAmount += amount;

      const lastSipInstalment = new Date(sipData.lastInstalment);
      if (lastSipInstalment.getMonth() === currentMonth && lastSipInstalment.getFullYear() === currentYear && lastSipInstalment < today) {
        contributionThisMonth += amount;
      } else {
        upcomingSips += 1;
      }
      if (sipData.status === 'ACTIVE') {
        activeSips += 1;
      }
    }

    return {
      totalSipAmount,
      contributionThisMonth,
      upcomingSips,
      activeSips,
      pausedSips
    };
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.updateFilteredAndGroupedSips();
    this.updateFilteredAndGroupedMutualFundOrders();
    this.saveUserViewState();
    this.populateTimelineChartData();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.updateFilteredAndGroupedSips();
    this.updateFilteredAndGroupedMutualFundOrders();
    this.saveUserViewState();
    this.populateTimelineChartData();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  onSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedSips();
    this.saveUserViewState();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      mfSips: {
        selectedSectionType: this.activeParentTab,
        selectedUsersIds: this.selectedUserIds,
        searchQuery: this.searchQuery,
        mfSearchQuery: this.mfSearchQuery,
        filterSelection: this.filterSelection,
        mfOrderFilterSelection: this.mfOrderFilterSelection,
      }
    });
  }

  actions: any[] = [
    {action: 'activate', color:'blue', label: 'Activate SIP'},
    {action: 'pause', color:'orange', label: 'Pause SIP'},
    {action: 'coinReport', color:'gray', label: 'Coin Fund Report'},
    {action: 'info', color:'gray', label: 'SIP Details'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'activate' || event.action === 'pause') {
      this.updateSip(event.action, event.row);
    } else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, event.row.mfSip.fund);
    } else if(event.action === 'coinReport') {
      window.open(coinReportUrl(event.row.mfSip.tradingsymbol), '_blank', 'noopener,noreferrer');
    }
  }

  updateSip(action: string, mfSipDetails: any): void {
    const sipRequest = {
      frequency : mfSipDetails.mfSip.frequency,
      day : mfSipDetails.mfSip.day,
      instalments  : mfSipDetails.mfSip.instalments,
      amount :  mfSipDetails.mfSip.amount,
      status: action === 'activate'? 'ACTIVE':'PAUSED',
      sipId : mfSipDetails.mfSip.sipId,
    }
    this.mfService.updateSip(mfSipDetails.userId, mfSipDetails.mfSip.sipId, sipRequest).pipe(
      catchError(err => {
        return of({
          error: true,
          id: mfSipDetails.mfSip.sipId,
          message: err?.error?.message || 'Unknown error'
        } as ApiErrorResponse);
      })
    ).subscribe({
      next: (result) => {
        if ('error' in result && result.error) {
          console.error(`❌ SIP update failed:`, result);
          this.toastrService.error(`Account ID: ${mfSipDetails.userId}. SIP update for ${result.id} failed: ${result.message}`, 'Error');
        } else {
          this.toastrService.success(`Account ID: ${mfSipDetails.userId}. SIP updated successfully. SIP ID: ${mfSipDetails.mfSip.sipId}`, 'Success');
          this.sips = result.data;
          this.updateFilteredAndGroupedSips();
        }
      },
      error: (err) => {
        // This only triggers for stream-breaking (non-caught) errors
        console.error('Fatal error:', err);
        this.toastrService.error('Something went wrong in the processing pipeline.', 'Fatal Error');
      },
      complete: () => {
        console.log('✅ All updates processed');
      }
    });
  }
  calculateSummary(): void {
    const summary = this.calculateGroupSummary(this.filteredSips);
    this.totalSipAmount = summary.totalSipAmount;
    this.contributionThisMonth = summary.contributionThisMonth;
    this.upcomingSips = summary.upcomingSips;
    this.activeSips = summary.activeSips
    this.pausedSips = summary.pausedSips;
  }

  calculateMfOrderSummary(): void {
    const summary = this.calculateMfOrderGroupSummary(this.filteredMfOrders);
    this.totalBuyOrders = summary.totalBuyOrders;
    this.totalBuyAmount = summary.totalBuyAmount;
    this.totalSellOrders = summary.totalSellOrders;
    this.totalSellAmount = summary.totalSellAmount
  }

  private calculateMfOrderGroupSummary(orders: any[]) {
    let totalBuyOrders = 0;
    let totalBuyAmount = 0;
    let totalSellOrders = 0;
    let totalSellAmount = 0;

    for (const order of orders) {
      const orderData = order?.mfOrder;
      if (!orderData) continue;

      if(orderData.status === 'COMPLETE' || orderData.status === 'OPEN' || orderData.status === 'PROCESSING') {
        if (orderData.transactionType === 'BUY') {
          totalBuyOrders += 1;
          totalBuyAmount += orderData.amount;
        } else if (orderData.transactionType === 'SELL') {
          totalSellOrders += 1;
          totalSellAmount += orderData.amount;
        }
      }
    }

    return {
      totalBuyOrders,
      totalBuyAmount,
      totalSellOrders,
      totalSellAmount,
    };
  }

  updateFilteredAndGroupedSips(): void {
    // Filtering
    let filtered = this.sips;
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter((sip: any) => this.selectedUserIds.includes(sip.userId));
    }
    //search
    if (this.searchQuery) {
      filtered = filtered.filter((sip: any) =>
        sip.mfSip.fund.toLowerCase().includes(this.searchQuery) ||
        sip.userId.toLowerCase().includes(this.searchQuery)
      );
    }
    //filter
    if (this.filterSelection.length > 0) {
      filtered = this.utilsService.filter(this.filterSelection, filtered);
    }
    this.filteredSips = filtered;
    // Grouping
    const grouped: { [key: string]: any[] } = {};
    this.filteredSips.forEach((sip: any) => {
      if (!grouped[sip.userId]) {
        grouped[sip.userId] = [];
      }
      grouped[sip.userId].push(sip);
    });
    this.groupedSips = Object.entries(grouped).map(([userId, sips]) => {
      return {
        userId,
        sips,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId),
        ...this.calculateGroupSummary(sips) };
    });

    this.calculateSummary();
  }

  filterSelection: any[] = [];
  applyMfSipFilters(event: any) {
    this.filterSelection = event;
    this.updateFilteredAndGroupedSips();
    this.saveUserViewState();
  }

  mfPendingStatus = ['PROCESSING', 'OPEN'];

  updateFilteredAndGroupedMutualFundOrders(): void {
    // Filtering
    let filtered = this.mfOrders;
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter(o => this.selectedUserIds.includes(o.userId));
    }
    if (this.mfSearchQuery) {
      filtered = filtered.filter(o =>
        o.mfOrder.fund.toLowerCase().includes(this.mfSearchQuery) ||
        o.userId.toLowerCase().includes(this.mfSearchQuery)
      );
    }
    //filter
    if (this.mfOrderFilterSelection.length > 0) {
      filtered = this.utilsService.filter(this.mfOrderFilterSelection, filtered);
      filtered = this.filterCustomDayRange(filtered);
    }
    this.filteredMfOrders = filtered;
    // Grouping
    const grouped: { [key: string]: any[] } = {};
    filtered.forEach(order => {
      if (!grouped[order.userId]) {
        grouped[order.userId] = [];
      }
      grouped[order.userId].push(order);
    });
    this.mfGroupedOrders = Object.entries(grouped).map(([userId, orders]) => {
      return {
        userId,
        orders,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId),
        ...this.calculateMfOrderGroupSummary(orders)
      };
    });
    this.calculateMfOrderSummary();
  }

  private filterCustomDayRange(filtered: any[]) {
    const dayRangeFilter = this.mfOrderFilterSelection.find(filter => filter.key === 'custom.dayRange' && filter.selected.length > 0 );
    if (dayRangeFilter) {
      // custom filter
      const now = new Date();
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      // Start of current week (Monday)
      const startOfWeek = new Date(startOfToday);
      startOfWeek.setDate(startOfWeek.getDate() - startOfWeek.getDay() + 1); // Adjust if week starts on Monday

      // Start of current month
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

      // Start of last month
      const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      // End of last month
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0); // last day of previous month
      filtered = filtered.filter((order: any) => {
        const orderDate = new Date(order.mfOrder.orderTimestamp);
        return dayRangeFilter.selected.some((range: string) => {
          if (range === 'This Week') {
            return orderDate >= startOfWeek && orderDate <= now;
          }
          if (range === 'This Month') {
            return orderDate >= startOfMonth && orderDate <= now;
          }
          if (range === 'Last Month') {
            return orderDate >= startOfLastMonth && orderDate <= endOfLastMonth;
          }
          return true; // fallback
        });
      });
    }
    return filtered;
  }

  mfOrderFilterSelection: any[] = [];
  applyMfOrderFilters(event: any) {
    this.mfOrderFilterSelection = event;
    this.updateFilteredAndGroupedMutualFundOrders();
    this.saveUserViewState();
  }

  handleRefreshForSips(done: () => void) {
    this.fetchSips(done);
  }

  handleRefreshForMfOrders(done: () => void) {
    this.fetchMutualFundOrders(done);
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;

  ngAfterViewInit(): void {
  }

  mfOrderActions: any[] = [
    {action: 'coinReport', color:'gray', label: 'Coin Fund Report'},
    {action: 'info', color:'gray', label: 'MF Order Details'},
  ]

  handleMfOrderAction(event: { action: string, row: any }) {
    if(event.action === 'info') {
      this.utilsService.showInfo(event.row, event.row.mfOrder.fund);
    } else if(event.action === 'coinReport') {
      window.open(coinReportUrl(event.row.mfOrder.tradingsymbol), '_blank', 'noopener,noreferrer');
    }
  }
}
