import {Component, Inject, OnInit} from '@angular/core';
import {DOCUMENT, NgClass, NgForOf, NgIf} from "@angular/common";
import {ToastrService} from "ngx-toastr";
import {NoteComponent} from "../shared/note/note.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {UserDataStateService} from "../../services/user-data-state-service";
import {IposService} from "../../services/ipos.service";
import {SearchInputComponent} from "../shared/search-input/search-input.component";
import {IpoTableComponent} from "../tables/ipo-table/ipo-table.component";
import {IpoApplicationsTableComponent} from "../tables/ipo-applications-table/ipo-applications-table.component";
import {faRefresh, faSpinner} from "@fortawesome/free-solid-svg-icons";
import {UserFilterComponent} from "../shared/user-filter/user-filter.component";
import {UtilsService} from "../../services/utils.service";
import {fallbackAvatarUrl} from "../../constants/constants";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";

@Component({
  selector: 'app-ipos',
  templateUrl: './ipos.component.html',
  imports: [NoteComponent, NgIf, NgForOf, SearchInputComponent, NgClass, IpoTableComponent, IpoApplicationsTableComponent, UserFilterComponent, ToolBarComponent],
  standalone: true,
  styleUrls: ['./ipos.component.css']
})
export class IposComponent implements OnInit {

  constructor(private iposService: IposService,
              private toastrService: ToastrService,
              private userDataStateService: UserDataStateService,
              private utilsService: UtilsService,
              @Inject(DOCUMENT) private document: Document,
              private userViewStateService: UserViewStateService) { }

  ngOnInit(): void {
    const userViewState = this.userViewStateService.getState();
    this.activeSubTab = userViewState.ipos.selectedIpoType || 'IPO';
    this.searchQuery = userViewState.ipos.searchQuery;
    this.searchIpoApplicationsQuery = userViewState.ipos.searchIpoApplicationsQuery;
    this.selectedUserIds = userViewState.ipos.selectedUsersIds || [];
    if(userViewState.ipos.filterSelection?.length > 0) {
      this.filterSelection = userViewState.ipos.filterSelection;
    }
    if(userViewState.ipos.applicationsFilterSelection?.length > 0) {
      this.applicationsFilterSelection = userViewState.ipos.applicationsFilterSelection;
    }
    this.getCachedData();
    this.fetchedIPOs();
    this.fetchIPOApplications();
  }

  ipos: any[] = [];
  ipoApplications: any[] = [];
  groupedApplications: any[] = [];
  filteredIpos: any[] = [];
  users: any[] = [];
  errorMessage = '';
  subTabs = ['IPO', 'Applications'];
  activeSubTab = 'IPO';
  selectedUserIds: string[] = [];
  tabCounts: Record<string, number> = {
    'IPO': 0,
    'Applications': 0,
  };
  isLoadingData: boolean = false;

  filterOptions = [
    { key: 'sub_type', values: ['IPO', 'SME'] },
    { key: 'status', values: ['UPCOMING', 'PREAPPLY', 'ONGOING', 'CLOSED'] }
  ];
  filterSelection: any[] = [];

  applicationsFilterOptions = [
    { key: 'category', values: ['IPO', 'SME'] },
    { key: 'status', values: ['PENDING', 'SUBMITTED', 'ALLOTTED', 'NOT ALLOTTED', 'CANCELLED'] }
  ];
  applicationsFilterSelection: any[] = [];

  setActiveSubTab(tab: string): void {
    this.activeSubTab = tab;
    this.saveUserViewState();
  }

  updateFilteredIPOs(): void {
    let filtered = this.ipos;
    if (this.searchQuery) {
      filtered = filtered.filter(ipo =>
        ipo.sub_type.toLowerCase().includes(this.searchQuery) ||
        ipo.name.toLowerCase().includes(this.searchQuery) ||
        ipo.symbol.toLowerCase().includes(this.searchQuery)
      );
    }
    if (this.filterSelection.length > 0) {
      filtered = this.utilsService.filter(this.filterSelection, filtered);
    }
    this.filteredIpos = filtered;
  }

