import {Component, Input} from '@angular/core';
import {MarketDepthComponent} from "../../dialogs/market-depth/market-depth.component";
import {MatDialog} from "@angular/material/dialog";
import {Overlay} from "@angular/cdk/overlay";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faList} from "@fortawesome/free-solid-svg-icons";
import {NgClass} from "@angular/common";
import {ActionButtonComponent} from "../action-button/action-button.component";
import {MatTooltip} from "@angular/material/tooltip";

@Component({
  selector: 'app-market-depth-invoker',
  imports: [
    FaIconComponent,
    ActionButtonComponent,
    NgClass,
    MatTooltip
  ],
  templateUrl: './market-depth-invoker.component.html',
  standalone: true,
  styleUrl: './market-depth-invoker.component.css'
})
export class MarketDepthInvokerComponent {

  constructor( private dialog: MatDialog,
               private overlay: Overlay) {}

  @Input() title: string = '';
  @Input() instrumentToken: number = 0;
  @Input() iconBackground: boolean = false;
  @Input() iconColor: string = 'gray';

  private textStyles: Record<string, string> = {
    blue: 'text-blue-500 hover:text-blue-600',
    red: 'text-red-500  hover:text-red-600',
    green: 'text-green-500 hover:text-green-600',
    orange: 'text-orange-500 hover:text-orange-600',
    gray: 'text-gray-500 hover:text-gray-600',
    white: 'text-white',
  };

  getTextStyle() {
    return this.textStyles[this.iconColor];
  }

  handleAction() {
    this.openMarketDepthDialog();
  }

  openMarketDepthDialog() {
    const dialogRef = this.dialog.open(MarketDepthComponent, {
      disableClose: true,
      hasBackdrop: false,
      closeOnNavigation: false,
      scrollStrategy: this.overlay.scrollStrategies.noop(),
      data: { title: this.title, instrumentToken: this.instrumentToken}
    });
  }

  protected readonly faList = faList;

}
