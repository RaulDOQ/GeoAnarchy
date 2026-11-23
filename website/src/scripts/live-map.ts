// The live map card on the home page.
//
// What is shown: the server's Dynmap site in an iframe, framed so the whole world
// exactly fills the card (scripts/detect-map.mjs works out the frame). Under it lies
// the newest snapshot of the map (worker/), which is visible while the live map loads
// and stays if the live map is down.
//
// How it is used: the visitor never touches Dynmap itself. A pad on top of the card
// takes the pointer and this script moves and enlarges the whole map, so it can only
// be zoomed in from the full view, never out past it, and never dragged off the world.
//   mouse     click, then wheel to zoom and drag to move; double click zooms in
//   touch     two fingers zoom and move (one finger still scrolls the page)
//   keyboard  + and − zoom, the arrows move, 0 or Escape goes back to the full view
import { initTilt } from './tilt';

const PROBE_TIMEOUT = 8000;
const LIVE_TIMEOUT = 20000;
const RETRY_AFTER = 60000;
/** The map is shown a touch larger than the card, so no edge line can show. */
const OVERSCAN = 1.01;
/** How far past its real pixels the map may be enlarged. */
const MAX_UPSCALE = 1.5;

export function initLiveMap(figure: HTMLElement): void {
  const card = figure.querySelector<HTMLElement>('.livemap-card');
  const world = figure.querySelector<HTMLElement>('[data-world]');
  const frame = figure.querySelector<HTMLIFrameElement>('iframe[data-src]');
  const snapshot = figure.querySelector<HTMLImageElement>('[data-snapshot]');
  const pad = figure.querySelector<HTMLElement>('[data-pad]');
  const caption = figure.querySelector<HTMLElement>('[data-caption]');
  const captionText = figure.querySelector<HTMLElement>('[data-caption-text]');
  const noteText = figure.querySelector<HTMLElement>('[data-note-text]');
  if (!card || !world || !frame || !snapshot || !pad || !caption || !captionText || !noteText) return;
  setup({ figure, card, world, frame, snapshot, pad, caption, captionText, noteText });
}

interface Parts {
  figure: HTMLElement;
  card: HTMLElement;
  world: HTMLElement;
  frame: HTMLIFrameElement;
  snapshot: HTMLImageElement;
  pad: HTMLElement;
  caption: HTMLElement;
  captionText: HTMLElement;
  noteText: HTMLElement;
}

