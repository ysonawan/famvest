import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {BehaviorSubject, map, Observable, of, Subject, timer} from 'rxjs';
import {AuthUserService} from "./auth/auth-user.service";
import {ApplicationPropertiesService} from "../application-properties.service";

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client: Client;
  private connected$ = new BehaviorSubject(false);
  private tick$ = new Subject<any[]>();
  private order$ = new Subject<any>();

  private tokenRefCount: Map<number, number> = new Map();

  constructor(private authUserService: AuthUserService,
              private appProperties: ApplicationPropertiesService) {
    this.client = new Client({
      webSocketFactory: () => {
        const token = authUserService.getToken();
        if(token) {
          const wsUrl = this.appProperties.getConfig().wsUrl;
          return new SockJS(`${wsUrl}?token=${token}`);
        } else {
          throw new Error("Cannot establish WebSocket connection: Token is missing");
        }
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected$.next(true);
        this.client.subscribe('/user/queue/ticks', (m: IMessage) => {
          let ticks= JSON.parse(m.body);
          this.tick$.next(JSON.parse(m.body));
        });

        this.client.subscribe('/user/queue/orders', (m: IMessage) => {
          this.order$.next(JSON.parse(m.body));
        });
      },
      onWebSocketClose: () => this.connected$.next(false),
      debug: (str) => console.debug(str),
    });
    this.client.activate();
  }

  connectionState(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  ticks(): Observable<any[]> {
    return this.tick$.asObservable();
  }

  orderUpdates(): Observable<any> {
    return this.order$.asObservable();
  }

  subscribe(instrumentTokens: number[]): void {
    for (const token of instrumentTokens) {
      const count = this.tokenRefCount.get(token) || 0;
      this.tokenRefCount.set(token, count + 1);
    }
    if(instrumentTokens.length < 1) {
      console.log('No new instrument tokens for subscription.');
      return;
    }
    if (this.client.connected) {
      this.client.publish({ destination: '/app/subscribe', body: JSON.stringify({ instrumentTokens }) });
    } else {
      console.warn('WebSocket client is not connected. Cannot subscribe.');
    }
  }

  unsubscribe(inputInstrumentTokens: number[]): Observable<void> {
    const instrumentTokens: number[] = [];
    for (const token of inputInstrumentTokens) {
      const count = this.tokenRefCount.get(token);
      if (count !== undefined) {
        if (count <= 1) {
          this.tokenRefCount.delete(token);
          instrumentTokens.push(token);
        } else {
          this.tokenRefCount.set(token, count - 1);
        }
      }
    }
    if (instrumentTokens.length < 1) {
      console.log('No unique instrument tokens for un-subscribe.');
      return of(void 0); // still return observable
    }
    if (this.client.connected) {
      this.client.publish({ destination: '/app/unsubscribe', body: JSON.stringify({ instrumentTokens }) });
      // add a slight delay to ensure processing
      return timer(100).pipe(map(() => void 0));
    } else {
      console.warn('WebSocket client is not connected. Cannot unsubscribe.');
      return of(void 0);
    }
  }



  disconnect(): void {
    this.client.deactivate();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
