import { Injectable } from '@angular/core';
import {ApiService} from "./api.service";
import {Observable} from "rxjs";
import {ApiResponse} from "../models/api-response.model";

@Injectable({
  providedIn: 'root'
})
export class OrdersService {

  constructor(private api: ApiService) {
  }

  getOrders(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/orders');
  }

  placeOrder(orderRequest: any, variety: string): Observable<ApiResponse> {
    return this.api.post<ApiResponse>(`/v1/orders?variety=${variety}`, orderRequest);
  }

  modifyOrder(tradingAccountId: string, orderId: any, orderRequest: any, variety: string): Observable<ApiResponse> {
    return this.api.put<ApiResponse>(`/v1/orders/${tradingAccountId}/${orderId}?variety=${variety}`, orderRequest);
  }

  cancelOrder(tradingAccountId: string, orderId: string, variety: string): Observable<ApiResponse> {
    return this.api.delete<ApiResponse>(`/v1/orders/${tradingAccountId}/${orderId}?variety=${variety}`);
  }

  fetchCharges(): Observable<ApiResponse> {
    return this.api.get<ApiResponse>('/v1/orders/charges');
  }

}
