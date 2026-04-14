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
    WORKFLOW_ROUTES: '/workflow-routes'
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
