import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faSearch, faTrash } from '@fortawesome/free-solid-svg-icons';
import { debounceTime, distinctUntilChanged, Subject, Subscription, filter, take, from, concatMap, of } from 'rxjs';
import { WatchlistService } from '../../services/watchlist.service';
import { ToastrService } from 'ngx-toastr';
import { FundsService } from '../../services/funds.service';
import { TradingAccountService } from '../../services/trading-account.service';
import { WebSocketService } from '../../services/web-socket.service';
import {SmallChipComponent} from "../shared/small-chip/small-chip.component";
import {MatTooltip} from "@angular/material/tooltip";
import {OrdersService} from "../../services/orders.service";
import {OrderRequest} from "../../models/order-request";
import {OrderParams} from "../../models/order-params";
import {catchError} from "rxjs/operators";
import {ApiErrorResponse} from "../../models/api-error-response.model";
import {UserDropdownComponent} from "../shared/user-dropdown/user-dropdown.component";
import { MarketDepthInvokerComponent } from '../shared/market-depth-invoker/market-depth-invoker.component';
import { BasketCommunicationService } from '../../services/basket-communication.service';

@Component({
  selector: 'app-basket-order',
  imports: [
    CommonModule,
    FormsModule,
    FaIconComponent,
    SmallChipComponent,
    MatTooltip,
    UserDropdownComponent,
    MarketDepthInvokerComponent
  ],
  templateUrl: './basket-order.component.html',
  styleUrl: './basket-order.component.css',
  standalone: true
})
export class BasketOrderComponent implements OnInit, OnDestroy {
  isLoading: boolean = true;
  basketItems: any[] = [];
  searchTerm: string = '';
  fetchedInstruments: any[] = [];
  searchTermChanged: Subject<string> = new Subject<string>();
  private searchSubscription?: Subscription;
  private wsSubscription?: Subscription;
  private marginCalculationSubscription?: Subscription;
  private basketCommunicationSubscription?: Subscription;
  requiredMargin: number = 0;
  finalMargin: number = 0;
  isCalculatingMargin: boolean = false;
  tradingAccounts: any[] = [];
  selectedTradingAccountId: string = '';
  includeExistingPositions: boolean = false;
  instrumentTokens: number[] = [];
  marginCalculationTrigger: Subject<void> = new Subject<void>();
  funds: any[] = [];
  utilizedMargin: number = 0;
  availableMargin: number = 0;
  isIntraday: boolean = false; // false = Overnight, true = Intraday (MIS)
  netPremium: number = 0;
  private readonly BASKET_CACHE_KEY = 'basket_order_items';

  protected readonly faSearch = faSearch;
  protected readonly faTrash = faTrash;

  constructor(
    private watchlistService: WatchlistService,
    private toastr: ToastrService,
    private fundsService: FundsService,
    private tradingAccountService: TradingAccountService,
    private ws: WebSocketService,
    private ordersService: OrdersService,
    private basketCommunicationService: BasketCommunicationService
  ) {
  }

  ngOnInit(): void {
    this.isLoading = true;
    // Set up the debounce logic for search
    this.searchSubscription = this.searchTermChanged.pipe(
      debounceTime(300), // Wait for 300ms pause in events
      distinctUntilChanged() // Only emit if the value has changed
    ).subscribe(term => {
      this.searchInstruments(term);
    });

    // Set up the debounce logic for margin calculation
    this.marginCalculationSubscription = this.marginCalculationTrigger.pipe(
      debounceTime(500) // Wait for 500ms pause before calculating margin
    ).subscribe(() => {
      this.performMarginCalculation();
    });

    // Subscribe to basket communication service to listen for instruments added from watchlist
    this.basketCommunicationSubscription = this.basketCommunicationService.addToBasket$.subscribe(
      (instrument: any) => {
        this.addToBasket(instrument);
      }
    );

    this.fetchTradingAccounts();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.wsSubscription?.unsubscribe();
    this.marginCalculationSubscription?.unsubscribe();
    this.basketCommunicationSubscription?.unsubscribe();
    if (this.instrumentTokens.length > 0) {
      this.ws.unsubscribe(this.instrumentTokens);
    }

    // Save basket items to cache on destroy
    this.saveBasketItems();
  }

