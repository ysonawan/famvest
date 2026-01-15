export interface ExchangeTimingDetail {
  exchange: string;
  start_time: number;
  end_time: number;
}

export interface MarketHoliday {
  date: string;
  description: string;
  holiday_type: string;
  closed_exchanges: string[];
  open_exchanges: ExchangeTimingDetail[];
}

export interface MarketHolidaysResponse {
  status: string;
  data: MarketHoliday[];
}

