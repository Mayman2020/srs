import { Component, Inject, OnInit } from '@angular/core';
import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeNestedDataSource } from '@angular/material/tree';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTreeModule } from '@angular/material/tree';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { CdkTreeModule } from '@angular/cdk/tree';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { DepartmentApiService } from '../../../core/api/department-api.service';
import { DepartmentFlatDto } from '../../../core/api/api-types';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';

export interface DepartmentTreeNode {
  id: number;
  label: string;
  children?: DepartmentTreeNode[];
}

@Component({
  selector: 'app-department-tree-dialog',
  templateUrl: './department-tree-dialog.component.html',
  styleUrls: ['./department-tree-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatTreeModule,
    MatCheckboxModule,
    CdkTreeModule,
    MatDialogModule,
    TranslatePipe
  ]
})
export class DepartmentTreeDialogComponent implements OnInit {
  treeControl = new NestedTreeControl<DepartmentTreeNode>((node) => node.children);
  dataSource = new MatTreeNestedDataSource<DepartmentTreeNode>();
  selected = new Set<number>();

  loadError = false;

  constructor(
    private dialogRef: MatDialogRef<DepartmentTreeDialogComponent, number[]>,
    private departmentApi: DepartmentApiService,
    private i18n: I18nService,
    @Inject(MAT_DIALOG_DATA) public data: number[]
  ) {
    if (data?.length) {
      data.forEach((id) => this.selected.add(id));
    }
  }

  ngOnInit(): void {
    this.departmentApi.list().subscribe({
      next: (rows) => {
        this.loadError = false;
        const tree = this.buildTree(rows ?? []);
        this.dataSource.data = tree;
        this.expandAllNodes(tree);
      },
      error: () => {
        this.loadError = true;
        this.dataSource.data = [];
      }
    });
  }

  private displayName(row: DepartmentFlatDto): string {
    return this.i18n.currentLang() === 'en' ? row.nameEn : row.nameAr;
  }

  private buildTree(flat: DepartmentFlatDto[]): DepartmentTreeNode[] {
    const byParent = new Map<number | null, DepartmentFlatDto[]>();
    for (const row of flat) {
      const p = row.parentId ?? null;
      const list = byParent.get(p) ?? [];
      list.push(row);
      byParent.set(p, list);
    }
    for (const list of byParent.values()) {
      list.sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id);
    }

    const build = (parentId: number | null): DepartmentTreeNode[] => {
      const rows = byParent.get(parentId) ?? [];
      return rows.map((r) => {
        const children = build(r.id);
        const node: DepartmentTreeNode = {
          id: r.id,
          label: this.displayName(r)
        };
        if (children.length) {
          node.children = children;
        }
        return node;
      });
    };

    return build(null);
  }

  expandAllNodes(nodes: DepartmentTreeNode[]) {
    nodes.forEach((node) => {
      this.treeControl.expand(node);
      if (node.children?.length) {
        this.expandAllNodes(node.children);
      }
    });
  }

  hasChild = (_: number, node: DepartmentTreeNode): boolean => !!node.children?.length;

  getAllNodes(nodes: DepartmentTreeNode[]): DepartmentTreeNode[] {
    let out: DepartmentTreeNode[] = [];
    for (const n of nodes) {
      out.push(n);
      if (n.children?.length) {
        out = out.concat(this.getAllNodes(n.children));
      }
    }
    return out;
  }

  isSelected(id: number): boolean {
    return this.selected.has(id);
  }

  isAllSelected(): boolean {
    const all = this.getAllNodes(this.dataSource.data);
    return all.length > 0 && all.every((n) => this.selected.has(n.id));
  }

  isSomeSelected(): boolean {
    const all = this.getAllNodes(this.dataSource.data);
    const sel = all.filter((n) => this.selected.has(n.id)).length;
    return sel > 0 && sel < all.length;
  }

  isIndeterminate(node: DepartmentTreeNode): boolean {
    if (!node.children?.length) {
      return false;
    }
    const sel = node.children.filter((c) => this.selected.has(c.id)).length;
    return sel > 0 && sel < node.children.length;
  }

  toggleAll(checked: boolean): void {
    const all = this.getAllNodes(this.dataSource.data);
    if (checked) {
      all.forEach((n) => this.selected.add(n.id));
    } else {
      this.selected.clear();
    }
  }

  toggleNode(node: DepartmentTreeNode, checked: boolean): void {
    if (checked) {
      this.selected.add(node.id);
    } else {
      this.selected.delete(node.id);
    }
  }

  toggleLeaf(id: number, checked: boolean): void {
    if (checked) {
      this.selected.add(id);
    } else {
      this.selected.delete(id);
    }
  }

  confirm(): void {
    this.dialogRef.close(Array.from(this.selected));
  }
}
