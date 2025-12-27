import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {TradingAccountService} from "../../../services/trading-account.service";
import {ToastrService} from "ngx-toastr";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {concatMap, filter, from, of, Subscription, take} from "rxjs";
import {WebSocketService} from "../../../services/web-socket.service";
import {OrdersService} from "../../../services/orders.service";
import {OrderRequest} from "../../../models/order-request";
import {OrderParams} from "../../../models/order-params";
import {catchError} from "rxjs/operators";
import {ApiErrorResponse} from "../../../models/api-error-response.model";
import {FundsService} from "../../../services/funds.service";
import {SmallChipComponent} from "../../shared/small-chip/small-chip.component";
import {UserViewStateService} from "../../../services/user-view-state-service";
import {MarketDepthInvokerComponent} from "../../shared/market-depth-invoker/market-depth-invoker.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faChartPie, faPlus, faTrash} from "@fortawesome/free-solid-svg-icons";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-add-manage-order-dialog',
  templateUrl: './add-manage-order-dialog.component.html',
  styleUrls: ['./add-manage-order-dialog.component.css'],
  standalone: true,
  imports: [FormsModule, CommonModule, DragDropModule, SmallChipComponent, MarketDepthInvokerComponent, FaIconComponent, MatTooltip]
})
export class AddManageOrderDialogComponent implements OnInit, OnDestroy {

  instrumentTokens: number[] = [];
  private sub?: Subscription;

