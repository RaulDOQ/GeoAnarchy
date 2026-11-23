// The live map window on the home page. The map itself is the server's Dynmap site in
// an iframe; this script only decides when it loads and when it takes the pointer:
//  - the iframe gets its address when the window comes near the screen;
//  - a guard sits over the map so scrolling the page never zooms it by accident;
//    a click (or tap, or Enter) hands the pointer to the map, and leaving the window,
//    clicking elsewhere or scrolling it away gives the page back its scroll;
//  - if the map has not answered after a while, a link to open it directly appears.
import { initTilt } from './tilt';

const LOAD_TIMEOUT = 15000;

export function initLiveMap(figure: HTMLElement): void {
  const win = figure.querySelector<HTMLElement>('.livemap-window');
  const frame = figure.querySelector<HTMLIFrameElement>('iframe[data-src]');
  const guard = figure.querySelector<HTMLButtonElement>('.livemap-guard');
  if (!win || !frame || !guard) return;

  const tilt = initTilt(figure, win);
  let failTimer = 0;

  const load = () => {
    if (frame.src) return;
    frame.addEventListener(
      'load',
      () => {
        window.clearTimeout(failTimer);
        figure.classList.remove('is-failed');
        figure.classList.add('is-loaded');
      },
      { once: true },
    );
    failTimer = window.setTimeout(() => {
      if (!figure.classList.contains('is-loaded')) figure.classList.add('is-failed');
    }, LOAD_TIMEOUT);
    frame.src = frame.dataset.src ?? '';
  };

  const setActive = (active: boolean) => {
    if (figure.classList.contains('is-active') === active) return;
    figure.classList.toggle('is-active', active);
    guard.tabIndex = active ? -1 : 0;
    tilt.hold(active);
    if (active) frame.focus({ preventScroll: true });
  };

  guard.addEventListener('click', () => setActive(true));
  // Mouse: leaving the window gives the page its scroll back.
  win.addEventListener('pointerleave', (e) => {
    if (e.pointerType !== 'touch') setActive(false);
  });
  // Touch and keyboard: a tap or click anywhere outside the window does the same.
  document.addEventListener('pointerdown', (e) => {
    if (!win.contains(e.target as Node)) setActive(false);
  });

  if ('IntersectionObserver' in window) {
    const near = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        near.disconnect();
        load();
      },
      { rootMargin: '300px' },
    );
    near.observe(figure);
    new IntersectionObserver((entries) => {
      if (entries.every((entry) => !entry.isIntersecting)) setActive(false);
    }).observe(win);
  } else {
    load();
  }
}
