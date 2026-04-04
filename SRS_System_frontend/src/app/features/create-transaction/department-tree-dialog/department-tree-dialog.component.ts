import { Component, Inject, OnInit } from '@angular/core';
import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeNestedDataSource } from '@angular/material/tree';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTreeModule } from '@angular/material/tree';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { CdkTreeModule } from '@angular/cdk/tree';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';
import { AfterViewInit } from '@angular/core';

interface DepartmentNode {
  name: string;
  children?: DepartmentNode[];
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
    MatDialogModule
  ]
})
export class DepartmentTreeDialogComponent implements OnInit {

  treeControl = new NestedTreeControl<DepartmentNode>(node => node.children);
  dataSource = new MatTreeNestedDataSource<DepartmentNode>();
  selected = new Set<string>();

  departments: DepartmentNode[] = [
    {
      name: 'الإدارة العامة للموارد البشرية',
      children: [
        { name: 'إدارة التوظيف' },
        { name: 'إدارة التدريب والتطوير' }
      ]
    },
    {
      name: 'الإدارة العامة لتقنية المعلومات',
      children: [
        { name: 'إدارة البنية التحتية' },
        { name: 'إدارة الأمن السيبراني' }
      ]
    },
    {
      name: 'الإدارة العامة للشؤون المالية',
      children: [
        { name: 'إدارة الميزانية' },
        { name: 'إدارة الحسابات' }
      ]
    }
  ];

  constructor(
    private dialogRef: MatDialogRef<DepartmentTreeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string[]
  ) {
    // this.dataSource.data = this.departments;


    if (data) {
      data.forEach(x => this.selected.add(x));
    }
  }

  ngOnInit(): void {
    this.dataSource.data = this.departments;

    // Expand كل parent
    this.expandAllNodes(this.departments);
  }

  expandAllNodes(nodes: DepartmentNode[]) {
    nodes.forEach(node => {
      this.treeControl.expand(node);
      if (node.children && node.children.length) {
        this.expandAllNodes(node.children);
      }
    });
  }
  // ─────────────────────────────────────────
  //  Tree predicate
  // ─────────────────────────────────────────

  hasChild = (_: number, node: DepartmentNode): boolean =>
    !!node.children && node.children.length > 0;

  // ─────────────────────────────────────────
  //  Helpers
  // ─────────────────────────────────────────

  getAllNames(nodes: DepartmentNode[]): string[] {
    let result: string[] = [];
    nodes.forEach(node => {
      result.push(node.name);
      if (node.children) {
        result = result.concat(this.getAllNames(node.children));
      }
    });
    return result;
  }

  /** إيجاد الأب المباشر لابن معين */
  private findParent(childName: string): DepartmentNode | null {
    for (const parent of this.departments) {
      if (parent.children?.some(c => c.name === childName)) {
        return parent;
      }
    }
    return null;
  }

  // ─────────────────────────────────────────
  //  State checks
  // ─────────────────────────────────────────

  isSelected(name: string): boolean {
    return this.selected.has(name);
  }

  isAllSelected(): boolean {
    const allNames = this.getAllNames(this.departments);
    return allNames.length > 0 && allNames.every(name => this.selected.has(name));
  }

  /** ✅ ناقصة — indeterminate لـ "اختيار الكل" */
  isSomeSelected(): boolean {
    const allNames = this.getAllNames(this.departments);
    const selCount = allNames.filter(name => this.selected.has(name)).length;
    return selCount > 0 && selCount < allNames.length;
  }

  /** ✅ ناقصة — indeterminate للأب لما بعض أبنائه محددين */
  isIndeterminate(node: DepartmentNode): boolean {
    if (!node.children || node.children.length === 0) return false;
    const selCount = node.children.filter(c => this.selected.has(c.name)).length;
    return selCount > 0 && selCount < node.children.length;
  }

  // ─────────────────────────────────────────
  //  Toggle actions
  // ─────────────────────────────────────────

  toggleAll(checked: boolean): void {
    const allNames = this.getAllNames(this.departments);
    if (checked) {
      allNames.forEach(name => this.selected.add(name));
    } else {
      this.selected.clear();
    }
  }

  // toggleNode(node: DepartmentNode, checked: boolean): void {
  //   if (checked) {
  //     this.selected.add(node.name);
  //     node.children?.forEach(child => this.selected.add(child.name));
  //   } else {
  //     this.selected.delete(node.name);
  //     node.children?.forEach(child => this.selected.delete(child.name));
  //   }
  // }

  toggleNode(node: DepartmentNode, checked: boolean): void {
    checked
      ? this.selected.add(node.name)
      : this.selected.delete(node.name);
  }

  /** ✅ محدَّثة — تحديد ابن + تحديث حالة الأب تلقائياً */
  // toggleLeaf(name: string, checked: boolean): void {
  //   checked ? this.selected.add(name) : this.selected.delete(name);

  //   // تحديث حالة الأب بناءً على أبنائه
  //   const parent = this.findParent(name);
  //   if (!parent) return;

  //   const allSelected = parent.children!.every(c => this.selected.has(c.name));
  //   const noneSelected = parent.children!.every(c => !this.selected.has(c.name));

  //   if (allSelected) this.selected.add(parent.name);
  //   else if (noneSelected) this.selected.delete(parent.name);
  //   // لو indeterminate → الأب مش موجود في selected (بس checkbox هتظهر indeterminate)
  // }

  toggleLeaf(name: string, checked: boolean): void {
    checked
      ? this.selected.add(name)
      : this.selected.delete(name);
  }

  // ─────────────────────────────────────────
  //  Dialog actions
  // ─────────────────────────────────────────

  confirm(): void {
    this.dialogRef.close(Array.from(this.selected));
  }
}
