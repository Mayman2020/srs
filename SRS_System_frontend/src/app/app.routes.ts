import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { permissionCanMatch } from './core/auth/permission.guard';

/**
 * Routing table for the SRS shell.
 *
 * Permission strings use the canonical SCREAMING_SNAKE codes from V7 (e.g.
 * {@code CORRESPONDENCE_VIEW}). The backend resolves both legacy and canonical
 * codes through {@code permission_alias} until Phase 9 removes the legacy set.
 *
 * Legacy `transactions*` URLs are preserved as 301-style redirects to
 * `correspondence*` so deep links from email / saved tabs keep working.
 */
export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component')
        .then(c => c.LoginComponent)
  },

  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent
      )
  },

  {
    path: 'verify/:token',
    loadComponent: () =>
      import('./features/public-verify/public-verify.component').then(
        (m) => m.PublicVerifyComponent
      )
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
        data: { titleKey: 'dashboard.pageTitle', permission: 'DASHBOARD_VIEW' }
      },

      // ===================== Correspondence (canonical path) =====================
      {
        path: 'correspondence',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/transactions/transactions.component')
            .then(m => m.TransactionsComponent),
        data: { titleKey: 'transactions.pageTitle', permission: 'CORRESPONDENCE_VIEW' }
      },
      {
        path: 'correspondence/list/:type',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/transactions-list/transactions-list.component')
            .then(m => m.TransactionsListComponent),
        data: { titleKey: 'transactionsList.title', permission: 'CORRESPONDENCE_VIEW' }
      },
      {
        path: 'correspondence/create',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/create-transaction/create-transaction-component/create-transaction-component')
            .then(m => m.CreateTransactionComponent),
        data: { titleKey: 'createTx.pageTitle', supplyMode: false, permission: 'CORRESPONDENCE_CREATE' }
      },
      {
        path: 'correspondence/supply',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/create-transaction/create-transaction-component/create-transaction-component')
            .then(m => m.CreateTransactionComponent),
        data: { titleKey: 'supplyTx.pageTitle', supplyMode: true, permission: 'CORRESPONDENCE_CREATE' }
      },
      {
        path: 'correspondence/:id',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/new_transaction_details/transaction-details')
            .then(m => m.TransactionDetailsComponent),
        data: { titleKey: 'transactionDetails.pageTitle', permission: 'CORRESPONDENCE_VIEW' }
      },

      // ===================== Legacy transaction paths -> redirects =====================
      { path: 'transactions', redirectTo: 'correspondence', pathMatch: 'full' },
      { path: 'transactions/list/:type', redirectTo: 'correspondence/list/:type', pathMatch: 'full' },
      { path: 'create-transaction', redirectTo: 'correspondence/create', pathMatch: 'full' },
      { path: 'supply-transaction', redirectTo: 'correspondence/supply', pathMatch: 'full' },
      { path: 'transactions/:id', redirectTo: 'correspondence/:id', pathMatch: 'full' },

      {
        path: 'workflow-tasks',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/workflow-task-inbox/workflow-task-inbox.component')
            .then(m => m.WorkflowTaskInboxComponent),
        data: { titleKey: 'workflowTasks.pageTitle', permission: 'WORKFLOW_TASK_VIEW' }
      },

      {
        path: 'registration-desk',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/registration-desk/registration-desk.component').then(
            (m) => m.RegistrationDeskComponent
          ),
        data: { titleKey: 'registrationDesk.pageTitle', permission: 'CORRESPONDENCE_CREATE' }
      },

      {
        path: 'outbound-delivery',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/outbound-delivery/outbound-delivery.component').then(
            (m) => m.OutboundDeliveryComponent
          ),
        data: { titleKey: 'outboundDelivery.pageTitle', permission: 'CORRESPONDENCE_VIEW' }
      },

      {
        path: 'circulars/read-report',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/circular-read-report/circular-read-report.component').then(
            (m) => m.CircularReadReportComponent
          ),
        data: { titleKey: 'circularReadReport.pageTitle', permission: 'CORRESPONDENCE_CREATE' }
      },

      // ===================== Organization =====================
      {
        path: 'org-structure',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/org-structure/org-structure.component').then((m) => m.OrgStructureComponent),
        data: { titleKey: 'orgStructure.pageTitle', permission: 'CORRESPONDENCE_VIEW' }
      },
      {
        path: 'org-structure/tree',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/org-structure/org-structure-tree.component').then(
            (m) => m.OrgStructureTreeComponent
          ),
        data: { titleKey: 'orgStructure.treePageTitle', permission: 'ADMIN_ORG_MANAGE' }
      },
      {
        path: 'org-levels',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/org-levels/org-levels.component').then((m) => m.OrgLevelsComponent),
        data: { titleKey: 'orgLevels.pageTitle', permission: 'ADMIN_ORG_MANAGE' }
      },
      {
        path: 'org-structure/routing-preview',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/org-routing-preview/org-routing-preview.component').then(
            (m) => m.OrgRoutingPreviewComponent
          ),
        data: { titleKey: 'routingPreview.pageTitle', permission: 'CORRESPONDENCE_CREATE' }
      },

      // ===================== HR / leave =====================
      {
        path: 'leave-requests',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/leave-requests/leave-requests.component').then((m) => m.LeaveRequestsComponent),
        data: { titleKey: 'leave.pageTitle', permission: 'LEAVE_SELF' }
      },

      // ===================== Communication =====================
      {
        path: 'notifications',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notifications/notifications')
            .then(m => m.NotificationsComponent),
        data: { titleKey: 'notifications.pageTitle', permission: 'NOTIFICATION_VIEW' }
      },
      {
        path: 'circulars',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/circular-inbox/circular-inbox.component').then(
            (m) => m.CircularInboxComponent
          ),
        data: { titleKey: 'circularInbox.title', subtitleKey: 'circularInbox.subtitle', permission: 'CORRESPONDENCE_VIEW' }
      },
      {
        path: 'circulars/create',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/circular-create/circular-create.component').then(
            (m) => m.CircularCreateComponent
          ),
        data: { titleKey: 'circularCreate.pageTitle', permission: 'CORRESPONDENCE_CREATE' }
      },
      {
        path: 'sms-dispatch',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/sms-dispatch/sms-dispatch.component').then((m) => m.SmsDispatchComponent),
        data: { titleKey: 'smsDispatch.pageTitle', permission: 'NOTIFICATION_DISPATCH' }
      },
      {
        path: 'email-dispatch',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/email-dispatch/email-dispatch.component').then((m) => m.EmailDispatchComponent),
        data: { titleKey: 'emailDispatch.pageTitle', permission: 'NOTIFICATION_DISPATCH' }
      },

      // ===================== Reports =====================
      {
        path: 'reports',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/reports/reports')
            .then(m => m.ReportsComponent),
        data: { titleKey: 'reports.pageTitle', permission: 'REPORT_VIEW' }
      },
      {
        path: 'correspondence-search',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/correspondence-search/correspondence-search.component').then(
            (m) => m.CorrespondenceSearchComponent
          ),
        data: { titleKey: 'correspondenceSearch.pageTitle', permission: 'CORRESPONDENCE_VIEW' }
      },

      // ===================== Delegations =====================
      {
        path: 'delegations',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/delegations/delegations.component').then((m) => m.DelegationsComponent),
        data: { titleKey: 'delegations.pageTitle', permission: 'DELEGATION_MANAGE' }
      },
      {
        path: 'task-delegations',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/task-delegations/task-delegations.component').then(
            (m) => m.TaskDelegationsComponent
          ),
        data: { titleKey: 'taskDelegations.pageTitle', permission: 'TASK_DELEGATION_MANAGE_OWN' }
      },
      {
        path: 'acting-assignments',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/acting-assignments/acting-assignments.component').then(
            (m) => m.ActingAssignmentsComponent
          ),
        data: { titleKey: 'acting.pageTitle', permission: 'ACTING_ASSIGNMENT_VIEW' }
      },

      // ===================== Admin =====================
      {
        path: 'lookup-admin',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/lookup-admin/lookup-admin.component').then((m) => m.LookupAdminComponent),
        data: { titleKey: 'lookupAdmin.pageTitle', permission: 'ADMIN_LOOKUP_MANAGE' }
      },
      {
        path: 'admin/letter-templates',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/letter-template-admin/letter-template-admin.component').then(
            (m) => m.LetterTemplateAdminComponent
          ),
        data: { titleKey: 'letterTemplates.pageTitle', permission: 'LETTER_TEMPLATE_MANAGE' }
      },
      {
        path: 'admin/workflow-routes',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/workflow-routes-admin/workflow-routes-admin.component').then(
            (m) => m.WorkflowRoutesAdminComponent
          ),
        data: { titleKey: 'workflowRoutes.pageTitle', permission: 'ADMIN_USER_MANAGE' }
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
          permission: 'ADMIN_USER_MANAGE'
        }
      },
      {
        path: 'users',
        redirectTo: 'admin-communications-main',
        pathMatch: 'full'
      },
      {
        path: 'roles',
        redirectTo: 'admin-communications-main',
        pathMatch: 'full'
      },
      {
        path: 'audit-events',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/audit-events/audit-events.component').then((m) => m.AuditEventsComponent),
        data: { titleKey: 'audit.pageTitle', permission: 'ADMIN_AUDIT_VIEW' }
      },
      {
        path: 'sla-policies',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/sla-policies/sla-policies.component').then((m) => m.SlaPoliciesComponent),
        data: { titleKey: 'sla.pageTitle', permission: 'SLA_POLICY_MANAGE' }
      },

      {
        path: 'admin/retention/policies',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/retention-policies-admin/retention-policies-admin.component').then(
            (m) => m.RetentionPoliciesAdminComponent
          ),
        data: { titleKey: 'retention.policiesPageTitle', permission: 'RETENTION_POLICY_VIEW' }
      },
      {
        path: 'admin/retention/legal-holds',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/legal-holds-admin/legal-holds-admin.component').then(
            (m) => m.LegalHoldsAdminComponent
          ),
        data: { titleKey: 'retention.legalHoldsPageTitle', permission: 'LEGAL_HOLD_VIEW' }
      },
      {
        path: 'admin/retention/log',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/retention-archive-log/retention-archive-log.component').then(
            (m) => m.RetentionArchiveLogComponent
          ),
        data: { titleKey: 'retention.logPageTitle', permission: 'RETENTION_LOG_VIEW' }
      },
      {
        path: 'admin/notifications/channels',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notification-channels-admin/notification-channels-admin.component').then(
            (m) => m.NotificationChannelsAdminComponent
          ),
        data: { titleKey: 'notificationAdmin.channelsPageTitle', permission: 'NOTIFICATION_CHANNEL_ADMIN' }
      },
      {
        path: 'admin/notifications/outbox',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notification-outbox-admin/notification-outbox-admin.component').then(
            (m) => m.NotificationOutboxAdminComponent
          ),
        data: { titleKey: 'notificationAdmin.outboxPageTitle', permission: 'NOTIFICATION_CHANNEL_ADMIN' }
      },
      {
        path: 'admin/notifications/catalog',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notification-catalog-admin/notification-catalog-admin.component').then(
            (m) => m.NotificationCatalogAdminComponent
          ),
        data: { titleKey: 'notificationCatalogAdmin.pageTitle', permission: 'NOTIFICATION_CHANNEL_ADMIN' }
      },
      {
        path: 'admin/attachment-access-log',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/attachment-access-log-report/attachment-access-log-report.component').then(
            (m) => m.AttachmentAccessLogReportComponent
          ),
        data: { titleKey: 'attachmentAccessLog.pageTitle', permission: 'ATTACHMENT_ACCESS_LOG_VIEW' }
      },

      // ===================== Profile =====================
      {
        path: 'profile/notifications',
        canMatch: [permissionCanMatch],
        loadComponent: () =>
          import('./features/notification-preferences/notification-preferences.component').then(
            (m) => m.NotificationPreferencesComponent
          ),
        data: { titleKey: 'notificationAdmin.prefsPageTitle', permission: 'NOTIFICATION_PREFERENCE_MANAGE' }
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        data: { titleKey: 'profile.pageTitle' }
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
