import {Component, Inject, OnInit} from '@angular/core';
import {DOCUMENT, NgForOf, NgIf} from "@angular/common";
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ToastrService} from "ngx-toastr";
import {UserViewStateService} from "../../services/user-view-state-service";
import {faSearch} from "@fortawesome/free-solid-svg-icons";
import {debounceTime, distinctUntilChanged, Subject} from "rxjs";
import {WatchlistService} from "../../services/watchlist.service";
import {HistoricalCandleDataService} from "../../services/historical-candle-data.service";

@Component({
  selector: 'app-historical-data',
  imports: [
    ReactiveFormsModule,
    NgForOf,
    NgIf
  ],
  templateUrl: './historical-data.component.html',
  styleUrl: './historical-data.component.css'
})
export class HistoricalDataComponent implements OnInit {

  historicalForm: FormGroup;

  intervals = ['minute', '3minute', '5minute', '10minute', '15minute', '30minute', '60minute', 'day'];

  constructor(private fb: FormBuilder,
              @Inject(DOCUMENT) private document: Document,
              private toastrService: ToastrService,
              private userViewStateService: UserViewStateService,
              private watchlistService: WatchlistService,
              private historicalCandleDataService: HistoricalCandleDataService) {
    this.historicalForm = this.fb.group({
      instrument: ['', Validators.required],
      instrumentToken: ['', Validators.required],
      interval: ['5minute', Validators.required],
      from: ['', Validators.required],
      to: ['', Validators.required],
      continuous: [false],
      oi: [false]
    });
  }

  submit() {
    if (this.historicalForm.invalid) {
      this.historicalForm.markAllAsTouched();
      return;
    }
    const formData = {
      ...this.historicalForm.value,
      from: new Date(this.historicalForm.value.from).toISOString(), // e.g. '2025-07-14T09:15:00.000Z'
      to: new Date(this.historicalForm.value.to).toISOString()
    };
    this.historicalCandleDataService.getHistoricalCandleData(formData).subscribe({
      next: (response) => {
        const blob = new Blob([response.message!], { type: 'text/csv' });

        const downloadLink = document.createElement('a');
        const url = window.URL.createObjectURL(blob);
        downloadLink.href = url;
        downloadLink.download = 'historical_data.csv';
        downloadLink.click();
        window.URL.revokeObjectURL(url); // Cleanup
      },
      error: (error) => {
        if(error.error.message) {
          this.toastrService.error(error.error.message, 'Error');
        } else {
          this.toastrService.error('An unexpected error occurred while fetching historical candle data. Verify that the backend service is operational.', 'Error');
        }
      }
    });
  }

  ngOnInit(): void {
    // Set up the debounce logic
    this.searchTermChanged.pipe(
      debounceTime(300), // Wait for 300ms pause in events
      distinctUntilChanged() // Only emit if the value has changed
    ).subscribe(term => {
      this.searchInstruments(term);
    });
  }

  fetchedInstruments: any[] = [];
  searchInstruments(term: string): void {
    this.watchlistService.searchInstruments(term.trim()).subscribe({
      next: (response) => {
        this.fetchedInstruments = response.data;
      },
      error: (error) => {
        console.error('Error while fetching instruments:', error);
        this.toastrService.error(error.error.message, 'Error');
      }
    });
  }


  selectInstrument(instrument: any) {
    this.historicalForm.patchValue({
      instrument: `${instrument.displayName} (${instrument.exchange})`,
      instrumentToken: instrument.instrumentToken,
    });
    this.fetchedInstruments = []; // Clear fetched instruments after adding
  }

  searchTermChanged: Subject<string> = new Subject<string>();
  searchTerm: string = '';
  onSearchTermChange(event: Event): void {
    const inputValue = (event.target as HTMLInputElement).value;
    this.searchTermChanged.next(inputValue); // Emit the search term
  }

  protected readonly faSearch = faSearch;
}
