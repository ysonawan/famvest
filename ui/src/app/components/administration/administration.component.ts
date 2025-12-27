import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {faSpinner, faRefresh, faPlus} from '@fortawesome/free-solid-svg-icons';
import { ApplicationUsersTableComponent } from '../tables/application-users-table/application-users-table.component';
import { ScheduledTasksTableComponent } from '../tables/scheduled-tasks-table/scheduled-tasks-table.component';
import { DataLoadingMessageComponent } from '../shared/data-loading-message/data-loading-message.component';
import { NoteComponent } from '../shared/note/note.component';
import { ApplicationUsersListService } from '../../services/application-users-list.service';
import { ScheduledTasksService } from '../../services/scheduled-tasks.service';
import { LogsService } from '../../services/logs.service';
import { ToastrService } from 'ngx-toastr';
import { UserDataStateService } from '../../services/user-data-state-service';
import { UserViewStateService } from '../../services/user-view-state-service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {UtilsService} from "../../services/utils.service";
import {AlertService} from "../../services/alert.service";

@Component({
  selector: 'app-administration',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    FaIconComponent,
    ApplicationUsersTableComponent,
    ScheduledTasksTableComponent,
    DataLoadingMessageComponent,
    NoteComponent],
  templateUrl: './administration.component.html',
  styleUrl: './administration.component.css'
})
export class AdministrationComponent implements OnInit, OnDestroy {

  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  tabs = ['Scheduled Tasks', 'Application Users', 'Log Viewer'];
  activeTab = 'Application Users';

  // Application Users
  applicationUsers: any[] = [];
  isRefreshingUsers = false;

  // Scheduled Tasks
  scheduledTasks: any[] = [];
  isRefreshingTasks = false;

  // Log Viewer
  logs: string = '';
  searchTerm: string = '';
  filteredLogs: string[] = [];
  logFileType: string = 'application';
  logFileName: string = 'fam-vest.log';
  isRefreshingLogs = false;
  showLogLoader: boolean = false;
  lastUpdated: Date = new Date();
  private _autoRefresh: boolean = true;
  private _refreshInterval: number = 10000;

  get autoRefresh(): boolean {
    return this._autoRefresh;
  }
  set autoRefresh(value: boolean) {
    this._autoRefresh = value;
    this.updateAutoRefreshTimer();
  }

  get refreshInterval(): number {
    return this._refreshInterval;
  }
  set refreshInterval(value: number) {
    this._refreshInterval = value;
    this.updateAutoRefreshTimer();
  }

  private timer: any;
  private destroy$ = new Subject<void>();

  logFileTypeOptions = [
    { value: 'application', label: 'Application' },
    { value: 'algo', label: 'Algorithm' }
  ];
  logFileNameOptions: { value: string, label: string }[] = [];
  applicationLogFiles = ['fam-vest.log'];
  algoLogFiles = ['straddle.log'];

  faSpinner = faSpinner;
  faRefresh = faRefresh;

  constructor(
    private applicationUsersListService: ApplicationUsersListService,
    private scheduledTasksService: ScheduledTasksService,
    private logsService: LogsService,
    private toastrService: ToastrService,
    private userDataStateService: UserDataStateService,
    private userViewStateService: UserViewStateService,
    private utilsService: UtilsService,
    private alertService: AlertService
  ) { }

  ngOnInit(): void {
    this.loadCachedViewState();
    this.getCachedData();
    this.loadTabData();
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCachedViewState(): void {
    const userViewState = this.userViewStateService.getState();
    if (userViewState?.administration?.selectedTab) {
      this.activeTab = userViewState.administration.selectedTab;
    }
  }

  private getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if (userDataState && Object.keys(userDataState).length > 0) {
      if (userDataState.applicationUsers) {
        this.applicationUsers = userDataState.applicationUsers;
      }
      if (userDataState.scheduledTasks) {
        this.scheduledTasks = userDataState.scheduledTasks;
      }
    }
  }

  private loadTabData(): void {
    this.fetchApplicationUsers();
    this.fetchScheduledTasks();
    this.initializeLogViewer();
    this.fetchLogs();
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    // Save tab state
    const userViewState = this.userViewStateService.getState();
    userViewState.administration = userViewState.administration || {};
    userViewState.administration.selectedTab = tab;
    this.userViewStateService.setState(userViewState);
  }

