import {Component, Inject, OnDestroy, OnInit, TemplateRef} from '@angular/core';
import {DOCUMENT, NgClass, NgForOf, NgIf} from "@angular/common";
import {SearchInputComponent} from "../shared/search-input/search-input.component";
import {UserFilterComponent} from "../shared/user-filter/user-filter.component";
import {UserViewStateService} from "../../services/user-view-state-service";
import {ToastrService} from "ngx-toastr";
import {fallbackAvatarUrl} from "../../constants/constants";
import {UtilsService} from "../../services/utils.service";
import {MatDialog} from "@angular/material/dialog";
import {ToolBarComponent} from "../shared/tool-bar/tool-bar.component";
import {UserDataStateService} from "../../services/user-data-state-service";
import {NoteComponent} from "../shared/note/note.component";
import {expandCollapseAnimation} from "../shared/animations";
import {AlgoService} from "../../services/algo.service";
import {AlertService} from "../../services/alert.service";
import {AddManageStraddleStrategyComponent} from "../dialogs/add-manage-straddle-strategy/add-manage-straddle-strategy.component";
import {StraddleExecutionsDialogComponent} from "../dialogs/straddle-executions-dialog/straddle-executions-dialog.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faPlus} from "@fortawesome/free-solid-svg-icons";
import {MatTooltip} from "@angular/material/tooltip";
import {AlgoStraddleStrategyTableComponent} from "../tables/algo-straddle-strategy-table/algo-straddle-strategy-table.component";


@Component({
  selector: 'app-algo',
  imports: [NgForOf, NgIf, SearchInputComponent, UserFilterComponent, ToolBarComponent, NoteComponent, FaIconComponent, MatTooltip, NgClass, AlgoStraddleStrategyTableComponent],
  templateUrl: './algo.component.html',
  styleUrl: './algo.component.css',
  animations: [expandCollapseAnimation],
})
export class AlgoComponent implements  OnInit, OnDestroy {

  constructor(private algoService: AlgoService,
              private alertService:  AlertService,
              private userViewStateService: UserViewStateService,
              private userDataStateService: UserDataStateService,
              private toastrService: ToastrService,
              private utilsService: UtilsService,
              private dialog: MatDialog,
              @Inject(DOCUMENT) private document: Document) { }

  users: any[] = [];
  straddles: any[] = [];
  filteredStraddles: any[] = [];
  groupedStraddles: any[] = [];
  errorMessage = '';
  parentTabs = ['Straddles'];
  activeParentTab = 'Straddles';
  parentTabCounts: Record<string, number> = {
    'Straddles': 0,
    'ORB': 0,
  };
  cellTemplates!: { [key: string]: TemplateRef<any> };

  selectedUserIds: string[] = [];
  searchQuery: string = '';

