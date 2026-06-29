/** Values of `correspondence_status.kpi_segment` (see Flyway V26, backend `KpiSegmentCodes`). */
export const CorrespondenceKpiSegment = {
  SLA_DONE: 'SLA_DONE',
  PIPELINE: 'PIPELINE',
  INBOX: 'INBOX',
} as const;

export interface LookupItemDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
  /** Parent lookup id when row is scoped (e.g. status→type, classification tree). */
  parentId?: number | null;
  /** Correspondence status: lifecycle terminal flag from DB. */
  terminal?: boolean | null;
  /** Correspondence status: home-dashboard KPI segment (SLA_DONE / PIPELINE / INBOX). */
  kpiSegment?: string | null;
  /** Correspondence type: highlight as inbound traffic in list KPIs. */
  dashboardInboundHighlight?: boolean | null;
  /** Correspondence type: highlight as outbound traffic in list KPIs. */
  dashboardOutboundHighlight?: boolean | null;
  /** Correspondence status: badge style from DB (`ui_variant`). */
  uiVariant?: string | null;
}

/** GET /api/v1/admin/lookup-tables/catalog */
export interface LookupCatalogDto {
  lookupCode: string;
  nameAr: string;
  nameEn: string;
  parentLookupCode: string | null;
  sortOrder: number;
}

/** Admin row (all optional extension fields per table). */
export interface LookupRowAdminDto {
  id: number;
  lookupCode: string;
  code: string;
  nameAr: string;
  nameEn: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
  parentId: number | null;
  terminal: boolean | null;
  slaDays: number | null;
  restrictsExport: boolean | null;
  requiresClearance: boolean | null;
  /** `correspondence_status` only. */
  uiVariant?: string | null;
  /** `leave_status` only. */
  initial?: boolean | null;
}

