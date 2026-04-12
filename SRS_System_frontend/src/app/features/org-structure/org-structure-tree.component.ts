import { CdkTreeModule, NestedTreeControl } from '@angular/cdk/tree';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTreeModule, MatTreeNestedDataSource } from '@angular/material/tree';
import { RouterLink } from '@angular/router';
import { take } from 'rxjs/operators';
import { DepartmentFlatDto } from '../../core/api/api-types';
import {
  DepartmentApiService,
  DepartmentUpsertRequest
} from '../../core/api/department-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { DialogService } from '../../core/services/dialog.service';
import { NotificationService } from '../../core/services/notification.service';
import {
  OrgStructureDepartmentDialogComponent,
  OrgStructureDepartmentDialogData
} from './org-structure-department-dialog.component';

interface OrgStructureTreeNode {
  id: number;
  parentId: number | null;
  code: string;
  nameAr: string;
  nameEn: string;
  sortOrder: number;
  label: string;
  children?: OrgStructureTreeNode[];
}

@Component({
  selector: 'app-org-structure-tree',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTreeModule,
    CdkTreeModule,
    TranslatePipe
  ],
  templateUrl: './org-structure-tree.component.html',
  styleUrl: './org-structure-tree.component.css'
})
export class OrgStructureTreeComponent implements OnInit {
  readonly treeControl = new NestedTreeControl<OrgStructureTreeNode>((node) => node.children);
  readonly dataSource = new MatTreeNestedDataSource<OrgStructureTreeNode>();

  loading = true;
  loadError = false;
  allDepartments: DepartmentFlatDto[] = [];
  busyNodeIds = new Set<number>();

  constructor(
    private readonly departmentApi: DepartmentApiService,
    private readonly dialog: MatDialog,
    private readonly dialogService: DialogService,
    private readonly notification: NotificationService,
    private readonly i18n: I18nService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  hasChild = (_: number, node: OrgStructureTreeNode): boolean => !!node.children?.length;

  isBusy(id: number): boolean {
    return this.busyNodeIds.has(id);
  }

  addRoot(): void {
    this.openEditor({
      mode: 'create',
      parentId: null,
      departments: this.allDepartments
    });
  }

  addChild(node: OrgStructureTreeNode): void {
    this.openEditor({
      mode: 'create',
      parentId: node.id,
      departments: this.allDepartments
    });
  }

  edit(node: OrgStructureTreeNode): void {
    const blocked = [node.id, ...this.collectDescendantIds(node)];
    this.openEditor({
      mode: 'edit',
      department: this.toDepartmentDto(node),
      parentId: node.parentId,
      departments: this.allDepartments,
      blockedParentIds: blocked
    });
  }

  delete(node: OrgStructureTreeNode): void {
    this.dialogService
      .openConfirm({
        titleKey: 'orgStructure.tree.deleteDialogTitle',
        messageKey: 'orgStructure.tree.deleteDialogMessage',
        params: { name: node.label },
        confirmButton: { labelKey: 'orgStructure.tree.deleteNode', color: 'warn' },
        cancelButton: { labelKey: 'common.close' }
      })
      .pipe(take(1))
      .subscribe((ok) => {
        if (!ok) {
          return;
        }
        this.setNodeBusy(node.id, true);
        this.departmentApi.delete(node.id).subscribe({
          next: () => {
            this.notification.success('orgStructure.tree.deleted');
            this.reload();
          },
          error: () => {
            this.setNodeBusy(node.id, false);
          }
        });
      });
  }

  private reload(): void {
    this.loading = true;
    this.loadError = false;
    this.departmentApi.list().subscribe({
      next: (rows) => {
        this.loading = false;
        this.loadError = false;
        this.allDepartments = [...(rows ?? [])];
        const tree = this.buildTree(this.allDepartments);
        this.dataSource.data = tree;
        this.treeControl.dataNodes = tree;
        this.expandAllNodes(tree);
        this.busyNodeIds.clear();
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
        this.allDepartments = [];
        this.dataSource.data = [];
        this.busyNodeIds.clear();
        this.notification.error('orgStructure.tree.loadError');
        this.cdr.markForCheck();
      }
    });
  }

  private openEditor(data: OrgStructureDepartmentDialogData): void {
    this.dialog
      .open(OrgStructureDepartmentDialogComponent, {
        width: 'min(680px, 96vw)',
        maxWidth: '96vw',
        autoFocus: false,
        restoreFocus: true,
        panelClass: ['app-dialog-panel'],
        direction: this.i18n.currentDirection,
        data
      })
      .afterClosed()
      .pipe(take(1))
      .subscribe((result) => {
        if (!result) {
          return;
        }

        const request = this.normalizeRequest(result);
        if (data.mode === 'create') {
          this.departmentApi.create(request).subscribe({
            next: () => {
              this.notification.success('orgStructure.tree.created');
              this.reload();
            }
          });
          return;
        }

        if (!data.department) {
          return;
        }
        this.setNodeBusy(data.department.id, true);
        this.departmentApi.update(data.department.id, request).subscribe({
          next: () => {
            this.notification.success('orgStructure.tree.updated');
            this.reload();
          },
          error: () => {
            this.setNodeBusy(data.department!.id, false);
          }
        });
      });
  }

  private normalizeRequest(request: DepartmentUpsertRequest): DepartmentUpsertRequest {
    return {
      code: request.code.trim(),
      nameAr: request.nameAr.trim(),
      nameEn: request.nameEn.trim(),
      parentId: request.parentId,
      sortOrder: Math.max(0, request.sortOrder ?? 0)
    };
  }

  private buildTree(flat: DepartmentFlatDto[]): OrgStructureTreeNode[] {
    const byParent = new Map<number | null, DepartmentFlatDto[]>();
    for (const row of flat) {
      const parent = row.parentId ?? null;
      const list = byParent.get(parent) ?? [];
      list.push(row);
      byParent.set(parent, list);
    }
    for (const list of byParent.values()) {
      list.sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);
    }

    const buildChildren = (parentId: number | null): OrgStructureTreeNode[] => {
      const rows = byParent.get(parentId) ?? [];
      return rows.map((row) => {
        const children = buildChildren(row.id);
        return {
          id: row.id,
          parentId: row.parentId,
          code: row.code,
          nameAr: row.nameAr,
          nameEn: row.nameEn,
          sortOrder: row.sortOrder,
          label: this.displayName(row),
          children: children.length ? children : undefined
        };
      });
    };

    return buildChildren(null);
  }

  private expandAllNodes(nodes: OrgStructureTreeNode[]): void {
    for (const node of nodes) {
      this.treeControl.expand(node);
      if (node.children?.length) {
        this.expandAllNodes(node.children);
      }
    }
  }

  private displayName(row: DepartmentFlatDto): string {
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  private collectDescendantIds(node: OrgStructureTreeNode): number[] {
    const ids: number[] = [];
    if (!node.children?.length) {
      return ids;
    }
    for (const child of node.children) {
      ids.push(child.id);
      ids.push(...this.collectDescendantIds(child));
    }
    return ids;
  }

  private toDepartmentDto(node: OrgStructureTreeNode): DepartmentFlatDto {
    return {
      id: node.id,
      parentId: node.parentId,
      code: node.code,
      nameAr: node.nameAr,
      nameEn: node.nameEn,
      sortOrder: node.sortOrder
    };
  }

  private setNodeBusy(id: number, busy: boolean): void {
    if (busy) {
      this.busyNodeIds.add(id);
      return;
    }
    this.busyNodeIds.delete(id);
  }
}
