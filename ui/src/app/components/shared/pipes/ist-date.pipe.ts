import { Pipe, PipeTransform } from '@angular/core';
import { ApplicationPropertiesService } from "../../../application-properties.service";

@Pipe({
  standalone: true,
  name: 'istDate'
})
export class IstDatePipe implements PipeTransform {

  constructor(private applicationProperties: ApplicationPropertiesService) { }

  transform(value: number | string | Date, format: 'time' | 'time-no-mod' | 'time-force-mod' | 'date' | 'datetime' | 'datetime-ordinal' | 'datetime-ordinal-no-mod' | 'date-ordinal' = 'time'): string {
    if (!value) return '';

    if (typeof value === 'string' && /^\d{2}:\d{2}:\d{2}$/.test(value)) {
      value = `1970-01-01T${value}Z`;
    }

    let inputDate = new Date(value);
    if (this.applicationProperties.getEnvironment() === 'prod' && format !== 'time-no-mod' && format !== 'datetime-ordinal-no-mod') {
      // Manually subtract IST offset in prod to "undo" default UTC interpretation
      const utcMillis = inputDate.getTime();
      const istMillis = utcMillis - (5.5 * 60 * 60 * 1000); // minus 5:30 hours
      inputDate = new Date(istMillis);
    }

    const timeZone = 'Asia/Kolkata';

    if (format === 'date') {
      return inputDate.toLocaleDateString('en-IN', {
        timeZone,
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } else if (format === 'datetime') {
      return inputDate.toLocaleString('en-IN', {
        timeZone,
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    } else if (format === 'time-no-mod') {
      return inputDate.toLocaleTimeString('en-IN', {
        timeZone,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    } else if (format === 'date-ordinal') {
      const day = inputDate.getDate();
      const month = inputDate.toLocaleString('en-IN', { timeZone, month: 'short' }); // e.g., Jul
      const year = inputDate.getFullYear();
      return `${getOrdinal(day)} ${month} ${year}`;
    }else if (format === 'datetime-ordinal' || format === 'datetime-ordinal-no-mod') {
      const day = inputDate.getDate();
      const ordinalDay = getOrdinal(day);
      const month = inputDate.toLocaleString('en-IN', { timeZone, month: 'short' });
      const year = inputDate.getFullYear();
      const time = inputDate.toLocaleTimeString('en-IN', {
        timeZone,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true,
      });
      return `${ordinalDay} ${month} ${year}, ${time}`;
    } else {
      // Default: time only
      return inputDate.toLocaleTimeString('en-IN', {
        timeZone,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
      });
    }
    function getOrdinal(day: number): string {
      if (day > 3 && day < 21) return `${day}th`; // 11th–13th
      switch (day % 10) {
        case 1: return `${day}st`;
        case 2: return `${day}nd`;
        case 3: return `${day}rd`;
        default: return `${day}th`;
      }
    }
  }
}