export interface LookupBundleDto {
  correspondenceTypes: LookupItemDto[];
  correspondenceStatuses: LookupItemDto[];
  priorities: LookupItemDto[];
  confidentialities: LookupItemDto[];
  classifications: LookupItemDto[];
  workflowActionTypes: LookupItemDto[];
  workflowHistoryEventTypes: LookupItemDto[];
  /** Org-chart visual workflow node border states (`code` matches CSS modifier class). */
  orgVisualNodeStatuses: LookupItemDto[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface LookupLabelDto {
  code: string;
  nameAr: string;
  nameEn: string;
  /** Correspondence status: `correspondence_status.ui_variant`. */
  uiVariant?: string | null;
  /** Slice 5 — only populated for `confidentiality`; true marks the correspondence as classified. */
  requiresClearance?: boolean | null;
}

export interface DepartmentSummaryDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
}

/** Resolved app_user for audit columns (`created_by` / `updated_by`). */
export interface UserAuditRefDto {
  id: string;
  fullNameAr: string;
  fullNameEn: string;
}

/** Flat row for building internal department tree (`GET /api/v1/departments`). */
export interface DepartmentFlatDto {
  id: number;
  parentId: number | null;
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
}

export interface CorrespondenceListItemDto {
  id: string;
  referenceNumber: string;
  subject: string;
  createdAt: string;
  updatedAt: string;
  dueDate: string | null;
  correspondenceType: LookupLabelDto | null;
  correspondenceStatus: LookupLabelDto | null;
  priority: LookupLabelDto | null;
  ownerDepartment: DepartmentSummaryDto | null;
  createdByUser?: UserAuditRefDto | null;
  updatedByUser?: UserAuditRefDto | null;
}

export interface OrganizationSummaryDto {
  id: number;
  code: string | null;
  nameAr: string;
  nameEn: string;
}

export interface AttachmentVersionDto {
  id: number;
  versionNumber: number;
  byteSize: number;
  mimeType: string | null;
  checksumSha256: string | null;
  createdAt: string;
  /** Slice 5 — when present the bytes on disk are encrypted (AES_256_GCM). */
  encryptionAlgo?: string | null;
  /** Slice 5 — canonical content hash that digital signatures bind to. */
  plaintextSha256?: string | null;
}

export interface CorrespondenceAttachmentDetailDto {
  id: number;
  displayName: string;
  active: boolean;
  currentVersionId: number | null;
  contentType: LookupLabelDto | null;
  versions: AttachmentVersionDto[];
}

export interface CorrespondenceCommentDetailDto {
  id: number;
  body: string;
  createdAt: string;
  parentCommentId: number | null;
  author: UserSummaryDto | null;
}

export interface UserSummaryDto {
  id: string;
  username: string;
  fullNameAr: string;
  fullNameEn: string;
}

export interface CorrespondenceTimelineEntryDto {
  historyId: number;
  sequenceNo: number;
  action: string;
  eventTypeCode: string;
  user: UserSummaryDto | null;
  timestamp: string;
  comment: string | null;
  status: string | null;
  previousStatusCode: string | null;
}

export interface CorrespondenceDetailResponseDto {
  id: string;
  referenceNumber: string;
  correspondenceType: LookupLabelDto | null;
  correspondenceStatus: LookupLabelDto | null;
  priority: LookupLabelDto | null;
  confidentiality: LookupLabelDto | null;
  classification: LookupLabelDto | null;
  subject: string;
  description: string | null;
  bodyHtml: string | null;
  /** Unsaved reply/editor HTML from server */
  replyDraftHtml: string | null;
  senderOrganization: OrganizationSummaryDto | null;
  recipientOrganization: OrganizationSummaryDto | null;
  ownerDepartment: DepartmentSummaryDto | null;
  externalReferenceNumber: string | null;
  externalReferenceDate: string | null;
  dueDate: string | null;
  barcodeValue: string | null;
  totalAttachmentBytes: number;
  createdAt: string;
  updatedAt: string;
  workflowRouteMode?: string | null;
  serviceWorkflowRouteId?: number | null;
  workflowProcessDefinitionKey?: string | null;
  supplyTransaction?: boolean;
  beneficiaryName?: string | null;
  beneficiaryOrganization?: string | null;
  beneficiaryIdentifier?: string | null;
  attachments: CorrespondenceAttachmentDetailDto[];
  timeline: CorrespondenceTimelineEntryDto[];
  comments: CorrespondenceCommentDetailDto[];
  /** Camunda task decisions allowed for the current user (from `workflow_action_type`). */
  availableWorkflowActions?: WorkflowActionAvailableDto[];
  /** From `correspondence_status.allows_cancel` + cancel outcome row; hides cancel when false. */
  cancelAllowed?: boolean;
  /** Slice 1 — calling user's read receipt for this correspondence; `null` until tracked. */
  myReadReceipt?: CorrespondenceReadReceiptDto | null;
  /** Slice 1 — whether the workspace should expose the Acknowledge action; defaults to true. */
  acknowledgementSupported?: boolean;
}

/** Slice 1 — per-(correspondence,user) read receipt. */
export interface CorrespondenceReadReceiptDto {
  id: number;
  /** Null when this is the caller's own receipt (compact embedded form). */
  userId: string | null;
  username: string | null;
  fullNameAr: string | null;
  fullNameEn: string | null;
  firstOpenedAt: string;
  lastOpenedAt: string;
  openCount: number;
  acknowledgedAt: string | null;
  acknowledgementComment: string | null;
}

/** Slice 1 — body of `POST /api/v1/correspondence/{id}/ack`. */
export interface CorrespondenceAckRequestDto {
  comment?: string | null;
}

/** Slice 1 — cross-user read status; gated by `CORRESPONDENCE_READ_STATUS_VIEW`. */
export interface CorrespondenceReadStatusSummaryDto {
  correspondenceId: string;
  totalReaders: number;
  acknowledgedReaders: number;
  receipts: CorrespondenceReadReceiptDto[];
}

/** Slice 1 — single row from `attachment_access_log`. */
export interface AttachmentAccessLogDto {
  id: number;
  attachmentId: number | null;
  attachmentVersionId: number | null;
  correspondenceId: string | null;
  userId: string | null;
  username: string | null;
  fullNameAr: string | null;
  fullNameEn: string | null;
  actionCode: string;
  occurredAt: string;
  ipAddress: string | null;
  userAgent: string | null;
  success: boolean;
}

/** Row from GET correspondence detail — dynamic workflow buttons. */
export interface WorkflowActionAvailableDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  requiresComment: boolean;
  /** Slice 5 — when true, the actor must sign every active attachment first. */
  requiresSignature?: boolean;
  /** From workflow_action_type.requires_target_user (V26). */
  requiresTargetUser?: boolean;
  /** From workflow_action_type.requires_target_department (V26). */
  requiresTargetDepartment?: boolean;
  sortOrder: number;
  /** Semantic style: primary | secondary | danger | warning | success */
  uiVariant?: string;
}