  // Unique identifier for this dialog instance to avoid radio button conflicts
  protected readonly dialogInstanceId: string = `dialog_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  orders: any = [{
    tradingAccountId: '',
    quantity: 0,
    requiredMargin: 0,
    charges: 0,
    availableMargin: 0
  }];

  positionData: any = {
    isBuyMode : false,
    marketOrder: false,
    slOrder: false,
    price: 0,
    triggerPrice: 0,
    productType : 'NON-MIS', // Default product type, can be adjusted based on the instrument
  };

  funds: any[] = [];
  users: any[] = [];
  instrument: any = {};
  sourceData: any = {};
  variety: string | null = null;

  requestType: 'place' | 'modify' = 'place'; // Default to 'place' unless orderId is provided
  lotSize: number = 1; // Default lot size, can be adjusted based on the instrument

  private lastEnteredPrice: number = 0;
  private lastEnteredTriggeredPrice: number = 0;

  constructor(public dialogRef: MatDialogRef<AddManageOrderDialogComponent>,
              private userService: TradingAccountService,
              private toastr: ToastrService,
              private ws: WebSocketService,
              private ordersService: OrdersService,
              private fundsService: FundsService,
              private userViewStateService: UserViewStateService,
              @Inject(MAT_DIALOG_DATA) public data: { sourceData: any, instrument: any}) {
    this.sourceData = data.sourceData;
    this.instrument = data.instrument;
    this.initializePositionData();
  }

  ngOnInit(): void {
    this.fetchUsers();
    this.fetchFunds();
    this.subscribeToWebSocket();
  }

  toggleMode() {
    this.positionData.isBuyMode = !this.positionData.isBuyMode;
    //compute margin for each order
    this.computeAllRequiredMargins();
  }

  addAccount() {
    if(this.orders.length >= 5) {
      this.toastr.error('You can only add up to 5 positions in one request', 'Error');
      return;
    }
    this.orders.push({ tradingAccountId: '', quantity: 0 });
  }

  removeAccount(index: number) {
    this.orders.splice(index, 1);
  }

  subscribeToWebSocket(): void {
    this.instrumentTokens.push(this.instrument.instrumentToken);
    if (this.instrumentTokens.length > 0) {
      this.ws.connectionState().pipe(
        filter(c => c), // only when connected = true
        take(1)
      ).subscribe(() => {
        this.ws.subscribe(this.instrumentTokens);
      });
      this.sub = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updatePositionFormOnUpdate(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  updatePositionFormOnUpdate(ticks: any[]): void {
    // Update the LTP of the position based on the tick received
    ticks.forEach(tick => {
      const matchedInstruments: any[] = this.instrument.instrumentToken === tick.instrumentToken ? [this.instrument] : [];
      matchedInstruments.forEach(instrument => {
        if (instrument && instrument.lastPrice !== tick.lastTradedPrice) {
          instrument.change = tick.change;
          instrument.lastPrice = tick.lastTradedPrice;
          this.lotSize = tick.lotSize;
        }
      });
    });
  }

  getDefaultVariety(): string {
    const now = new Date();
    const start = new Date();
    start.setHours(9, 0, 0, 0); // 09:00:00.000
    const cutoff = new Date();
    cutoff.setHours(15, 30, 0, 0); // 15:30:00.000
    const isWeekend = now.getDay() === 0 || now.getDay() === 6;
    return (now < start || now > cutoff || isWeekend) ? 'amo' : 'regular';
  }

  onSubmit(): void {
    const orderRequests: OrderRequest[] = [];
    let variety = this.getDefaultVariety()
    this.orders.forEach((order: any) => {
      const orderParams: OrderParams = {
        quantity: order.quantity,
        orderType: this.orderType,
        tradingsymbol: this.instrument.tradingSymbol,
        product: this.productType,
        exchange: this.instrument.exchange,
        transactionType: this.buySell,
        validity: 'DAY',
        price: this.positionData.price,
        triggerPrice: this.positionData.triggerPrice,
        tag: 'famvest-single-order',
      }
      this.saveUserViewState(order, this.instrument.segment);
      orderRequests.push({
        tradingAccountId: order.tradingAccountId,
        orderParams: orderParams
      })
    });
    if (!this.validateOrders(orderRequests)) {
      return; // Stop submission if validation fails
    }
    if(this.requestType === 'modify') {
          this.ordersService.modifyOrder(orderRequests[0].tradingAccountId, this.sourceData.orderId, orderRequests[0], variety).pipe(
            catchError(err => {
              return of({
                error: true,
                id: this.orders[0].tradingAccountId,
                message: err?.error?.message || 'Unknown error'
              } as ApiErrorResponse);
            })
      ).subscribe({
        next: (result) => {
          if ('error' in result && result.error) {
            console.error(`❌ Order modification failed:`, result);
            this.toastr.error(`Order modification for ${result.id} failed: ${result.message}`, 'Error');
          } else {
            this.toastr.success(`Order modification successfully. Order ID:${result.data.orderId}`, 'Success');
          }
          this.dialogRef.close(result);
        },
        error: (err) => {
          // This only triggers for stream-breaking (non-caught) errors
          console.error('Fatal error:', err);
          this.toastr.error('Something went wrong in the processing pipeline.', 'Fatal Error');
        },
        complete: () => {
          console.log('✅ All orders processed');
        }
      });
    } else {
      from(orderRequests).pipe(
        concatMap(orderRequest =>
          this.ordersService.placeOrder(orderRequest, variety).pipe(
            catchError(err => {
              return of({
                error: true,
                id: orderRequest.tradingAccountId,
                message: err?.error?.message || 'Unknown error'
              } as ApiErrorResponse);
            })
          )
        )
      ).subscribe({
        next: (result) => {
          if ('error' in result && result.error) {
            console.error(`❌ Order placement failed:`, result);
            this.toastr.error(`Order placement for ${result.id} failed: ${result.message}`, 'Error');
          } else {
            this.toastr.success(`Order placed successfully. Order ID:${result.data.orderId}`, 'Success');
          }
          this.dialogRef.close(result);
        },
        error: (err) => {
          // This only triggers for stream-breaking (non-caught) errors
          console.error('Fatal error:', err);
          this.toastr.error('Something went wrong in the processing pipeline.', 'Fatal Error');
        },
        complete: () => {
          console.log('✅ All orders processed');
        }
      });
    }

  }

  validateOrders(orderRequests: OrderRequest[]): boolean {
    if (orderRequests.length === 0) {
      this.toastr.error('Please add at least one trading account position', 'Error');
      return false;
    }
    for (const request of orderRequests) {
      if (!request.tradingAccountId || request.orderParams.quantity <= 0) {
        this.toastr.error('All positions must have a valid trading account and quantity', 'Error');
        return false;
      }
    }
    return true;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  initializePositionData(): void {
    switch (this.sourceData.orderType) {
      case 'SL':
        this.positionData.marketOrder = false;
        this.positionData.slOrder = true;
        break;
      case 'SL-M':
        this.positionData.marketOrder = true;
        this.positionData.slOrder = true;
        break;
      case 'LIMIT':
        this.positionData.marketOrder = false;
        this.positionData.slOrder = false;
        break;
      case 'MARKET':
        this.positionData.marketOrder = true;
        this.positionData.slOrder = false;
        break;
    }
    if(this.sourceData.product) {
      this.positionData.productType = this.sourceData.product === 'MIS' ? 'MIS' : 'NON-MIS';
    }
    if(this.sourceData.variety) {
      this.variety = this.sourceData.variety;
    }
    this.positionData.isBuyMode = this.sourceData.isBuyMode;
    this.requestType = this.sourceData.orderId? 'modify' : 'place';
    if(this.sourceData.isCopy) {
      this.requestType = 'place';
    }
    if(this.sourceData.originalPrice) {
      this.positionData.price = this.sourceData.originalPrice;
    } else {
      this.positionData.price = this.instrument.lastPrice;
    }
    this.lastEnteredPrice = this.positionData.price;
    if(this.sourceData.originalTriggerPrice > 0) {
      this.positionData.triggerPrice = this.sourceData.originalTriggerPrice;
      this.lastEnteredTriggeredPrice = this.positionData.triggerPrice;
    } else {
      this.lastEnteredTriggeredPrice = this.instrument.lastPrice;
    }
    if (this.sourceData.orderId || this.sourceData.isExitPosition || this.sourceData.isCopy) {
      this.orders[0].tradingAccountId = this.instrument.tradingAccountId;
      this.orders[0].quantity = this.instrument.quantity;
      this.computeRequiredMargin(this.orders[0]);
    }
  }

  get orderType(): string {
    if(this.positionData.slOrder) {
      return this.positionData.marketOrder ? 'SL-M' : 'SL';
    } else {
      return this.positionData.marketOrder ? 'MARKET' : 'LIMIT';
    }
  }

  get productType(): string {
    if(this.positionData.productType === 'MIS') {
      return 'MIS';
    } else if(this.instrument.exchange === 'NFO' || this.instrument.exchange === 'BFO') {
      return 'NRML';
    } else {
      return 'CNC';
    }
  }

  get buySell(): string {
    return this.positionData.isBuyMode ? 'BUY' : 'SELL';
  }

  get priceInputIcon(): string {
    // Unicode: cross (×) or pencil (✎)
    return !this.positionData.marketOrder ? '×' : '✎';
  }

  get triggerPriceInputIcon(): string {
    // Unicode: cross (×) or pencil (✎)
    return this.positionData.slOrder ? '×' : '✎';
  }

  onPriceInputIconClick(): void {
    if (this.positionData.marketOrder) {
      this.positionData.price = this.lastEnteredPrice;
      this.positionData.marketOrder = false;
    } else {
      this.lastEnteredPrice = this.positionData.price;
      this.positionData.price = 0;
      this.positionData.marketOrder = true;
    }
  }

  onTriggerPriceInputIconClick(): void {
    if (this.positionData.slOrder) {
      this.lastEnteredTriggeredPrice = this.positionData.triggerPrice;
      this.positionData.triggerPrice = 0;
      this.positionData.slOrder = false;
    } else {
      this.positionData.triggerPrice = this.lastEnteredTriggeredPrice;
      this.positionData.slOrder = true;
    }
  }

  getPriceInputTitle() {
    if(this.positionData.slOrder) {
      return this.positionData.marketOrder ? 'Convert to SL order' : 'Convert to SL-M order';
    } else {
      return this.positionData.marketOrder ? 'Convert to LIMIT order' : 'Convert to MARKET order';
    }
  }

  getTriggerPriceInputTitle() {
    if(this.positionData.slOrder) {
      return this.positionData.marketOrder ? 'Convert to MARKET order' : 'Convert to LIMIT order';
    } else {
      return this.positionData.marketOrder ? 'Convert to SL-M order' : 'Convert to SL order';
    }
  }


  fetchUsers(): void {
    console.log('fetching user profiles');
    this.userService.getTradingAccounts().subscribe({
      next: (response) => {
        this.users = response.data.filter(user => user.active);
      },
      error: (error) => {
        this.toastr.error(error.error.message, 'Error');
        console.error(error);      }
    });
  }

    fetchFunds(): void {
      this.fundsService.getFunds().subscribe({
        next: (response) => {
          this.funds = response.data;
        },
        error: (error) => {
          if(error.error.message) {
            this.toastr.error(error.error.message, 'Error');
          } else {
            this.toastr.error('An unexpected error occurred while fetching funds. Verify that the backend service is operational.', 'Error');
          }
        },
        complete: () => {
          if((this.sourceData.orderId || this.sourceData.isCopy) && this.orders.length > 0) {
            this.orders[0].availableMargin = this.funds.find(fund => fund.userId === this.orders[0].tradingAccountId)?.margin?.net || 0;
          }
        }
      });
    }

  getHistoricalQuantity(order: any, segment: string): void {
    let userViewState = this.userViewStateService.getState();
    if(userViewState.ordersHistory) {
      let ordersHistory = userViewState.ordersHistory[`${order.tradingAccountId}_${segment}`];
      if(order.quantity === 0 && ordersHistory && ordersHistory.quantity) {
        order.quantity = ordersHistory.quantity;
      }
    }
  }

  saveUserViewState(order: any, segment: string): void {
    let userViewState = this.userViewStateService.getState();
    if (!userViewState.ordersHistory) {
      userViewState.ordersHistory = {};
    }
    const key = `${order.tradingAccountId}_${segment}`;
    userViewState.ordersHistory[key] = { quantity: order.quantity };
    this.userViewStateService.setState({
      ordersHistory: userViewState.ordersHistory
    });
  }

  computeAllRequiredMargins() {
    this.orders.forEach((order: any) => {
      this.computeRequiredMargin(order);
    });
  }

  computeRequiredMargin(order: any): void {
    order.availableMargin = this.funds.find(fund => fund.userId === order.tradingAccountId)?.margin?.net || 0;
    if (order.quantity > 0 && order.tradingAccountId) {
      const marginCalculationParams = {
        tradingSymbol: this.instrument.tradingSymbol,
        exchange: this.instrument.exchange,
        transactionType: this.buySell,
        variety: this.variety,
        product: this.productType,
        orderType: this.orderType,
        quantity: order.quantity,
        price: this.positionData.price || 0,
        triggerPrice: this.positionData.triggerPrice || 0,
      }
      const marginCalculationRequest = {
        tradingAccountId: order.tradingAccountId,
        marginCalculationParams : marginCalculationParams
      }
      this.fundsService.calculateMargin(marginCalculationRequest).subscribe({
        next: (response) => {
          order.requiredMargin = response.data[0].total;
          order.charges = response.data[0].charges.total;
        },
        error: (error) => {
          this.toastr.error(error.error.message, 'Error');
          console.error(error);      }
      });
    } else {
      order.requiredMargin = 0;
      order.charges = 0;
    }
  }

  ngOnDestroy(): void {
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
  }

  protected readonly faChartPie = faChartPie;
  protected readonly faPlus = faPlus;
  protected readonly faTrash = faTrash;
}
