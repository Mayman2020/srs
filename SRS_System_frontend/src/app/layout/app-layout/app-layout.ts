import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ChatBubbleComponent } from '../chat-bubble/chat-bubble.component';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent, ChatBubbleComponent],
  templateUrl: './app-layout.html',
  styleUrls: ['./app-layout.css']
})
export class AppLayout {
  private readonly sidebarService = inject(SidebarService);

  /** One subscription via signal — avoids leaking a new `collapsed$` subscription per toggle. */
  readonly sidebarCollapsed = toSignal(this.sidebarService.collapsed$, { initialValue: false });
}
