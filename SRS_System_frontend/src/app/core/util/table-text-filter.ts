/**
 * Client-side “search all columns” for simple tables.
 * Concatenates cell values (and optional extra strings) and tests inclusion of `query` (case-insensitive).
 */
export function matchesTableSearch(
  query: string,
  parts: Array<string | number | boolean | null | undefined>
): boolean {
  const q = query.trim().toLowerCase();
  if (!q) {
    return true;
  }
  const blob = parts
    .map((p) => (p === null || p === undefined ? '' : String(p)))
    .join('\u0001')
    .toLowerCase();
  return blob.includes(q);
}
