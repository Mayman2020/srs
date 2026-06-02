import { TestBed } from '@angular/core/testing';
import { Router, type Route, type UrlSegment } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { permissionCanMatch } from './permission.guard';
import { CapabilitiesService } from './capabilities.service';

class CapabilitiesStub {
  ready$: Observable<void> = of(void 0);
  permissions = new Set<string>();
  ensureReady(): Observable<void> {
    return this.ready$;
  }
  can(code: string): boolean {
    return this.permissions.has(code);
  }
}

describe('permissionCanMatch', () => {
  let cap: CapabilitiesStub;
  let router: { createUrlTree: (cmd: string[]) => unknown };
  let createdUrlTrees: string[][];

  beforeEach(() => {
    cap = new CapabilitiesStub();
    createdUrlTrees = [];
    router = {
      createUrlTree: (commands: string[]) => {
        createdUrlTrees.push(commands);
        return { __urlTree: commands.join('/') };
      }
    };
    TestBed.configureTestingModule({
      providers: [
        { provide: CapabilitiesService, useValue: cap },
        { provide: Router, useValue: router }
      ]
    });
  });

  function runGuard(route: Route): unknown {
    const segments: UrlSegment[] = [];
    return TestBed.runInInjectionContext(() => permissionCanMatch(route, segments));
  }

  it('returns true when route has no data.permission', (done) => {
    const result = runGuard({});
    if (typeof result === 'object' && 'subscribe' in (result as object)) {
      (result as Observable<unknown>).subscribe((value) => {
        expect(value).toBe(true);
        done();
      });
    } else {
      expect(result).toBe(true);
      done();
    }
  });

  it('returns true when the user has the required permission', (done) => {
    cap.permissions.add('CORRESPONDENCE_VIEW');
    const result = runGuard({ data: { permission: 'CORRESPONDENCE_VIEW' } });

    (result as Observable<unknown>).subscribe((value) => {
      expect(value).toBe(true);
      expect(createdUrlTrees.length).toBe(0);
      done();
    });
  });

  it('redirects to /dashboard when the user lacks the required permission', (done) => {
    const result = runGuard({ data: { permission: 'ADMIN_USER_MANAGE' } });

    (result as Observable<unknown>).subscribe((value) => {
      expect(value).not.toBe(true);
      expect(createdUrlTrees).toEqual([['/dashboard']]);
      done();
    });
  });

  it('redirects to /login when capability load fails', (done) => {
    cap.ready$ = throwError(() => new Error('http 401'));
    const result = runGuard({ data: { permission: 'CORRESPONDENCE_VIEW' } });

    (result as Observable<unknown>).subscribe((value) => {
      expect(value).not.toBe(true);
      expect(createdUrlTrees).toEqual([['/login']]);
      done();
    });
  });
});
