import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {DecimalPipe, NgClass, NgForOf, NgIf, NgStyle, PercentPipe} from "@angular/common";
import {faArrowTrendUp} from "@fortawesome/free-solid-svg-icons";
import {filter, Subscription, take} from "rxjs";
import {WebSocketService} from "../../../services/web-socket.service";
import {IstDatePipe} from "../../shared/pipes/ist-date.pipe";
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from "@angular/material/dialog";
import {CdkDrag, CdkDragHandle} from "@angular/cdk/drag-drop";
import {ActionButtonComponent} from "../../shared/action-button/action-button.component";
import {Overlay} from "@angular/cdk/overlay";
import {InstrumentsService} from "../../../services/instruments.service";
import {ToastrService} from "ngx-toastr";
import {expandCollapseAnimation} from "../../shared/animations";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-market-depth',
  templateUrl: './market-depth.component.html',
  standalone: true,
  imports: [
    NgClass,
    DecimalPipe,
    NgForOf,
    IstDatePipe,
    CdkDrag,
    CdkDragHandle,
    NgIf,
    PercentPipe,
    ActionButtonComponent,
    NgStyle,
    MatTooltip
  ],
  animations: [expandCollapseAnimation]
})
export class MarketDepthComponent implements OnInit, OnDestroy {

  constructor(private ws: WebSocketService,
              private dialogRef: MatDialogRef<MarketDepthComponent>,
              private dialog: MatDialog,
              @Inject(MAT_DIALOG_DATA) public data: { title: string, instrumentToken: number},
              private overlay: Overlay,
              private instrumentsService: InstrumentsService,
              private toastrService: ToastrService) {
    this.instrumentToken = Number(data.instrumentToken);
    this.title = data.title;
  }

  private sub?: Subscription;
  title: string = 'Instrument Name';
  instrumentToken: number = 0;
  instrument: any = {};

  ngOnInit(): void {
    const maxDepth = Math.min(this.marketDepth.buy.length, this.marketDepth.sell.length);
    this.depthLevels = Array.from({ length: maxDepth }, (_, i) => i);
    this.subscribeToWebSocket();
    this.fetchInstrument();
  }

  fetchInstrument(done?: () => void): void {
    this.instrumentsService.getInstrumentByToken(this.instrumentToken).subscribe({
      next: (response) => {
        this.instrument = response.data;
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching instrument. Verify that the backend service is operational.', 'Error');
        }
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  color(value: number): string {
    return value > 0 ? 'text-green-600' : value < 0 ? 'text-red-600' : '';
  }

  marketDepth = {
    buy: [
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0}],
    sell: [
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
      {quantity: 0, price: 0, orders: 0},
    ]
  };
  depthLevels: number[] = [];

  maxBidQty = 0;
  maxOfferQty = 0;
  tick: any = {};


  protected readonly faArrowTrendUp = faArrowTrendUp;

  ngOnDestroy(): void {
    this.ws.unsubscribe([this.instrumentToken]);
    this.sub?.unsubscribe();
    this.instrumentToken = 0;
  }

  subscribeToWebSocket(): void {
    if (this.instrumentToken > 0) {
      this.ws.connectionState().pipe(
        filter(c => c), // only when connected = true
        take(1)
      ).subscribe(() => {
        this.ws.subscribe([this.instrumentToken]);
      });
      this.sub = this.ws.ticks().subscribe((ticks: any[]) => {
        this.updateMarketDepth(ticks);
      });
    } else {
      console.log('No instrument tokens available for web socket subscription.');
    }
  }

  updateMarketDepth(ticks: any[]) {
    ticks.forEach(tick => {
      if(this.instrumentToken === tick.instrumentToken) {
        this.tick = tick;
        if(tick.tradable) {
          this.marketDepth = tick.marketDepth;
          const maxDepth = Math.min(this.marketDepth.buy.length, this.marketDepth.sell.length);
          this.depthLevels = Array.from({ length: maxDepth }, (_, i) => i);
          // For highlighting max quantity
          this.maxBidQty = Math.max(...this.marketDepth.buy.map(b => b.quantity));
          this.maxOfferQty = Math.max(...this.marketDepth.sell.map(s => s.quantity));
        } else {
          this.isSummaryCollapsed = false;
        }
      }
    });
  }

  onClose(): void {
    this.dialogRef.close();
  }

  isSummaryCollapsed = true;

  openOrderDialog(price: number, transactionType: string): void {

    const instrument = {
      exchange: this.instrument.exchange,
      tradingSymbol: this.instrument.tradingSymbol,
      instrumentToken: this.instrument.instrumentToken,
      lastPrice: price,
      displayName : this.instrument.displayName,
      segment: this.instrument.segment || this.instrument.order.exchange,
    }
    const sourceData = {
      isCopy: true,
      isBuyMode: transactionType,
      originalPrice: price,
      originalTriggerPrice: 0
    }
   /* const dialogRef = this.dialog.open(AddManageOrderDialogComponent, {
      disableClose: true,
      autoFocus: true,
      hasBackdrop: false,
      closeOnNavigation: false,
      scrollStrategy: this.overlay.scrollStrategies.noop(),
      data: { sourceData: sourceData, instrument: { ...instrument } }
    });*/
  }

  getPercent(value: number): string {
    const range = this.tick.highPrice - this.tick.lowPrice;
    const percent = ((value - this.tick.lowPrice) / range) * 100;
    return `${percent}%`;
  }

  getMinPercent(a: number, b: number): string {
    const min = Math.min(a, b);
    return this.getPercent(min);
  }

  getWidthBetween(a: number, b: number): string {
    const range = this.tick.highPrice - this.tick.lowPrice;
    const width = (Math.abs(a - b) / range) * 100;
    return `${width}%`;
  }

}
