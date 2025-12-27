import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router, RouterModule, RouterOutlet} from '@angular/router';
import {HeaderComponent} from "./components/header/header.component";
import {CommonModule} from "@angular/common";
import {FontAwesomeModule} from '@fortawesome/angular-fontawesome';
import {DragDropModule} from "@angular/cdk/drag-drop";
import {ReactiveFormsModule} from "@angular/forms";

import { registerLocaleData } from '@angular/common';
import localeIn from '@angular/common/locales/en-IN';
import {Title} from "@angular/platform-browser";
import {filter, map, mergeMap} from "rxjs";
import {WatchlistComponent} from "./components/watchlist/watchlist.component";
import {AuthUserService} from "./services/auth/auth-user.service";
import {UserPreferencesService} from "./services/user-preferences.service";
import {UserDataStateService} from "./services/user-data-state-service";

registerLocaleData(localeIn, 'en-IN');

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterModule,
    HeaderComponent,
    CommonModule,
    FontAwesomeModule,
    DragDropModule,
    ReactiveFormsModule,
    WatchlistComponent,
  ],
  templateUrl: './app.component.html',
  standalone: true,
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private titleService: Title,
    private authUserService: AuthUserService,
    private userPreferencesService: UserPreferencesService,
    private userDataStateService : UserDataStateService,
  ) {
    this.authUserService.authStatus$.subscribe((status) => {
      this.isAuthenticated = status;
    });
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => this.activatedRoute),
      map(route => {
        while (route.firstChild) route = route.firstChild;
        return route;
      }),
      mergeMap(route => route.data)
    ).subscribe(data => {
      // Detect if route is /watchlist or similar
      this.isWatchlistRoute = this.router.url.includes('/watchlist');
      this.titleService.setTitle(data['title'] || 'FamVest App');
    });
  }

  isAuthenticated: boolean = false;
  isWatchlistCollapsed = false;
  isWatchlistRoute = false;

  onCollapseChanged(collapsed: boolean) {
    this.isWatchlistCollapsed = collapsed;
  }

  themeClass = 'from-gray-50';
  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.themeClass) {
        this.themeClass = userDataState.themeClass;
      }
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      themeClass: this.themeClass
    });
  }

  //create map of themes to css classes
  themeMap: {[key: string]: string} = {
    "BLUE": "from-blue-50",
    "PINK": "from-pink-50",
    "GREEN": "from-green-50",
    "ORANGE": "from-orange-50",
    "YELLOW": "from-yellow-50",
    "GRAY": "from-gray-50",
  };

  ngOnInit() {
    this.getCachedData();
    //skip this if user is not authenticated
    if(!this.isAuthenticated) {
      return;
    }
    this.userPreferencesService.getUserPreferences().subscribe((res: any) => {
      const prefs = res.data || [];
      const themePref = prefs.find((p: any) => p.preference === 'THEME_COLOR');
      if (themePref && themePref.value) {
        this.themeClass = this.themeMap[themePref.value] || this.themeClass;
        this.setCachedData();
      }
    });
  }
}
