import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {NgClass, NgIf} from "@angular/common";
import {FaIconComponent, IconDefinition} from "@fortawesome/angular-fontawesome";
import {
  faEdit,
  faXmark,
  faSync,
  faEye,
  faTrash,
  faB,
  faS,
  faChartSimple,
  faCopy,
  faRightFromBracket,
  faAdd,
  faPause,
  faPlay,
  faUnlink,
  faLink,
  faRightToBracket,
  faList,
  faAngleLeft,
  faAngleRight, faUserPlus, faExpand, faCartPlus, faAngleUp, faAngleDown, faCircleInfo, faRocket,
} from "@fortawesome/free-solid-svg-icons";
import {faYahoo} from "@fortawesome/free-brands-svg-icons";

import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-action-button',
  imports: [
    NgClass,
    FaIconComponent,
    NgIf,
    MatTooltip
  ],
  templateUrl: './action-button.component.html',
  standalone: true,
  styleUrl: './action-button.component.css'
})
export class ActionButtonComponent implements OnInit {

  ngOnInit(): void {
  }

  @Input() title = '';
  @Input() label!: string;
  @Input() color: 'blue' | 'red' | 'green' | 'orange' | 'gray' = 'gray';
  @Input() action: string = 'view';
  @Input() row?: any;
  @Input() noIconBackground: boolean = false;

  @Output() handleAction = new EventEmitter<{ action: string, row: any }>();

  private bgStyles: Record<string, string> = {
    blue: 'bg-blue-500 text-white hover:bg-blue-600 shadow-sm',
    red: 'bg-red-500 text-white hover:bg-red-600 shadow-sm',
    green: 'bg-green-500 text-white hover:bg-green-600 shadow-sm',
    orange: 'bg-orange-500 text-white hover:bg-orange-600 shadow-sm',
    gray: 'bg-gray-500 text-white hover:bg-gray-700 shadow-sm',
  };

  private textStyles: Record<string, string> = {
    blue: 'text-blue-500 hover:text-blue-600',
    red: 'text-red-500  hover:text-red-600',
    green: 'text-green-500 hover:text-green-600',
    orange: 'text-orange-500 hover:text-orange-600',
    gray: 'text-gray-600 hover:text-gray-700',
  };

  buttonStyle() : string {
    if(!this.noIconBackground) {
      return this.bgStyles[this.color];
    } else {
      return this.textStyles[this.color];
    }
  }

  buttonColor() : string {
    if(!this.noIconBackground) {
      return 'text-white';
    } else {
      return this.textStyles[this.color];
    }
  }

  handleClick(action: string, row: any) {
    this.handleAction.emit({action, row});
  }

  // Font Awesome icons
  view = faEye;
  add = faAdd;
  addUser = faUserPlus;
  modify = faEdit;
  cancel = faXmark;
  exit = faRightFromBracket;
  delete = faTrash;
  refresh = faSync;
  chart = faChartSimple;
  copy = faCopy;
  buy = faB;
  sell = faS;
  pause = faPause;
  activate = faPlay;
  info = faCircleInfo
  unlink = faUnlink;
  link = faLink;
  login = faRightToBracket;
  list = faList
  collapseLeft = faAngleLeft;
  enlargeRight = faAngleRight;
  collapseUp = faAngleUp;
  enlargeDown = faAngleDown;
  expand = faExpand;
  addToCart = faCartPlus;
  execute = faRocket;
  yahooFinance = faYahoo;
  basket = faCartPlus;

  get icon(): IconDefinition {
    switch (this.action) {
      case 'view':
        return this.view;
      case 'add':
        return this.add;
      case 'addUser':
        return this.addUser;
      case 'modify':
        return this.modify;
      case 'cancel':
        return this.cancel;
      case 'exit':
        return this.exit;
      case 'delete':
        return this.delete;
      case 'refresh':
        return this.refresh;
      case 'chart':
        return this.chart;
      case 'copy':
        return this.copy;
      case 'buy':
        return this.buy;
      case 'sell':
        return this.sell;
      case 'pause':
        return this.pause;
      case 'activate':
        return this.activate;
      case 'unlink':
        return this.unlink;
      case 'link':
        return this.link;
      case 'login':
        return this.login;
      case 'list':
        return this.list;
      case 'collapseLeft':
        return this.collapseLeft;
      case 'enlargeRight':
        return this.enlargeRight;
      case 'collapseUp':
        return this.collapseUp;
      case 'enlargeDown':
        return this.enlargeDown;
      case 'expand':
        return this.expand;
      case 'addToCart':
        return this.addToCart;
      case 'execute':
        return this.execute;
      case 'yahooFinance':
        return this.yahooFinance;
      case 'basket':
        return this.basket;
      case 'info':
      default:
        return this.info;
    }
  }
}
