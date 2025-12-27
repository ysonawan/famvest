import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  success(message: string) {
    Swal.fire('Success', message, 'success');
  }

  error(message: string) {
    Swal.fire('Error', message, 'error');
  }

  info(title: string, text: string, confirmCallback: () => void) {
    Swal.fire({
      title,
      text,
      icon: 'info',
      confirmButtonText: 'Ok',
      confirmButtonColor: '#10B981' // Tailwind's green-500
    }).then((result) => {
      if (result.isConfirmed) {
        confirmCallback();
      }
    });
  }

  confirm(title: string, text: string, confirmCallback: () => void) {
    Swal.fire({
      title,
      text,
      icon: 'warning',
      confirmButtonColor: '#10B981',
      cancelButtonColor: '#6b7280',
      showCancelButton: true,
      confirmButtonText: 'Yes',
      cancelButtonText: 'Cancel',
    }).then((result) => {
      if (result.isConfirmed) {
        confirmCallback();
      }
    });
  }
}
