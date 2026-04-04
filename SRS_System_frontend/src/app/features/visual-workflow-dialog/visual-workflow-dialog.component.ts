import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, Inject, ViewChild } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Transaction } from '../new_transaction_details/transaction-details';
import { VisualTrackingTreeComponent } from '../visual-tracking-tree/visual-tracking-tree.component';


export interface VisualWorkflowDialogData {
  transaction: Transaction;
  activeIndex: number;
}

@Component({
  selector: 'app-visual-workflow-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    VisualTrackingTreeComponent,
  ],
  templateUrl: "./visual-workflow-dialog.component.html",
  styleUrl: "./visual-workflow-dialog.component.scss"

})
export class VisualWorkflowDialogComponent implements AfterViewInit {

  @ViewChild(VisualTrackingTreeComponent) treeComp!: VisualTrackingTreeComponent;

  constructor(
    private ref: MatDialogRef<VisualWorkflowDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: VisualWorkflowDialogData
  ) { }

  ngAfterViewInit(): void {
    this.ref.afterOpened().subscribe(() => {
      setTimeout(() => {
        this.treeComp?.focusRoot();
      }, 50);
    });
  }

  close(): void {
    this.ref.close();
  }

  zoomIn(): void {
    this.treeComp?.zoomIn();
  }
  zoomOut(): void {
    this.treeComp?.zoomOut();
  }
  reset(): void { this.treeComp?.resetView(); }

}
