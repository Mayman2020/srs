export interface LookupItemDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
}

export interface LookupBundleDto {
  correspondenceTypes: LookupItemDto[];
  correspondenceStatuses: LookupItemDto[];
  priorities: LookupItemDto[];
  confidentialities: LookupItemDto[];
  classifications: LookupItemDto[];
  workflowActionTypes: LookupItemDto[];
  workflowHistoryEventTypes: LookupItemDto[];
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
}

export interface DepartmentSummaryDto {
  id: number;
  code: string;
  nameAr: string;
  nameEn: string;
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

export interface CorrespondenceDetailResponse {
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
  attachments: CorrespondenceAttachmentDetailDto[];
  timeline: CorrespondenceTimelineEntryDto[];
  comments: CorrespondenceCommentDetailDto[];
}

export interface CorrespondenceAttachmentFormDto {
  displayName: string;
  storageKey: string;
  byteSize: number;
  mimeType?: string | null;
  contentTypeCode?: string | null;
  checksumSha256?: string | null;
}

export interface CorrespondenceCreateRequest {
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
}

export interface CorrespondenceCreatedResponse {
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
}

export interface DashboardResponseDto {
  totalCorrespondences: number;
  byStatus: DashboardBucketDto[];
  byPriority: DashboardBucketDto[];
  overdueCount: number;
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
  userId: string;
  username: string;
}

export interface AttachmentUploadResponseDto {
  storageKey: string;
  byteSize: number;
  mimeType: string;
}
