import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-dark-light-mode-switch-main',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dark-light-mode-switch-main.component.html',
  styleUrl: './dark-light-mode-switch-main.component.scss'
})
export class DarkLightModeSwitchMainComponent {
  @Input() checked = false;
  @Input() compact = false;
  @Input() ariaLabel = '';

  @Output() toggled = new EventEmitter<void>();
}

