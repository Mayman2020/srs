import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { permissionCanMatch } from './core/auth/permission.guard';

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
    component: MainLayoutComponent,
    children: [
      {
        path: 'dashboard',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent),
        data: { titleKey: 'dashboard.pageTitle', permission: 'VIEW_DASHBOARD' }
      },
      {
        path: 'transactions',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/transactions/transactions.component')
            .then(m => m.TransactionsComponent),
        data: { titleKey: 'transactions.pageTitle', permission: 'VIEW_TRANSACTIONS' }
      },
      {
        path: 'transactions/list/:type',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/transactions-list/transactions-list.component')
            .then(m => m.TransactionsListComponent),
        data: { titleKey: 'transactionsList.title', permission: 'VIEW_TRANSACTIONS' }
      },
      {
        path: 'create-transaction',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/create-transaction/create-transaction-component/create-transaction-component')
            .then(m => m.CreateTransactionComponent),
        data: { titleKey: 'createTx.pageTitle', supplyMode: false, permission: 'CREATE_TRANSACTION' }
      },
      {
        path: 'supply-transaction',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/create-transaction/create-transaction-component/create-transaction-component')
            .then(m => m.CreateTransactionComponent),
        data: { titleKey: 'supplyTx.pageTitle', supplyMode: true, permission: 'CREATE_TRANSACTION' }
      },
      {
        path: 'org-structure',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/org-structure/org-structure.component').then((m) => m.OrgStructureComponent),
        data: { titleKey: 'orgStructure.pageTitle', permission: 'correspondence.view' }
      },
      {
        path: 'leave-requests',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/leave-requests/leave-requests.component').then((m) => m.LeaveRequestsComponent),
        data: { titleKey: 'leave.pageTitle', permission: 'leave.self' }
      },
      {
        path: 'notifications',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notifications/notifications')
            .then(m => m.NotificationsComponent),
        data: { titleKey: 'notifications.pageTitle', permission: 'correspondence.view' }
      },
      {
        path: 'circulars',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/circular-inbox/circular-inbox.component').then(
            (m) => m.CircularInboxComponent
          ),
        data: { titleKey: 'circularInbox.title', subtitleKey: 'circularInbox.subtitle', permission: 'correspondence.view' }
      },
      {
        path: 'reports',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/reports/reports')
            .then(m => m.ReportsComponent),
        data: { titleKey: 'reports.pageTitle', permission: 'report.view' }
      },
      {
        path: 'correspondence-search',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/correspondence-search/correspondence-search.component').then(
            (m) => m.CorrespondenceSearchComponent
          ),
        data: { titleKey: 'correspondenceSearch.pageTitle', permission: 'correspondence.view' }
      },
      {
        path: 'delegations',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/delegations/delegations.component').then((m) => m.DelegationsComponent),
        data: { titleKey: 'delegations.pageTitle', permission: 'correspondence.view' }
      },
      {
        path: 'lookup-admin',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/lookup-admin/lookup-admin.component').then((m) => m.LookupAdminComponent),
        data: { titleKey: 'lookupAdmin.pageTitle', permission: 'lookup.manage' }
      },
      {
        path: 'admin-communications-main',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/admin-communications-main/administration.component').then(
            (m) => m.AdministrationComponent
          ),
        data: {
          defaultAdminTab: 'users',
          titleKey: 'admin.pageTitle',
          subtitleKey: 'admin.pageSubtitle',
          permission: 'lookup.manage'
        }
      },
      {
        path: 'users',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/users/users').then((m) => m.UsersComponent),
        data: { titleKey: 'users.pageTitle', permission: 'user.manage' }
      },
      {
        path: 'roles',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/roles/roles').then((m) => m.RolesComponent),
        data: { titleKey: 'roles.pageTitle', permission: 'role.manage' }
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        data: { titleKey: 'profile.pageTitle' }
      },

      {
        path: 'transactions/:id',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/new_transaction_details/transaction-details')
            .then(m => m.TransactionDetailsComponent),
        data: { titleKey: 'transactionDetails.pageTitle', permission: 'VIEW_TRANSACTIONS' }
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
