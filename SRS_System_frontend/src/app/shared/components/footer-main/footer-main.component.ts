import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { UiFormatService } from '../../../core/i18n/ui-format.service';

@Component({
  selector: 'app-footer-main',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './footer-main.component.html',
  styleUrl: './footer-main.component.scss'
})
export class FooterMainComponent {
  readonly year = new Date().getFullYear();

  constructor(private readonly format: UiFormatService) {}

  formattedYear(): string {
    return this.format.formatNumber(this.year, { useGrouping: false });
  }
}

