import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { FormsModule, NgModel } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { EditorModule } from '@tinymce/tinymce-angular';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';




interface TimelineStep {
  action: string;
  user: string;
  avatar?: string;
  date: string;
  note?: string;
}

interface Attachment {
  id: number;
  name: string;
  type: string;
  secrecy: string;
  date: string;
  user: string;
}

interface Transaction {
  id: string;
  type: string;
  subject: string;
  description: string;
  from: string;
  to: string;
  created: string;
  status: string;
  secrecy: string;
  maxDays: number;
  timeline: TimelineStep[];
  attachments: Attachment[];
}

@Component({
  selector: 'app-transaction-details',
  templateUrl: './transaction-details.html',
  styleUrls: ['./transaction-details.css'],
  imports: [
    CommonModule,
    FormsModule,
    EditorModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatIconModule,


  ],
  standalone: true,
  encapsulation: ViewEncapsulation.None,


})
export class TransactionDetailsComponent implements OnInit {

  transaction!: Transaction;
  showModal = false;
  actionTitle = '';
  actionNote = '';

  editorInit: any;
  form!: FormGroup;

  activeIndex = 2;
  activeTab = 'details';


  constructor(private route: ActivatedRoute, private fb: FormBuilder) {


    this.editorInit = {
      language: 'ar',
      directionality: 'rtl',
      height: 600,
      menubar: false,
      branding: false,
      statusbar: true,
      resize: true,
      plugins: [
        'lists', 'link', 'table', 'code', 'fullscreen', 'wordcount'
      ],
      toolbar:
        'undo redo | formatselect | bold italic underline | ' +
        'alignright aligncenter alignleft | ' +
        'bullist numlist | table | removeformat code fullscreen',
      font_family_formats:
        'Cairo=Cairo, sans-serif;' +
        'Tajawal=Tajawal, sans-serif;' +
        'Arial=arial,helvetica,sans-serif',
      content_style: `
        @import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap');
        body {
          font-family: 'Cairo', sans-serif;
          font-size: 15px;
          line-height: 2;
          color: #1a1a1a;
          direction: rtl;
          padding: 40px;
          max-width: 210mm;
          margin: 0 auto;
        }
        h1, h2, h3 { color: #0B6E4F; font-weight: 700; }
        .label { font-weight: 600; color: #0B6E4F; }
      `
    };

    this.form = this.fb.group({
      letterContent: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.loadTransaction(id!);
  }

  loadTransaction(id: string) {
    // DEMO DATA
    this.transaction = {
      id,
      type: 'وارد',
      subject: 'طلب اعتماد ميزانية',
      description: 'نرجو التكرم باعتماد الميزانية المرفقة للعام القادم.',
      from: 'وزارة المالية',
      to: 'إدارة الموارد',
      created: '2026-01-22',
      status: 'قيد الإجراء',
      secrecy: 'عادي',
      maxDays: 8,
      timeline: [
        {
          action: 'إنشاء المعاملة',
          user: 'أحمد',
          date: '2026-01-22T09:00:00',
          note: 'تم إنشاء المعاملة عبر نظام الاتصالات الإدارية.'
        },
        {
          action: 'تسجيل وتصنيف',
          user: 'إدارة الوارد',
          date: '2026-01-22T10:00:00',
          note: 'تم تسجيلها رسمياً وتصنيفها كطلب اعتماد مالي.'
        },
        {
          action: 'إحالة للإدارة المختصة',
          user: 'إدارة الوارد',
          date: '2026-01-22T11:30:00',
          note: 'تم تحويل المعاملة إلى إدارة الموارد لدراستها.'
        },
        {
          action: 'مراجعة ودراسة الطلب',
          user: 'مدير القسم',
          date: '2026-01-23T09:15:00',
          note: 'تمت مراجعة المستندات وجاري استكمال التوصيات.'
        },
        {
          action: 'اعتماد مبدئي',
          user: 'مدير الإدارة',
          date: '2026-01-24T12:00:00',
          note: 'تم الاعتماد المبدئي وجاري الرفع للاعتماد النهائي.'
        }
      ],
      attachments: [
        { id: 1, name: 'ميزانية.pdf', type: 'إلكتروني', secrecy: 'عادي', date: '2026-01-22', user: 'أحمد' }
      ]
    };

    this.form = this.fb.group({
      letterContent: this.defaultTemplate()
    });
  }

  openAction(title: string) {
    this.actionTitle = title;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  submitAction() {
    this.transaction.timeline.push({
      action: this.actionTitle,
      user: 'المستخدم الحالي',
      date: new Date().toISOString(),
      note: this.actionNote
    });

    this.closeModal();
    this.actionNote = '';
  }


  defaultTemplate(): string {
    const today = new Date().toLocaleDateString('ar-EG', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });

    return `
      <div style="text-align: center; margin-bottom: 30px; border-bottom: 2px solid #0B6E4F; padding-bottom: 20px;">
        <h1 style="color: #0B6E4F; margin: 0;">🏛️ [اسم الجهة]</h1>
        <p style="color: #666; margin: 5px 0;">الإدارة العامة للاتصالات الإدارية</p>
      </div>

      <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 30px;">
        <p style="margin: 5px 0;"><span class="label">التاريخ:</span> ${today}</p>
        <p style="margin: 5px 0;"><span class="label">رقم المعاملة:</span> [يُملأ تلقائياً]</p>
      </div>

      <p><span class="label">إلى:</span> ......................................................................</p>
      <p><span class="label">الموضوع:</span> ......................................................................</p>

      <hr style="border: 1px solid #e5e7eb; margin: 30px 0;">

      <p style="text-align: center; font-size: 18px; font-weight: 600; color: #0B6E4F;">
        السلام عليكم ورحمة الله وبركاته، وبعد:
      </p>

      <p style="text-align: justify; line-height: 2.2;">
        نحيطكم علماً بأنه تم استلام خطابكم المؤرخ في <strong>[التاريخ]</strong> 
        والمتعلق بـ <strong>[الموضوع]</strong>، وبناءً عليه نفيدكم بما يلي:
      </p>

      <ol style="margin: 20px 0; padding-right: 40px; line-height: 2.2;">
        <li>......................................................................</li>
        <li>......................................................................</li>
        <li>......................................................................</li>
      </ol>

      <p style="margin-top: 40px;">نشكر لكم حُسن تعاونكم.</p>

      <div style="margin-top: 60px; text-align: left;">
        <p style="font-weight: 600; color: #0B6E4F;">مدير الإدارة</p>
        <p>التوقيع: ___________________</p>
      </div>
    `;
  }


  // ── trackBy لتحسين الأداء ──
  trackByStep(index: number, step: TimelineStep): string {
    return `${index}-${step.action}`;
  }


  getInitial(name: string): string {
    return name ? name.trim().charAt(0).toUpperCase() : '?';
  }

  // Helper: لون ثابت للـ avatar بناءً على الاسم
  getAvatarColor(name: string): string {
    const colors = [
      '#1B6CA8', '#C0392B', '#16A085', '#8E44AD',
      '#D35400', '#27AE60', '#2C3E50', '#F39C12'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    // return colors[Math.abs(hash) % colors.length];
    return '#fff';
  }

  // في الـ component.ts أضف الـ method دي
  getAvatarUrl(name: string): string {
    const seed = encodeURIComponent(name ?? 'user');
    // personas = أشخاص كارتونية واضحة
    return `https://api.dicebear.com/7.x/personas/svg?seed=${seed}`;
  }


}