export interface CorrespondenceAttachmentFormDto {
  displayName: string;
  storageKey: string;
  byteSize: number;
  mimeType?: string | null;
  contentTypeCode?: string | null;
  checksumSha256?: string | null;
  /** Slice 5 — at-rest encryption metadata round-tripped from POST /attachments/upload. */
  plaintextSha256?: string | null;
  encryptionAlgo?: string | null;
  encryptionKeyRef?: string | null;
  encryptionWrappedDekB64?: string | null;
  encryptionIvB64?: string | null;
  ciphertextSha256?: string | null;
}

export interface CorrespondenceCreateRequestDto {
  correspondenceTypeCode: string;
  priorityCode: string;
  confidentialityCode: string;
  classificationCode: string;
  subject: string;
  description?: string | null;
  bodyHtml?: string | null;
  senderOrganizationId?: number | null;
  recipientOrganizationId?: number | null;
  externalReferenceNumber?: string | null;
  externalReferenceDate?: string | null;
  ownerDepartmentId?: number | null;
  dueDate?: string | null;
  barcodeValue?: string | null;
  primaryComment?: string | null;
  attachments?: CorrespondenceAttachmentFormDto[] | null;
  /** First Camunda task assigned to this user (UUID). Mutually exclusive with workflowFirstCandidateGroup. */
  workflowFirstAssigneeUserId?: string | null;
  /** First Camunda task as candidate pool for this role code (e.g. STAFF). Mutually exclusive with workflowFirstAssigneeUserId. */
  workflowFirstCandidateGroup?: string | null;
  /** AUTO = default Camunda route for type; MANUAL = use serviceWorkflowRouteId. */
  workflowRouteMode?: 'AUTO' | 'MANUAL' | string | null;
  serviceWorkflowRouteId?: number | null;
  supplyTransaction?: boolean;
  beneficiaryName?: string | null;
  beneficiaryOrganization?: string | null;
  beneficiaryIdentifier?: string | null;
  recipientDepartmentIds?: number[];
  ccDepartmentIds?: number[];
  userRecipients?: { recipientUserId: string; recipientKindCode: string }[];
}

export interface CorrespondenceCreatedResponseDto {
  id: string;
  referenceNumber: string;
  correspondenceTypeCode: string;
  correspondenceStatusCode: string;
  workflowInstanceId: string;
  processDefinitionKey: string;
  camundaProcessInstanceId: string;
  createdAt: string;
}

export interface DashboardBucketDto {
  lookupId: number;
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
  count: number;
  /** Chart/badge colour key from DB (`ui_variant` on status/priority). */
  uiVariant?: string | null;
}

/** `GET /api/v1/reports/monthly-trend` */
export interface ReportMonthlyPointDto {
  period: string;
  count: number;
}

/** `GET /api/v1/reports/department-sla-heatmap` */
export interface DepartmentSlaRowDto {
  departmentId: number;
  code: string;
  nameAr: string;
  nameEn: string;
  totalCorrespondences: number;
  overdueOpen: number;
}