  ngOnInit(): void {
    this.cellTemplates = {};
    const userViewState = this.userViewStateService.getState();
    if(userViewState.straddles) {
      this.activeParentTab = userViewState.straddles.selectedSectionType;
      this.selectedUserIds = userViewState.straddles.selectedUsersIds;
      this.searchQuery = userViewState.straddles.searchQuery;
    }
    this.getCachedData();
    this.fetchStraddles();
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

  setActiveParentTab(tab: string): void {
    this.activeParentTab = tab;
    this.saveUserViewState();
  }

  onStraddleSearch(query: string): void {
    this.searchQuery = query.toLowerCase();
    this.updateFilteredAndGroupedStraddles();
    this.saveUserViewState();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
      if(userDataState.straddles) {
        this.straddles = userDataState.straddles;
        this.renderStraddles();
      }
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      straddles: this.straddles,
    });
  }

  fetchStraddles(done?: () => void): void {
    this.algoService.getStraddles().subscribe({
      next: (response) => {
        this.straddles = response.data;
        this.renderStraddles();
        this.setCachedData();
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching straddles. Verify that the backend service is operational.', 'Error');
        }
        this.errorMessage = `Status Code: ${error.error.status}, Error Message: ${error.error.message}`;
      },
      complete: () => {
        if (done) done();
      }
    });
  }

  renderStraddles() {
    this.updateStraddlesCount();
    this.updateFilteredAndGroupedStraddles();
  }

  updateStraddlesCount() {
    this.parentTabCounts['Straddles'] = this.straddles.length;
  }

  onUserSelection(userIds: string[]): void {
    this.selectedUserIds = userIds;
    this.updateFilteredAndGroupedStraddles();
    this.saveUserViewState();
  }

  onAllUserSelection(): void {
    this.selectedUserIds = [];
    this.updateFilteredAndGroupedStraddles();
    this.saveUserViewState();
  }

  captureUsers(users: any[]): void {
    this.users = users;
  }

  saveUserViewState(): void {
    this.userViewStateService.setState({
      straddles: {
        selectedSectionType: this.activeParentTab,
        selectedUsersIds: this.selectedUserIds,
        searchQuery: this.searchQuery,
      }
    });
  }

  updateFilteredAndGroupedStraddles(): void {
    // Filtering
    let filtered = this.straddles;
    if (this.selectedUserIds.length > 0) {
      filtered = filtered.filter(s => this.selectedUserIds.includes(s.userId));
    }
    if (this.searchQuery) {
      filtered = filtered.filter(s =>
        s.instrument.toLowerCase().includes(this.searchQuery) ||
        s.userId.toLowerCase().includes(this.searchQuery)
      );
    }
    this.filteredStraddles = filtered;

    // Grouping
    const grouped: { [key: string]: any[] } = {};
    filtered.forEach(straddle => {
      if (!grouped[straddle.userId]) {
        grouped[straddle.userId] = [];
      }
      grouped[straddle.userId].push(straddle);
    });
    this.groupedStraddles = Object.entries(grouped).map(([userId, straddles]) => {
      return {
        userId,
        straddles,
        userName: this.getUserName(userId),
        fullName: this.getUserFullName(userId),
        avatarUrl: this.getAvatarUrl(userId)
      };
    });
  }

  handleRefreshForStraddles(done: () => void) {
    this.fetchStraddles(done);
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;

  straddleActions: any[] = [
    {action: 'modify', color:'blue', label: 'Modify Straddle'},
    {action: 'activate', color:'blue', label: 'Activate Straddle'},
    {action: 'pause', color:'orange', label: 'Pause Straddle'},
    {action: 'execute', color:'blue', label: 'Execute Straddle'},
    {action: 'delete', color:'red', label: 'Delete Straddle'},
    {action: 'viewExecutions', color:'gray', label: 'Execution History'},
    {action: 'info', color:'gray', label: 'Straddle Details'},
  ]

  handleStraddleActions(event: { action: string, row: any }) {
    if(event.action === 'modify') {
      this.createManageStraddleStrategy(event.row);
    } else if(event.action === 'activate' || event.action === 'pause') {
      this.toggleStraddleStrategyStatus(event.action, event.row);
    } if(event.action === 'execute') {
      this.executeStraddle(event.row);
    } else if(event.action === 'delete') {
      this.deleteStraddleStrategy(event.row);
    }  else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, event.row.instrument);
    } else if(event.action === 'viewExecutions') {
      this.viewStraddleExecutions(event.row);
    }
  }

  createManageStraddleStrategy(row: any) {
    const dialogRef = this.dialog.open(AddManageStraddleStrategyComponent, {
      disableClose: true,       // Prevent closing via escape or backdrop click
      autoFocus: true,          // Focus the first form element inside the dialog
      hasBackdrop: true,        // Show a dark background overlay
      closeOnNavigation: false,  // Optional: closes the dialog if navigation occurs
      data: row
    });
    dialogRef.afterClosed().subscribe((result: any) => {
       this.fetchStraddles();
    });
  }

  toggleStraddleStrategyStatus(action: string, straddle: any) {
    this.algoService.updateStraddleStatus(straddle.id, {status: action}).subscribe({
      next: (response) => {
        this.toastrService.success(response.message, 'Success');
        this.fetchStraddles();
      },
      error: (error) => {
        this.toastrService.error(error.error.message, 'Error');
        console.error(error);
      }
    });
  }

  deleteStraddleStrategy(straddle: any) {
    this.alertService.confirm(`Delete Straddle Strategy`,
      `Are you sure you want to delete this straddle strategy?`, () => {
        this.algoService.deleteStraddleStrategy(straddle.id).subscribe({
          next: (response) => {
            this.toastrService.success(response.message, 'Success');
            this.fetchStraddles();
          },
          error: (error) => {
            this.toastrService.error(error.error.message, 'Error');
            console.error(error);
          }
        });
      });
  }

  executeStraddle(straddle: any) {
    this.alertService.confirm(`Execute Straddle Strategy`,
      `Are you sure you want to execute this straddle now? Note: This action will force immediate execution, ignoring the configured entry time and strategy status.`, () => {
        this.algoService.executeStraddle(straddle.id).subscribe({
          next: (response) => {
            this.toastrService.success(response.message, 'Success');
          },
          error: (error) => {
            this.toastrService.error(error.error.message, 'Error');
            console.error(error);
          }
        });
      });
  }

  viewStraddleExecutions(straddle: any) {
    const dialogRef = this.dialog.open(StraddleExecutionsDialogComponent, {
      disableClose: false,       // Prevent closing via escape or backdrop click
      autoFocus: false,          // Focus the first form element inside the dialog
      hasBackdrop: true,        // Show a dark background overlay
      closeOnNavigation: true,  // Optional: closes the dialog if navigation occurs
      data: {
        straddleId: straddle.id,
        strategyName: `${straddle.instrument} - ${straddle.userId}`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      // Optionally handle result here
    });
  }

  protected readonly faPlus = faPlus;

  ngOnDestroy(): void {

  }
}
