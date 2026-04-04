import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ChatBubbleComponent } from '../chat-bubble/chat-bubble.component';
import { SidebarService } from '../../services/sidebar.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent , TopbarComponent , ChatBubbleComponent],
  templateUrl: './app-layout.html',
  styleUrls: ['./app-layout.css']
})
export class AppLayout {


  
collapsed = false;

constructor(private sidebarService: SidebarService) {}

ngOnInit() {
  this.sidebarService.collapsed$.subscribe(val => {
    this.collapsed = val;
  });
}


  user = {
    name: 'مستخدم تجريبي',
    role: 'موظف'
  };

  toggleTheme() {
    const root = document.documentElement;
    root.dataset['theme'] =
      root.dataset['theme'] === 'dark' ? 'light' : 'dark';
  }

  logout() {
    sessionStorage.clear();
    location.href = '/login';
  }
}