/** `GET /api/v1/letter-templates` */
export interface LetterTemplateDto {
  code: string;
  nameAr: string;
  nameEn: string;
  bodyHtml: string;
  sortOrder: number;
  /** When set server-side, HTML may be loaded from disk under storage root. */
  templateFilePath?: string | null;
}

export interface LetterTemplateAdminDto extends LetterTemplateDto {
  id: number;
  active: boolean;
}

/** GET /api/v1/workflow-routes?correspondenceTypeCode= */
export interface ServiceWorkflowRouteDto {
  id: number;
  correspondenceTypeId: number;
  correspondenceTypeCode: string;
  processDefinitionKey: string;
  nameAr: string;
  nameEn: string;
  defaultRoute: boolean;
  sortOrder: number;
  active: boolean;
}

/** GET /api/v1/organizations */
export interface OrganizationFlatDto {
  id: number;
  parentId: number | null;
  code: string | null;
  nameAr: string;
  nameEn: string;
  external: boolean;
}

export interface LeaveRequestDto {
  id: string;
  userId: string;
  username: string | null;
  fullNameAr: string | null;
  fullNameEn: string | null;
  startDate: string;
  endDate: string;
  reason: string | null;
  statusCode: string;
  statusUiVariant?: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionNote: string | null;
  createdAt: string;
}

export interface DashboardResponseDto {
  totalCorrespondences: number;
  byStatus: DashboardBucketDto[];
  byPriority: DashboardBucketDto[];
  /** Distribution by owner-department organizational level (Q/L/K/S) — Phase 7. */
  byOrgLevel: DashboardBucketDto[];
  /** Distribution by confidentiality level (filtered by viewer clearance) — Phase 7. */
  byConfidentiality: DashboardBucketDto[];
  overdueCount: number;
  /** Headline KPI counts (see Flyway `kpi_segment` / `dashboard_outbound_highlight`). */
  kpiSlaDoneCount: number;
  kpiPipelineCount: number;
  kpiInboxCount: number;
  kpiOutboundCount: number;
}

/** `GET /api/v1/reports/workflow-sla-trend` */
export interface WorkflowSlaPointDto {
  bucketStart: string;
  averageRoutingSeconds: number;
  completedCount: number;
}

export interface UserListDto {
  id: string;
  username: string;
  fullNameAr: string;
  fullNameEn: string;
  email: string;
  departmentCode: string;
  active: boolean;
}

export interface WorkflowHistoryEntryDto {
  id: number;
  correspondenceId: string;
  eventTypeCode: string;
  workflowActionTypeCode: string | null;
  workflowActionId: number | null;
  actorUserId: string | null;
  actorDisplayName: string | null;
  occurredAt: string;
  sequenceNo: number;
  primaryCommentText: string | null;
  detail: Record<string, unknown> | null;
  slaDueAt: string | null;
  slaBreachedAt: string | null;
  actualDurationMs: number | null;
  previousStatusCode: string | null;
  newStatusCode: string | null;
  camundaTaskId: string | null;
}

export interface NotificationItemDto {
  id: string;
  userId: string;
  type: string;
  messageKey: string;
  messageParams: Record<string, unknown> | null;
  read: boolean;
  createdAt: string;
}

export interface LoginResponseDto {
  accessToken: string;
  /** Opaque refresh JTI issued by `/api/v1/auth/login` or `/auth/refresh` (null on switch-role). */
  refreshToken?: string | null;
  expiresInSeconds?: number;
  userId: string;
  username: string;
  roles: string[];
  currentRole: string;
  profileImageUrl?: string | null;
}

export interface UserDetailDto {
  id: string;
  username: string;
  fullNameAr: string;
  fullNameEn: string;
  email: string;
  departmentCode: string | null;
  departmentId: number | null;
  active: boolean;
  roleIds: number[];
  securityClearanceId?: number | null;
}

