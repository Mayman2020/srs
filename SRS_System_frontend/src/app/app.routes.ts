import { Routes } from '@angular/router';
import { AppLayout } from './layout/app-layout/app-layout';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component')
        .then(c => c.LoginComponent)
  },

  {
    path: '',
    component: AppLayout,
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./features/transactions/transactions.component')
            .then(m => m.TransactionsComponent)
      },
      {
        path: 'transactions/list/:type',
        loadComponent: () =>
          import('./features/transactions-list/transactions-list.component')
            .then(m => m.TransactionsListComponent)
      },
      {
        path: 'create-transaction',
        loadComponent: () =>
          import('./features/create-transaction/create-transaction-component/create-transaction-component')
            .then(m => m.CreateTransactionComponent)
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications')
            .then(m => m.NotificationsComponent)
      },
      {
        path: 'circulars',
        loadComponent: () =>
          import('./features/circular-inbox/circular-inbox.component').then(
            (m) => m.CircularInboxComponent
          ),
      },
      {
        path: 'reports',
        loadComponent: () =>
          import('./features/reports/reports')
            .then(m => m.ReportsComponent)
      },
      {
        path: 'admin-communications-main',
        loadComponent: () =>
          import('./features/admin-communications-main/administration.component').then(
            (m) => m.AdministrationComponent
          ),
        data: { defaultAdminTab: 'users' }
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/users/users').then((m) => m.UsersComponent)
      },
      {
        path: 'roles',
        loadComponent: () =>
          import('./features/roles/roles').then((m) => m.RolesComponent)
      },


      {
        path: 'transactions/:id', loadComponent: () =>
          import('./features/new_transaction_details/transaction-details')
            .then(m => m.TransactionDetailsComponent)
      }




    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
