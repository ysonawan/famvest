import {AfterViewInit, Component, EventEmitter, HostListener, Inject, OnDestroy, OnInit, Output} from '@angular/core';
import {debounceTime, distinctUntilChanged, filter, Subject, Subscription, take} from "rxjs";
import {DecimalPipe, DOCUMENT, NgClass, NgForOf, NgIf, PercentPipe} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {WatchlistService} from "../../services/watchlist.service";
import {WebSocketService} from "../../services/web-socket.service";
import {ToastrService} from "ngx-toastr";
import {AddManageOrderDialogComponent} from "../dialogs/add-position-dialog/add-manage-order-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {faSearch} from "@fortawesome/free-solid-svg-icons";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ActionButtonComponent} from "../shared/action-button/action-button.component";
import {TradingAccountService} from "../../services/trading-account.service";
import {AlertService} from "../../services/alert.service";
import {ActivatedRoute, Router} from "@angular/router";
import {Overlay} from "@angular/cdk/overlay";
import {UserDataStateService} from "../../services/user-data-state-service";
import {expandCollapseAnimation, listItemAnimation} from "../shared/animations";
import {tradingViewUrl} from "../../constants/constants";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {MarketDepthInvokerComponent} from "../shared/market-depth-invoker/market-depth-invoker.component";
import {MatTooltip} from "@angular/material/tooltip";
import { BasketCommunicationService } from "../../services/basket-communication.service";

@Component({
  selector: 'app-watchlist',
  templateUrl: './watchlist.component.html',
  styleUrls: ['./watchlist.component.css'],
  standalone: true,
  animations: [expandCollapseAnimation, listItemAnimation],
  imports: [NgForOf, FormsModule, NgIf, ActionButtonComponent, NgClass, FaIconComponent, DecimalPipe, PercentPipe, MarketDepthInvokerComponent, MatTooltip]
})
export class WatchlistComponent implements OnInit, OnDestroy, AfterViewInit {

  isInsideRouterOutlet = false;

  searchTermChanged: Subject<string> = new Subject<string>();
  searchTerm: string = '';
  watchlist: any[] = [];

  instrumentTokens: number[] = [];
  private sub?: Subscription;

  errorMessage = '';

  currentListIndex = 0;
  hoveredIndex: number | null = null;
  chartSymbol: string | null = null;
  fetchedInstruments: any[] = [];

  isEditingName = false;
  editWatchlistName = '';
  isNameHovered = false;

  dragStartIndex: number | null = null;
  dragOverIndex: number | null = null;

  readonly reservedNames = [
    'Holdings',
    'Positions',
    'Nifty Current Week Expiry',
    'Nifty Next Week Expiry',
    'Sensex Current Week Expiry',
    'Sensex Next Week Expiry'
  ];

  constructor(private watchlistService: WatchlistService,
              private ws: WebSocketService,
              private toastr: ToastrService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              @Inject(MatDialog) private dialog: MatDialog,
              private tradingAccountService: TradingAccountService,
              private alertService: AlertService,
              private router: Router,
              private route: ActivatedRoute,
              private overlay: Overlay,
              @Inject(DOCUMENT) private document: Document,
              private basketCommunicationService: BasketCommunicationService) {
  }

