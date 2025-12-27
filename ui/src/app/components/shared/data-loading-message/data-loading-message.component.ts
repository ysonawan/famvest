import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-data-loading-message',
  imports: [],
  templateUrl: './data-loading-message.component.html',
  standalone: true,
  styleUrl: './data-loading-message.component.css'
})
export class DataLoadingMessageComponent {
  @Input() message: string = 'Just a moment, we\'re preparing your view';
}
