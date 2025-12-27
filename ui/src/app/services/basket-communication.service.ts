import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Service to handle communication between watchlist and basket components
 * Allows watchlist to send instruments to basket
 */
@Injectable({
  providedIn: 'root'
})
export class BasketCommunicationService {
  private addToBasketSubject = new Subject<any>();

  // Observable that basket component can subscribe to
  addToBasket$ = this.addToBasketSubject.asObservable();

  constructor() { }

  /**
   * Emit an instrument to be added to basket
   * @param instrument The instrument object to add to basket
   */
  emitAddToBasket(instrument: any): void {
    this.addToBasketSubject.next(instrument);
  }
}