  ngOnInit(): void {
    this.isMobileView = (globalThis as any).window?.innerWidth <= 768 || false;
    this.route.data.subscribe(data => {
      this.isInsideRouterOutlet = data['standaloneWatchlist'] || false;
    });
    const userViewState = this.userViewStateService.getState();
    this.isWatchlistCollapsed = userViewState.watchlist.isCollapsed || false;
    this.collapseChanged.emit(this.isWatchlistCollapsed);
    this.currentListIndex = userViewState.watchlist.activeWatchlist;
    // Set up the debounce logic
    this.searchTermChanged.pipe(
      debounceTime(300), // Wait for 300ms pause in events
      distinctUntilChanged() // Only emit if the value has changed
    ).subscribe(term => {
      this.searchInstruments(term);
    });
    this.getCachedData();
    this.fetchWatchlist();
    this.informUserToOnboardTradingAccount();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0 && userDataState.watchlist) {
      this.watchlist = userDataState.watchlist;
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      watchlist: this.watchlist
    });
  }

  get currentList(): any[] {
    let watchlistInstruments: any[] = [];
    if(this.watchlist.length > 0) {
       watchlistInstruments = this.watchlist[this.currentListIndex].watchlistInstruments;
    }
    return watchlistInstruments;
  }

  get isCurrentWatchlistReserved(): boolean {
    const name = this.getWatchListName();
    return this.reservedNames.includes(name);
  }

  openPlaceOrderDialog(side: string, instrument: any): void {
    const sourceData = {
      isBuyMode: side === 'BUY',
    }
    this.dialog.open(AddManageOrderDialogComponent, {
      disableClose: true,
      autoFocus: true,
      hasBackdrop: false,
      closeOnNavigation: false,
      scrollStrategy: this.overlay.scrollStrategies.noop(),
      data: { sourceData: sourceData, instrument: { ...instrument } }
    });
  }

  switchList(index: number) {
    this.ws.unsubscribe(this.instrumentTokens).subscribe({
      next: () => {
        this.currentListIndex = index;
        this.subscribeToWebSocket();
        this.chartSymbol = null;
        this.saveUserViewState();
      }
    });
  }

  trackById(index: number, item: any): any {
    return item.id;
  }

  addToWatchlist(instrument: any) {
    let watchlist =  this.watchlist[this.currentListIndex];
    this.searchTerm = ''; // Clear search term
    const existingInstrument = watchlist.watchlistInstruments.find((item: any) => (item.tradingSymbol === instrument.tradingSymbol && item.exchange === instrument.exchange));
    if(!existingInstrument) {
      let watchlistInstrument = {
        watchlistId : watchlist.id,
        displayName : instrument.displayName,
        tradingSymbol : instrument.tradingSymbol,
        instrumentToken : instrument.instrumentToken,
        segment : instrument.segment,
        exchange : instrument.exchange
      }
      this.fetchedInstruments = []; // Clear fetched instruments after adding
      this.saveWatchlistInstrument(watchlist, watchlistInstrument);
    } else {
      this.toastr.warning('Instrument already exists in watchlist', 'Warning');
    }
  }

  deleteFromWatchlist(instrument: any) {
    let watchlist =  this.watchlist[this.currentListIndex];
    const existingInstrument = watchlist.watchlistInstruments.find((item: any) => (item.tradingSymbol === instrument.tradingSymbol && item.exchange === instrument.exchange));
    if(existingInstrument) {
      //remove instrument from watchlist
      watchlist.watchlistInstruments = watchlist.watchlistInstruments.filter((item: any) => !(item.tradingSymbol === instrument.tradingSymbol && item.exchange === instrument.exchange));
      this.deleteWatchlistInstrument(this.watchlist[this.currentListIndex], existingInstrument.id);
    } else {
      this.toastr.warning('Instrument does not exists in watchlist', 'Warning');
    }
  }

  fetchWatchlist(): void {
    this.watchlistService.getWatchlist().subscribe({
      next: (response) => {
        this.watchlist = response.data;
        this.subscribeToWebSocket();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastr.error(error.error.message, 'Error');
        } else {
          this.toastr.error('An unexpected error occurred while fetching watchlist. Verify that the backend service is operational.', 'Error');
        }
        console.error('Error while fetching watchlist:', error);
      }
    });
  }

  subscribeToWebSocket(): void {
    //get instrument tokens from watchlist
    const watchlistInstruments = this.watchlist[this.currentListIndex].watchlistInstruments;
    this.instrumentTokens = watchlistInstruments.map((instrument: { instrumentToken: any; }) => instrument.instrumentToken);
    const BATCH_SIZE = 50;
    if (this.instrumentTokens.length > 0) {
      this.ws.connectionState().pipe(
        filter(c => c), // only when connected = true
        take(1)
      ).subscribe(() => {
        for (let i = 0; i < this.instrumentTokens.length; i += BATCH_SIZE) {
          const batch = this.instrumentTokens.slice(i, i + BATCH_SIZE);
          this.ws.subscribe(batch);
        }
      });
      this.sub = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updateWatchlistOnUpdate(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  unsubscribeToWebSocket(): void {
    const BATCH_SIZE = 50;
    if (this.instrumentTokens.length > 0) {
      for (let i = 0; i < this.instrumentTokens.length; i += BATCH_SIZE) {
        const batch = this.instrumentTokens.slice(i, i + BATCH_SIZE);
        this.ws.unsubscribe(batch);
      }
    }
  }

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'delete') {
      this.deleteFromWatchlist(event.row);
    } else if(event.action === 'chart') {
      let exchange = event.row.exchange;
      let instrument = event.row.tradingSymbol;
      instrument = instrument === 'NIFTY 50' ? 'NIFTY' : instrument;
      (globalThis as any).window?.open(tradingViewUrl(exchange, instrument), '_blank', 'noopener,noreferrer');
    } else if(event.action === 'buy') {
      this.openPlaceOrderDialog('BUY', event.row);
    } else if(event.action === 'sell') {
      this.openPlaceOrderDialog('SELL', event.row);
    }
  }

  addToBasketOrder(instrument: any): void {
    this.basketCommunicationService.emitAddToBasket(instrument);
  }

  updateWatchlistOnUpdate(ticks: any[]): void {
    // Update the LTP of the watchlist instruments based on the tick received
    ticks.forEach(tick => {
      const watchlistInstruments = this.watchlist[this.currentListIndex].watchlistInstruments;
      const matchedInstruments: any[] = watchlistInstruments.filter((wl: { instrumentToken: any; }) => wl.instrumentToken === tick.instrumentToken);
      matchedInstruments.forEach(instrument => {
        if (instrument && instrument.lastPrice !== tick.lastTradedPrice) {
          instrument.change = tick.change;
          instrument.changeAbs = (tick.change * tick.closePrice) / 100;
          instrument.lastPrice = tick.lastTradedPrice;
          instrument.tradable = tick.tradable;
        }
      });
    });
  }

  saveWatchlistInstrument(watchlist: any, watchlistInstrument: any): void {
    console.log('saving watchlist for user');
    this.watchlistService.saveWatchListInstrument(watchlist.id, watchlistInstrument).subscribe({
      next: (response) => {
        this.watchlist = response.data;
        this.subscribeToWebSocket();
      },
      error: (error) => {
        console.error('Error while saving watchlist:', error);
        this.toastr.error(error.error.message, 'Error');
      }
    });
  }

  deleteWatchlistInstrument(watchlist: any, watchlistInstrumentId: number): void {
    console.log('deleting watchlist instrument for user');
    this.watchlistService.deleteWatchListInstrument(watchlist.id, watchlistInstrumentId).subscribe({
      next: (response) => {
        this.watchlist = response.data;
        this.subscribeToWebSocket();
      },
      error: (error) => {
        console.error('Error while saving watchlist:', error);
        this.toastr.error(error.error.message, 'Error');
      }
    });
  }

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  onWatchlistNameMouseEnter() {
    this.isNameHovered = true;
  }

  onWatchlistNameMouseLeave() {
    this.isNameHovered = false;
  }

  onWatchlistNameClick() {
    if (this.isCurrentWatchlistReserved) {
      this.toastr.warning('This watchlist name cannot be edited.', 'Warning');
      return;
    }
    this.isEditingName = true;
    this.editWatchlistName = this.getWatchListName();
    setTimeout(() => {
      const input = this.document.getElementById('edit-watchlist-name-input');
      if (input) (input as HTMLInputElement).focus();
    });
  }

  onWatchlistNameBlur() {
    this.isEditingName = false;
    const trimmed = this.editWatchlistName.trim();
    if (trimmed && trimmed !== this.getWatchListName()) {
      this.saveWatchlistName(trimmed);
    }
  }

  onWatchlistNameInputKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      (event.target as HTMLInputElement).blur();
    }
  }

  saveWatchlistName(newName: string) {
    const watchlist = this.watchlist[this.currentListIndex];
    this.watchlistService.updateWatchlistName(watchlist.id, newName).subscribe({
      next: (_response) => {
        watchlist.name = newName;
        this.toastr.success('Watchlist name updated successfully', 'Success');
      },
      error: (error) => {
        console.error('Error while saving watchlist name:', error);
        this.toastr.error(error.error.message, 'Error');
      }
    });

  }

  onDragStart(event: DragEvent, index: number) {
    this.dragStartIndex = index;
    event.dataTransfer?.setData('text/plain', index.toString());
    event.dataTransfer!.effectAllowed = 'move';
  }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault();
    this.dragOverIndex = index;
  }

  onDrop(event: DragEvent, dropIndex: number) {
    event.preventDefault();
    const dragIndex = this.dragStartIndex;
    if (dragIndex === null || dragIndex === dropIndex) {
      this.dragStartIndex = null;
      this.dragOverIndex = null;
      return;
    }
    const list = this.watchlist[this.currentListIndex].watchlistInstruments;
    const [moved] = list.splice(dragIndex, 1);
    list.splice(dropIndex, 0, moved);
    this.dragStartIndex = null;
    this.dragOverIndex = null;
    this.persistWatchlistOrder();
  }

  onDragEnd() {
    this.dragStartIndex = null;
    this.dragOverIndex = null;
  }

  persistWatchlistOrder() {
    //call a service to persist the new order to backend
    const watchlist = this.watchlist[this.currentListIndex];
    const watchlistInstrumentIds = watchlist.watchlistInstruments.map((item: any) => item.id);
    this.watchlistService.reorderWatchlistInstruments(watchlist.id, watchlistInstrumentIds).subscribe({
      next: (_response) => {
        console.log('Watchlist order saved successfully');
      },
      error: (error) => {
        console.error('Error while saving watchlist order:', error);
        this.toastr.error(error.error.message, 'Error');
      }
    });
  }

  informUserToOnboardTradingAccount() {
    this.tradingAccountService.getTradingAccounts().subscribe({
      next: (response) => {
        const tradingAccounts = response.data;
        if(tradingAccounts.length === 0) {
          this.alertService.info(`Hi there!`,
            `You haven't onboarded any trading account yet. Click OK to go to the Manage Accounts screen and onboard your trading accounts.`, () => {
              this.router.navigate(['/profile']);
            });
        }
      },
      error: (error) => {
        if(error.error.message) {
          this.toastr.error(error.error.message, 'Error');
        } else {
          this.toastr.error('An unexpected error occurred while fetching trading accounts. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  onSearchTermChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const inputValue = target?.value || '';
    this.searchTermChanged.next(inputValue); // Emit the search term
  }

  getWatchListName() {
    if(this.watchlist.length > 0) {
      return this.watchlist[this.currentListIndex].name;
    }
    return '';
  }

  getWatchListCount() {
    if(this.watchlist[this.currentListIndex]?.watchlistInstruments) {
      return this.watchlist[this.currentListIndex].watchlistInstruments.length;
    } else {
      return 0;
    }
  }

  searchInstruments(term: string): void {
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

  saveUserViewState(): void {
    this.userViewStateService.setState({
      watchlist: {
        activeWatchlist: this.currentListIndex,
        isCollapsed: this.isWatchlistCollapsed,
      }
    });
  }

  ngOnDestroy(): void {
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
  }

  @Output() collapseChanged = new EventEmitter<boolean>();
  isWatchlistCollapsed = true;

  toggleCollapse() {
    this.isWatchlistCollapsed = !this.isWatchlistCollapsed;
    this.collapseChanged.emit(this.isWatchlistCollapsed);
    this.saveUserViewState();
  }

  get canCollapse(): boolean {
    return !this.isInsideRouterOutlet;
  }

  expand() {
    this.router.navigate(['/watchlist']);
  }

  protected readonly faSearch = faSearch;


  isMobileView: boolean = false;

  @HostListener('window:resize', ['$event'])
  onResize(_event: Event): void {
    this.isMobileView = (globalThis as any).window?.innerWidth <= 768 || false;
  }

  ngAfterViewInit(): void {
  }

}
