import {Component, Input, OnInit,} from '@angular/core';
import {NgForOf, NgIf, DecimalPipe} from "@angular/common";
import {SmallChipComponent} from "../small-chip/small-chip.component";
import {NoteComponent} from "../note/note.component";
import {expandCollapseAnimation} from "../animations";

@Component({
  selector: 'app-charges-summary',
  standalone: true,
  imports: [
    NgForOf,
    NgIf,
    DecimalPipe,
    SmallChipComponent,
    NoteComponent
  ],
  templateUrl: './charges-summary.component.html',
  styleUrl: './charges-summary.component.css',
  animations: [expandCollapseAnimation]
})
export class ChargesSummaryComponent implements OnInit {
  orders: any[] = [];
  totalCharges = {
    brokerage: 0,
    stt: 0,
    stampDuty: 0,
    exchangeTurnover: 0,
    sebiTurnover: 0,
    gst: 0,
    total: 0
  };

  ngOnInit(): void {
  }

  showChargesDetails(charges: any[]): void {
    this.resetTotalCharges();
    this.orders = [];
    charges.forEach((account: any) => {
        (account.contractNoteDtos || []).forEach((note: any) => {
          this.orders.push(note);
          this.totalCharges.brokerage += note.contractNote.charges?.brokerage || 0;
          this.totalCharges.stt += note.contractNote.charges?.transactionTax || 0;
          this.totalCharges.stampDuty += note.contractNote.charges?.stampDuty || 0;
          this.totalCharges.exchangeTurnover += note.contractNote.charges?.exchangeTurnoverCharge || 0;
          this.totalCharges.sebiTurnover += note.contractNote.charges?.SEBITurnoverCharge || 0;
          this.totalCharges.gst += note.contractNote.charges?.gst?.total || 0;
          this.totalCharges.total += note.contractNote.charges?.total || 0;
        });
      });
  }

  resetTotalCharges(): void {
    this.totalCharges = {
      brokerage: 0,
      stt: 0,
      stampDuty: 0,
      exchangeTurnover: 0,
      sebiTurnover: 0,
      gst: 0,
      total: 0
    };
  }

  getTransactionTypeChipColor(status: string): "blue" | "red" | "green" | "gray" {
    switch (status) {
      case "BUY": return "blue";
      case "SELL": return "red";
      default: return "gray";
    }
  }

}

