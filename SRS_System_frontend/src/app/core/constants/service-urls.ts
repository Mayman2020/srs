export interface SrsServiceUrl {
  serviceCode: string;
  url: string;
}

export const SrsServicesUrls: readonly SrsServiceUrl[] = [
  { serviceCode: 'CREATE_CORRESPONDENCE', url: '/create-transaction' },
  { serviceCode: 'SUPPLY_CORRESPONDENCE', url: '/supply-transaction' },
  { serviceCode: 'TRANSACTIONS', url: '/transactions' },
  { serviceCode: 'CORRESPONDENCE_SEARCH', url: '/correspondence-search' },
  { serviceCode: 'NOTIFICATIONS', url: '/notifications' },
  { serviceCode: 'REPORTS', url: '/reports' }
];

export const SrsServicesRequestUrls: readonly SrsServiceUrl[] = [
  { serviceCode: 'CORRESPONDENCE_TASK', url: '/transactions' },
  { serviceCode: 'CORRESPONDENCE_DETAILS', url: '/transactions/:id' }
];

export function serviceUrlFor(
  serviceCode: string,
  urls: readonly SrsServiceUrl[] = SrsServicesUrls
): string | null {
  return urls.find((item) => item.serviceCode === serviceCode)?.url ?? null;
}
