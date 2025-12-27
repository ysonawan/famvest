import { Component, Inject, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faWallet,
  faChartLine,
  faListCheck,
  faCoins,
  faBuilding,
  faArrowUp,
  faArrowDown,
  faRefresh,
  faSpinner,
  faDollarSign,
  faChartBar,
  faArrowTrendDown,
} from '@fortawesome/free-solid-svg-icons';
import { HoldingsService } from '../../services/holdings.service';
import { PositionsService } from '../../services/positions.service';
import { OrdersService } from '../../services/orders.service';
import { FundsService } from '../../services/funds.service';
import { MfService } from '../../services/mf.service';
import { IposService } from '../../services/ipos.service';
import { ToastrService } from 'ngx-toastr';
import { UserViewStateService } from '../../services/user-view-state-service';
import { forkJoin, filter, take, Subscription, of, catchError } from 'rxjs';
import {TimelineChartComponent} from "../shared/timeline-chart/timeline-chart.component";
import { UtilsService } from '../../services/utils.service';
import {expandCollapseAnimation, fadeInUpAnimation} from "../shared/animations";
import { UserFilterComponent } from "../shared/user-filter/user-filter.component";
import { WebSocketService } from '../../services/web-socket.service';
import { PnlDetailsPopupComponent, UserDetailsData, ColumnConfig } from "../shared/pnl-details-popup/pnl-details-popup.component";

