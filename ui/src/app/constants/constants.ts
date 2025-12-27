export const fallbackAvatarUrl = 'assets/images/default-avatar.png';
export const multiUserAvatarUrl = 'assets/images/default-multi-user.png';

export function tradingViewUrl(exchange: string, instrument: string): string {
  return `https://www.tradingview.com/chart/?symbol=${exchange}%3A${instrument}`;
}
export function screenerInUrl(instrument: string): string {
  return `https://www.screener.in/company/${instrument}`;
}
export function yahooFinanceUrl(instrument: string, exchangeIdentifier: string): string {
  return `https://finance.yahoo.com/quote/${instrument}.${exchangeIdentifier}`;
}
export function coinReportUrl(instrument: string): string {
  return `https://coin.zerodha.com/mf/fund/${instrument}`;
}
