import {Component, Inject, OnInit} from '@angular/core';
import {NgForOf} from "@angular/common";
import {ReactiveFormsModule} from "@angular/forms";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {DragDropModule} from '@angular/cdk/drag-drop';
import {UtilsService} from "../../../services/utils.service";

@Component({
  selector: 'app-detail-info-dialog',
  imports: [
    NgForOf,
    ReactiveFormsModule,
    DragDropModule
  ],
  templateUrl: './detail-info-dialog.component.html',
  standalone: true,
  styleUrl: './detail-info-dialog.component.css'
})
export class DetailInfoDialogComponent implements OnInit {

  title: string = 'Information';
  flattenedData: any = {};

  constructor(private utilsService: UtilsService,
              private dialogRef: MatDialogRef<DetailInfoDialogComponent>,
              @Inject(MAT_DIALOG_DATA) public data: { sourceData: any, title: string}) {
    if(data.title) {
      this.title = data.title;
    }
  }

  ngOnInit() {
    this.flattenedData = this.flattenObject(this.data.sourceData);
  }

  getKeys(obj: any): string[] {
    return Object.keys(obj);
  }

  toLabel(key: string): string {
    const acronyms = ['URL', 'ID', 'API', 'HTTP', 'HTML', 'SIP', 'MF'];

    return key
      .split('.') // split nested keys
      .map(part => {
        // Convert snake_case or kebab-case to space
        let result = part.replace(/[_-]/g, ' ');
        // Insert space before capital letters
        result = result.replace(/([a-z])([A-Z])/g, '$1 $2');
        // Capitalize each word
        return result
          .split(' ')
          .map(word => {
            const upper = word.toUpperCase();
            return acronyms.includes(upper) ? upper : word.charAt(0).toUpperCase() + word.slice(1);
          })
          .join(' ');
      })
      .join(' - '); // separate levels visually
  }

  formatValue(key: string, value: any): string {
    if (typeof value === 'boolean') {
      return value ? 'Yes' : 'No';
    }

    if (typeof value === 'number') {
      // Handle timestamps (milliseconds)
      if (key.toLowerCase().includes('date') || (value > 1e12 && value < 1e14)) {
        return this.utilsService.formatCustomDate(new Date(value));
      }
      return value.toLocaleString(); // e.g., 1,234.56
    }

    if (typeof value === 'string' && key.toLowerCase().includes('date')) {
      const parsedDate = new Date(value);
      if (!isNaN(parsedDate.getTime())) {
        return this.utilsService.formatCustomDate(parsedDate);
      }
    }

    return value;
  }


  flattenObject(obj: any, parentKey: string = '', result: any = {}, level: number = 1): any {
    if (level > 3) return result; // limit recursion depth to 3
    for (const key of Object.keys(obj)) {
      const newKey = parentKey ? `${parentKey}.${key}` : key;
      const value = obj[key];
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        this.flattenObject(value, newKey, result, level + 1);
      } else {
        result[newKey] = value;
      }
    }
    return result;
  }

  onClose(): void {
    this.dialogRef.close();
  }

}
