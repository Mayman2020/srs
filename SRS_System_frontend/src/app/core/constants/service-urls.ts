export interface SrsServiceUrl {
  serviceCode: string;
  url: string;
}

export const SrsServicesUrls: readonly SrsServiceUrl[] = [
  { serviceCode: 'CREATE_CORRESPONDENCE', url: '/correspondence/create' },
  { serviceCode: 'SUPPLY_CORRESPONDENCE', url: '/correspondence/supply' },
  { serviceCode: 'TRANSACTIONS', url: '/correspondence' },
  { serviceCode: 'CORRESPONDENCE_SEARCH', url: '/correspondence-search' },
  { serviceCode: 'NOTIFICATIONS', url: '/notifications' },
  { serviceCode: 'REPORTS', url: '/reports' }
];

export const SrsServicesRequestUrls: readonly SrsServiceUrl[] = [
  { serviceCode: 'CORRESPONDENCE_TASK', url: '/correspondence' },
  { serviceCode: 'CORRESPONDENCE_DETAILS', url: '/correspondence/:id' }
];

export function serviceUrlFor(
  serviceCode: string,
  urls: readonly SrsServiceUrl[] = SrsServicesUrls
): string | null {
  return urls.find((item) => item.serviceCode === serviceCode)?.url ?? null;
}
