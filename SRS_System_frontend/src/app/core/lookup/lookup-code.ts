export enum LookupCode {
  CorrespondenceType = 'correspondence_type',
  CorrespondenceStatus = 'correspondence_status',
  Priority = 'priority',
  Confidentiality = 'confidentiality',
  Classification = 'classification',
  WorkflowActionType = 'workflow_action_type',
  WorkflowHistoryEventType = 'workflow_history_event_type',
  OrgVisualNodeStatus = 'org_visual_node_status',
  LeaveStatus = 'leave_status',
}

export type LookupTableKey =
  | 'correspondenceType'
  | 'correspondenceStatus'
  | 'priority'
  | 'confidentiality'
  | 'classification'
  | 'workflowActionType'
  | 'workflowHistoryEventType'
  | 'orgVisualNodeStatus'
  | 'leaveStatus';

export const LOOKUP_CODE_TO_TABLE_KEY: Record<LookupCode, LookupTableKey> = {
  [LookupCode.CorrespondenceType]: 'correspondenceType',
  [LookupCode.CorrespondenceStatus]: 'correspondenceStatus',
  [LookupCode.Priority]: 'priority',
  [LookupCode.Confidentiality]: 'confidentiality',
  [LookupCode.Classification]: 'classification',
  [LookupCode.WorkflowActionType]: 'workflowActionType',
  [LookupCode.WorkflowHistoryEventType]: 'workflowHistoryEventType',
  [LookupCode.OrgVisualNodeStatus]: 'orgVisualNodeStatus',
  [LookupCode.LeaveStatus]: 'leaveStatus',
};

export function lookupTableKey(codeOrKey: LookupCode | LookupTableKey | string): string {
  return LOOKUP_CODE_TO_TABLE_KEY[codeOrKey as LookupCode] ?? codeOrKey;
}
