/** Soft list loading — keeps table visible while filters refetch data. */
export class ListLoadController {
  loading = false;
  refreshing = false;
  ready = false;

  begin(): void {
    if (this.ready) {
      this.refreshing = true;
      return;
    }
    this.loading = true;
  }

  end(): void {
    this.loading = false;
    this.refreshing = false;
    this.ready = true;
  }

  reset(): void {
    this.loading = false;
    this.refreshing = false;
    this.ready = false;
  }

  get showInitialSpinner(): boolean {
    return this.loading && !this.ready;
  }

  get showSurface(): boolean {
    return this.ready || !this.loading;
  }
}
