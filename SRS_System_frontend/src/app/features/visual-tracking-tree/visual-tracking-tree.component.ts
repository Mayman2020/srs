import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, ViewChild, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { LatinDigitsPipe } from '../../core/i18n/latin-digits.pipe';
import { LookupLabelsService } from '../../core/lookup/lookup-labels.service';
import { LookupCode } from '../../core/lookup/lookup-code';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import {
  NgxInteractiveOrgChart,
  OrgChartNode,
  NgxInteractiveOrgChartTheme,
} from 'ngx-interactive-org-chart';

export interface TimelineStep {
  action: string;
  note: string;
  user: string;
  date: Date | string | null;
}

export interface Transaction {
  id: string;
  subject: string;
  from: string;
  to: string;
  currentHandler: string;
  created?: Date | string;
  timeline: TimelineStep[];
}

export interface NodeData {
  title: string;
  note?: string;
  user?: string;
  date?: Date | string | null;
  /** Lookup `org_visual_node_status.code` (drives card CSS class). */
  status: string;

  /** Used for stagger reveal animation */
  delayMs?: number;
}

type OrgNode = OrgChartNode<NodeData>;

@Component({
  selector: 'app-visual-tracking-tree',
  standalone: true,
  imports: [CommonModule, NgxInteractiveOrgChart, TranslatePipe, LatinDigitsPipe],
  templateUrl: './visual-tracking-tree.component.html',
  styleUrls: ['./visual-tracking-tree.component.scss'],
})
export class VisualTrackingTreeComponent implements OnChanges {
  private readonly i18n = inject(I18nService);
  private readonly lookupLabels = inject(LookupLabelsService);

  @Input({ required: true }) transaction!: Transaction;
  @Input() activeIndex = 0;

  @ViewChild(NgxInteractiveOrgChart)
  orgChart!: NgxInteractiveOrgChart<NodeData>;

  /**
   * Keep null (not `{}`) so the value is not widened to unknown; use *ngIf in the template.
   */
  data: OrgNode | null = null;

  themeOptions: NgxInteractiveOrgChartTheme = {
    container: {
      background: '#f8fafc',
      border: '1px solid #eef2f7',
    },
    connector: {
      color: '#d6dde6',
      activeColor: '#0ea5e9',
      width: '2px',
      borderRadius: '999px',
    },
    node: {
      minWidth: '320px',
      maxWidth: '380px',
      borderRadius: '16px',
      padding: '0px',
      outlineColor: '#e6edf5',
      activeOutlineColor: '#f59e0b',
      shadow: '0 10px 26px rgba(15, 23, 42, 0.08)',
    },
  };

  /** Stagger timing (tune as needed) */
  private readonly baseLevelDelay = 420; // delay between levels (root → handler → steps)
  private readonly baseItemDelay = 170; // delay between siblings on the same level

  ngOnChanges(): void {
    if (!this.transaction) return;

    const rawTree = this.mapTxToTree(this.transaction, this.activeIndex);
    this.data = this.applyStaggerDelays(rawTree);
  }

  // ===== Toolbar methods called from dialog =====
  zoomIn(): void {
    this.orgChart?.zoomIn({ by: 10, relative: true });
  }

  zoomOut(): void {
    this.orgChart?.zoomOut({ by: 10, relative: true });
  }

  resetView(): void {
    this.orgChart?.resetPanAndZoom(80);
  }

  /** Call from the dialog after `afterOpened()` so the root stays centered. */
  focusRoot(): void {
    if (!this.orgChart) return;

    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.orgChart.resetPanAndZoom(100);
        // this.orgChart.resetPan();        
        this.orgChart.highlightNode('tx');

        setTimeout(() => this.orgChart.pan(0, 120, true), 80);
      });
    });
  }

  // ===== Mapping =====
  /** Border style codes — must stay aligned with `org_visual_node_status` seed / admin rows. */
  private statusFor(i: number, activeIndex: number): string {
    if (i < activeIndex) return 'done';
    if (i === activeIndex) return 'active';
    return 'pending';
  }

  /**
   * Restricts the CSS class to codes present in the lookup bundle (safe if admins add rows or fix typos).
   */
  cardStatusClass(code: string | undefined): string {
    const rows = this.lookupLabels.orderedRows(LookupCode.OrgVisualNodeStatus);
    const allowed = new Set(rows.map((r) => r.code));
    const fallback = 'pending';
    const c = (code ?? fallback).trim();
    if (allowed.size === 0) {
      return c;
    }
    if (allowed.has(c)) {
      return c;
    }
    return allowed.has(fallback) ? fallback : (rows[0]?.code ?? fallback);
  }

  /** Localized label from DB-backed lookup (`org_visual_node_status`). */
  statusLookupLabel(code: string | undefined): string {
    return this.lookupLabels.label(LookupCode.OrgVisualNodeStatus, code);
  }

  private mapTxToTree(tx: Transaction, activeIndex: number): OrgNode {
    return {
      id: 'tx',
      name: tx.subject,
      data: {
        title: tx.subject,
        note: `${this.i18n.instant('transactions.refNo')}: ${tx.id}`,
        user: tx.from,
        date: tx.created ?? null,
        status: 'info',
      },
      children: [
        {
          id: 'handler',
          name: tx.currentHandler,
          data: {
            title: this.i18n.instant('transactionDetails.visualCurrentHandler'),
            note: tx.to,
            user: tx.currentHandler,
            date: null,
            status: 'active',
          },
          children: (tx.timeline ?? []).map(
            (s, i): OrgNode => ({
              id: `step-${i}`,
              name: s.action,
              data: {
                title: s.action,
                note: s.note,
                user: s.user,
                date: s.date,
                status: this.statusFor(i, activeIndex),
              },
              children: [],
            })
          ),
        },
      ],
    };
  }

  /**
   * Org chart node data is effectively readonly — return a new tree with `delayMs` instead of mutating.
   */
  private applyStaggerDelays(root: OrgNode): OrgNode {
    const perLevelIndex = new Map<number, number>();

    const cloneWithDelay = (orig: OrgNode, depth: number): OrgNode => {
      const idx = perLevelIndex.get(depth) ?? 0;
      perLevelIndex.set(depth, idx + 1);

      const delay = depth * this.baseLevelDelay + idx * this.baseItemDelay;

      return {
        ...orig,
        data: {
          ...(orig.data ?? ({} as NodeData)),
          delayMs: delay,
        },
        children: [],
      };
    };

    const rootClone = cloneWithDelay(root, 0);

    const q: Array<{ orig: OrgNode; clone: OrgNode; depth: number }> = [
      { orig: root, clone: rootClone, depth: 0 },
    ];

    while (q.length) {
      const { orig, clone, depth } = q.shift()!;
      const children = (orig.children ?? []) as OrgNode[];

      for (const child of children) {
        const childClone = cloneWithDelay(child, depth + 1);
        (clone.children as OrgNode[]).push(childClone);
        q.push({ orig: child, clone: childClone, depth: depth + 1 });
      }
    }

    return rootClone;
  }
}
