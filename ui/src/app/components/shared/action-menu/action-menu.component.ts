import {Component, Output, EventEmitter, Input, OnInit} from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {NgForOf, NgIf} from "@angular/common";
import {ActionButtonComponent} from "../action-button/action-button.component";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faEllipsisV, faRefresh} from "@fortawesome/free-solid-svg-icons";

@Component({
  selector: 'app-action-menu',
  standalone: true,
  imports: [MatMenuModule, MatButtonModule, MatIconModule, NgForOf, ActionButtonComponent, NgIf, FaIconComponent],
  templateUrl: './action-menu.component.html',
})
export class ActionMenuComponent implements OnInit {

  @Output() handleAction = new EventEmitter<{ action: string, row: any }>();
  @Input() actions: any[] = [];
  @Input() row?: any;

  handleClick(event: {action: string, row: any}) {
    this.handleAction.emit(event);
  }

  ngOnInit() {
  }

  protected readonly faRefresh = faRefresh;
  protected readonly faEllipsisV = faEllipsisV;
}
