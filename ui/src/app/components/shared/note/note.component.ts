import {Component, Input} from '@angular/core';
import {faCircleCheck, faCircleInfo, faCircleXmark, faTriangleExclamation} from "@fortawesome/free-solid-svg-icons";
import {FaIconComponent, IconDefinition} from "@fortawesome/angular-fontawesome";
import {NgClass, NgIf} from "@angular/common";

@Component({
  selector: 'app-note',
  imports: [
    NgIf,
    NgClass,
    FaIconComponent
  ],
  templateUrl: './note.component.html',
  standalone: true,
  styleUrl: './note.component.css'
})
export class NoteComponent {

  @Input() type: 'info' | 'warning' | 'error' | 'success' = 'info';
  @Input() dismissible: boolean = false;
  @Input() atCenter: boolean = false;

  dismissed = false;

  // Font Awesome icons
  faCircleInformation = faCircleInfo;
  faWarning = faTriangleExclamation;
  faSuccess = faCircleCheck;
  faError = faCircleXmark;

  get icon(): IconDefinition {
    switch (this.type) {
      case 'warning':
        return this.faWarning;
      case 'error':
        return this.faError;
      case 'success':
        return this.faSuccess;
      case 'info':
      default:
        return this.faCircleInformation;
    }
  }

  closeNote() {
    this.dismissed = true;
  }

}
