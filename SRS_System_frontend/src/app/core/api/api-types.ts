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

export interface CorrespondenceListDto {
  id: string;
  referenceNumber: string;
  subject: string;
  typeCode: string;
  statusCode: string;
  priorityCode: string;
  createdAt: string;
}

export interface DashboardSummaryDto {
  totalCorrespondence: number;
  inboundCount: number;
  outboundCount: number;
  inProgressCount: number;
  completedCount: number;
}

export interface CodeCountDto {
  code: string;
  count: number;
}

export interface DashboardChartsDto {
  byCorrespondenceStatus: CodeCountDto[];
  byCorrespondenceType: CodeCountDto[];
  byPriority: CodeCountDto[];
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
