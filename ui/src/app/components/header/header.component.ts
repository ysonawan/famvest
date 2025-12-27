import {Component, ElementRef, HostListener, Inject, OnDestroy, OnInit, ViewChild, AfterViewInit} from '@angular/core';
import {Router, RouterModule} from "@angular/router";
import {CommonModule, DOCUMENT, NgOptimizedImage} from "@angular/common";
import {AuthUserService} from "../../services/auth/auth-user.service";
import {IndexTickerComponent} from "../shared/index-ticker/index-ticker.component";
import {WebSocketService} from "../../services/web-socket.service";
import {IndexQuote} from "../../models/index-quote";
import {filter, Subscription, take} from "rxjs";
import {ActiveToast, ToastrService} from "ngx-toastr";
import {
  faChevronDown,
  faSignOutAlt,
  faUserTie,
  faCircleExclamation,
  faCircle, faHome, faCartPlus,
  faWallet, faChartLine, faListCheck, faStar, faGear
} from "@fortawesome/free-solid-svg-icons";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {UserDataStateService} from "../../services/user-data-state-service";
import {MatDialog} from "@angular/material/dialog";
import {Overlay} from "@angular/cdk/overlay";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-header',
  imports: [RouterModule, NgOptimizedImage, CommonModule, IndexTickerComponent, FaIconComponent, MatTooltip],
  templateUrl: './header.component.html',
  standalone: true,
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('header', { static: true }) headerElement!: ElementRef;

  instrumentTokens: number[] = [];
  private sub?: Subscription;
  private orderSub?: Subscription;

  indexTickers: IndexQuote[] = [
    {
      symbol: 'NIFTY 50', instrumentToken: 256265,
      lastTradedPrice: 0,
      change: 0,
      percentChange: 0
    },
    { symbol: 'SENSEX', instrumentToken: 265,
      lastTradedPrice: 0,
      change: 0,
      percentChange: 0
    }
  ];
  showTickers: boolean = true;
  constructor(private authUserService: AuthUserService,
              private ws: WebSocketService,
              private toastrService: ToastrService,
              private router: Router,
              private userDataStateService: UserDataStateService,
              private dialog: MatDialog,
              @Inject(DOCUMENT) private document: Document,
              private overlay: Overlay) {

    this.instrumentTokens = this.indexTickers.map((quote) => quote.instrumentToken);
  }

  lastFeedTimestamp: number = 0;
  lastFeedChangedTimestamp: number = 0;
  feedStatus: 'live' | 'idle' | 'disconnected' = 'idle';

  checkFeedStatus() {
    const now = Date.now();
    const idleThreshold = 5 * 1000;          // No data change for this time = idle
    const disconnectThreshold = 60 * 1000;    // No data received at all = disconnected

    if (this.lastFeedTimestamp === 0) {
      // Never received any feed — disconnected
      this.feedStatus = 'disconnected';
      return;
    }

    const timeSinceLastUpdate = now - this.lastFeedTimestamp;
    const timeSinceLastChange = now - this.lastFeedChangedTimestamp;

    if (timeSinceLastUpdate > disconnectThreshold) {
      // No update received at all recently → treat as disconnected
      this.feedStatus = 'disconnected';
    } else if (timeSinceLastChange > idleThreshold) {
      // Updates are received, but no change in data → idle
      this.feedStatus = 'idle';
    } else {
      // Receiving and processing data → live
      this.feedStatus = 'live';
    }
  }

  isAdmin() {
    return this.authUserService.isAdmin();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0 && userDataState.indexTickers) {
      this.indexTickers = userDataState.indexTickers;
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      indexTickers: this.indexTickers
    });
  }

  statusCheckInterval: any;

  ngOnInit(): void {
    this.getCachedData();
    this.subscribeToWebSocket();
    this.statusCheckInterval = setInterval(() => {
      this.checkFeedStatus();
    }, 10000); // check every 10 seconds
  }

  ngAfterViewInit(): void {
    // Initial height calculation
    this.updateHeaderHeight();
    // Use setInterval to check every 100ms for up to 20 times (2 seconds total)
    this.startPeriodicHeightCheck();
  }

  private headerHeightUpdateTimeout: any;
  private heightCheckAttempts = 0;
  private maxHeightCheckAttempts = 20;
  private heightCheckInterval: any;

  private startPeriodicHeightCheck(): void {
    this.heightCheckAttempts = 0;
    this.heightCheckInterval = setInterval(() => {
      this.heightCheckAttempts++;
      this.updateHeaderHeight();

      // Stop checking only when we've reached max attempts
      if (this.heightCheckAttempts >= this.maxHeightCheckAttempts) {
        clearInterval(this.heightCheckInterval);
      }
    }, 100);
  }

  private updateHeaderHeight(): void {
    if (this.headerElement?.nativeElement) {
      const headerHeight = this.headerElement.nativeElement.offsetHeight;

      // Additional validation for production environments
      if (headerHeight === 0) {
        // Element not yet rendered, continue with interval checks
        return;
      }

      // Only update if the height has changed significantly (prevents constant updates)
      const currentHeight = parseInt(
        getComputedStyle(this.document.documentElement).getPropertyValue('--header-height') || '43'
      );

      if (Math.abs(headerHeight - currentHeight) > 1) {
        this.document.documentElement.style.setProperty('--header-height', `${headerHeight}px`);
        console.log(`Header height updated: ${headerHeight}px (attempt ${this.heightCheckAttempts})`);
      }
    }
  }

  disconnect(): void {
    this.ws.unsubscribe(this.instrumentTokens);
  }

  ngOnDestroy(): void {
    if (this.statusCheckInterval) {
      clearInterval(this.statusCheckInterval);
    }
    if (this.headerHeightUpdateTimeout) {
      clearTimeout(this.headerHeightUpdateTimeout);
    }
    if (this.heightCheckInterval) {
      clearInterval(this.heightCheckInterval);
    }
    this.ws.unsubscribe(this.instrumentTokens);
    this.sub?.unsubscribe();
    this.orderSub?.unsubscribe();
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
        this.updateIndexTickerOnUpdate(ticks);
      });
      this.orderSub = this.ws.orderUpdates().subscribe((order: any) => {
        console.log('Received order update:', order);
        this.playOrderNotificationSound();
        const toast: ActiveToast<any> = this.toastrService.info(`Account ID:${order.user_id}. ${order.transaction_type} order with
        quantity ${order.quantity} and price ${order.price} is ${order.status}. Order ID:${order.order_id}.`, `Order Update`, {
          timeOut: 0,
          extendedTimeOut: 0,
          closeButton: true,
          tapToDismiss: true
        });
        const activeRoute = this.router.url;
        if (activeRoute.includes('/holdings')) {
          this.router.navigateByUrl('/refresh', { skipLocationChange: true }).then(() => {
            this.router.navigate(['/holdings']);
          });
        } else if (activeRoute.includes('/orders')) {
          this.router.navigateByUrl('/refresh', { skipLocationChange: true }).then(() => {
            this.router.navigate(['/orders']);
          });
        } else if (activeRoute.includes('/positions')) {
          this.router.navigateByUrl('/refresh', { skipLocationChange: true }).then(() => {
            this.router.navigate(['/positions']);
          });
        } else {
          console.log('No matching route found for refresh.');
        }
        toast.onTap.subscribe(() => {

        });
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  updateIndexTickerOnUpdate(ticks: any[]): void {
    // Update the LTP of the position based on the tick received
    ticks.forEach(tick => {
      this.lastFeedTimestamp = Date.now(); // store timestamp of last update
      const matchedIndices: any[] = this.indexTickers.filter(i => i.instrumentToken === tick.instrumentToken);
      matchedIndices.forEach(index => {
        this.showTickers = true;
        if (index && index.lastTradedPrice !== tick.lastTradedPrice) {
          this.lastFeedChangedTimestamp = Date.now(); // store timestamp of last update when value is changed
          this.feedStatus = 'live'; // update status immediately
          index.lastTradedPrice = tick.lastTradedPrice;
          index.change = tick.lastTradedPrice - tick.closePrice;
          index.percentChange = tick.change;
        }
      });
    });
    this.setCachedData();
  }

  showMoreMenu = false;

  @ViewChild('menuTrigger') menuTrigger!: ElementRef;
  @ViewChild('profileTrigger') profileTrigger!: ElementRef;

  closeMoreMenu() {
    this.showMoreMenu = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const menuTriggerEl = this.menuTrigger?.nativeElement;
    const menuEl = menuTriggerEl?.nextElementSibling;
    if (!menuTriggerEl || !menuEl) return;
    if (this.showMoreMenu) {
      if (!menuTriggerEl.contains(event.target) && !menuEl.contains(event.target)) {
        this.closeMoreMenu();
      }
    }
  }

  toggleMoreMenu() {
    this.showMoreMenu = !this.showMoreMenu;
  }

  protected readonly faSignOutAlt = faSignOutAlt;
  logout() {
    this.closeMoreMenu();
    this.authUserService.logout();
    this.router.navigate(['/login'], { queryParams: { reason: 'logged-out' } });
  }

  protected readonly faChevronDown = faChevronDown;
  protected readonly faUserTie = faUserTie;
  protected readonly faCircleExclamation = faCircleExclamation;
  protected readonly faCircle = faCircle;
  protected readonly faHome = faHome;
  protected readonly faCartPlus = faCartPlus;
  protected readonly faWallet = faWallet;
  protected readonly faChartLine = faChartLine;
  protected readonly faListCheck = faListCheck;
  protected readonly faStar = faStar;
  protected readonly faGear = faGear;

  private playOrderNotificationSound(): void {
    try {
      const audioContext = new (window as any).AudioContext || new (window as any).webkitAudioContext();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();
      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);
      // Set frequency and duration for a short beep - higher pitch for pleasant notification
      oscillator.frequency.value = 1200; // Frequency in Hz (higher pitch)
      oscillator.type = 'sine';
      // Set volume
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.15);
      // Play the sound (200ms duration)
      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + 0.2);
    } catch (error) {
      console.error('Error playing notification sound:', error);
    }
  }
}
