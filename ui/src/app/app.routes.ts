import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {OrdersComponent} from "./components/orders/orders.component";
import {HoldingsComponent} from "./components/holdings/holdings.component";
import {PositionsComponent} from "./components/positions/positions.component";
import {FundsComponent} from "./components/funds/funds.component";
import {AuthGuard} from "./gaurd/auth.guard";
import {LoginComponent} from "./components/login/login.component";
import {WatchlistComponent} from "./components/watchlist/watchlist.component";
import {MfSipsComponent} from "./components/mf-sips/mf-sips.component";
import {SignUpComponent} from "./components/signup/signup.component";
import { PAGE_TITLES } from './constants/page-titles';
import {AlgoComponent} from "./components/algo/algo.component";
import {HistoricalDataComponent} from "./components/historical-data/historical-data.component";
import {AdminGuard} from "./gaurd/admin.guard";
import {IposComponent} from "./components/ipos/ipos.component";
import {HomeComponent} from "./components/home/home.component";
import {BasketOrderComponent} from "./components/basket-order/basket-order.component";
import {ProfileComponent} from "./components/profile/profile.component";
import {AdministrationComponent} from "./components/administration/administration.component";

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full'},
  { path: 'home', component: HomeComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.home } },
  { path: 'watchlist', component: WatchlistComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.watchlist, standaloneWatchlist: true } },
  { path: 'orders', component: OrdersComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.orders } },
  { path: 'basket-order', component: BasketOrderComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.basketOrder } },
  { path: 'holdings', component: HoldingsComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.holdings } },
  { path: 'positions', component: PositionsComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.positions } },
  { path: 'funds', component: FundsComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.funds } },
  { path: 'mf', component: MfSipsComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.mf } },
  { path: 'algo', component: AlgoComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.algo } },
  { path: 'historical-data', component: HistoricalDataComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.historicalData } },
  { path: 'administration', component: AdministrationComponent, canActivate: [AuthGuard, AdminGuard], data: { title: PAGE_TITLES.administration } },
  { path: 'ipos', component: IposComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.ipos } },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.myProfile } },
  { path: 'login', component: LoginComponent, canActivate: [AuthGuard], data: { title: PAGE_TITLES.login } },
  { path: 'signup', component: SignUpComponent, data: { title: PAGE_TITLES.signup }},
  // Wildcard route to catch all undefined routes
  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
