import {OrderParams} from "./order-params";

export interface OrderRequest {
  tradingAccountId: string;
  orderParams: OrderParams;
}
