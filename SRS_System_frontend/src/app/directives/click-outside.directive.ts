import { Directive, ElementRef, EventEmitter, HostListener, Output } from '@angular/core';

@Directive({
    selector: '[clickOutside]',
    standalone: true
})
export class ClickOutsideDirective {

    @Output() clickOutside = new EventEmitter<void>();

    constructor(private el: ElementRef) { }


    @HostListener('document:click', ['$event'])
    onDocumentClick(event: MouseEvent) {
        const target = event.target as HTMLElement | null;
        if (!target) return;

        if (!this.el.nativeElement.contains(target)) {
            this.clickOutside.emit();
        }
    }


}
