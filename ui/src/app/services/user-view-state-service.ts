import { Injectable } from '@angular/core';
import {UserViewState} from "../models/user-view-state";

@Injectable({
  providedIn: 'root'
})
export class UserViewStateService {

  constructor() { }

  private readonly key = 'userViewState';

  getState(): UserViewState {
    const state = localStorage.getItem(this.key);
    if (state) {
      return JSON.parse(state);
    }
    const defaultUserViewState: UserViewState = this.getDefaultState();
    localStorage.setItem(this.key, JSON.stringify(defaultUserViewState));
    return defaultUserViewState;
  }

  getDefaultState(): UserViewState {
    return {
      holdings: {
        selectedUsersIds: [],
        selectedHoldingType: 'All',
        searchQuery: '',
        filterSelection: [],
      },
      positions: {
        selectedUsersIds: [],
        searchQuery: '',
        filterSelection: [],
      },
      orders: {
        selectedUsersIds: [],
        selectedOrderType: 'Open',
        searchQuery: '',
        filterSelection: [],
      },
      funds: {
        selectedUsersIds: [],
      },
      watchlist: {
        activeWatchlist: 0,
        isCollapsed: false,
      },
      mfSips: {
        selectedSectionType: 'SIPs',
        selectedUsersIds: [],
        searchQuery: '',
        mfSearchQuery: '',
        filterSelection: [],
        mfOrderFilterSelection: [],
      },
      ordersHistory: {},
      charges: {
        selectedUsersIds: []
      },
      userManagementDashboard: {
      },
      straddles: {
        selectedSectionType: 'Straddles',
        selectedUsersIds: [],
        searchQuery: '',
      },
      historicalData: {
      },
      ipos: {
        selectedIpoType: 'IPO',
        searchQuery: '',
        searchIpoApplicationsQuery: '',
        selectedUsersIds: [],
        filterSelection: [],
        applicationsFilterSelection: [],
      },
      userPreferences: {
      },
      home: {
        selectedUsersIds: []
      },
      profile: {
        selectedTab: 'Profile'
      },
      administration: {
        selectedTab: 'Scheduled Tasks'
      }
    };
  }
  setState(partialState: Partial<UserViewState>): void {
    const currentState = this.getState();
    const newState = { ...currentState, ...partialState };
    localStorage.setItem(this.key, JSON.stringify(newState));
  }

  clearState(): void {
    localStorage.removeItem(this.key);
  }
}