export interface CurrentUserProfileDto {
  id: string;
  username: string;
  fullNameAr: string;
  fullNameEn: string;
  email: string;
  profileImageUrl?: string | null;
  phone: string | null;
  nationalId: string | null;
  departmentId: number | null;
  departmentCode: string | null;
  departmentNameAr: string | null;
  departmentNameEn: string | null;
  active: boolean;
  mfaEnabled: boolean;
  lastLoginAt: string | null;
  passwordChangedAt: string | null;
  roleIds: number[];
  roleCodes: string[];
  uiTheme: 'light' | 'dark';
  uiLocale: 'ar' | 'en';
}

export interface PermissionAdminDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
  /** Optional FK to {@link UiScreenDto#id} (inventory / capabilities). */
  uiScreenId: number | null;
}

/** {@code GET /api/v1/me/capabilities} */
export interface UserCapabilitiesDto {
  roles: string[];
  permissions: string[];
  screens: { code: string; route: string }[];
}

export interface UiScreenDto {
  id: number;
  code: string;
  routePath: string;
  nameAr: string;
  nameEn: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
  requiredPermissionId: number | null;
  iconKey: string;
  showInShellNav: boolean;
}

/** {@code GET /profile/me/navigation} */
export interface ShellNavItemDto {
  code: string;
  routePath: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
  iconKey: string;
}

export interface SystemIssueDto {
  id: number;
  source: string;
  severity: string;
  message: string;
  detail: string | null;
  pageUrl: string | null;
  userId: string | null;
  httpStatus: number | null;
  createdAt: string;
  resolvedAt: string | null;
  resolvedBy: string | null;
  resolutionNote: string | null;
}

export interface AttachmentUploadResponseDto {
  storageKey: string;
  byteSize: number;
  mimeType: string;
  /** Slice 5 — at-rest encryption envelope; non-null when the upload was AES-256-GCM encrypted. */
  plaintextSha256?: string | null;
  encryptionAlgo?: string | null;
  encryptionKeyRef?: string | null;
  encryptionWrappedDekB64?: string | null;
  encryptionIvB64?: string | null;
  ciphertextSha256?: string | null;
}

/** Slice 5 — response of POST /attachments/{id}/download-intent. */
export interface AttachmentDownloadIntentDto {
  /** Opaque single-use token. Returned exactly once; pair with `Authorization: Bearer` to stream. */
  token: string;
  /** ISO timestamp at which the token stops being accepted. */
  expiresAt: string;
}

/** Slice 5 — `document_signature` row. */
export interface DocumentSignatureDto {
  id: string;
  attachmentId: number | null;
  attachmentVersionId: number | null;
  signerUserId: string | null;
  signerUsername: string | null;
  signerFullNameAr: string | null;
  signerFullNameEn: string | null;
  algorithm: string;
  canonicalHashSha256: string;
  keyRef: string;
  signedAt: string;
  status: 'VALID' | 'REVOKED' | string;
  verificationStatus: 'PENDING' | 'VERIFIED' | 'FAILED' | string;
  verificationAt: string | null;
  verificationDetail: string | null;
}

/** Slice 5 — response of GET /verify/attachment-versions/{id}. */
export interface AttachmentVerificationDto {
  attachmentVersionId: number;
  plaintextSha256: string | null;
  encryptionAlgo: string | null;
  signatures: DocumentSignatureDto[];
}

/** Guide §9 — administrative delegation */
export interface AuthorityDelegationDto {
  id: string;
  delegator: UserSummaryDto;
  delegate: UserSummaryDto;
  validFrom: string;
  validTo: string;
  allowedCorrespondenceTypeCodes: string | null;
  allowedConfidentialityCodes: string | null;
  canSignOnBehalf: boolean;
  notes: string | null;
}

