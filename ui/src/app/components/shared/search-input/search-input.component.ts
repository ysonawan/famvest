import { Component, EventEmitter, Input, Output, ViewChild, ElementRef } from '@angular/core';
import {FormsModule} from "@angular/forms";
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {faSearch} from "@fortawesome/free-solid-svg-icons";
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-search-input',
  templateUrl: './search-input.component.html',
  imports: [
    FormsModule,
    FaIconComponent,
    CommonModule
  ],
  standalone: true,
  styleUrls: ['./search-input.component.css']
})
export class SearchInputComponent {
  @Input() placeholder: string = 'Search...';
  @Output() inputChange = new EventEmitter<string>();
  @Input() searchTerm: string = '';
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

  isExpanded: boolean = false;
  faSearch = faSearch;

  expand(): void {
    this.isExpanded = true;
    // Focus the input after expansion
    setTimeout(() => {
      if (this.searchInput) {
        this.searchInput.nativeElement.focus();
      }
    }, 100);
  }

  collapse(): void {
    if (!this.searchTerm) {
      this.isExpanded = false;
    }
  }

  onBlur(): void {
    // Only collapse if there's no search term
    if (!this.searchTerm) {
      setTimeout(() => {
        this.isExpanded = false;
      }, 150); // Small delay to allow for other interactions
    }
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.inputChange.emit('');
    this.collapse();
  }

  onInputChange(event: Event): void {
    const inputValue = (event.target as HTMLInputElement).value;
    this.searchTerm = inputValue;
    this.inputChange.emit(inputValue);
  }
}
