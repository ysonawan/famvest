import {Component, HostBinding, Input} from '@angular/core';
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-big-chip',
  imports: [
    NgIf
  ],
  templateUrl: './big-chip.component.html',
  standalone: true,
  styleUrl: './big-chip.component.css'
})

export class BigChipComponent {
  @Input() text = ''
  @Input() color: 'blue' | 'red' | 'green' | 'gray' = 'gray';

  private styles: Record<string, string> = {
    blue: 'bg-blue-100 text-blue-700',
    red: 'bg-red-100 text-red-700',
    green: 'bg-green-100 text-green-700',
    gray: 'bg-gray-100 text-gray-700',
  };

  @HostBinding('class')
  get classes(): string {
    const base = 'rounded-md mr-2 px-3 py-1';
    return `${base} ${this.styles[this.color]}`;
  }
}
