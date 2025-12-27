import { Component, HostListener, OnInit, Renderer2, ElementRef } from '@angular/core';

@Component({
  selector: 'app-scroll-hover',
  templateUrl: './scroll-hover.component.html',
  standalone: true,
  styleUrls: ['./scroll-hover.component.css']
})
export class ScrollHoverComponent implements OnInit {
  private scrollbarWidth = 20; // Width of detection zone in pixels

  constructor(private renderer: Renderer2, private el: ElementRef) { }

  ngOnInit() {
    // Set initial state
    this.hideScrollbar();
  }

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(event: MouseEvent) {
    const windowWidth = window.innerWidth;

    if (event.clientX > windowWidth - this.scrollbarWidth) {
      this.showScrollbar();
    } else {
      this.hideScrollbar();
    }
  }

  @HostListener('document:mouseleave')
  onMouseLeave() {
    this.hideScrollbar();
  }

  private showScrollbar() {
    this.renderer.addClass(document.documentElement, 'show-scrollbar');
  }

  private hideScrollbar() {
    this.renderer.removeClass(document.documentElement, 'show-scrollbar');
  }
}
