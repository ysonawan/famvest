import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import { TradingAccountService } from '../../../services/trading-account.service';
import {fallbackAvatarUrl, multiUserAvatarUrl} from "../../../constants/constants";
import {UserDataStateService} from "../../../services/user-data-state-service";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-user-filter',
  imports: [CommonModule, NgOptimizedImage, FormsModule],
  templateUrl: './user-filter.component.html',
  standalone: true,
  styleUrls: ['./user-filter.component.css']
})
export class UserFilterComponent implements OnInit {

  constructor(private userService: TradingAccountService,
              private userDataStateService: UserDataStateService) {

  }

  ngOnInit(): void {
   this.fetchTradingAccounts();
  }

  users: any[] = [];
  filteredUsers: any[] = [];
  @Input() selectedUserIds: string[] = [];

  @Output() selectAllUsers = new EventEmitter<void>();
  @Output() selectUsers = new EventEmitter<string[]>();
  @Output() totalUsers = new EventEmitter<string[]>();

  // Dropdown state
  isDropdownOpen = false;
  searchTerm = '';
  maxDisplayAvatars = 3;

  getCachedData(): void {
    const userDataState = this.userDataStateService.getState();
    if(userDataState && Object.keys(userDataState).length > 0 && userDataState.users) {
      this.users = userDataState.users;
      this.filteredUsers = [...this.users];
      this.totalUsers.emit(this.users);
    }
  }

  setCachedData(): void {
    this.userDataStateService.setState({
      users: this.users
    });
  }

  fetchTradingAccounts(): void {
    this.getCachedData();
    this.userService.getTradingAccounts().subscribe({
      next: (response) => {
        this.users = response.data.filter(user => user.active);
        this.filteredUsers = [...this.users];
        this.totalUsers.emit(response.data);
        this.setCachedData();
      },
    });
  }

  onAllUserClick(): void {
    this.selectedUserIds = [];
    this.selectAllUsers.emit();
    this.closeDropdown();
  }

  toggleUserSelection(userId: string): void {
    if (this.getUserByUserId(userId)?.tokenStatus !== 'Valid') {
      return;
    }

    this.selectedUserIds.includes(userId)
      ? (this.selectedUserIds = this.selectedUserIds.filter(id => id !== userId))
      : this.selectedUserIds.push(userId);
    this.selectUsers.emit(this.selectedUserIds);
  }

  // New dropdown methods
  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.searchTerm = '';
      this.filteredUsers = [...this.users];
    }
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
    this.searchTerm = '';
  }

  onSearchChange(): void {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) {
      this.filteredUsers = [...this.users];
    } else {
      this.filteredUsers = this.users.filter(user =>
        user.name.toLowerCase().includes(term) ||
        user.userId.toLowerCase().includes(term)
      );
    }
  }

  clearSelection(): void {
    this.selectedUserIds = [];
    this.selectUsers.emit(this.selectedUserIds);
  }

  getFilteredUsers(): any[] {
    return this.filteredUsers;
  }

  getDisplayUserIds(): string[] {
    return this.selectedUserIds.slice(0, this.maxDisplayAvatars);
  }

  getSelectionText(): string {
    if (this.selectedUserIds.length === 0) {
      return 'All Accounts';
    } else if (this.selectedUserIds.length === 1) {
      const user = this.getUserByUserId(this.selectedUserIds[0]);
      return user?.name || this.selectedUserIds[0];
    } else {
      return `Users Selected`;
    }
  }

  getAvatarUrl(userId: string): string | null {
    const user = this.getUserByUserId(userId);
    return user?.profile?.avatarURL || null;
  }

  getUserName(userId: string): string {
    const user = this.getUserByUserId(userId);
    return user?.name || userId;
  }

  private getUserByUserId(userId: string): any {
    return this.users.find(user => user.userId === userId);
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
  protected readonly multiUserAvatarUrl = multiUserAvatarUrl;
}
