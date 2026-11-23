// The live map on the home page.
//
// Normally the page draws the map itself: it fetches the map's own picture tiles from
// the server's Dynmap site and paints them on a canvas, always snapped to whole pixels
// (so no lines show between tiles) and at the level of detail the current zoom needs.
// scripts/detect-map.mjs finds the tiles at build time and checks that they answer.
//
// If the build could not verify the tiles, the page shows Dynmap's own page in a frame
// instead, scaled so the world fills the card. That works, but zooms less far.
//
// Either way, under the map lies the newest snapshot (worker/): it is visible while
// the map loads and stays, with its time, when the live map is down.
//
// The visitor never touches Dynmap. A pad on top takes the pointer:
//   mouse     click, then wheel to zoom and drag to move; double click zooms in
//   touch     two fingers zoom and move (one finger still scrolls the page)
//   keyboard  + and − zoom, the arrows move, 0 or Escape goes back to the full view
// The map can be zoomed in from the full view, never out past it or off the world.

const PROBE_TIMEOUT = 8000;
const LIVE_TIMEOUT = 20000;
const RETRY_AFTER = 60000;
/** How often the tiles on screen are fetched again, while the map is being looked at. */
const REFRESH_EVERY = 5 * 60000;
/** In a frame, the map is shown a touch larger than the card so no edge line shows. */
const FRAME_OVERSCAN = 1.01;
/** How far past its real pixels a frame may be enlarged. */
const FRAME_MAX_UPSCALE = 1.5;
/** Tiles may be enlarged to twice their real pixels at the deepest level. */
const TILE_MAX_UPSCALE = 2;
const MAX_LOADS = 24;
const MAX_TILES_KEPT = 600;

/** What scripts/detect-map.mjs found out about the map's tiles. */
interface TileSource {
  base: string;
  prefix: string;
  format: string;
  tileSize: number;
  /** The zoom level at which tiles are at full detail (0 is zoomed all the way out). */
  nativeZoom: number;
  /** Where the world lies, in pixels at zoom level 0. */
  world: { x: number; y: number; width: number; height: number };
}

interface Tile {
  image: HTMLImageElement;
  state: 'loading' | 'ready' | 'failed';
  /** The drawing pass that last used it (for deciding what to forget). */
  used: number;
}

interface Parts {
  figure: HTMLElement;
  card: HTMLElement;
  world: HTMLElement;
  frame: HTMLIFrameElement;
  snapshot: HTMLImageElement;
  canvas: HTMLCanvasElement;
  pad: HTMLElement;
  caption: HTMLElement;
  captionText: HTMLElement;
  noteText: HTMLElement;
}

export function initLiveMap(figure: HTMLElement): void {
  const card = figure.querySelector<HTMLElement>('.livemap-card');
  const world = figure.querySelector<HTMLElement>('[data-world]');
  const frame = figure.querySelector<HTMLIFrameElement>('iframe[data-src]');
  const snapshot = figure.querySelector<HTMLImageElement>('[data-snapshot]');
  const canvas = figure.querySelector<HTMLCanvasElement>('canvas[data-tiles]');
  const pad = figure.querySelector<HTMLElement>('[data-pad]');
  const caption = figure.querySelector<HTMLElement>('[data-caption]');
  const captionText = figure.querySelector<HTMLElement>('[data-caption-text]');
  const noteText = figure.querySelector<HTMLElement>('[data-note-text]');
  if (!card || !world || !frame || !snapshot || !canvas || !pad || !caption || !captionText || !noteText) return;
  setup({ figure, card, world, frame, snapshot, canvas, pad, caption, captionText, noteText });
}

/** The file name of one tile, exactly as Dynmap's page builds it (see detect-map.mjs). */
function tileName(source: TileSource, zoom: number, col: number, row: number): string {
  const level = Math.max(0, source.nativeZoom - zoom);
  const scale = 2 ** level;
  const x = scale * col;
  const y = -(scale * row);
  const prefix = level === 0 ? '' : `${'z'.repeat(level)}_`;
  return `${source.prefix}/${x >> 5}_${y >> 5}/${prefix}${x}_${y}.${source.format}`;
}

