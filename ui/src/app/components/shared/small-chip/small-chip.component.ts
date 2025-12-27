import {Component, HostBinding, Input} from '@angular/core';
import {NgIf, UpperCasePipe} from "@angular/common";

@Component({
  selector: 'app-small-chip',
  imports: [
    NgIf,
    UpperCasePipe
  ],
  templateUrl: './small-chip.component.html',
  standalone: true,
  styleUrl: './small-chip.component.css'
})

export class SmallChipComponent {
  @Input() text = ''
  @Input() color: 'blue' | 'red' | 'green' | 'orange' | 'purple' | 'gray' = 'gray';

  private styles: Record<string, string> = {
    blue: 'bg-blue-100 text-blue-700',
    red: 'bg-red-100 text-red-700',
    green: 'bg-green-100 text-green-700',
    orange: 'bg-orange-100 text-orange-700',
    purple: 'bg-purple-100 text-purple-700',
    gray: 'bg-gray-100 text-gray-700',
  };

  @HostBinding('class')
  get classes(): string {
    // Use inline-flex and items-center for vertical alignment, adjust py for height
    const base = 'inline-flex items-center text-[10px] rounded-md px-2';
    return `${base} ${this.styles[this.color]}`;
  }
}
