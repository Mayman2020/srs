import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, ViewChild } from '@angular/core';
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

type StepStatus = 'done' | 'active' | 'pending' | 'info';

export interface NodeData {
  title: string;
  note?: string;
  user?: string;
  date?: Date | string | null;
  status: StepStatus;

  /** Used for stagger reveal animation */
  delayMs?: number;
}

type OrgNode = OrgChartNode<NodeData>;

@Component({
  selector: 'app-visual-tracking-tree',
  standalone: true,
  imports: [CommonModule, NgxInteractiveOrgChart],
  templateUrl: './visual-tracking-tree.component.html',
  styleUrls: ['./visual-tracking-tree.component.scss'],
})
export class VisualTrackingTreeComponent implements OnChanges {
  @Input({ required: true }) transaction!: Transaction;
  @Input() activeIndex = 0;

  @ViewChild(NgxInteractiveOrgChart)
  orgChart!: NgxInteractiveOrgChart<NodeData>;

  /**
   * مهم: لا تبدأ بـ {} عشان TypeScript ما يحولها unknown.
   * خليها null واعمل *ngIf في الـ HTML
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

  // سرعة التدرج (عدّلها على ذوقك)
  private readonly baseLevelDelay = 420; // فرق بين المستويات (root -> handler -> steps)
  private readonly baseItemDelay = 170;  // فرق بين عناصر نفس المستوى (الخطوات)

  ngOnChanges(): void {
    if (!this.transaction) return;

    const rawTree = this.mapTxToTree(this.transaction, this.activeIndex);
    // ✅ لازم تسند return value (مش مجرد استدعاء)
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

  /**
   * استدعيها من الـ Dialog بعد afterOpened() عشان يبقى root في النص.
   */
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
  private statusFor(i: number, activeIndex: number): StepStatus {
    if (i < activeIndex) return 'done';
    if (i === activeIndex) return 'active';
    return 'pending';
  }

  private mapTxToTree(tx: Transaction, activeIndex: number): OrgNode {
    return {
      id: 'tx',
      name: tx.subject,
      data: {
        title: tx.subject,
        note: `رقم المعاملة: ${tx.id}`,
        user: tx.from,
        date: tx.created ?? null,
        status: 'info',
      },
      children: [
        {
          id: 'handler',
          name: tx.currentHandler,
          data: {
            title: 'المعالج الحالي',
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
   * ✅ مهم: data في OrgChartNode غالباً readonly
   * لذلك لا نعدّل الـ tree الحالي، بل نرجع نسخة جديدة مع delayMs.
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