interface DashboardStats {
  totalInvestment: number;
  currentValue: number;
  totalGains: number;
  totalGainsPercent: number;
  todayGains: number;
  todayGainsPercent: number;
  openPositions: number;
  totalPnL: number;
  maxProfit: number;
  maxLoss: number;
  profitLeft: number;
  openOrders: number;
  activeSips: number;
  openIpos: number;
  availableFunds: number;
  // MF SIP specific stats
  totalSipAmount: number;
  contributionThisMonth: number;
  pausedSips: number;
  // Funds specific stats
  utilizedFunds: number;
  totalCollateralMargin: number;
  totalCash: number;
  availableMarginPercent: number;
}

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: true,
  imports: [CommonModule, FaIconComponent, RouterModule, TimelineChartComponent, UserFilterComponent, PnlDetailsPopupComponent],
  styleUrls: ['./home.component.css'],
  animations: [expandCollapseAnimation, fadeInUpAnimation],
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {

  stats: DashboardStats = {
    totalInvestment: 0,
    currentValue: 0,
    totalGains: 0,
    totalGainsPercent: 0,
    todayGains: 0,
    todayGainsPercent: 0,
    openPositions: 0,
    totalPnL: 0,
    maxProfit: 0,
    maxLoss: 0,
    profitLeft: 0,
    openOrders: 0,
    activeSips: 0,
    openIpos: 0,
    availableFunds: 0,
    totalSipAmount: 0,
    contributionThisMonth: 0,
    pausedSips: 0,
    utilizedFunds: 0,
    totalCollateralMargin: 0,
    totalCash: 0,
    availableMarginPercent: 0
  };

  isLoading = true;
  isInitialLoad = true; // Flag to track if this is the first load
  errorMessage = '';
  currentDateTime = new Date();

  // User filter properties
  users: any[] = [];
  selectedUserIds: string[] = [];

  // Raw data for filtering
  rawData: any = {};

  // WebSocket
  instrumentTokens: number[] = [];
  private sub?: Subscription;

  // P&L Details Popup
  showPnLDetailsPopup = false;
  popupUserData: UserDetailsData[] = [];
  popupColumns: ColumnConfig[] = [];
  popupTitle: string = '';
  popupSortByColumn?: string;

  // Font Awesome icons
  protected readonly faWallet = faWallet;
  protected readonly faChartLine = faChartLine;
  protected readonly faListCheck = faListCheck;
  protected readonly faCoins = faCoins;
  protected readonly faBuilding = faBuilding;
  protected readonly faArrowUp = faArrowUp;
  protected readonly faArrowDown = faArrowDown;
  protected readonly faRefresh = faRefresh;
  protected readonly faSpinner = faSpinner;
  protected readonly faDollarSign = faDollarSign;
  protected readonly faChartBar = faChartBar;
  protected readonly faArrowTrendDown = faArrowTrendDown;

  constructor(
    private holdingsService: HoldingsService,
    private positionsService: PositionsService,
    private ordersService: OrdersService,
    private fundsService: FundsService,
    private mfService: MfService,
    private iposService: IposService,
    private toastrService: ToastrService,
    private userViewStateService: UserViewStateService,
    private utilsService: UtilsService,
    private ws: WebSocketService,
    @Inject(DOCUMENT) private document: Document,
    private router: Router
  ) {}

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    this.selectedUserIds = userViewState.home?.selectedUsersIds || [];
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      const header = this.document.querySelector('.header');
      if (header) {
        this.saveUserViewState();
      }
    }, 0);
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // Use forkJoin with catchError to handle individual API failures gracefully
    forkJoin({
      holdings: this.holdingsService.getHoldings().pipe(
        catchError(error => {
          console.error('Error loading holdings:', error);
          this.toastrService.error('Failed to load holdings data', 'Holdings Error');
          return of({ data: [] }); // Return empty data on error
        })
      ),
      positions: this.positionsService.getPositions().pipe(
        catchError(error => {
          console.error('Error loading positions:', error);
          this.toastrService.error('Failed to load positions data', 'Positions Error');
          return of({ data: [] });
        })
      ),
      orders: this.ordersService.getOrders().pipe(
        catchError(error => {
          console.error('Error loading orders:', error);
          this.toastrService.error('Failed to load orders data', 'Orders Error');
          return of({ data: [] });
        })
      ),
      funds: this.fundsService.getFunds().pipe(
        catchError(error => {
          console.error('Error loading funds:', error);
          this.toastrService.error('Failed to load funds data', 'Funds Error');
          return of({ data: [] });
        })
      ),
      mfSips: this.mfService.getSips().pipe(
        catchError(error => {
          console.error('Error loading MF SIPs:', error);
          this.toastrService.error('Failed to load mutual fund SIPs data', 'MF SIPs Error');
          return of({ data: [] });
        })
      ),
      ipos: this.iposService.getIPOs().pipe(
        catchError(error => {
          console.error('Error loading IPOs:', error);
          this.toastrService.error('Failed to load IPOs data', 'IPOs Error');
          return of({ data: [] });
        })
      ),
    }).subscribe({
      next: (data) => {
        // Store raw data for filtering
        this.rawData = {
          holdings: data.holdings?.data || [],
          positions: data.positions?.data || [],
          orders: data.orders?.data || [],
          funds: data.funds?.data || [],
          mfSips: data.mfSips?.data || [],
          ipos: data.ipos?.data || []
        };
        this.applyUserFilter();

        // Collect instrument tokens and subscribe to WebSocket
        this.collectInstrumentTokensAndSubscribe();

        // Fetch historical timeline values
        this.fetchHistoricalTimelineValues();

        this.isLoading = false;
        this.isInitialLoad = false;
      },
      error: (error) => {
        // This should rarely happen now since individual errors are caught
        console.error('Error loading dashboard data:', error);
        this.errorMessage = 'Failed to load dashboard data';
        this.isLoading = false;
        if (this.isInitialLoad) {
          this.toastrService.error('Failed to load dashboard data', 'Error');
        }
      }
    });
  }

  private collectInstrumentTokensAndSubscribe(): void {
    // Collect instrument tokens from holdings
    const holdingTokens = this.rawData.holdings
      .filter((holding: any) => holding.instrumentToken)
      .map((holding: any) => holding.instrumentToken);

    // Collect instrument tokens from positions
    const positionTokens = this.rawData.positions
      .filter((position: any) => position.instrumentToken)
      .map((position: any) => position.instrumentToken);

    // Combine and get unique tokens
    const allTokens = [...holdingTokens, ...positionTokens];
    this.instrumentTokens = Array.from(new Set(allTokens));

    // Subscribe to WebSocket
    this.subscribeToWebSocket();
  }

  private subscribeToWebSocket(): void {
    if (this.instrumentTokens.length > 0) {
      this.ws.connectionState().pipe(
        filter(c => c), // only when connected = true
        take(1)
      ).subscribe(() => {
        this.ws.subscribe(this.instrumentTokens);
      });
      this.sub = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updatePricesOnTick(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  private updatePricesOnTick(ticks: any[]): void {
    // Update holdings prices
    ticks.forEach(tick => {
      const matchedHoldings: any[] = this.rawData.holdings.filter((h: any) => h.instrumentToken === tick.instrumentToken);
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

      // Update positions prices
      const matchedPositions: any[] = this.rawData.positions.filter((p: any) => p.instrumentToken === tick.instrumentToken);
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

    // Recalculate stats with updated prices (without updating timeline chart)
    this.recalculateStats();
  }

  private recalculateStats(): void {
    if (this.selectedUserIds.length === 0) {
      // Calculate stats for all accounts data
      this.calculateStats({
        holdings: { data: this.rawData.holdings },
        positions: { data: this.rawData.positions },
        orders: { data: this.rawData.orders },
        funds: { data: this.rawData.funds },
        mfSips: { data: this.rawData.mfSips },
        ipos: { data: this.rawData.ipos }
      });
    } else {
      // Calculate stats for filtered users
      const filteredData = {
        holdings: { data: this.rawData.holdings.filter((item: any) => this.selectedUserIds.includes(item.userId)) },
        positions: { data: this.rawData.positions.filter((item: any) => this.selectedUserIds.includes(item.userId)) },
        orders: { data: this.rawData.orders.filter((item: any) => this.selectedUserIds.includes(item.userId)) },
        funds: { data: this.rawData.funds.filter((item: any) => this.selectedUserIds.includes(item.userId)) },
        mfSips: { data: this.rawData.mfSips.filter((item: any) => this.selectedUserIds.includes(item.userId)) },
        ipos: { data: this.rawData.ipos.filter((item: any) => this.selectedUserIds.includes(item.userId)) }
      };
      this.calculateStats(filteredData);
    }

    // Update popup data if popup is currently open
    this.updatePopupDataIfOpen();
  }
  protected stocksFnoPendingStatus = ['TRIGGER PENDING', 'AMO REQ RECEIVED', 'MODIFY AMO REQ RECEIVED', 'OPEN', 'OPEN PENDING'];

  private calculateStats(data: any): void {
    // Calculate holdings stats
    const holdings = data.holdings?.data || [];
    const holdingStats = holdings.reduce((acc: any, holding: any) => {
      const investment = holding.quantity * holding.averagePrice;
      const currentValue = holding.quantity * holding.lastPrice;
      const gain = currentValue - investment;

      // Use the already-calculated dayPnl from WebSocket updates
      const dayPnl = holding.dayPnl || 0;

      acc.totalInvestment += investment;
      acc.currentValue += currentValue;
      acc.totalGains += gain;
      acc.todayGains += dayPnl;

      return acc;
    }, { totalInvestment: 0, currentValue: 0, totalGains: 0, todayGains: 0 });

    // Calculate positions stats - following the same logic as positions component
    const positions = data.positions?.data || [];
    const nrmlPositions = positions.filter((p: any) => p.position?.product === 'NRML');
    const positionStats = nrmlPositions.reduce((acc: any, positionData: any) => {
      const position = positionData.position;
      if (position.netQuantity !== 0) {
        acc.openPositions++;
      }

      // Total current P&L
      acc.totalPnL += position.pnl || 0;

      // Max Profit calculation: sum of (sellValue - buyValue)
      const maxProfitForPosition = (position.sellValue || 0) - (position.buyValue || 0);
      acc.maxProfit += maxProfitForPosition;

      // Track individual max and min for display purposes
      if (position.pnl > acc.maxSingleProfit) acc.maxSingleProfit = position.pnl;
      if (position.pnl < acc.maxLoss) acc.maxLoss = position.pnl;

      return acc;
    }, { openPositions: 0, totalPnL: 0, maxProfit: 0, maxSingleProfit: 0, maxLoss: 0 });

    // Profit Left = Max Profit - Current Total P&L
    const profitLeft = positionStats.maxProfit - positionStats.totalPnL;

    // Calculate orders stats
    const orders = data.orders?.data || [];
    const openOrders = orders.filter((order: any) =>
      this.stocksFnoPendingStatus.indexOf(order.order.status) > -1
    ).length;

    // Calculate funds - sum of all margin.net values
    const funds = data.funds?.data || [];
    const availableFunds = funds.reduce((acc: number, userFund: any) => {
      return acc + (parseFloat(userFund.margin?.net) || 0);
    }, 0);

    // Calculate MF SIPs using the same logic as MF SIPs component
    const mfSips = data.mfSips?.data || [];
    const sipStats = this.calculateSipSummary(mfSips);

    // Calculate IPOs
    const ipos = data.ipos?.data || [];
    const openIpos = ipos.filter((ipo: any) => ipo.status === 'ongoing' || ipo.status === 'preapply').length;

    // Calculate funds-specific stats based on funds component logic
    const fundsStats = this.calculateFundsStats(funds, availableFunds);

    this.stats = {
      totalInvestment: holdingStats.totalInvestment,
      currentValue: holdingStats.currentValue,
      totalGains: holdingStats.totalGains,
      totalGainsPercent: holdingStats.totalInvestment > 0 ?
        (holdingStats.totalGains / holdingStats.totalInvestment) : 0,
      todayGains: holdingStats.todayGains,
      todayGainsPercent: holdingStats.totalInvestment > 0 ?
        (holdingStats.todayGains / holdingStats.totalInvestment) : 0,
      openPositions: positionStats.openPositions,
      totalPnL: positionStats.totalPnL,
      maxProfit: positionStats.maxProfit,
      maxLoss: positionStats.maxLoss,
      openOrders,
      activeSips: sipStats.activeSips,
      openIpos,
      availableFunds,
      totalSipAmount: sipStats.totalSipAmount,
      contributionThisMonth: sipStats.contributionThisMonth,
      pausedSips: sipStats.pausedSips,
      utilizedFunds: fundsStats.utilizedFunds,
      totalCollateralMargin: fundsStats.totalCollateralMargin,
      totalCash: fundsStats.totalCash,
      availableMarginPercent: fundsStats.availableMarginPercent,
      profitLeft
    };
  }

  // SIP calculation method based on MF SIPs component logic
  private calculateSipSummary(sips: any[]) {
    let totalSipAmount = 0;
    let contributionThisMonth = 0;
    let activeSips = 0;
    let pausedSips = 0;

    const today = new Date();
    const currentMonth = today.getMonth();
    const currentYear = today.getFullYear();

    for (const sip of sips) {
      const sipData = sip?.mfSip;
      if (!sipData) continue;

      if (sipData.status === 'PAUSED') {
        pausedSips += 1;
        continue;
      }

      const amount = sipData.instalmentAmount || 0;
      totalSipAmount += amount;

      const lastSipInstalment = new Date(sipData.lastInstalment);
      if (lastSipInstalment.getMonth() === currentMonth &&
          lastSipInstalment.getFullYear() === currentYear &&
          lastSipInstalment < today) {
        contributionThisMonth += amount;
      }

      if (sipData.status === 'ACTIVE') {
        activeSips += 1;
      }
    }

    return {
      totalSipAmount,
      contributionThisMonth,
      activeSips,
      pausedSips
    };
  }

  // Funds calculation method based on funds component logic
  private calculateFundsStats(funds: any[], availableFunds: number) {
    let utilizedFunds = 0;
    let totalCollateralMargin = 0;

    funds.forEach(fund => {
      utilizedFunds += Number(fund.margin.utilised.debits);
      totalCollateralMargin += Number(fund.margin.available.collateral);
    });

    // Calculate total cash as: utilized + available - total collateral
    const totalCash = utilizedFunds + availableFunds - totalCollateralMargin;

    // Calculate available margin percent
    const availableMarginPercent = totalCollateralMargin > 0 ?
      (availableFunds / totalCollateralMargin) * 100 : 0;

    return {
      utilizedFunds,
      totalCollateralMargin,
      totalCash,
      availableMarginPercent
    };
  }

  refresh(): void {
    this.currentDateTime = new Date(); // Update current date time on refresh
    this.loadDashboardData();
  }

  // Navigation method for clickable statistics
  navigateToPage(route: string): void {
    this.router.navigate([route]);
  }

  private saveUserViewState(): void {
    this.userViewStateService.setState({
      home: {
        selectedUsersIds: this.selectedUserIds,
      }
    });
  }

  showChart = true;
  dateLabels: string[] = [];
  legendLabels: string[] = [];
  series: any[] = [];
  historicalTimelineValues: any[] = [];
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

  populateTimelineChartData() {
    const chartData = this.utilsService.populateHoldingsTimelineChartData(this.historicalTimelineValues, []);
    this.dateLabels = chartData.dateLabels;
    this.legendLabels = chartData.legendLabels;
    this.series = chartData.series;
  }

  // User filter event handlers
  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.applyUserFilter();
    this.saveUserViewState();
  }

  onUserSelection(selectedUserIds: string[]): void {
    this.selectedUserIds = selectedUserIds;
    this.applyUserFilter();
    this.saveUserViewState();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  // Helper method to check if multiple users exist
  hasMultipleUsers(): boolean {
    return this.users.length > 1;
  }

  private applyUserFilter(): void {
    // Recalculate stats based on selected users
    this.recalculateStats();

    // Update timeline chart based on user selection
    if (this.selectedUserIds.length === 0) {
      this.populateTimelineChartData();
    } else {
      this.populateTimelineChartDataForUsers(this.selectedUserIds);
    }
  }

  private populateTimelineChartDataForUsers(userIds: string[]): void {
    // Pass the selected user IDs to filter the timeline chart data
    const chartData = this.utilsService.populateHoldingsTimelineChartData(this.historicalTimelineValues, userIds);
    this.dateLabels = chartData.dateLabels;
    this.legendLabels = chartData.legendLabels;
    this.series = chartData.series;
  }

  ngOnDestroy(): void {
    // Unsubscribe from WebSocket
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
  }

  // Update popup data if popup is currently open (called on websocket updates)
  private updatePopupDataIfOpen(): void {
    if (!this.showPnLDetailsPopup) {
      return; // Popup is not open, no need to update
    }

    // Determine which popup is open based on the title and update accordingly
    if (this.popupTitle === 'Holdings P&L Details by User' || this.popupTitle === 'Positions P&L Details by User') {
      this.popupUserData = this.calculateUserPnLData();
    } else if (this.popupTitle === 'Investment Details by User') {
      this.popupUserData = this.calculateUserInvestmentData();
    }
    // Note: Funds and MF SIPs don't update on websocket, so we skip those
  }

  // P&L Details Popup methods
  closePnLDetailsPopup(): void {
    this.showPnLDetailsPopup = false;
  }

  // Show Holdings P&L Details (Total P&L and Day's P&L)
  showHoldingsPnLDetails(): void {
    this.popupTitle = 'Holdings P&L Details by User';
    this.popupSortByColumn = 'totalPnL';
    this.popupColumns = [
      { key: 'totalPnL', label: 'Total P&L', type: 'currency-with-percent', colorize: true },
      { key: 'daysPnL', label: "Day's P&L", type: 'currency-with-percent', colorize: true }
    ];
    this.popupUserData = this.calculateUserPnLData();
    this.showPnLDetailsPopup = true;
  }

  // Show Positions P&L Details (Positions P&L and Max Profit)
  showPositionsPnLDetails(): void {
    this.popupTitle = 'Positions P&L Details by User';
    this.popupSortByColumn = 'positionsPnL';
    this.popupColumns = [
      { key: 'positionsPnL', label: 'Positions P&L', type: 'currency', colorize: true },
      { key: 'maxProfit', label: 'Max Profit', type: 'currency', colorize: false }
    ];
    this.popupUserData = this.calculateUserPnLData();
    this.showPnLDetailsPopup = true;
  }

  // Show Funds Details
  showFundsDetails(): void {
    this.popupTitle = 'Funds Details by User';
    this.popupSortByColumn = 'availableMargin';
    this.popupColumns = [
      { key: 'utilizedFunds', label: 'Utilized Funds', type: 'currency', colorize: false },
      { key: 'availableMargin', label: 'Available Margin', type: 'currency', colorize: false },
      { key: 'totalCash', label: 'Total Cash', type: 'currency', colorize: false }
    ];
    this.popupUserData = this.calculateUserFundsData();
    this.showPnLDetailsPopup = true;
  }

  // Show Mutual Funds Details
  showMutualFundsDetails(): void {
    this.popupTitle = 'Mutual Funds Details by User';
    this.popupSortByColumn = 'totalSipAmount';
    this.popupColumns = [
      { key: 'activeSips', label: 'Active SIPs', type: 'number', colorize: false },
      { key: 'totalSipAmount', label: 'Total SIP Amount', type: 'currency', colorize: false },
      { key: 'monthlyContribution', label: 'This Month', type: 'currency', colorize: false }
    ];
    this.popupUserData = this.calculateUserMFData();
    this.showPnLDetailsPopup = true;
  }

  // Show Investment Details (Total Investment and Current Value)
  showInvestmentDetails(): void {
    this.popupTitle = 'Investment Details by User';
    this.popupSortByColumn = 'currentValue';
    this.popupColumns = [
      { key: 'totalInvestment', label: 'Total Investment', type: 'currency', colorize: false },
      { key: 'currentValue', label: 'Current Value', type: 'currency', colorize: false },
      { key: 'totalGains', label: 'Total Gains', type: 'currency-with-percent', colorize: true }
    ];
    this.popupUserData = this.calculateUserInvestmentData();
    this.showPnLDetailsPopup = true;
  }

  // Calculate P&L details per user
  private calculateUserPnLData(): UserDetailsData[] {
    const userPnLMap = new Map<string, UserDetailsData>();

    // Initialize user data
    this.users.forEach(user => {
      userPnLMap.set(user.userId, {
        userId: user.userId,
        userName: user.profile?.userName || user.name,
        avatarUrl: user.profile?.avatarURL,
        totalPnL: 0,
        daysPnL: 0,
        positionsPnL: 0,
        maxProfit: 0,
        totalInvestment: 0,
        totalPnLPercent: 0,
        daysPnLPercent: 0
      });
    });

    // Calculate holdings P&L per user (Total P&L and Day's P&L)
    this.rawData.holdings.forEach((holding: any) => {
      const userPnL = userPnLMap.get(holding.userId);
      if (userPnL) {
        const investment = holding.quantity * holding.averagePrice;
        const currentValue = holding.quantity * holding.lastPrice;
        const gain = currentValue - investment;
        const dayPnl = holding.dayPnl || 0;

        userPnL['totalPnL'] += gain;
        userPnL['daysPnL'] += dayPnl;
        userPnL['totalInvestment'] += investment;
      }
    });

    // Calculate percentages
    userPnLMap.forEach((userPnL) => {
      if (userPnL['totalInvestment'] > 0) {
        userPnL['totalPnLPercent'] = (userPnL['totalPnL'] / userPnL['totalInvestment']) * 100;
        userPnL['daysPnLPercent'] = (userPnL['daysPnL'] / userPnL['totalInvestment']) * 100;
      }
    });

    // Calculate positions P&L per user
    const nrmlPositions = this.rawData.positions.filter((p: any) => p.position?.product === 'NRML');
    nrmlPositions.forEach((positionData: any) => {
      const userPnL = userPnLMap.get(positionData.userId);
      if (userPnL) {
        const position = positionData.position;
        userPnL['positionsPnL'] += position.pnl || 0;

        // Max Profit calculation: (sellValue - buyValue)
        const maxProfitForPosition = (position.sellValue || 0) - (position.buyValue || 0);
        userPnL['maxProfit'] += maxProfitForPosition;
      }
    });

    // Convert map to array
    return Array.from(userPnLMap.values());
  }

  // Calculate Funds details per user
  private calculateUserFundsData(): UserDetailsData[] {
    const userFundsMap = new Map<string, UserDetailsData>();

    // Initialize user data
    this.users.forEach(user => {
      userFundsMap.set(user.userId, {
        userId: user.userId,
        userName: user.profile?.userName || user.name,
        avatarUrl: user.profile?.avatarURL,
        utilizedFunds: 0,
        availableMargin: 0,
        totalCash: 0,
        collateralMargin: 0
      });
    });

    // Calculate funds per user
    this.rawData.funds.forEach((fund: any) => {
      const userFunds = userFundsMap.get(fund.userId);
      if (userFunds) {
        const utilized = Number(fund.margin?.utilised?.debits || 0);
        const available = Number(fund.margin?.net || 0);
        const collateral = Number(fund.margin?.available?.collateral || 0);

        userFunds['utilizedFunds'] = utilized;
        userFunds['availableMargin'] = available;
        userFunds['collateralMargin'] = collateral;
        userFunds['totalCash'] = utilized + available - collateral;
      }
    });

    // Convert map to array
    return Array.from(userFundsMap.values());
  }

  // Calculate Mutual Funds details per user
  private calculateUserMFData(): UserDetailsData[] {
    const userMFMap = new Map<string, UserDetailsData>();

    // Initialize user data
    this.users.forEach(user => {
      userMFMap.set(user.userId, {
        userId: user.userId,
        userName: user.profile?.userName || user.name,
        avatarUrl: user.profile?.avatarURL,
        activeSips: 0,
        totalSipAmount: 0,
        monthlyContribution: 0
      });
    });

    const today = new Date();
    const currentMonth = today.getMonth();
    const currentYear = today.getFullYear();

    // Calculate MF SIPs per user
    this.rawData.mfSips.forEach((sip: any) => {
      const userMF = userMFMap.get(sip.userId);
      if (userMF && sip.mfSip) {
        const sipData = sip.mfSip;
        const amount = sipData.instalmentAmount || 0;

        if (sipData.status === 'ACTIVE') {
          userMF['activeSips'] += 1;
          userMF['totalSipAmount'] += amount;

          const lastSipInstalment = new Date(sipData.lastInstalment);
          if (lastSipInstalment.getMonth() === currentMonth &&
              lastSipInstalment.getFullYear() === currentYear &&
              lastSipInstalment < today) {
            userMF['monthlyContribution'] += amount;
          }
        }
      }
    });

    // Convert map to array
    return Array.from(userMFMap.values());
  }

  // Calculate Investment details per user
  private calculateUserInvestmentData(): UserDetailsData[] {
    const userInvestmentMap = new Map<string, UserDetailsData>();

    // Initialize user data
    this.users.forEach(user => {
      userInvestmentMap.set(user.userId, {
        userId: user.userId,
        userName: user.profile?.userName || user.name,
        avatarUrl: user.profile?.avatarURL,
        totalInvestment: 0,
        currentValue: 0,
        totalGains: 0,
        totalGainsPercent: 0
      });
    });

    // Calculate holdings investment per user
    this.rawData.holdings.forEach((holding: any) => {
      const userInvestment = userInvestmentMap.get(holding.userId);
      if (userInvestment) {
        const investment = holding.quantity * holding.averagePrice;
        const currentValue = holding.quantity * holding.lastPrice;
        const gain = currentValue - investment;

        userInvestment['totalInvestment'] += investment;
        userInvestment['currentValue'] += currentValue;
        userInvestment['totalGains'] += gain;
      }
    });

    // Calculate percentages
    userInvestmentMap.forEach((userInvestment) => {
      if (userInvestment['totalInvestment'] > 0) {
        userInvestment['totalGainsPercent'] = (userInvestment['totalGains'] / userInvestment['totalInvestment']) * 100;
      }
    });

    // Convert map to array
    return Array.from(userInvestmentMap.values());
  }
}
