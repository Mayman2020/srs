export interface TimelineStep {
  action: string;
  user: string;
  date: Date;
  note?: string;
}

export interface Attachment {
  id: number;
  type: string;
  classification: string;
  name: string;
  secrecy: string;
  createdAt: Date;
  user: string;
}

/** List/detail view model; codes mirror PostgreSQL lookup `code` values. */
export interface Transaction {
  id: string;
  referenceNumber?: string;
  type: string;
  typeCode: string;
  status: string;
  statusCode: string;
  priorityCode?: string;
  subject: string;
  description: string;
  createdAt: Date;
  secrecy: string;
  from: string;
  to: string;
  created: string;
  maxDays: number;
  timeline: TimelineStep[];
  attachments: Attachment[];
}
