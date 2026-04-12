import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { ChatBubbleComponent } from '../chat-bubble/chat-bubble.component';
import { SidebarService } from '../../services/sidebar.service';
import { HeaderContainerComponent } from '../../shared/components/header/header-container.component';
import { FooterMainComponent } from '../../shared/components/footer-main/footer-main.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    SidebarComponent,
    ChatBubbleComponent,
    HeaderContainerComponent,
    FooterMainComponent
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {
  private readonly sidebarService = inject(SidebarService);

  readonly sidebarCollapsed = toSignal(this.sidebarService.collapsed$, { initialValue: false });
}
