import { Component, Input } from '@angular/core';
import { CommonModule, NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-user-info-header',
  standalone: true,
  imports: [CommonModule, NgOptimizedImage],
  templateUrl: './user-info-header.component.html',
  styleUrl: './user-info-header.component.css'
})
export class UserInfoHeaderComponent {
  @Input() userId!: string;
  @Input() userName!: string;
  @Input() fullName!: string;
  @Input() avatarUrl!: string;
  @Input() fallbackAvatarUrl: string = '';
}