  updateFilteredIpoApplications(): void {
    let filtered = this.ipoApplications;
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter(app =>
        this.selectedUserIds.includes(app.user_id)
      );
    }
    if (this.searchIpoApplicationsQuery) {
      filtered = filtered.filter(app =>
        app.symbol.toLowerCase().includes(this.searchIpoApplicationsQuery) ||
        app.user_id.toLowerCase().includes(this.searchIpoApplicationsQuery) ||
        app.category.toLowerCase().includes(this.searchIpoApplicationsQuery) ||
        app.status.toLowerCase().includes(this.searchIpoApplicationsQuery)
      );
    }
    if (this.applicationsFilterSelection.length > 0) {
      filtered = this.utilsService.filter(this.applicationsFilterSelection, filtered);
    }
    // Group by userId
    this.groupApplicationsByUser(filtered);
  }

  groupApplicationsByUser(applications: any[]): void {
    const grouped: { [key: string]: any[] } = {};
    applications.forEach((app: any) => {
      if (!grouped[app.user_id]) {
        grouped[app.user_id] = [];
      }
      grouped[app.user_id].push(app);
    });
    this.groupedApplications = Object.entries(grouped).map(([userId, applications]) => {
      return {
        userId,
        applications,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId)
      };
    });
  }

  searchQuery = '';
  onSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredIPOs();
    this.saveUserViewState();
  }

  searchIpoApplicationsQuery = '';
  onIpoApplicationSearch(query: string): void {
    this.searchIpoApplicationsQuery = query.toLowerCase();
    this.updateFilteredIpoApplications();
    this.saveUserViewState();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  getAvatarUrl(userId: string) {
    return this.utilsService.getAvatarUrl(this.users, userId);
  }

  getUserName(userId: string) {
    return this.utilsService.getUserName(this.users, userId);
  }

  getUserFullName(userId: string) {
    return this.utilsService.getUserFullName(this.users, userId);
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.updateFilteredIpoApplications();
    this.saveUserViewState();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.updateFilteredIpoApplications();
    this.saveUserViewState();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.ipos) {
        this.ipos = userDataState.ipos;
      }
      if(userDataState.ipoApplications) {
        this.ipoApplications = userDataState.ipoApplications;
      }
      this.renderIPOs();
      this.renderIPOApplications();
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      ipos: this.ipos,
      ipoApplications: this.ipoApplications,
    });
  }

  fetchedIPOs(done?: () => void): void {
    this.isLoadingData = true;
    this.iposService.getIPOs().subscribe({
      next: (response) => {
        this.ipos = response.data;
        this.renderIPOs();
        this.setCachedData();
        this.isLoadingData = false;
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching ipos. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
        this.isLoadingData = false;
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  fetchIPOApplications(done?: () => void): void {
    this.iposService.getIPOApplications().subscribe({
      next: (response) => {
        this.ipoApplications = response.data;
        this.renderIPOApplications();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching IPO applications. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  renderIPOApplications(): void {
    this.tabCounts['Applications'] = this.ipoApplications.length;
    this.updateFilteredIpoApplications();
  }

  renderIPOs(): void {
    this.tabCounts['IPO'] = this.ipos.filter(ipo => ipo.status === 'ongoing' || ipo.status === 'preapply' || ipo.status === 'upcoming').length;
    this.updateFilteredIPOs();
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      ipos: {
        selectedIpoType: this.activeSubTab,
        searchQuery: this.searchQuery,
        searchIpoApplicationsQuery: this.searchIpoApplicationsQuery,
        selectedUsersIds: this.selectedUserIds,
        filterSelection: this.filterSelection,
        applicationsFilterSelection: this.applicationsFilterSelection,
      }
    });
  }

  onApplicationCancelled(): void {
    this.fetchIPOApplications();
  }

  applyFilters(event: any) {
    this.filterSelection = event;
    this.updateFilteredIPOs();
    this.saveUserViewState();
  }

  applyApplicationsFilters(event: any) {
    this.applicationsFilterSelection = event;
    this.updateFilteredIpoApplications();
    this.saveUserViewState();
  }

  handleRefresh(done: () => void) {
    this.fetchedIPOs(done);
  }

  handleApplicationsRefresh(done: () => void) {
    this.fetchIPOApplications(done);
  }

  protected readonly faSpinner = faSpinner;
  protected readonly faRefresh = faRefresh;
  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
}
