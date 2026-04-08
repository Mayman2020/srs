import type { SortDirection } from '../../shared/data-table/table-sort.util';

/** UI column id → Spring whitelist key (see CorrespondenceListPageables). */
export const REPORT_TABLE_SORT_PROPERTY: Record<string, string> = {
  ref: 'referenceNumber',
  subject: 'subject',
  created: 'createdAt',
  type: 'type',
  status: 'status',
};

export function reportSpringSort(columnId: string, direction: SortDirection): string[] {
  const prop = REPORT_TABLE_SORT_PROPERTY[columnId];
  if (!prop) {
    return ['createdAt,desc'];
  }
  return [`${prop},${direction}`];
}
