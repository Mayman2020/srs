export const AppConstants = {
  API: {
    ADMIN: '/admin',
    ATTACHMENTS: '/attachments',
    AUTH: '/auth',
    AUTHORITY_DELEGATIONS: '/authority-delegations',
    CIRCULARS: '/circulars',
    CORRESPONDENCE: '/correspondence',
    DASHBOARD: '/dashboard',
    DEPARTMENTS: '/departments',
    LEAVE_REQUESTS: '/leave-requests',
    ADMIN_LEAVE_REQUESTS: '/admin/leave-requests',
    LETTER_TEMPLATES: '/letter-templates',
    LOOKUPS: '/lookups',
    LOOKUP_TABLE_ADMIN: '/admin/lookup-tables',
    ME_CAPABILITIES: '/me/capabilities',
    NOTIFICATIONS: '/notifications',
    ORGANIZATIONS: '/organizations',
    PROFILE_ME: '/profile/me',
    REPORTS: '/reports',
    ROLES: '/roles',
    SYSTEM_ISSUES_REPORT: '/system-issues/report',
    USERS: '/users',
    WORKFLOW_ROUTES: '/workflow-routes',
    AUDIT: '/audit',
    NOTIFICATION_DISPATCH: '/notifications/dispatch',
    ORG_LEVELS: '/organization/levels',
    ORG_ROUTING_PREVIEW: '/organization/routing/preview',
    WORKFLOW_TASKS: '/workflow/tasks',
    TASK_DELEGATIONS: '/delegations/tasks',
    ACTING_ASSIGNMENTS: '/acting-assignments',
    SLA_POLICIES_ADMIN: '/admin/sla/policies',
    SLA_BREACHES_ADMIN: '/admin/sla/breaches',
    SLA_TASK_STATUS: '/sla/tasks',
    SIGNATURES: '/signatures',
    VERIFY: '/verify',
    ATTACHMENT_DOWNLOAD_INTENT_SUFFIX: '/download-intent',
    ATTACHMENT_TOKEN_DOWNLOAD: '/attachments/download',
    PUBLIC_VERIFY: '/public/verify',
    NOTIFICATION_PREFERENCES_ME: '/me/notification-preferences',
    NOTIFICATION_CATALOG: '/notification-catalog',
    NOTIFICATION_CHANNEL_TARGETS: '/notification-channel-targets',
    NOTIFICATION_OUTBOX_ADMIN: '/notification-outbox',
    RETENTION_POLICIES_ADMIN: '/retention/policies',
    LEGAL_HOLDS: '/retention/legal-holds',
    ARCHIVE_LOG_ADMIN: '/retention/archive-log'
  },

  LOOKUP: {
    CORRESPONDENCE_TYPE: 'correspondence_type',
    CORRESPONDENCE_STATUS: 'correspondence_status',
    PRIORITY: 'priority',
    CONFIDENTIALITY: 'confidentiality',
    CLASSIFICATION: 'classification',
    ATTACHMENT_CONTENT_TYPE: 'attachment_content_type',
    NOTIFICATION_EVENT_TYPE: 'notification_event_type',
    WORKFLOW_ACTION_TYPE: 'workflow_action_type',
    WORKFLOW_HISTORY_EVENT_TYPE: 'workflow_history_event_type',
    WORKFLOW_INSTANCE_STATUS: 'workflow_instance_status',
    ORG_VISUAL_NODE_STATUS: 'org_visual_node_status'
  }
} as const;

export function apiPath(base: string, path: string): string {
  return `${base.replace(/\/$/, '')}${path}`;
}

export function apiPathWithId(base: string, path: string, id: string | number): string {
  return `${apiPath(base, path)}/${encodeURIComponent(String(id))}`;
}
