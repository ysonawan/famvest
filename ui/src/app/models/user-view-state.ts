export interface UserViewState {
  holdings: {
    selectedUsersIds: string[];
    selectedHoldingType: string;
    searchQuery: string;
    filterSelection: any[];
  },
  positions: {
    selectedUsersIds: string[];
    searchQuery: string;
    filterSelection: any[];
  },
  orders: {
    selectedUsersIds: string[];
    selectedOrderType: string;
    searchQuery: string;
    filterSelection: any[];
  },
  funds: {
    selectedUsersIds: string[];
  },
  watchlist: {
    activeWatchlist: number;
    isCollapsed: boolean;
  },
  mfSips: {
    selectedUsersIds: string[];
    selectedSectionType: string;
    searchQuery: string;
    mfSearchQuery: string;
    filterSelection: any[];
    mfOrderFilterSelection: any[];
  },
  userManagementDashboard: {},
  ordersHistory: Record<any, any>,
  charges: {
    selectedUsersIds: string[];
  },
  straddles: {
    selectedSectionType: string;
    selectedUsersIds: string[];
    searchQuery: string;
  },
  historicalData: {},
  ipos: {
    selectedIpoType: string;
    searchQuery: string;
    searchIpoApplicationsQuery: string;
    selectedUsersIds: string[];
    filterSelection: any[];
    applicationsFilterSelection: any[];
  },
  userPreferences: {},
  home: {
    selectedUsersIds: string[];
  },
  profile: {
    selectedTab: string;
  },
  administration: {
    selectedTab: string;
  }
}

