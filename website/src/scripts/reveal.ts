// Entrances: [data-in] elements get .is-in when they scroll into view (motion.css has
// the looks). With reduced motion, or without IntersectionObserver, everything shows at
// once.
const reduced = matchMedia('(prefers-reduced-motion: reduce)');

export function initReveal(root: ParentNode = document): void {
  const items = [...root.querySelectorAll<HTMLElement>('[data-in]')];
  if (!items.length) return;
  const show = (el: Element) => el.classList.add('is-in');
  if (reduced.matches || !('IntersectionObserver' in window)) {
    items.forEach(show);
    return;
  }
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        show(entry.target);
        observer.unobserve(entry.target);
      }
    },
    { rootMargin: '0px 0px -9% 0px', threshold: 0 },
  );
  items.forEach((el) => observer.observe(el));
  reduced.addEventListener('change', () => reduced.matches && items.forEach(show));
}