  fetchTradingAccounts(): void {
    this.isLoading = true;
    this.tradingAccountService.getTradingAccounts().subscribe({
      next: (response) => {
        this.tradingAccounts = response.data;
        if (this.tradingAccounts.length > 0) {
          this.selectedTradingAccountId = this.tradingAccounts[0].userId;
          this.fetchFundsData(); // Fetch funds data for the first trading account

          // Load basket items from cache after trading account is set
          this.loadBasketItems();
          this.isLoading = false;
        }
      },
      error: (error) => {
        console.error('Error while fetching trading accounts:', error);
        this.toastr.error(error.error?.message || 'Failed to fetch trading accounts', 'Error');
      }
    });
  }

  fetchFundsData(): void {
    if (!this.selectedTradingAccountId) {
      return;
    }

    this.fundsService.getFunds().subscribe({
      next: (response) => {
        this.funds = response.data || [];
        this.updateMarginValues();
      },
      error: (error) => {
        console.error('Error while fetching funds data:', error);
        this.toastr.error(error.error?.message || 'Failed to fetch funds data', 'Error');
      }
    });
  }

  updateMarginValues(): void {
    // Filter funds for the selected trading account
    const selectedFund = this.funds.find(fund => fund.userId === this.selectedTradingAccountId);

    if (selectedFund) {
      this.utilizedMargin = selectedFund.margin?.utilised?.debits || 0;
      this.availableMargin = selectedFund.margin?.net || 0;
    } else {
      this.utilizedMargin = 0;
      this.availableMargin = 0;
    }
  }

  onSearchTermChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const inputValue = target?.value || '';
    this.searchTermChanged.next(inputValue); // Emit the search term
  }

  searchInstruments(term: string): void {
    if (!term.trim()) {
      this.fetchedInstruments = [];
      return;
    }

    this.watchlistService.searchInstruments(term.trim()).subscribe({
      next: (response) => {
        this.fetchedInstruments = response.data;
      },
      error: (error) => {
        console.error('Error while fetching instruments:', error);
        this.toastr.error(error.error.message, 'Error');
      }
    });
  }

  addToBasket(instrument: any): void {
    // Check if maximum limit is reached
    if (this.basketItems.length >= 6) {
      this.toastr.warning('Maximum 6 instruments allowed in basket', 'Limit Reached');
      return;
    }

    this.searchTerm = ''; // Clear search term
    this.fetchedInstruments = []; // Clear fetched instruments after adding

    // Get cached quantity for this exchange
    const cachedQty = this.getCachedQuantity(instrument.exchange);

    const basketItem = {
      displayName: instrument.displayName,
      tradingSymbol: instrument.tradingSymbol,
      instrumentToken: instrument.instrumentToken,
      segment: instrument.segment,
      exchange: instrument.exchange,
      quantity: cachedQty,
      orderType: 'LIMIT',
      transactionType: 'SELL',
      price: 0,
      marginRequired: 0,
      lastPrice: 0,
      change: 0,
      priceUpdated: false, // Flag to track if price has been set from LTP
      isIntraday: this.isIntraday, // Set the intraday flag based on component state
      productType: 'NRML'
    };
    basketItem.productType = this.getProductType(basketItem);
    this.basketItems.push(basketItem);
    this.subscribeToInstrument(instrument.instrumentToken);
    this.calculateBasketMargin();
    this.calculateNetPremium();
  }

  removeFromBasket(index: number): void {
    const item = this.basketItems[index];
    this.basketItems.splice(index, 1);
    this.unsubscribeFromInstrument(item.instrumentToken);
    if (this.basketItems.length > 0) {
      this.calculateBasketMargin();
      this.calculateNetPremium();
    } else {
      this.requiredMargin = 0;
      this.finalMargin = 0;
      this.netPremium = 0;
    }
  }

  toggleTransactionType(item: any): void {
    item.transactionType = item.transactionType === 'BUY' ? 'SELL' : 'BUY';
    this.calculateBasketMargin();
    this.calculateNetPremium();
  }

  toggleOrderType(item: any): void {
    if (item.orderType === 'MARKET') {
      item.orderType = 'LIMIT';
    } else if (item.orderType === 'LIMIT') {
      item.orderType = 'MARKET';
      item.price = 0; // Reset price for market orders
    }
    this.calculateBasketMargin();
    this.calculateNetPremium();
  }

  updateQuantity(item: any, event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = parseInt(target.value) || 1;
    item.quantity = Math.max(1, value);
    // Cache the quantity for this exchange
    this.cacheQuantity(item.exchange, item.quantity);
    this.calculateBasketMargin();
    this.calculateNetPremium();
  }

  updatePrice(item: any, event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = parseFloat(target.value) || 0;
    item.price = Math.max(0, value);
    this.calculateBasketMargin();
    this.calculateNetPremium();
  }

  calculateBasketMargin(): void {
    // Trigger the debounced margin calculation
    this.marginCalculationTrigger.next();
  }

  private performMarginCalculation(): void {
    if (this.basketItems.length === 0) {
      return;
    }
    if (!this.selectedTradingAccountId) {
      this.toastr.warning('Please select a trading account', 'Warning');
      return;
    }
    // Prepare the payload in CombinedMarginCalculationRequest format
    const marginCalculationParams = this.basketItems.map(item => ({
      exchange: item.exchange,
      tradingSymbol: item.tradingSymbol,
      transactionType: item.transactionType,
      variety: 'regular',
      product: this.getProductType(item),
      orderType: item.orderType,
      quantity: item.quantity,
      price: item.orderType === 'MARKET' ? 0 : item.price
    }));

    const payload = {
      tradingAccountId: this.selectedTradingAccountId,
      marginCalculationParams: marginCalculationParams,
      includeExistingPositions: this.includeExistingPositions
    };

    this.fundsService.calculateCombinedMargin(payload).subscribe({
      next: (response) => {
        const marginData: any = response.data;

        // Update individual margins
        if (marginData?.orders && Array.isArray(marginData.orders)) {
          marginData.orders.forEach((orderMargin: any, index: number) => {
            if (this.basketItems[index]) {
              this.basketItems[index].marginRequired = orderMargin.total || 0;
            }
          });
        }

        // Update total margin
        this.requiredMargin = marginData?.initialMargin?.total || 0;
        this.finalMargin = marginData?.finalMargin?.total  || 0;
        this.finalMargin = this.finalMargin > 0 ? this.finalMargin : 0;
        this.isCalculatingMargin = false;
      },
      error: (error) => {
        console.error('Error while calculating margin:', error);
        this.toastr.error(error.error?.message || 'Failed to calculate margin', 'Error');
        this.isCalculatingMargin = false;
      }
    });
  }

  private getProductType(item: any): string {
    if (item.isIntraday) {
      return 'MIS';
    } else {
      // Overnight: determine based on exchange
      if (item.segment === 'NFO-OPT' || item.segment === 'NFO-FUT' || item.segment === 'BFO-OPT' || item.segment === 'BFO-FUT') {
        return 'NRML';
      } else {
        return 'CNC';
      }
    }
  }

  onIntradayChange(): void {
    // Update all existing basket items when global intraday setting changes
    this.basketItems.forEach(item => {
      item.isIntraday = this.isIntraday;
      item.productType = this.getProductType(item);
    });
    this.calculateBasketMargin();
  }

  subscribeToInstrument(instrumentToken: number): void {
    this.instrumentTokens.push(instrumentToken);

    this.ws.connectionState().pipe(
      filter(c => c),
      take(1)
    ).subscribe(() => {
      this.ws.subscribe([instrumentToken]);
    });

    if (!this.wsSubscription) {
      this.wsSubscription = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updateBasketItemsOnTick(ticks);
      });
    }
  }

  unsubscribeFromInstrument(instrumentToken: number): void {
    const index = this.instrumentTokens.indexOf(instrumentToken);
    if (index > -1) {
      this.instrumentTokens.splice(index, 1);
      this.ws.unsubscribe([instrumentToken]);
    }
  }

  updateBasketItemsOnTick(ticks: any[]): void {
    ticks.forEach(tick => {
      const matchedItems = this.basketItems.filter(
        item => item.instrumentToken === tick.instrumentToken
      );

      matchedItems.forEach(item => {
        if (item.lastPrice !== tick.lastTradedPrice) {
          item.lastPrice = tick.lastTradedPrice;
          item.change = tick.change;

          // Auto-fill price on first update if order type is LIMIT and price not yet updated
          if (!item.priceUpdated && item.orderType === 'LIMIT' && tick.lastTradedPrice > 0) {
            item.price = tick.lastTradedPrice;
            item.priceUpdated = true;
            this.calculateBasketMargin();
            this.calculateNetPremium();
          }
        }
      });
    });
  }

  getCachedQuantity(exchange: string): number {
    const cacheKey = `basket_qty_${this.selectedTradingAccountId}_${exchange}`;
    const cached = localStorage.getItem(cacheKey);
    return cached ? parseInt(cached) : 1;
  }

  cacheQuantity(exchange: string, quantity: number): void {
    const cacheKey = `basket_qty_${this.selectedTradingAccountId}_${exchange}`;
    localStorage.setItem(cacheKey, quantity.toString());
  }

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  trackById(index: number, item: any): any {
    return item.instrumentToken + item.exchange;
  }

  private getDefaultVariety(): string {
    const now = new Date();
    const start = new Date();
    start.setHours(9, 0, 0, 0); // 09:00:00.000
    const cutoff = new Date();
    cutoff.setHours(15, 30, 0, 0); // 15:30:00.000
    const isWeekend = now.getDay() === 0 || now.getDay() === 6;
    return (now < start || now > cutoff || isWeekend) ? 'amo' : 'regular';
  }

  /**
   * Validates the basket items before placing orders
   * Returns true if validation passes, false otherwise
   */
  private validateBasketOrders(): boolean {
    if (this.basketItems.length === 0) {
      this.toastr.error('Please add at least one instrument to the basket', 'Error');
      return false;
    }

    if (!this.selectedTradingAccountId) {
      this.toastr.error('Please select a trading account', 'Error');
      return false;
    }

    // Validate each basket item
    for (const item of this.basketItems) {
      if (!item.tradingSymbol || !item.exchange) {
        this.toastr.error('All instruments must have valid trading symbol and exchange', 'Error');
        return false;
      }

      if (item.quantity <= 0) {
        this.toastr.error(`Quantity must be greater than 0 for ${item.displayName}`, 'Error');
        return false;
      }

      if (item.orderType === 'LIMIT' && item.price <= 0) {
        this.toastr.error(`Price must be greater than 0 for LIMIT order on ${item.displayName}`, 'Error');
        return false;
      }
    }

    return true;
  }

  /**
   * Places orders for all instruments in the basket
   * Uses the same logic as the add-manage-order-dialog onSubmit method for non-modify type
   */
  placeBasketOrders(): void {
    // Validate before proceeding
    if (!this.validateBasketOrders()) {
      return;
    }

    const orderRequests: OrderRequest[] = [];
    const variety = this.getDefaultVariety();

    // Convert each basket item to an order request
    this.basketItems.forEach((item: any) => {
      const orderParams: OrderParams = {
        quantity: item.quantity,
        orderType: item.orderType,
        tradingsymbol: item.tradingSymbol,
        product: this.getProductType(item),
        exchange: item.exchange,
        transactionType: item.transactionType,
        validity: 'DAY',
        price: item.orderType === 'MARKET' ? 0 : item.price,
        triggerPrice: 0, // Basket orders don't support trigger price
        tag: 'famvest-basket-order',
      };

      orderRequests.push({
        tradingAccountId: this.selectedTradingAccountId,
        orderParams: orderParams
      });
    });

    // Place orders sequentially using the same pattern as add-manage-order-dialog
    from(orderRequests).pipe(
      concatMap(orderRequest =>
        this.ordersService.placeOrder(orderRequest, variety).pipe(
          catchError(err => {
            return of({
              error: true,
              id: orderRequest.tradingAccountId,
              message: err?.error?.message || 'Unknown error',
              instrument: orderRequest.orderParams.tradingsymbol
            } as ApiErrorResponse & { instrument: string });
          })
        )
      )
    ).subscribe({
      next: (result: any) => {
        if ('error' in result && result.error) {
          console.error(`❌ Order placement failed for ${result.instrument}:`, result);
          this.toastr.error(
            `Order placement for ${result.instrument} failed: ${result.message}`,
            'Error'
          );
        } else {
          console.log(`✅ Order placed successfully for ${result.data.tradingSymbol}`);
          this.toastr.success(
            `Order placed successfully. Order ID: ${result.data.orderId}`,
            'Success'
          );
        }
      },
      error: (err) => {
        // This only triggers for stream-breaking (non-caught) errors
        console.error('Fatal error:', err);
        this.toastr.error('Something went wrong in the processing pipeline.', 'Fatal Error');
      },
      complete: () => {
        console.log('✅ All basket orders processed');
        this.toastr.info('All basket orders have been processed', 'Complete');
        // Optionally clear the basket after successful order placement
        // this.clearBasket();
      }
    });
  }

  clearBasket(): void {
    // Unsubscribe from all instruments
    this.basketItems.forEach(item => {
      this.unsubscribeFromInstrument(item.instrumentToken);
    });

    // Clear the basket
    this.basketItems = [];
    this.requiredMargin = 0;
    this.finalMargin = 0;

    // Clear cache
    localStorage.removeItem(this.BASKET_CACHE_KEY);
  }

  calculateNetPremium(): void {
    this.netPremium = this.basketItems.reduce((total, item) => {
      const itemPremium = (item.quantity || 0) * (item.price || 0);

      if (item.transactionType === 'BUY') {
        return total + itemPremium; // Money going out (positive)
      } else {
        return total - itemPremium; // Money coming in (negative)
      }
    }, 0);
  }

  getAbsoluteNetPremium(): number {
    return Math.abs(this.netPremium);
  }

  isNetDebit(): boolean {
    return this.netPremium > 0;
  }

  isNetCredit(): boolean {
    return this.netPremium < 0;
  }

  private saveBasketItems(): void {
    const cachedItems = this.basketItems.map(item => ({
      displayName: item.displayName,
      tradingSymbol: item.tradingSymbol,
      instrumentToken: item.instrumentToken,
      segment: item.segment,
      exchange: item.exchange,
      quantity: item.quantity,
      orderType: item.orderType,
      transactionType: item.transactionType,
      price: item.price,
      marginRequired: item.marginRequired,
      lastPrice: item.lastPrice,
      change: item.change,
      priceUpdated: item.priceUpdated,
      isIntraday: item.isIntraday,
      productType: item.productType
    }));

    localStorage.setItem(this.BASKET_CACHE_KEY, JSON.stringify(cachedItems));
  }

  private loadBasketItems(): void {
    const cached = localStorage.getItem(this.BASKET_CACHE_KEY);
    if (cached) {
      try {
        const parsedItems = JSON.parse(cached);
        this.basketItems = parsedItems;
        if (this.basketItems.length > 0) {
          this.isIntraday = this.basketItems[0].isIntraday || false;
        }
        // Re-subscribe to instruments for loaded basket items
        this.basketItems.forEach(item => {
          this.subscribeToInstrument(item.instrumentToken);
        });
        // Calculate initial margin and net premium
        this.calculateBasketMargin();
        this.calculateNetPremium();
      } catch (e) {
        console.error('Error parsing cached basket items:', e);
        localStorage.removeItem(this.BASKET_CACHE_KEY); // Clear invalid cache
      }
    }
  }

  get totalMargin(): number {
    return Number(this.utilizedMargin) + Number(this.availableMargin);
  }
}