/**
 * Slice 2 — Task Delegation. Scope codes match the backend CHECK constraint:
 * - `TASK` targets a specific Camunda task or correspondence.
 * - `TYPE_CONFIDENTIALITY` applies to every open task whose correspondence matches the
 *    optional csv filters (empty = all).
 */
export type TaskDelegationScope = 'TASK' | 'TYPE_CONFIDENTIALITY';

export interface TaskDelegationDto {
  id: string;
  delegator: UserSummaryDto;
  delegate: UserSummaryDto;
  scopeType: TaskDelegationScope;
  correspondenceId: string | null;
  camundaTaskId: string | null;
  processInstanceId: string | null;
  allowedCorrespondenceTypeCodes: string | null;
  allowedConfidentialityCodes: string | null;
  validFrom: string;
  validTo: string;
  notes: string | null;
  revokedAt: string | null;
  revokedBy: string | null;
  authorityDelegationId: string | null;
  active: boolean;
}

export interface CreateTaskDelegationRequestDto {
  delegateUserId: string;
  scopeType: TaskDelegationScope;
  correspondenceId?: string | null;
  camundaTaskId?: string | null;
  processInstanceId?: string | null;
  allowedCorrespondenceTypeCodes?: string | null;
  allowedConfidentialityCodes?: string | null;
  validFrom: string;
  validTo: string;
  notes?: string | null;
  authorityDelegationId?: string | null;
}

export interface TaskDelegationListDto {
  outgoingActive: TaskDelegationDto[];
  incomingActive: TaskDelegationDto[];
  inactive: TaskDelegationDto[];
}

// ============================================================================
// Slice 4 — Acting manager assignments
// ============================================================================

export type ActingAssignmentLifecycleStatus = 'ACTIVE' | 'UPCOMING' | 'EXPIRED' | 'REVOKED';

export interface ActingAssignmentDto {
  id: string;
  absentUserId: string;
  absentUsername: string;
  actingUserId: string;
  actingUsername: string;
  departmentId: number | null;
  includeDepartmentSubtree: boolean;
  orgLevelCode: string | null;
  correspondenceTypeId: number | null;
  confidentialityId: number | null;
  workflowActionTypeId: number | null;
  processDefinitionKey: string | null;
  taskDefinitionKey: string | null;
  validFrom: string;
  validTo: string;
  notes: string | null;
  revokedAt: string | null;
  active: boolean;
  lifecycleStatus: ActingAssignmentLifecycleStatus;
}

export interface CreateActingAssignmentRequestDto {
  absentUserId: string;
  actingUserId: string;
  departmentId?: number | null;
  includeDepartmentSubtree?: boolean | null;
  orgLevelCode?: string | null;
  correspondenceTypeId?: number | null;
  confidentialityId?: number | null;
  workflowActionTypeId?: number | null;
  processDefinitionKey?: string | null;
  taskDefinitionKey?: string | null;
  validFrom: string;
  validTo: string;
  notes?: string | null;
}

export interface ActingAssignmentListDto {
  asAbsent: ActingAssignmentDto[];
  asActing: ActingAssignmentDto[];
  upcoming: ActingAssignmentDto[];
  inactive: ActingAssignmentDto[];
}

// ============================================================================
// Slice 3 — SLA Policy Engine
// ============================================================================

/** Escalation action codes loaded from {@code sla_escalation_action_type}. */
export type SlaEscalationActionCode = string;

export interface SlaEscalationActionTypeDto {
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
}

export interface SlaEscalationStepDto {
  id: number;
  stepOrder: number;
  actionCode: SlaEscalationActionCode | string;
  delayAfterBreachMinutes: number;
  targetRoleCode: string | null;
  description: string | null;
  active: boolean;
}

export interface SlaPolicyDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  description: string | null;
  correspondenceTypeId: number | null;
  correspondenceTypeCode: string | null;
  priorityId: number | null;
  priorityCode: string | null;
  confidentialityId: number | null;
  confidentialityCode: string | null;
  orgLevelCode: string | null;
  workflowActionTypeId: number | null;
  workflowActionTypeCode: string | null;
  targetHours: number;
  breachGraceMinutes: number;
  active: boolean;
  specificity: number;
  steps: SlaEscalationStepDto[];
  createdAt: string | null;
  updatedAt: string | null;
}

