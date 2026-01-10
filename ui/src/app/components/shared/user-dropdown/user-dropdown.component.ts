import {Component, EventEmitter, Input, OnInit, Output, ViewChild, ElementRef} from '@angular/core';
import {CommonModule, NgOptimizedImage} from '@angular/common';
import { TradingAccountService } from '../../../services/trading-account.service';
import {fallbackAvatarUrl} from "../../../constants/constants";
import {UserDataStateService} from "../../../services/user-data-state-service";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-user-dropdown',
  imports: [CommonModule, NgOptimizedImage, FormsModule],
  templateUrl: './user-dropdown.component.html',
  standalone: true,
  styleUrls: ['./user-dropdown.component.css']
})
export class UserDropdownComponent implements OnInit {

  constructor(private userService: TradingAccountService,
              private userDataStateService: UserDataStateService) {
  }

  @ViewChild('dropdownContainer') dropdownContainer!: ElementRef;
  @ViewChild('dropdownButton') dropdownButton!: ElementRef;

  ngOnInit(): void {
    this.fetchTradingAccounts();
  }

  users: any[] = [];
  filteredUsers: any[] = [];
  @Input() selectedUserId: string = '';
  @Input() placeholder: string = 'Select Trading Account';
  @Input() disabled: boolean = false;
  @Input() openAbove: boolean = false;

  @Output() selectedUserIdChange = new EventEmitter<string>();
  @Output() selectUser = new EventEmitter<string>();
  @Output() totalUsers = new EventEmitter<any[]>();

  // Dropdown state
  isDropdownOpen = false;
  searchTerm = '';

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

  selectUserById(userId: string): void {
    if (this.getUserByUserId(userId)?.tokenStatus !== 'Valid') {
      return;
    }
    this.selectedUserId = userId;
    this.selectUser.emit(userId);
    this.selectedUserIdChange.emit(userId); // Emit the change event
    this.closeDropdown();
  }

  toggleDropdown(): void {
    if (this.disabled) {
      return;
    }
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

  getFixedPositionStyles(): string {
    if (!this.dropdownButton?.nativeElement) {
      return '';
    }
    try {
      const rect = this.dropdownButton.nativeElement.getBoundingClientRect();
      const bottom = window.innerHeight - rect.top + 8;
      return `position: fixed; left: ${rect.left}px; width: ${rect.width}px; bottom: ${bottom}px; top: auto; right: auto; z-index: 9999;`;
    } catch (e) {
      return '';
    }
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

  getFilteredUsers(): any[] {
    return this.filteredUsers;
  }

  getSelectionText(): string {
    if (!this.selectedUserId) {
      return this.placeholder;
    }
    const user = this.getUserByUserId(this.selectedUserId);
    return (user?.profile?.userName || user?.name) + ' - ' + user?.userId;
  }

  getAvatarUrl(userId: string): string | null {
    const user = this.getUserByUserId(userId);
    return user?.profile?.avatarURL || null;
  }

  getUserName(userId: string): string {
    const user = this.getUserByUserId(userId);
    return user?.profile?.userName || user?.name || userId;
  }

  getSelectedUser(): any {
    return this.getUserByUserId(this.selectedUserId);
  }

  private getUserByUserId(userId: string): any {
    return this.users.find(user => user.userId === userId);
  }

  protected readonly fallbackAvatarUrl = fallbackAvatarUrl;
}
