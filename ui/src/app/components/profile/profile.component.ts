import {Component, OnInit, OnDestroy} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProfileService } from '../../services/profile.service';
import { AuthUserService } from '../../services/auth/auth-user.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faEye, faEyeSlash, faSpinner, faCheck, faUserPlus, faRefresh, faRightToBracket } from '@fortawesome/free-solid-svg-icons';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {IstDatePipe} from "../shared/pipes/ist-date.pipe";
import {TradingAccountService} from "../../services/trading-account.service";
import {ApplicationUserService} from "../../services/application-user.service";
import {SmallChipComponent} from "../shared/small-chip/small-chip.component";
import {UserPreferencesService} from "../../services/user-preferences.service";
import {ActionMenuComponent} from "../shared/action-menu/action-menu.component";
import {fallbackAvatarUrl} from "../../constants/constants";
import {UpdateTradingAccountComponent} from "../dialogs/update-trading-account/update-trading-account.component";
import {AddTradingAccountComponent} from "../dialogs/add-trading-account/add-trading-account.component";
import {AlertService} from "../../services/alert.service";
import {MatDialog} from "@angular/material/dialog";
import { UtilsService } from '../../services/utils.service';
import {MatTooltip} from "@angular/material/tooltip";
import {UserViewStateService} from "../../services/user-view-state-service";
import {UserDataStateService} from "../../services/user-data-state-service";

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FaIconComponent,
    IstDatePipe,
    SmallChipComponent,
    ActionMenuComponent,
    NgOptimizedImage,
    MatTooltip
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit, OnDestroy {

  profileForm!: FormGroup;
  passwordForm!: FormGroup;

  profile: any = null;
  tradingAccounts: any[] = [];

  isLoadingProfile = false;
  isUpdatingProfile = false;
  isChangingPassword = false;

  showOldPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;
  tabs = ['Trading Accounts', 'My Preferences', 'Profile'];
  activeTab = 'Trading Accounts';

  // Sorting for Trading Accounts
  tradingAccountsSortColumn: string | null = 'user';
  tradingAccountsSortDirection: 'asc' | 'desc' | null = 'asc';

  // Sorting for My Preferences
  preferencesSortColumn: string | null = 'displayName';
  preferencesSortDirection: 'asc' | 'desc' | null = 'asc';

  private destroy$ = new Subject<void>();

  constructor(
    private userService: TradingAccountService,
    private userPreferencesService: UserPreferencesService,
    private profileService: ProfileService,
    private applicationUserService: ApplicationUserService,
    private authUserService: AuthUserService,
    private toastrService: ToastrService,
    private router: Router,
    private fb: FormBuilder,
    private alertService: AlertService,
    private dialog: MatDialog,
    private utilsService: UtilsService,
    private userViewStateService: UserViewStateService,
    private userDataStateService: UserDataStateService
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.getCachedData();
    this.loadCachedViewState();
    this.loadProfileData();
    this.fetchTradingAccounts();
    this.fetchedUserPreferences();
  }

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0) {
      if(userDataState.users) {
        this.users = userDataState.users;
      }
    }
  }

  ngOnDestroy(): void {
    // Clear all TOTP timers
    this.totpRefreshTimers.forEach((timer) => {
      if (timer) {
        timer.unsubscribe();
      }
    });
    this.totpRefreshTimers.clear();
    this.totpData.clear();

    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.profileForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      userName: ['', [Validators.required, Validators.minLength(3)]]
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;

    if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      return { passwordMismatch: true };
    }
    return null;
  }

  private loadCachedViewState(): void {
    const userViewState = this.userViewStateService.getState();
    if (userViewState?.profile?.selectedTab) {
      this.activeTab = userViewState.profile.selectedTab;
    }
  }

  private saveUserViewState(): void {
    this.userViewStateService.setState({
      profile: {
        selectedTab: this.activeTab
      }
    });
  }

  private loadProfileData(): void {
    this.isLoadingProfile = true;
      this.applicationUserService.getApplicationUserProfile().subscribe({
        next: (response) => {
          if (response.data) {
            this.profile = response.data;
            this.profileForm.patchValue({
              fullName: this.profile.fullName,
              userName: this.profile.userName
            });
          }
          this.isLoadingProfile = false;
        },
        error: (error) => {
          this.isLoadingProfile = false;
          if(error.error.message) {
            this.toastrService.error(error.error.message, 'Error');
          } else {
            this.toastrService.error('An unexpected error occurred while fetching user profile. Verify that the backend service is operational.', 'Error');
          }
        }
      });
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    this.saveUserViewState();
  }

  onUpdateProfile(): void {
    if (this.profileForm.invalid) {
      this.toastrService.error('Please fix the form errors', 'Validation Error');
      return;
    }

    this.isUpdatingProfile = true;
    const formData = {
      fullName: this.profileForm.value.fullName.trim(),
      userName: this.profileForm.value.userName.trim()
    };

    this.profileService.updateProfile(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.toastrService.success('Profile updated successfully', 'Success');
          this.isUpdatingProfile = false;

          // If username was changed, logout the user
          if (formData.userName !== this.profile.userName) {
            setTimeout(() => {
              this.authUserService.logout();
              this.router.navigate(['/login'], {
                queryParams: { reason: 'username-changed' }
              });
            }, 1500);
          } else {
            // Just reload profile data
            setTimeout(() => {
              this.loadProfileData();
            }, 500);
          }
        },
        error: (error: any) => {
          this.isUpdatingProfile = false;
          const errorMessage = error.error?.message || 'Failed to update profile';
          this.toastrService.error(errorMessage, 'Error');
        }
      });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) {
      this.toastrService.error('Please fix the form errors', 'Validation Error');
      return;
    }

    this.isChangingPassword = true;
    const formData = this.passwordForm.value;

    this.profileService.changePassword(formData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.toastrService.success('Password changed successfully. Please log in again.', 'Success');
          this.isChangingPassword = false;

          // Logout the user
          setTimeout(() => {
            this.authUserService.logout();
            this.router.navigate(['/login'], {
              queryParams: { reason: 'password-changed' }
            });
          }, 1500);
        },
        error: (error: any) => {
          this.isChangingPassword = false;
          const errorMessage = error.error?.message || 'Failed to change password';
          this.toastrService.error(errorMessage, 'Error');
        }
      });
  }

  togglePasswordVisibility(field: string): void {
    switch (field) {
      case 'old':
        this.showOldPassword = !this.showOldPassword;
        break;
      case 'new':
        this.showNewPassword = !this.showNewPassword;
        break;
      case 'confirm':
        this.showConfirmPassword = !this.showConfirmPassword;
        break;
    }
  }

  userPreferences: any[] = [];
  fetchedUserPreferences(done?: () => void): void {
    this.userPreferencesService.getUserPreferences().subscribe({
      next: (response: any) => {
        this.userPreferences = response.data;
      },
      error: (error: any) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching user preferences. Verify that the backend service is operational.', 'Error');
        }
      },
    });
  }

  trackById(index: number, item: any): any {
    return item.id;
  }

  onChange(row: any) {
    this.userPreferencesService.updatePreferenceValue(row.id, row.value).subscribe({
      next: (response: any) => {
        this.toastrService.success(response.message, 'Success');
        this.fetchedUserPreferences();
      },
      error: (error: any) => {
        this.toastrService.error(error.error.message, 'Error');
        console.error(error);
      }
    });
  }

  getPreferenceValueColor(value: string): "blue" | "red" | "green" | "gray" {
    if (value === 'NO' || value === 'NONE') {
      return 'red';
    } else if (value === 'YES' || value === 'BOTH') {
      return 'green';
    }else {
      return 'blue';
    }
  }

  getActionButtons(row: any): any [] {
    let actions: any[] = this.actions;
    if(row.active) {
      actions = actions.filter(action => action.action !== 'link');
    } else {
      actions = actions.filter(action => action.action !== 'unlink');
    }
    return actions;
  }

  getTokenStatusColor(status: string): "blue" | "red" | "green" | "gray" {
    return status === 'Valid' ? 'green' : 'red';
  }

  users: any[] = [];
  errorMessage = '';
  isRenewingTokens = false;

  // TOTP Management
  totpData: Map<string, { value: string; timeRemaining: number }> = new Map();
  totpRefreshTimers: Map<string, any> = new Map();

  fetchTradingAccounts(): void {
    this.isTradingAccountsRefreshing = true;
    this.userService.getTradingAccounts().subscribe({
      next: (response) => {
        this.users = response.data;
        this.isTradingAccountsRefreshing = false;
        // Auto-fetch TOTP for all trading accounts
        this.users.forEach(user => {
          this.fetchAndDisplayTotp(user.userId);
        });
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching trading accounts. Verify that the backend service is operational.', 'Error');
        }
        this.isTradingAccountsRefreshing = false;
      }
    });
  }

  renewTokensForAll() {
    this.alertService.confirm(`Renew All Tokens`,
      `Are you sure you want to renew tokens for all trading accounts? Note: Only accounts with both password and TOTP key configured will be renewed.`, () => {
        this.isRenewingTokens = true; // Start spinner
        this.userService.renewTokensForAll().subscribe({
          next: (response) => {
            this.toastrService.success(response.data.toString(), 'Success');
            this.fetchTradingAccounts();
            this.isRenewingTokens = false; // Stop spinner
          },
          error: (error) => {
            this.toastrService.error(error.error.message, 'Error');
            console.error(error);
            this.isRenewingTokens = false; // Stop spinner
          }
        });
      });
  }

  actions: any[] = [
    {action: 'login', color:'blue', label: 'Renew Token'},
    {action: 'modify', color:'blue', label: 'Modify Trading Account'},
    {action: 'unlink', color:'orange', label: 'Deactivate Trading Account'},
    {action: 'link', color:'green', label: 'Activate Trading Account'},
    {action: 'info', color:'gray', label: 'Account Details'},
    {action: 'delete', color:'red', label: 'Delete Trading Account'},
  ]

  handleAction(event: { action: string, row: any }) {
    if(event.action === 'login') {
      this.renewToken(event.row);
    } else if(event.action === 'modify') {
      this.updateTradingAccount(event.row);
    } else if (event.action === 'delete') {
      this.deleteTradingAccount(event.row);
    } else if (event.action === 'unlink') {
      this.temporarilyUnmapTradingAccount(event.row);
    } else if (event.action === 'link') {
      this.mapTradingAccount(event.row);
    } else if (event.action === 'info') {
      this.showInfo(event.row);
    }
  }

  renewToken(user: any) {
    // Open the external URL in a new tab
    if (user.kiteLoginEndPoint) {
      window.open(user.kiteLoginEndPoint, '_blank', 'noopener,noreferrer');
    } else {
      console.error('Kite login end point is not available for this user');
    }
  }

  updateTradingAccount(user: any) {
    this.userService.getTradingAccount(user.userId).subscribe({
      next: (response) => {
        const tradingAccount = response.data;
        const dialogRef = this.dialog.open(UpdateTradingAccountComponent, {
          disableClose: true,       // Prevent closing via escape or backdrop click
          autoFocus: true,          // Focus the first form element inside the dialog
          hasBackdrop: true,        // Show a dark background overlay
          closeOnNavigation: false,  // Optional: closes the dialog if navigation occurs
          data: { tradingAccount: tradingAccount }
        });
        dialogRef.afterClosed().subscribe((result: any) => {
          this.fetchTradingAccounts();
        });
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching trading accounts. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  onboardTradingAccount() {
    const dialogRef = this.dialog.open(AddTradingAccountComponent, {
      disableClose: true,       // Prevent closing via escape or backdrop click
      autoFocus: true,          // Focus the first form element inside the dialog
      hasBackdrop: true,        // Show a dark background overlay
      closeOnNavigation: false,  // Optional: closes the dialog if navigation occurs
      data: { }
    });
    dialogRef.afterClosed().subscribe((result: any) => {
      this.fetchTradingAccounts();
    });
  }

  deleteTradingAccount(user: any) {
    this.alertService.confirm(`Delete Trading Account: ${user.userId}`,
      `Are you sure you want to delete ${user.userId} trading account?`, () => {
        this.userService.deleteTradingAccount(user.userId).subscribe({
          next: (response) => {
            this.toastrService.success('Trading account deleted successfully.', 'Success');
            this.fetchTradingAccounts();
          },
          error: (error) => {
            console.error('Error while deleting trading account:', error);
            this.toastrService.error(error.error.message, 'Error');
          }
        });
      });
  }

  temporarilyUnmapTradingAccount(user: any) {
    this.userService.unmapTradingAccount(user.userId).subscribe({
      next: (response) => {
        this.toastrService.success('Trading account unmapped successfully.', 'Success');
        this.fetchTradingAccounts();
      },
      error: (error) => {
        console.error('Error while temporarily unmapping trading account:', error);
        this.toastrService.error(error.error.message, 'Error');
      }
    });
  }

  mapTradingAccount(user: any) {
    this.userService.mapTradingAccount(user.userId).subscribe({
      next: (response) => {
        this.toastrService.success('Trading account mapped successfully.', 'Success');
        this.fetchTradingAccounts();
      },
      error: (error) => {
        console.error('Error while mapping trading account:', error);
        this.toastrService.error(error.error.message, 'Error');
      }
    });
  }

  showInfo(user: any) {
    const sourceData = {
      name: user.name,
      userId: user.userId,
      tokenStatus: user.tokenStatus,
      active: user.active,
      ...user.profile
    };
    this.utilsService.showInfo(sourceData, `User Profile - ${user.name}`)
  }


  /**
   * Fetch TOTP for a trading account and start refresh interval
   */
  fetchAndDisplayTotp(userId: string): void {
    this.userService.getTotp(userId).subscribe({
      next: (response) => {
        const totp = String(response.data);
        // Initialize with 30 seconds remaining
        this.totpData.set(userId, { value: totp, timeRemaining: 30 });
        this.startTotpRefreshTimer(userId);
      },
      error: (error) => {
        console.error(`Error while fetching TOTP for ${userId}:`, error);
      }
    });
  }

  /**
   * Start a timer to refresh TOTP countdown every second and fetch new TOTP every 30 seconds
   */
  private startTotpRefreshTimer(userId: string): void {
    // Clear existing timer if any
    if (this.totpRefreshTimers.has(userId)) {
      this.totpRefreshTimers.get(userId)?.unsubscribe();
    }

    // Create a timer that fires every second
    const subscription = interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        const data = this.totpData.get(userId);
        if (data) {
          data.timeRemaining--;

          // Fetch new TOTP when time reaches 0
          if (data.timeRemaining <= 0) {
            this.fetchAndDisplayTotp(userId);
          }
        }
      });

    this.totpRefreshTimers.set(userId, subscription);
  }

  /**
   * Copy TOTP to clipboard and show success message
   */
  copyTotpToClipboard(userId: string, event: Event): void {
    event.stopPropagation();
    const data = this.totpData.get(userId);
    if (data?.value) {
      navigator.clipboard.writeText(data.value).then(() => {
        this.toastrService.success('TOTP copied to clipboard!', 'Success');
      }).catch(() => {
        this.toastrService.error('Failed to copy TOTP to clipboard', 'Error');
      });
    }
  }

  getTotpDisplayData(userId: string): { value: string; timeRemaining: number } | undefined {
    return this.totpData.get(userId);
  }

  isTradingAccountsRefreshing = false;
  refreshTradingAccounts() {
    this.isTradingAccountsRefreshing = true;
    this.fetchTradingAccounts();
  }

  // Sorting methods for Trading Accounts
  private readonly tradingAccountsSortAccessors: Record<string, (row: any) => string | number | null> = {
    user: (row) => (row.name ?? '').toString().toLowerCase(),
    userName: (row) => (row.profile?.userName ?? '').toString().toLowerCase(),
    userId: (row) => (row.userId ?? '').toString().toLowerCase(),
    tokenStatus: (row) => (row.tokenStatus ?? '').toString().toLowerCase(),
  };

  get sortedUsers(): any[] {
    if (!this.tradingAccountsSortColumn || !this.tradingAccountsSortDirection) {
      return this.users;
    }

    return [...this.users].sort((a, b) => this.compareTradingAccounts(a, b));
  }

  setTradingAccountsSort(column: string): void {
    if (this.tradingAccountsSortColumn === column) {
      this.tradingAccountsSortDirection = this.tradingAccountsSortDirection === 'desc' ? 'asc' : 'desc';
      return;
    }

    this.tradingAccountsSortColumn = column;
    this.tradingAccountsSortDirection = 'asc';
  }

  getTradingAccountsSortIndicator(column: string): string {
    if (this.tradingAccountsSortColumn !== column || !this.tradingAccountsSortDirection) {
      return '';
    }
    return this.tradingAccountsSortDirection === 'asc' ? '↑' : '↓';
  }

  private compareTradingAccounts(a: any, b: any): number {
    const accessor = this.tradingAccountsSortColumn ? this.tradingAccountsSortAccessors[this.tradingAccountsSortColumn] : null;
    if (!accessor || !this.tradingAccountsSortDirection) {
      return 0;
    }

    const valueA = accessor(a);
    const valueB = accessor(b);

    if (valueA == null && valueB == null) { return 0; }
    if (valueA == null) { return this.tradingAccountsSortDirection === 'asc' ? -1 : 1; }
    if (valueB == null) { return this.tradingAccountsSortDirection === 'asc' ? 1 : -1; }

    let comparison: number;
    if (typeof valueA === 'string' && typeof valueB === 'string') {
      comparison = valueA.localeCompare(valueB);
    } else {
      comparison = (valueA as number) - (valueB as number);
    }

    return this.tradingAccountsSortDirection === 'asc' ? comparison : -comparison;
  }

  // Sorting methods for My Preferences
  private readonly preferencesSortAccessors: Record<string, (row: any) => string | number | null> = {
    displayName: (row) => (row.displayName ?? '').toString().toLowerCase(),
    description: (row) => (row.description ?? '').toString().toLowerCase(),
    value: (row) => (row.value ?? '').toString().toLowerCase(),
  };

  get sortedUserPreferences(): any[] {
    if (!this.preferencesSortColumn || !this.preferencesSortDirection) {
      return this.userPreferences;
    }

    return [...this.userPreferences].sort((a, b) => this.comparePreferences(a, b));
  }

  setPreferencesSort(column: string): void {
    if (this.preferencesSortColumn === column) {
      this.preferencesSortDirection = this.preferencesSortDirection === 'desc' ? 'asc' : 'desc';
      return;
    }

    this.preferencesSortColumn = column;
    this.preferencesSortDirection = 'asc';
  }

  getPreferencesSortIndicator(column: string): string {
    if (this.preferencesSortColumn !== column || !this.preferencesSortDirection) {
      return '';
    }
    return this.preferencesSortDirection === 'asc' ? '↑' : '↓';
  }

  private comparePreferences(a: any, b: any): number {
    const accessor = this.preferencesSortColumn ? this.preferencesSortAccessors[this.preferencesSortColumn] : null;
    if (!accessor || !this.preferencesSortDirection) {
      return 0;
    }

    const valueA = accessor(a);
    const valueB = accessor(b);

    if (valueA == null && valueB == null) { return 0; }
    if (valueA == null) { return this.preferencesSortDirection === 'asc' ? -1 : 1; }
    if (valueB == null) { return this.preferencesSortDirection === 'asc' ? 1 : -1; }

    let comparison: number;
    if (typeof valueA === 'string' && typeof valueB === 'string') {
      comparison = valueA.localeCompare(valueB);
    } else {
      comparison = (valueA as number) - (valueB as number);
    }

    return this.preferencesSortDirection === 'asc' ? comparison : -comparison;
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
  protected readonly faSpinner = faSpinner;
  protected readonly faUserPlus = faUserPlus;
  protected readonly faRightToBracket = faRightToBracket;
  protected readonly faRefresh = faRefresh;
  protected readonly faEye = faEye;
  protected readonly faEyeSlash = faEyeSlash;
  protected readonly faCheck = faCheck;

}

