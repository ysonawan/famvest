import {Component, OnInit, OnDestroy, Inject, AfterViewInit, ViewChild} from '@angular/core';
import { UserFilterComponent } from "../shared/user-filter/user-filter.component";
import { SearchInputComponent } from "../shared/search-input/search-input.component";
import { Subscription } from 'rxjs/internal/Subscription';
import {filter, of, take} from 'rxjs';
import { WebSocketService } from '../../services/web-socket.service';
import { OrdersService } from '../../services/orders.service';
import {CommonModule, DOCUMENT} from '@angular/common';
import {OrdersTableComponent} from "../tables/orders-table/orders-table.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {catchError} from "rxjs/operators";
import {ApiErrorResponse} from "../../models/api-error-response.model";
import {MatDialog} from "@angular/material/dialog";
import {AddManageOrderDialogComponent} from "../dialogs/add-position-dialog/add-manage-order-dialog.component";
import {fallbackAvatarUrl} from "../../constants/constants";
import {RouterLink} from "@angular/router";
import {UtilsService} from "../../services/utils.service";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";
import {Overlay} from "@angular/cdk/overlay";
import {UserDataStateService} from "../../services/user-data-state-service";
import {NoteComponent} from "../shared/note/note.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faListCheck} from "@fortawesome/free-solid-svg-icons";
import {AlertService} from "../../services/alert.service";
import {ChargesSummaryComponent} from "../shared/charges-summary/charges-summary.component";
import {expandCollapseAnimation} from "../shared/animations";