export interface CreateSlaEscalationStepRequestDto {
  stepOrder: number;
  actionCode: SlaEscalationActionCode | string;
  delayAfterBreachMinutes: number;
  targetRoleCode?: string | null;
  description?: string | null;
  active?: boolean | null;
}

export interface CreateSlaPolicyRequestDto {
  code: string;
  nameAr: string;
  nameEn: string;
  description?: string | null;
  correspondenceTypeId?: number | null;
  priorityId?: number | null;
  confidentialityId?: number | null;
  orgLevelCode?: string | null;
  workflowActionTypeId?: number | null;
  targetHours: number;
  breachGraceMinutes?: number | null;
  active?: boolean | null;
  steps?: CreateSlaEscalationStepRequestDto[];
}

export interface SlaBreachEventDto {
  id: number;
  taskId: string;
  processInstanceId: string | null;
  workflowInstanceId: string | null;
  correspondenceId: string | null;
  correspondenceReferenceNumber: string | null;
  slaPolicyId: number | null;
  slaPolicyCode: string | null;
  targetAt: string;
  breachedAt: string;
  lastStepExecutedOrder: number | null;
  lastStepExecutedAt: string | null;
  lastStepActionCode: string | null;
  stepsExecutedTotal: number | null;
  resolvedAt: string | null;
  resolutionOutcome: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface SlaTaskStatusDto {
  taskId: string;
  slaPolicyId: number | null;
  slaPolicyCode: string | null;
  taskCreatedAt: string | null;
  targetAt: string | null;
  breachedAt: string | null;
  resolvedAt: string | null;
  overdue: boolean;
  secondsRemaining: number;
  secondsOverdue: number;
  lastStepExecutedOrder: number | null;
  lastStepActionCode: string | null;
  stepsExecutedTotal: number | null;
}

/** Guide — linked correspondences tab */
export interface CorrespondenceLinkListItemDto {
  id: number;
  linkedCorrespondenceId: string;
  linkedReferenceNumber: string;
  linkedSubject: string;
  linkKind: string;
  notes: string | null;
}

export interface CorrespondenceNonarchivedItemDto {
  id: number;
  itemType: string;
  descriptionText: string | null;
  quantity: number;
  sortOrder: number;
}

export interface CorrespondenceRecipientDto {
  id: number;
  departmentId: number;
  departmentCode: string;
  departmentNameAr: string;
  departmentNameEn: string;
  firstReadAt: string | null;
  lastReadAt: string | null;
  readCount: number;
}

export interface CorrespondenceUserRecipientDto {
  id: number;
  recipientUserId: string;
  recipientUsername: string;
  recipientFullNameAr: string;
  recipientFullNameEn: string;
  recipientKindCode: string;
  firstReadAt: string | null;
  lastReadAt: string | null;
  readCount: number;
  acknowledgedAt: string | null;
}

export interface CorrespondencePatchRequestDto {
  subject?: string | null;
  description?: string | null;
  bodyHtml?: string | null;
  priorityCode?: string | null;
  confidentialityCode?: string | null;
  classificationCode?: string | null;
  senderOrganizationId?: number | null;
  recipientOrganizationId?: number | null;
  externalReferenceNumber?: string | null;
  externalReferenceDate?: string | null;
  ownerDepartmentId?: number | null;
  dueDate?: string | null;
  barcodeValue?: string | null;
  beneficiaryName?: string | null;
  beneficiaryOrganization?: string | null;
  beneficiaryIdentifier?: string | null;
}

export interface AttachmentIndexEntryDto {
  id: number;
  pageFrom: number | null;
  pageTo: number | null;
  subjectText: string | null;
  sortOrder: number;
}
