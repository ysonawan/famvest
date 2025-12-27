import { Component, Input } from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule} from '@angular/forms';
import { faEye, faEyeSlash } from '@fortawesome/free-solid-svg-icons';
import {FaIconComponent} from "@fortawesome/angular-fontawesome";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-toggle-secret-field',
  imports: [
    ReactiveFormsModule,
    FaIconComponent,
    NgIf
  ],
  standalone: true,
  templateUrl: './toggle-secret-field.component.html'
})
export class ToggleSecretFieldComponent {
  @Input() formGroup!: FormGroup;
  @Input() controlName!: string;
  @Input() label!: string;
  @Input() placeholder: string = '';
  @Input() required: boolean = false;

  getControl(controlName: string): FormControl<any> {
    const control = this.formGroup.get(controlName);
    if (!control) {
      throw new Error(`Control with name '${controlName}' not found`);
    }
    return control as FormControl;
  }

  show = false;
  faEye = faEye;
  faEyeSlash = faEyeSlash;
  protected readonly FormControl = FormControl;
}