function setup({ figure, card, world, frame, snapshot, pad, caption, captionText, noteText }: Parts): void {
  const mapWidth = Number(figure.dataset.width) || 1152;
  const mapHeight = Number(figure.dataset.height) || 576;
  const tilt = initTilt(figure, card);

  // ---- The view: how much the map is enlarged and where it sits in the card.
  let cardWidth = 0;
  let cardHeight = 0;
  /** 1 is the full view; larger is zoomed in. */
  let zoom = 1;
  /** Where the map's top left corner is, in card pixels. */
  let left = 0;
  let top = 0;

  const cover = () => (cardWidth / mapWidth) * OVERSCAN;
  const maxZoom = () => Math.max(2, MAX_UPSCALE / cover());

  function apply(): void {
    const scale = cover() * zoom;
    // Keep the map over the whole card: it can never be dragged off the world.
    left = Math.min(0, Math.max(cardWidth - mapWidth * scale, left));
    top = Math.min(0, Math.max(cardHeight - mapHeight * scale, top));
    world.style.transform = `translate(${left.toFixed(2)}px, ${top.toFixed(2)}px) scale(${scale.toFixed(5)})`;
    figure.classList.toggle('is-zoomed', zoom > 1.001);
  }

  /** Zooms to `next`, keeping the point (x, y) of the card where it is. */
  function zoomAt(next: number, x: number, y: number): void {
    const before = cover() * zoom;
    zoom = Math.min(maxZoom(), Math.max(1, next));
    const after = cover() * zoom;
    left = x - ((x - left) / before) * after;
    top = y - ((y - top) / before) * after;
    apply();
  }

  function reset(): void {
    zoom = 1;
    const scale = cover();
    left = (cardWidth - mapWidth * scale) / 2;
    top = (cardHeight - mapHeight * scale) / 2;
    apply();
  }

  function measure(): void {
    const width = card.clientWidth;
    const height = card.clientHeight;
    if (!width || !height) return;
    if (!cardWidth) {
      cardWidth = width;
      cardHeight = height;
      reset();
      figure.classList.add('is-ready');
      return;
    }
    // On a resize the same part of the map stays in the middle.
    const scaleBefore = cover() * zoom;
    const midX = (cardWidth / 2 - left) / scaleBefore;
    const midY = (cardHeight / 2 - top) / scaleBefore;
    cardWidth = width;
    cardHeight = height;
    zoom = Math.min(zoom, maxZoom());
    const scaleAfter = cover() * zoom;
    left = cardWidth / 2 - midX * scaleAfter;
    top = cardHeight / 2 - midY * scaleAfter;
    apply();
  }
  new ResizeObserver(measure).observe(card);
  measure();

  // ---- Mouse: click to take the wheel, then wheel and drag.
  const setActive = (active: boolean) => {
    if (figure.classList.contains('is-active') === active) return;
    figure.classList.toggle('is-active', active);
    tilt.hold(active);
  };
  const point = (e: { clientX: number; clientY: number }) => {
    const box = card.getBoundingClientRect();
    // The card may be tilted or scaled by CSS: measure in its own pixels.
    return { x: ((e.clientX - box.left) / box.width) * cardWidth, y: ((e.clientY - box.top) / box.height) * cardHeight };
  };

  const pointers = new Map<number, { x: number; y: number }>();
  let drag: { id: number; x: number; y: number; moved: number } | null = null;
  let pinch: { distance: number; x: number; y: number } | null = null;

  const pinchState = () => {
    const [a, b] = [...pointers.values()];
    return { distance: Math.hypot(a.x - b.x, a.y - b.y), x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
  };

  pad.addEventListener('pointerdown', (e) => {
    const p = point(e);
    if (e.pointerType === 'touch') {
      pointers.set(e.pointerId, p);
      if (pointers.size === 2) {
        pinch = pinchState();
        for (const id of pointers.keys()) pad.setPointerCapture(id);
        setActive(true);
      }
      return;
    }
    if (e.button !== 0) return;
    drag = { id: e.pointerId, x: p.x, y: p.y, moved: 0 };
    pad.setPointerCapture(e.pointerId);
  });

  pad.addEventListener('pointermove', (e) => {
    const p = point(e);
    if (e.pointerType === 'touch') {
      if (!pointers.has(e.pointerId)) return;
      pointers.set(e.pointerId, p);
      if (pinch && pointers.size === 2) {
        const now = pinchState();
        left += now.x - pinch.x;
        top += now.y - pinch.y;
        zoomAt(zoom * (now.distance / (pinch.distance || 1)), now.x, now.y);
        pinch = now;
      }
      return;
    }
    if (!drag || drag.id !== e.pointerId) return;
    const dx = p.x - drag.x;
    const dy = p.y - drag.y;
    drag.moved += Math.abs(dx) + Math.abs(dy);
    drag.x = p.x;
    drag.y = p.y;
    if (figure.classList.contains('is-active') && zoom > 1) {
      figure.classList.add('is-dragging');
      left += dx;
      top += dy;
      apply();
    }
  });

  const release = (e: PointerEvent) => {
    if (e.pointerType === 'touch') {
      pointers.delete(e.pointerId);
      if (pointers.size < 2) pinch = null;
      if (pointers.size === 0) setActive(false);
      return;
    }
    if (!drag || drag.id !== e.pointerId) return;
    // A press without movement is a click: it hands the wheel to the map.
    if (e.type === 'pointerup' && drag.moved < 5) setActive(true);
    drag = null;
    figure.classList.remove('is-dragging');
  };
  pad.addEventListener('pointerup', release);
  pad.addEventListener('pointercancel', release);

  pad.addEventListener('pointerleave', (e) => {
    if (e.pointerType !== 'touch' && !drag) setActive(false);
  });
  pad.addEventListener('dblclick', (e) => {
    const p = point(e);
    setActive(true);
    zoomAt(zoom * 1.8, p.x, p.y);
  });
  pad.addEventListener(
    'wheel',
    (e) => {
      // Until the visitor clicks the map, the wheel keeps scrolling the page.
      if (!figure.classList.contains('is-active')) return;
      e.preventDefault();
      const p = point(e);
      const step = e.deltaMode === 1 ? e.deltaY * 16 : e.deltaY;
      zoomAt(zoom * Math.exp(-step * 0.0018), p.x, p.y);
    },
    { passive: false },
  );
  pad.addEventListener('keydown', (e) => {
    const step = 48;
    const moves: Record<string, [number, number]> = { ArrowLeft: [step, 0], ArrowRight: [-step, 0], ArrowUp: [0, step], ArrowDown: [0, -step] };
    if (e.key === '+' || e.key === '=') zoomAt(zoom * 1.4, cardWidth / 2, cardHeight / 2);
    else if (e.key === '-' || e.key === '_') zoomAt(zoom / 1.4, cardWidth / 2, cardHeight / 2);
    else if (e.key === '0' || e.key === 'Escape') reset();
    else if (e.key in moves && zoom > 1) {
      left += moves[e.key][0];
      top += moves[e.key][1];
      apply();
    } else return;
    e.preventDefault();
  });

  // ---- What is shown: the snapshot first, then the live map if it answers.
  let snapshotTime: number | null = null;
  let live = false;
  let started = false;
  let liveTimer = 0;

  const timeText = (takenAt: number) => {
    const date = new Date(takenAt);
    const time = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    const today = new Date().toDateString() === date.toDateString();
    return today ? time : `${time} de ${date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}`;
  };

  function showCaption(): void {
    if (live) captionText.textContent = caption.dataset.live ?? '';
    else if (!figure.classList.contains('is-offline')) return;
    else if (snapshotTime !== null) captionText.textContent = (caption.dataset.snapshot ?? '').replace('{time}', timeText(snapshotTime));
    else captionText.textContent = caption.dataset.offline ?? '';
  }

  async function loadSnapshot(): Promise<void> {
    const infoUrl = figure.dataset.snapshotInfo;
    if (!infoUrl) return;
    try {
      const response = await fetch(infoUrl);
      if (!response.ok) return;
      const info = (await response.json()) as { takenAt?: unknown; url?: unknown };
      if (typeof info.takenAt !== 'number' || typeof info.url !== 'string') return;
      const takenAt = info.takenAt;
      snapshot.addEventListener(
        'load',
        () => {
          snapshotTime = takenAt;
          figure.classList.add('has-snapshot');
          showCaption();
        },
        { once: true },
      );
      snapshot.src = info.url;
    } catch {
      // No snapshot service (for example in local development): the card just waits.
    }
  }

  function setOffline(): void {
    if (live) return;
    figure.classList.add('is-offline');
    noteText.textContent = figure.dataset.offlineText ?? '';
    showCaption();
    window.setTimeout(probe, RETRY_AFTER);
  }

  function setLive(): void {
    window.clearTimeout(liveTimer);
    live = true;
    figure.classList.remove('is-offline');
    figure.classList.add('is-live');
    showCaption();
  }

  /** Asks the map site for a small picture: an answer means the map is up. */
  function probe(): void {
    if (live) return;
    const probeUrl = figure.dataset.probe;
    if (!probeUrl) return;
    const image = new Image();
    const timer = window.setTimeout(() => {
      image.onload = image.onerror = null;
      setOffline();
    }, PROBE_TIMEOUT);
    image.onload = () => {
      window.clearTimeout(timer);
      // Setting the address again reloads the frame, which is what a retry needs.
      frame.addEventListener('load', setLive, { once: true });
      window.clearTimeout(liveTimer);
      liveTimer = window.setTimeout(setOffline, LIVE_TIMEOUT);
      frame.src = frame.dataset.src ?? '';
    };
    image.onerror = () => {
      window.clearTimeout(timer);
      setOffline();
    };
    image.src = `${probeUrl}?_=${Date.now()}`;
  }

  const start = () => {
    if (started) return;
    started = true;
    void loadSnapshot();
    probe();
  };
  if ('IntersectionObserver' in window) {
    const near = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        near.disconnect();
        start();
      },
      { rootMargin: '300px' },
    );
    near.observe(figure);
  } else {
    start();
  }
}
