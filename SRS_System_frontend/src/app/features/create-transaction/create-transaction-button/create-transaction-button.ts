import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

@Component({
   selector: 'app-create-transaction-button',
 standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './create-transaction-button.html',
  styleUrl: './create-transaction-button.css',
})
export class CreateTransactionButton {

  open = false;

  constructor(private router: Router) {}

  toggleMenu(event: MouseEvent) {
    event.stopPropagation();
    this.open = !this.open;
  }

  close() {
    this.open = false;
  }

  createDefault() {
    this.router.navigate(['/create-transaction']);
  }

  go(path: string) {
    this.open = false;
    this.router.navigate([path]);
  }
}
