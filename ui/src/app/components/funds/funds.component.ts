import { Component, Inject, OnInit} from '@angular/core';
import {CommonModule, DOCUMENT} from "@angular/common";
import {FundsService} from "../../services/funds.service";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {UserDataStateService} from "../../services/user-data-state-service";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faDollarSign, faMoneyBillTrendUp, faWallet} from "@fortawesome/free-solid-svg-icons";
import { UserFilterComponent } from "../shared/user-filter/user-filter.component";

@Component({
  selector: 'app-funds',
    imports: [CommonModule, FaIconComponent, UserFilterComponent],
  templateUrl: './funds.component.html',
  styleUrl: './funds.component.css'
})
export class FundsComponent implements OnInit {

  constructor(private fundsService: FundsService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              @Inject(DOCUMENT) private document: Document) {
  }

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    this.selectedUserIds = userViewState.funds.selectedUsersIds;
    this.getCachedData();
    this.fetchFunds();
  }

  funds: any[] = [];
  consolidatedFunds: any = {};
  errorMessage = '';
  users: any[] = [];

  selectedUserIds: string[] = [];

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0 && userDataState.funds) {
      this.funds = userDataState.funds;
      this.getConsolidateFunds();
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      funds: this.funds
    });
  }

  fetchFunds(): void {
    this.fundsService.getFunds().subscribe({
      next: (response) => {
        this.funds = response.data;
        this.getConsolidateFunds();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching funds. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      }
    });
  }

  resetConsolidatedFunds(): void {
    this.consolidatedFunds = {
      net : 0,
      availableCash: 0,
      usedMargin: 0,
      span: 0,
      exposure : 0,
      optionPremium : 0,
      payin : 0,
      holdingSales : 0,
      openingBalance : 0,
      totalCollateral : 0
    };
  }

  getConsolidateFunds(): void {
    this.resetConsolidatedFunds();
    let filteredFunds = this.funds;
    if (this.selectedUserIds.length > 0) {
      filteredFunds = filteredFunds.filter(f => this.selectedUserIds.includes(f.userId));
    }
    filteredFunds.forEach(fund => {
      this.consolidatedFunds.net += Number(fund.margin.net);
      this.consolidatedFunds.availableCash += Number(fund.margin.available.liveBalance);
      this.consolidatedFunds.usedMargin += Number(fund.margin.utilised.debits);
      this.consolidatedFunds.span += Number(fund.margin.utilised.span);
      this.consolidatedFunds.exposure += Number(fund.margin.utilised.exposure);
      this.consolidatedFunds.optionPremium += Number(fund.margin.utilised.optionPremium);
      this.consolidatedFunds.payin += Number(fund.margin.available.intradayPayin);
      this.consolidatedFunds.holdingSales += Number(fund.margin.utilised.holdingSales);
      this.consolidatedFunds.totalCollateral += Number(fund.margin.available.collateral);
    });
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.getConsolidateFunds();
    this.saveUserViewState();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.getConsolidateFunds();
    this.saveUserViewState();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      funds: {
        selectedUsersIds: this.selectedUserIds,
      }
    });
  }

  protected readonly faDollarSign = faDollarSign;
  protected readonly faMoneyBillTrendUp = faMoneyBillTrendUp;
  protected readonly faWallet = faWallet;
}