function setup({ figure, card, world, frame, snapshot, canvas, pad, caption, captionText, noteText }: Parts): void {
  const frameWidth = Number(figure.dataset.width) || 1152;
  const frameHeight = Number(figure.dataset.height) || 576;
  let source: TileSource | null = null;
  try {
    source = figure.dataset.tiles ? (JSON.parse(figure.dataset.tiles) as TileSource) : null;
  } catch {
    source = null;
  }
  const context = source ? canvas.getContext('2d') : null;
  if (!context) source = null;
  figure.classList.add(source ? 'draws-tiles' : 'shows-frame');

  // ---- The view: how much the map is enlarged and where it sits in the card.
  let cardWidth = 0;
  let cardHeight = 0;
  /** 1 is the full view; larger is zoomed in. */
  let zoom = 1;
  /** Where the map's top left corner is, in card pixels. */
  let left = 0;
  let top = 0;

  // In a frame, Dynmap lays out 128 px tiles; if the frame is scaled so that a tile is
  // not a whole number of screen pixels, thin lines show between tiles. So the frame is
  // enlarged just enough (never less than FRAME_OVERSCAN) for tiles to land on pixels.
  const overscan = () => {
    if (source || !cardWidth) return 1;
    const ratio = window.devicePixelRatio || 1;
    const tile = Math.ceil(128 * ((cardWidth * FRAME_OVERSCAN) / frameWidth) * ratio);
    return ((tile / (128 * ratio)) * frameWidth) / cardWidth;
  };
  const mapWidth = () => cardWidth * overscan() * zoom;
  const mapHeight = () => mapWidth() * (frameHeight / frameWidth);
  const maxZoom = () => {
    if (!cardWidth) return 2;
    const full = source ? 2 ** source.nativeZoom * source.world.width * TILE_MAX_UPSCALE : frameWidth * FRAME_MAX_UPSCALE;
    return Math.max(2, full / (cardWidth * overscan()));
  };

  function apply(): void {
    // Keep the map over the whole card: it can never be dragged off the world.
    left = Math.min(0, Math.max(cardWidth - mapWidth(), left));
    top = Math.min(0, Math.max(cardHeight - mapHeight(), top));
    world.style.transform = `translate(${left.toFixed(2)}px, ${top.toFixed(2)}px) scale(${(mapWidth() / frameWidth).toFixed(5)})`;
    figure.classList.toggle('is-zoomed', zoom > 1.001);
    requestDraw();
  }

  /** Zooms to `next`, keeping the point (x, y) of the card where it is. */
  function zoomAt(next: number, x: number, y: number): void {
    const before = mapWidth();
    zoom = Math.min(maxZoom(), Math.max(1, next));
    const ratio = mapWidth() / before;
    left = x - (x - left) * ratio;
    top = y - (y - top) * ratio;
    apply();
  }

  function reset(): void {
    zoom = 1;
    left = (cardWidth - mapWidth()) / 2;
    top = (cardHeight - mapHeight()) / 2;
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
    const midX = (cardWidth / 2 - left) / mapWidth();
    const midY = (cardHeight / 2 - top) / mapHeight();
    cardWidth = width;
    cardHeight = height;
    zoom = Math.min(zoom, maxZoom());
    left = cardWidth / 2 - midX * mapWidth();
    top = cardHeight / 2 - midY * mapHeight();
    apply();
  }

  // ---- Drawing the tiles.
  const tiles = new Map<string, Tile>();
  let pass = 0;
  let drawQueued = false;
  let loading = 0;
  const waiting: (() => void)[] = [];
  let tilesReady = 0;
  let tilesFailed = 0;
  /** Added to tile addresses when they are fetched again, so the browser really asks. */
  let stamp = '';
  /** The tiles the last drawing pass wanted, for the periodic refresh. */
  let onScreen: { key: string; url: string }[] = [];

  function load(tile: Tile, url: string): void {
    const start = () => {
      loading++;
      const image = new Image();
      image.decoding = 'async';
      const done = (ok: boolean) => {
        loading--;
        if (ok) {
          tile.image = image;
          if (tile.state !== 'ready') tilesReady++;
          tile.state = 'ready';
          onTileReady();
          requestDraw();
        } else if (tile.state === 'loading') {
          tile.state = 'failed';
          tilesFailed++;
          onTileFailed();
        }
        waiting.shift()?.();
      };
      image.onload = () => done(true);
      image.onerror = () => done(false);
      image.src = stamp ? `${url}?t=${stamp}` : url;
    };
    if (loading < MAX_LOADS) start();
    else waiting.push(start);
  }

  function tileAt(zoomLevel: number, col: number, row: number, wanted: boolean): Tile | undefined {
    const key = `${zoomLevel}:${col}:${row}`;
    let tile = tiles.get(key);
    if (!tile && wanted && source) {
      tile = { image: new Image(), state: 'loading', used: pass };
      tiles.set(key, tile);
      load(tile, source.base + tileName(source, zoomLevel, col, row));
    }
    if (tile) tile.used = pass;
    return tile;
  }

  function requestDraw(): void {
    if (!source || drawQueued) return;
    drawQueued = true;
    requestAnimationFrame(draw);
  }

  function draw(): void {
    drawQueued = false;
    if (!source || !context || !cardWidth) return;
    pass++;
    const ratio = Math.min(window.devicePixelRatio || 1, 2);
    const pixelWidth = Math.round(cardWidth * ratio);
    const pixelHeight = Math.round(cardHeight * ratio);
    if (canvas.width !== pixelWidth || canvas.height !== pixelHeight) {
      canvas.width = pixelWidth;
      canvas.height = pixelHeight;
    }
    context.setTransform(1, 0, 0, 1, 0, 0);
    context.clearRect(0, 0, pixelWidth, pixelHeight);

    const w = source.world;
    // Card pixels per map pixel (map pixels are counted at zoom level 0).
    const scaleX = mapWidth() / w.width;
    const scaleY = mapHeight() / w.height;
    // The level whose tiles are at least as sharp as the screen needs.
    const level = Math.min(source.nativeZoom, Math.max(0, Math.ceil(Math.log2(scaleX * ratio) - 0.25)));
    const span = source.tileSize / 2 ** level;
    // Past the tiles' real size, show crisp blocks rather than a blur.
    context.imageSmoothingEnabled = scaleX * ratio <= 2 ** level * 1.01;

    // The part of the world on screen, in map pixels.
    const x0 = Math.max(w.x, w.x - left / scaleX);
    const x1 = Math.min(w.x + w.width, w.x + (cardWidth - left) / scaleX);
    const y0 = Math.max(w.y, w.y - top / scaleY);
    const y1 = Math.min(w.y + w.height, w.y + (cardHeight - top) / scaleY);
    const edge = 1e-7;
    const firstCol = Math.floor(x0 / span);
    const lastCol = Math.floor((x1 - edge) / span);
    const firstRow = Math.floor(y0 / span);
    const lastRow = Math.floor((y1 - edge) / span);

    // Screen positions are rounded to whole pixels, and neighbours share the rounded
    // edge, so there is never a gap or an overlap between two tiles.
    const screenX = (mx: number) => Math.round((left + (mx - w.x) * scaleX) * ratio);
    const screenY = (my: number) => Math.round((top + (my - w.y) * scaleY) * ratio);

    onScreen = [];
    context.save();
    context.beginPath();
    context.rect(screenX(w.x), screenY(w.y), screenX(w.x + w.width) - screenX(w.x), screenY(w.y + w.height) - screenY(w.y));
    context.clip();
    for (let row = firstRow; row <= lastRow; row++) {
      for (let col = firstCol; col <= lastCol; col++) {
        const dx = screenX(col * span);
        const dy = screenY(row * span);
        const dw = screenX((col + 1) * span) - dx;
        const dh = screenY((row + 1) * span) - dy;
        const tile = tileAt(level, col, row, true);
        onScreen.push({ key: `${level}:${col}:${row}`, url: source.base + tileName(source, level, col, row) });
        if (tile?.state === 'ready') {
          context.drawImage(tile.image, dx, dy, dw, dh);
          continue;
        }
        // Not here yet (or missing): show the same place from a coarser tile.
        for (let up = 1; up <= level; up++) {
          const parts = 2 ** up;
          const parentCol = Math.floor(col / parts);
          const parentRow = Math.floor(row / parts);
          // Coarser tiles are only fetched once the visitor zooms in; in the full view
          // the snapshot underneath already fills the gaps while tiles arrive.
          const parent = tileAt(level - up, parentCol, parentRow, zoom > 1.001 && up <= 2);
          if (parent?.state !== 'ready') continue;
          const size = parent.image.naturalWidth / parts;
          context.drawImage(parent.image, (col - parentCol * parts) * size, (row - parentRow * parts) * size, size, size, dx, dy, dw, dh);
          break;
        }
      }
    }
    context.restore();

    // Forget tiles that have not been used for a while.
    if (tiles.size > MAX_TILES_KEPT) {
      const old = [...tiles.entries()].filter(([, tile]) => tile.state !== 'loading').sort((a, b) => a[1].used - b[1].used);
      for (const [key] of old.slice(0, tiles.size - MAX_TILES_KEPT)) tiles.delete(key);
    }
  }

  // ---- Mouse, touch and keyboard.
  const setActive = (active: boolean) => figure.classList.toggle('is-active', active);
  const point = (e: { clientX: number; clientY: number }) => {
    const box = card.getBoundingClientRect();
    // The card may be scaled by CSS while it enters: measure in its own pixels.
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
    zoomAt(zoom * 2, p.x, p.y);
  });
  pad.addEventListener(
    'wheel',
    (e) => {
      // Until the visitor clicks the map, the wheel keeps scrolling the page.
      if (!figure.classList.contains('is-active')) return;
      e.preventDefault();
      const p = point(e);
      const step = e.deltaMode === 1 ? e.deltaY * 16 : e.deltaY;
      zoomAt(zoom * Math.exp(-step * 0.003), p.x, p.y);
    },
    { passive: false },
  );
  pad.addEventListener('keydown', (e) => {
    const step = 64;
    const moves: Record<string, [number, number]> = { ArrowLeft: [step, 0], ArrowRight: [-step, 0], ArrowUp: [0, step], ArrowDown: [0, -step] };
    if (e.key === '+' || e.key === '=') zoomAt(zoom * 1.6, cardWidth / 2, cardHeight / 2);
    else if (e.key === '-' || e.key === '_') zoomAt(zoom / 1.6, cardWidth / 2, cardHeight / 2);
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
    window.clearTimeout(liveTimer);
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

  // With tiles, the map counts as live once the first tile has arrived, and as down
  // when the first several all fail.
  function onTileReady(): void {
    if (!live) setLive();
  }
  function onTileFailed(): void {
    if (!live && tilesReady === 0 && tilesFailed >= 6) setOffline();
  }

  function startTiles(): void {
    // A retry after the map was down: forget the failures and ask again.
    for (const [key, tile] of tiles) if (tile.state === 'failed') tiles.delete(key);
    tilesFailed = 0;
    liveTimer = window.setTimeout(setOffline, LIVE_TIMEOUT);
    requestDraw();
  }

  function startFrame(): void {
    // Setting the address again reloads the frame, which is what a retry needs.
    frame.addEventListener('load', setLive, { once: true });
    liveTimer = window.setTimeout(setOffline, LIVE_TIMEOUT);
    frame.src = frame.dataset.src ?? '';
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
      window.clearTimeout(liveTimer);
      if (source) startTiles();
      else startFrame();
    };
    image.onerror = () => {
      window.clearTimeout(timer);
      setOffline();
    };
    image.src = `${probeUrl}?_=${Date.now()}`;
  }

  // While the map is on screen in a visible tab, the tiles in view are fetched again
  // every few minutes, so the picture follows the server.
  let inView = false;
  window.setInterval(() => {
    if (!source || !live || !inView || document.hidden) return;
    stamp = String(Math.floor(Date.now() / REFRESH_EVERY));
    for (const { key, url } of onScreen) {
      const tile = tiles.get(key);
      if (tile && tile.state !== 'loading') load(tile, url);
    }
  }, REFRESH_EVERY);

  const start = () => {
    if (started) return;
    started = true;
    void loadSnapshot();
    probe();
  };

  new ResizeObserver(measure).observe(card);
  measure();
  if ('IntersectionObserver' in window) {
    new IntersectionObserver(
      (entries) => {
        inView = entries.some((entry) => entry.isIntersecting);
        if (inView) start();
      },
      { rootMargin: '300px' },
    ).observe(figure);
  } else {
    inView = true;
    start();
  }
}
