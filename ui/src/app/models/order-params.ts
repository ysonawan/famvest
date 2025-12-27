export interface OrderParams {
  quantity: number;
  orderType: string;
  tradingsymbol: string;
  product: string;
  exchange: string;
  transactionType: string;
  validity: string;
  price: number;
  triggerPrice: number;
  tag: string;
}
