import { Component, OnInit, AfterViewInit, Inject, PLATFORM_ID, OnDestroy } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';

// تعريف واجهة particles.js
declare var particlesJS: any;

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ]
})
export class LoginComponent implements OnInit, AfterViewInit , OnDestroy {

  form!: FormGroup;

  toast = {
    show: false,
    title: '',
    message: ''
  };

  constructor(
    private fb: FormBuilder,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}



ngOnDestroy() {
  document.body.classList.remove('login-page');
}

  ngOnInit(): void {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
      document.body.classList.add('login-page');
  }

  ngAfterViewInit(): void {
    // تهيئة الخلفية المتحركة بعد تحميل العنصر
    if (isPlatformBrowser(this.platformId)) {
      // تأخير بسيط للتأكد من تحميل DOM
      setTimeout(() => {
        this.initParticles();
      }, 100);
    }
  }

  /**
   * تهيئة الخلفية المتحركة (Particles.js)
   * بألوان حكومية سعودية (أخضر وذهبي)
   */
private initParticles(): void {
  if (typeof particlesJS === 'undefined') {
    console.error('❌ مكتبة particles.js غير محملة!');
    return;
  }

  console.log('✅ بدء تهيئة الخلفية المتحركة...');

  particlesJS('particles-js', {
    particles: {
      number: {
        value: 70, // ✅ عدد الجزيئات
        density: {
          enable: true,
          value_area: 800
        }
      },
      color: {
        value: '#FFD700' // ✅ ذهبي واضح بدلاً من الأبيض
      },
      shape: {
        type: 'circle'
      },
      opacity: {
        value: 0.8, // ✅ وضوح عالي
        random: true,
        anim: {
          enable: true,
          speed: 1,
          opacity_min: 0.5,
          sync: false
        }
      },
      size: {
        value: 6, // ✅ حجم أكبر
        random: true,
        anim: {
          enable: true,
          speed: 3,
          size_min: 2,
          sync: false
        }
      },
      line_linked: {
        enable: true,
        distance: 150,
        color: '#FFD700', // ✅ خطوط بيضاء واضحة
        opacity: 0.7, // ✅ وضوح الخطوط
        width: 2.5 // ✅ سُمك واضح
      },
      move: {
        enable: true,
        speed: 2.5, // ✅ سرعة مناسبة
        direction: 'none',
        random: true,
        straight: false,
        out_mode: 'out',
        bounce: false
      }
    },
    interactivity: {
      detect_on: 'canvas',
      events: {
        onhover: {
          enable: true,
          mode: 'grab'
        },
        onclick: {
          enable: true,
          mode: 'push'
        },
        resize: true
      },
      modes: {
        grab: {
          distance: 200,
          line_linked: {
            opacity: 1 // ✅ خطوط واضحة عند hover
          }
        },
        push: {
          particles_nb: 8
        }
      }
    },
    retina_detect: true
  });

  console.log('✅ تم تهيئة الخلفية المتحركة بنجاح!');
}



  /**
   * تسجيل الدخول
   */
  login(): void {
    // if (this.form.invalid) {
    //   this.showToast('تنبيه', 'يرجى إدخال اسم المستخدم وكلمة المرور');
    //   return;
    // }

    // const { username, password } = this.form.value;

    // يمكنك تفعيل هذا الكود للتحقق من بيانات الدخول
    // if (username === 'admin' && password === '1234') {
    //   sessionStorage.setItem(
    //     'user',
    //     JSON.stringify({ 
    //       name: 'مدير النظام', 
    //       role: 'مدير',
    //       loginTime: new Date().toISOString()
    //     })
    //   );
    //   this.showToast('✅ تم بنجاح', 'مرحباً بك في نظام ERP الحكومي');
    //   setTimeout(() => this.router.navigate(['/dashboard']), 800);
    // } else {
    //   this.showToast('❌ خطأ', 'اسم المستخدم أو كلمة المرور غير صحيحة');
    //   return;
    // }

    // للتجربة: تسجيل دخول مباشر
    sessionStorage.setItem(
      'user',
      JSON.stringify({ 
        name: 'مستخدم تجريبي', 
        role: 'موظف',
        loginTime: new Date().toISOString()
      })
    );
    
    this.showToast('✅ تم بنجاح', 'مرحباً بك في نظام ERP الحكومي');
    setTimeout(() => this.router.navigate(['/dashboard']), 800);
  }

  /**
   * استعادة كلمة المرور
   */
  forgotPassword(): void {
    this.showToast(
      '📧 استعادة كلمة المرور', 
      'يرجى التواصل مع قسم الدعم الفني لاستعادة كلمة المرور'
    );
  }

  /**
   * عرض رسالة Toast
   */
  private showToast(title: string, message: string): void {
    this.toast = { show: true, title, message };
    setTimeout(() => {
      this.toast.show = false;
    }, 3000); // 3 ثواني
  }
}
