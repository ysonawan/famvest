import {
  trigger,
  state,
  style,
  transition,
  animate, query, stagger,
} from '@angular/animations';

export const expandCollapseAnimation = trigger('expandCollapse', [
  state('collapsed', style({ height: '0px', opacity: 0, padding: '0' })),
  state('expanded', style({ height: '*', opacity: 1 })),
  transition('collapsed <=> expanded', animate('300ms ease')),
]);

export const listItemAnimation = trigger('listAnimation', [
  transition('* => *', [
    // Animate entering elements
    query(':enter', [
      style({ opacity: 0, transform: 'translateY(-10px)' }),
      stagger(50, [
        animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ], { optional: true }),

    // Animate leaving elements
    query(':leave', [
      stagger(50, [
        animate('150ms ease-out', style({ opacity: 0, transform: 'translateY(10px)' }))
      ])
    ], { optional: true })
  ])
]);

export const fadeInUpAnimation = trigger('fadeInUp', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateY(20px)' }),
    animate('400ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
  ]),
  transition(':leave', [
    animate('300ms ease-in', style({ opacity: 0, transform: 'translateY(-20px)' }))
  ])
]);