  // Application Users Methods
  private fetchApplicationUsers(): void {
    this.isRefreshingUsers = true;
    this.applicationUsersListService.getAllApplicationUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.applicationUsers = response.data || [];
          this.setCachedData();
          this.isRefreshingUsers = false;
        },
        error: (error: any) => {
          this.isRefreshingUsers = false;
          this.toastrService.error(error.error?.message || 'Error fetching application users', 'Error');
        }
      });
  }

  refreshApplicationUsers(): void {
    this.fetchApplicationUsers();
  }

  // Scheduled Tasks Methods
  private fetchScheduledTasks(): void {
    this.isRefreshingTasks = true;
    this.scheduledTasksService.getScheduledTasks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.scheduledTasks = response.data || [];
          this.setCachedData();
          this.isRefreshingTasks = false;
        },
        error: (error: any) => {
          this.isRefreshingTasks = false;
          this.toastrService.error(error.error?.message || 'Error fetching scheduled tasks', 'Error');
        }
      });
  }

  actions: any[] = [
    {action: 'activate', color:'blue', label: 'Activate Scheduled Task'},
    {action: 'pause', color:'orange', label: 'Pause Scheduled Task'},
    {action: 'execute', color:'blue', label: 'Execute Scheduled Task'},
    {action: 'info', color:'gray', label: 'Scheduled Task Details'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'activate' || event.action === 'pause') {
      this.toggleScheduledTaskStatus(event.action, event.row);
    } if(event.action === 'execute') {
      this.executeScheduledTask(event.row);
    }  else if(event.action === 'info') {
      this.utilsService.showInfo(event.row, `Scheduled Task - ${event.row.schedulerName}`)
    }
  }

  executeScheduledTask(scheduledTask: any) {
    this.alertService.confirm(`Execute Scheduled Task`,
      `Are you sure you want to execute this scheduled task now? This will trigger immediate execution.`, () => {
        this.isRefreshingTasks = true;
        scheduledTask.status='IN_PROGRESS';
        this.scheduledTasksService.executeScheduledTask(scheduledTask.id).subscribe({
          next: (response) => {
            this.toastrService.success(response.message, 'Success');
            this.fetchScheduledTasks();
            this.isRefreshingTasks = false;
          },
          error: (error) => {
            this.isRefreshingTasks = false;
            this.fetchScheduledTasks();
            this.toastrService.error(error.error.message, 'Error');
            console.error(error);
          }
        });
      });
  }

  toggleScheduledTaskStatus(action: string, scheduledTask: any) {
    this.scheduledTasksService.updateScheduledTaskStatus(scheduledTask.id, {status: action}).subscribe({
      next: (response) => {
        this.toastrService.success(response.message, 'Success');
        this.fetchScheduledTasks();
      },
      error: (error) => {
        this.toastrService.error(error.error.message, 'Error');
        console.error(error);
      }
    });
  }


  refreshScheduledTasks(): void {
    this.fetchScheduledTasks();
  }

  // Log Viewer Methods
  private initializeLogViewer(): void {
    this.updateLogFileOptions();
  }

  private fetchLogs(): void {
    this.showLogLoader = true;

    const request$ = this.logFileType === 'application'
      ? this.logsService.getApplicationLogs(this.logFileName)
      : this.logsService.getAlgoLogs(this.logFileName);

    request$.pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.logs = data;
          this.filterLogs();
          this.lastUpdated = new Date();
          this.showLogLoader = false;
          this.scrollToBottom();
        },
        error: (error: any) => {
          this.showLogLoader = false;
          this.logs = '';
          this.filteredLogs = [];
          if (error.error?.message) {
            this.toastrService.error(error.error.message, 'Error');
          } else {
            this.toastrService.error('An unexpected error occurred while fetching logs.', 'Error');
          }
        }
      });
  }

  private filterLogs(): void {
    const lines = this.logs.split('\n');
    this.filteredLogs = this.searchTerm
      ? lines.filter(line => line.toLowerCase().includes(this.searchTerm.toLowerCase()))
      : lines;
  }

  performSearch(): void {
    this.showLogLoader = true;
    setTimeout(() => {
      this.filterLogs();
      this.showLogLoader = false;
    }, 100);
  }

  refreshLogs(): void {
    this.fetchLogs();
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        if (this.scrollContainer && this.scrollContainer.nativeElement) {
          this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
        }
      }, 100);
    } catch (err) {
      console.warn('Scroll failed:', err);
    }
  }

  private updateAutoRefreshTimer(): void {
    clearInterval(this.timer);
    if (this.autoRefresh && this.activeTab === 'Log Viewer') {
      this.timer = setInterval(() => {
        this.fetchLogs();
      }, this.refreshInterval);
    }
  }

  onLogFileTypeChange(): void {
    this.updateLogFileOptions();
    this.fetchLogs();
  }

  onLogFileNameChange(): void {
    this.fetchLogs();
  }

  private updateLogFileOptions(): void {
    this.logFileNameOptions = this.logFileType === 'application'
      ? this.applicationLogFiles.map(file => ({ value: file, label: file }))
      : this.algoLogFiles.map(file => ({ value: file, label: file }));
    this.logFileName = this.logFileNameOptions.length > 0 ? this.logFileNameOptions[0].value : '';
  }

  private setCachedData(): void {
    this.userDataStateService.setState({
      applicationUsers: this.applicationUsers,
      scheduledTasks: this.scheduledTasks
    });
  }

  protected readonly faPlus = faPlus;
}