@Component({
    selector: 'app-orders',
  imports: [
    UserFilterComponent,
    SearchInputComponent,
    CommonModule,
    OrdersTableComponent,
    RouterLink,
    ToolBarComponent,
    NoteComponent,
    FaIconComponent,
    ChargesSummaryComponent
  ],
    templateUrl: './orders.component.html',
    styleUrls: ['./orders.component.css'],
    animations: [expandCollapseAnimation],
})
export class OrdersComponent implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild(OrdersTableComponent) orderTableComponent?: OrdersTableComponent;

  constructor(private ordersService: OrdersService,
              private ws: WebSocketService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              private dialog: MatDialog,
              private utilsService: UtilsService,
              private overlay: Overlay,
              private alertService: AlertService,
              @Inject(DOCUMENT) private document: Document) {

  }

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    this.activeSubTab = userViewState.orders.selectedOrderType;
    this.selectedUserIds = userViewState.orders.selectedUsersIds;
    this.searchQuery = userViewState.orders.searchQuery;
    if(userViewState.orders.filterSelection?.length > 0) {
      this.filterSelection = userViewState.orders.filterSelection;
    }
    this.getCachedData();
    this.fetchStocksFnoOrders();
    this.fetchCharges();
  }

  subTabs = ['Open', 'All'];
  activeSubTab = 'Open';
  tabCounts: Record<string, number> = {
    'All': 0,
    'Open': 0,
  };
  instrumentTokens: number[] = [];
  private sub?: Subscription;

  @ViewChild(ChargesSummaryComponent) chargesSummaryComponent?: ChargesSummaryComponent;

  users: any[] = [];
  stocksFnoOrders: any[] = [];
  stockFnoGroupedOrders: any[] = [];

  charges: any[] = [];
  filterCharges: any[] = [];
  showCharges = false;

  errorMessage = '';
  selectedUserIds: string[] = [];
  searchQuery: string = '';
  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
  selectedOrders: any[] = [];
  isLoadingData: boolean = false;

  // Filters
  filterOptions = [
    { key: 'custom.status', values: ['OPEN', 'COMPLETED', 'CANCELLED', 'REJECTED'] },
    { key: 'order.orderType', values: ['LIMIT', 'MARKET', 'SL', 'SL-M'] },
    { key: 'order.transactionType', values: ['BUY', 'SELL'] }
  ];

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

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.stocksFnoOrders) {
        this.stocksFnoOrders = userDataState.stocksFnoOrders;
        this.renderOrders();
      }
    }
  }
  setCachedData(): void {
    this.userDataStateService.setState({
      stocksFnoOrders: this.stocksFnoOrders
    });
  }
  renderOrders(): void {
    this.updateCounts();
    this.updateFilteredAndGroupedStockFnoOrders();
  }
  fetchStocksFnoOrders(done?: () => void): void {
    this.isLoadingData = true;
    this.ordersService.getOrders().subscribe({
        next: (response) => {
          this.stocksFnoOrders = response.data;
          this.renderOrders();
          this.instrumentTokens = this.stocksFnoOrders.filter(order => order.instrumentToken).map(order => order.instrumentToken);
          this.subscribeToWebSocket();
          this.setCachedData();
          // Clear selection in the table component
          if (this.orderTableComponent) {
            this.orderTableComponent.clearSelection();
          }
        },
        error: (error) => {
          if(error.error.message) {
            this.toastrService.error(error.error.message, 'Error');
          } else {
            this.toastrService.error('An unexpected error occurred while fetching stocks and fno orders. Verify that the backend service is operational.', 'Error');
          }
          this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
        },
        complete: () => {
          this.isLoadingData = false;
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

  updateCounts() {
    const open = this.stocksFnoOrders.filter(o => (this.stocksFnoPendingStatus.indexOf(o.order.status) > -1)).length;
    this.tabCounts['Open'] = open;
    const executed = this.stocksFnoOrders.filter(o => (this.stocksFnoPendingStatus.indexOf(o.order.status) < 0)).length;
    this.tabCounts['Executed'] = executed;
    this.tabCounts['All'] = open+executed;
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
          this.updateOrdersOnUpdate(ticks);
          });
      } else {
          console.log('No instrument tokens available for web socket subscription.');
      }
    }

    onUserSelection(userIds: string[]): void {
      this.selectedUserIds = userIds;
      this.updateFilteredAndGroupedOrders();
      this.filterChargesBySelectedUsers();
      this.saveUserViewState();
    }

    onAllUserSelection(): void {
      this.selectedUserIds = [];
      this.updateFilteredAndGroupedOrders();
      this.filterChargesBySelectedUsers();
      this.saveUserViewState();
    }

  onStocksFnoSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedStockFnoOrders();
    this.saveUserViewState();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      orders: {
        selectedUsersIds: this.selectedUserIds,
        selectedOrderType: this.activeSubTab,
        searchQuery: this.searchQuery,
        filterSelection: this.filterSelection,
      }
    });
  }

  updateOrdersOnUpdate(ticks: any[]): void {
    // Update the LTP of the order based on the tick received
    ticks.forEach(tick => {
      const matchedOrders: any[] = this.stocksFnoOrders.filter(o => o.instrumentToken === tick.instrumentToken);
      matchedOrders.forEach(order => {
        if(order.lastPrice !== tick.lastTradedPrice || order.change !== tick.change) {
          order.change = tick.change;
          order.lastPrice = tick.lastTradedPrice;
      }
      });
    });
  }

  setActiveSubTab(tab: string): void {
    this.activeSubTab = tab;
    this.updateFilteredAndGroupedOrders();
    this.saveUserViewState();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  stocksFnoPendingStatus = ['TRIGGER PENDING', 'AMO REQ RECEIVED', 'MODIFY AMO REQ RECEIVED', 'OPEN', 'OPEN PENDING'];
  updateFilteredAndGroupedOrders(): void {
    this.updateFilteredAndGroupedStockFnoOrders();
  }

  updateFilteredAndGroupedStockFnoOrders(): void {
    // Filtering
    let filtered = this.stocksFnoOrders;
    if (this.activeSubTab !== 'All') {
      if (this.activeSubTab === 'Open') {
        filtered = filtered.filter(o => (this.stocksFnoPendingStatus.indexOf(o.order.status) > -1));
      } else {
        filtered = filtered.filter(o => (this.stocksFnoPendingStatus.indexOf(o.order.status) < 0));
      }
    }
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter(o => this.selectedUserIds.includes(o.userId));
    }
    if (this.searchQuery) {
      filtered = filtered.filter(o =>
        o.displayName.toLowerCase().includes(this.searchQuery) ||
        o.userId.toLowerCase().includes(this.searchQuery)
      );
    }
    //filter
    if (this.filterSelection.length > 0) {
      filtered = this.utilsService.filter(this.filterSelection, filtered);
      filtered = this.filterCustomStatus(filtered);
    }
    // Grouping
    const grouped: { [key: string]: any[] } = {};
    filtered.forEach(order => {
      if (!grouped[order.userId]) {
        grouped[order.userId] = [];
      }
      grouped[order.userId].push(order);
    });
    this.stockFnoGroupedOrders = Object.entries(grouped).map(([userId, orders]) => {
      const daysPnL = 0;
      const totalPnL = 0;
      return {
        userId,
        orders,
        daysPnL,
        totalPnL,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId)
      };
    });
  }

  private filterCustomStatus(filtered: any[]) {
    const statusFilter = this.filterSelection.find(filter => filter.key === 'custom.status' && filter.selected.length > 0 );
    if (statusFilter) {
      const statusMap: { [key: string]: string[] } = {
        OPEN: this.stocksFnoPendingStatus,
        COMPLETED: ['COMPLETE'],
        REJECTED: ['REJECTED'],
        CANCELLED: ['CANCELLED']
      };
      // Flatten selected mapped statuses
      const allowedStatuses = statusFilter.selected.flatMap((status: string) => statusMap[status] || []);
      filtered = filtered.filter(o => allowedStatuses.includes(o.order.status));
    }
    return filtered;
  }

  actions: any[] = [
    {action: 'copy', color:'blue', label: 'Copy Order'},
    {action: 'modify', color:'blue', label: 'Modify Order'},
    {action: 'cancel', color:'red', label: 'Cancel Order'},
    {action: 'info', color:'gray', label: 'Order Details'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'modify') {
      this.openModifyOrCopyOrderDialog(event.row, false);
    } else if(event.action === 'cancel') {
      this.cancelOrder(event.row)
    } else if(event.action === 'copy') {
      this.openModifyOrCopyOrderDialog(event.row, true);
    } else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, `Order Details - ${event.row.displayName}`);
    }
  }

  cancelOrder(orderDetail: any): void {
    this.ordersService.cancelOrder( orderDetail.order.accountId, orderDetail.order.orderId, orderDetail.order.orderVariety).pipe(
      catchError(err => {
        return of({
          error: true,
          id: orderDetail.order.accountId,
          message: err?.error?.message || 'Unknown error'
        } as ApiErrorResponse);
      })
    ).subscribe({
      next: (result) => {
        if ('error' in result && result.error) {
          console.error(`❌ Order cancel failed:`, result);
          this.toastrService.error(`Account ID: ${orderDetail.order.accountId}. Order cancellation for ${result.id} failed: ${result.message}`, 'Error');
        } else {
          this.toastrService.success(`Account ID: ${orderDetail.order.accountId}. Order cancelled successfully. Order ID:${result.data.orderId}`, 'Success');
          this.fetchStocksFnoOrders();
        }
      },
      error: (err) => {
        // This only triggers for stream-breaking (non-caught) errors
        console.error('Fatal error:', err);
        this.toastrService.error('Something went wrong in the processing pipeline.', 'Fatal Error');
      },
      complete: () => {
        console.log('✅ All orders processed');
      }
    });
  }

  onSelectedOrdersChange(selectedOrders: any[]): void {
    this.selectedOrders = selectedOrders;
  }

  cancelSelectedOrders(): void {
    if (this.selectedOrders.length === 0) {
      this.toastrService.warning('No orders selected', 'Warning');
      return;
    }

    const count = this.selectedOrders.length;
    this.alertService.confirm(
      `Cancel ${count} Order${count > 1 ? 's' : ''}?`,
      `Are you sure you want to cancel ${count} selected order${count > 1 ? 's' : ''}? This action cannot be undone.`,
      () => {
        this.processBulkCancel();
      }
    );
  }

  private processBulkCancel(): void {
    const totalOrders = this.selectedOrders.length;
    let processed = 0;
    let succeeded = 0;
    let failed = 0;

    this.selectedOrders.forEach((orderDetail) => {
      this.ordersService.cancelOrder(
        orderDetail.order.accountId,
        orderDetail.order.orderId,
        orderDetail.order.orderVariety
      ).pipe(
        catchError(err => {
          return of({
            error: true,
            id: orderDetail.order.accountId,
            message: err?.error?.message || 'Unknown error'
          } as ApiErrorResponse);
        })
      ).subscribe({
        next: (result) => {
          processed++;
          if ('error' in result && result.error) {
            failed++;
            console.error(`❌ Order cancel failed:`, result);
          } else {
            succeeded++;
          }

          // Show summary after all processed
          if (processed === totalOrders) {
            if (succeeded > 0) {
              this.toastrService.success(
                `Successfully cancelled ${succeeded} order${succeeded > 1 ? 's' : ''}${failed > 0 ? `, ${failed} failed` : ''}`,
                'Bulk Cancel Complete'
              );
            }
            if (failed > 0 && succeeded === 0) {
              this.toastrService.error(`All ${failed} order cancellations failed`, 'Error');
            }
            this.fetchStocksFnoOrders();
            this.selectedOrders = [];
          }
        },
        error: (err) => {
          processed++;
          failed++;
          console.error('Fatal error:', err);

          if (processed === totalOrders) {
            this.toastrService.error('Something went wrong in the processing pipeline.', 'Fatal Error');
            this.fetchStocksFnoOrders();
            this.selectedOrders = [];
            // Clear selection in the table component
            if (this.orderTableComponent) {
              this.orderTableComponent.clearSelection();
            }
          }
        }
      });
    });
  }

  openModifyOrCopyOrderDialog(orderDetail: any, isCopy: boolean): void {
    const instrument = {
      exchange: orderDetail.order.exchange,
      tradingSymbol: orderDetail.order.tradingSymbol,
      instrumentToken: orderDetail.instrumentToken,
      lastPrice: orderDetail.order.price,
      product: orderDetail.order.product,
      quantity: orderDetail.order.quantity,
      displayName : orderDetail.displayName,
      segment: orderDetail.order.segment || orderDetail.order.exchange, // Use segment or exchange as needed
      tradingAccountId: orderDetail.order.accountId
    }
    const sourceData = {
      isCopy: isCopy,
      isBuyMode: orderDetail.order.transactionType === 'BUY',
      orderId: orderDetail.order.orderId,
      variety: orderDetail.order.orderVariety,
      orderType: orderDetail.order.orderType,
      originalPrice: orderDetail.order.price,
      originalTriggerPrice: orderDetail.order.triggerPrice,
      product: orderDetail.order.product,
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
        this.fetchStocksFnoOrders();
      }
    });
  }

  filterSelection: any[] = [];
  applyFilters(event: any) {
    this.filterSelection = event;
    this.updateFilteredAndGroupedStockFnoOrders();
    this.saveUserViewState();
  }

  handleRefresh(done: () => void) {
    this.fetchStocksFnoOrders(done);
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

  toggleCharges() {
    this.showCharges = !this.showCharges;
  }

  viewCharges() {
    this.toggleCharges();
  }

  ngAfterViewInit(): void {
  }

  protected readonly faListCheck = faListCheck;
}
