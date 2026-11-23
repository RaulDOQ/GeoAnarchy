// Paints the server's map on a canvas from the map's own picture tiles.
//
// The tiles come from the server's Dynmap site; scripts/detect-map.mjs finds them at
// build time. Every tile is drawn snapped to whole screen pixels and its neighbours
// share the rounded edge, so no line ever shows between two tiles, at any size.
// Used by the live map (live-map.ts) and by the still map picture (map-still.ts).

const MAX_LOADS = 24;
const MAX_TILES_KEPT = 600;

/** What scripts/detect-map.mjs found out about the map's tiles. */
export interface TileSource {
  base: string;
  prefix: string;
  format: string;
  tileSize: number;
  /** The zoom level at which tiles are at full detail (0 is zoomed all the way out). */
  nativeZoom: number;
  /** Where the world lies, in pixels at zoom level 0. */
  world: { x: number; y: number; width: number; height: number };
}

/** What to paint: a canvas of `width` x `height` CSS pixels showing part of the world. */
export interface PaintView {
  width: number;
  height: number;
  /** Where the whole world's top left corner is, in CSS pixels (zero or negative). */
  left: number;
  top: number;
  /** How large the whole world is drawn, in CSS pixels. */
  mapWidth: number;
  mapHeight: number;
  /** Also fetch coarser tiles to stand in for ones that have not arrived. */
  coarser: boolean;
}

export interface TilePainter {
  paint(view: PaintView): void;
  /** Fetches the tiles of the last painting again (they may have changed). */
  refresh(stamp: string): void;
  /** Forgets tiles that failed, so the next painting asks for them again. */
  retryFailed(): void;
  /** How many tiles have arrived and how many have failed so far. */
  readonly ready: number;
  readonly failed: number;
}

interface Tile {
  image: HTMLImageElement;
  state: 'loading' | 'ready' | 'failed';
  /** The painting that last used it (for deciding what to forget). */
  used: number;
}

/** Reads the tile settings an element carries in data-tiles, if any. */
export function readTileSource(element: HTMLElement): TileSource | null {
  try {
    return element.dataset.tiles ? (JSON.parse(element.dataset.tiles) as TileSource) : null;
  } catch {
    return null;
  }
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

/**
 * A painter for `canvas`, or null when the browser cannot draw on it. `onTile` is
 * called for every tile that arrives (true) or fails (false); `onChange` when a new
 * painting would look different, because a tile arrived.
 */
export function createTilePainter(canvas: HTMLCanvasElement, source: TileSource, hooks: { onTile?: (ok: boolean) => void; onChange: () => void }): TilePainter | null {
  const context = canvas.getContext('2d');
  if (!context) return null;

  const tiles = new Map<string, Tile>();
  const waiting: (() => void)[] = [];
  let pass = 0;
  let loading = 0;
  let ready = 0;
  let failed = 0;
  /** Added to tile addresses when they are fetched again, so the browser really asks. */
  let stamp = '';
  /** The tiles the last painting wanted, for refresh(). */
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
          if (tile.state !== 'ready') ready++;
          tile.state = 'ready';
          hooks.onTile?.(true);
          hooks.onChange();
        } else if (tile.state === 'loading') {
          // A tile that fails while being fetched again keeps its earlier picture.
          tile.state = 'failed';
          failed++;
          hooks.onTile?.(false);
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

  function tileAt(zoom: number, col: number, row: number, wanted: boolean): Tile | undefined {
    const key = `${zoom}:${col}:${row}`;
    let tile = tiles.get(key);
    if (!tile && wanted) {
      tile = { image: new Image(), state: 'loading', used: pass };
      tiles.set(key, tile);
      load(tile, source.base + tileName(source, zoom, col, row));
    }
    if (tile) tile.used = pass;
    return tile;
  }

  function paint(view: PaintView): void {
    if (!context || !view.width || !view.height) return;
    pass++;
    const ratio = Math.min(window.devicePixelRatio || 1, 2);
    const pixelWidth = Math.round(view.width * ratio);
    const pixelHeight = Math.round(view.height * ratio);
    if (canvas.width !== pixelWidth || canvas.height !== pixelHeight) {
      canvas.width = pixelWidth;
      canvas.height = pixelHeight;
    }
    context.setTransform(1, 0, 0, 1, 0, 0);
    context.clearRect(0, 0, pixelWidth, pixelHeight);

    const w = source.world;
    // CSS pixels per map pixel (map pixels are counted at zoom level 0).
    const scaleX = view.mapWidth / w.width;
    const scaleY = view.mapHeight / w.height;
    // The level whose tiles are at least as sharp as the screen needs.
    const level = Math.min(source.nativeZoom, Math.max(0, Math.ceil(Math.log2(scaleX * ratio) - 0.25)));
    const span = source.tileSize / 2 ** level;
    // Past the tiles' real size, show crisp blocks rather than a blur.
    context.imageSmoothingEnabled = scaleX * ratio <= 2 ** level * 1.01;

    // The part of the world on the canvas, in map pixels.
    const x0 = Math.max(w.x, w.x - view.left / scaleX);
    const x1 = Math.min(w.x + w.width, w.x + (view.width - view.left) / scaleX);
    const y0 = Math.max(w.y, w.y - view.top / scaleY);
    const y1 = Math.min(w.y + w.height, w.y + (view.height - view.top) / scaleY);
    const edge = 1e-7;
    const firstCol = Math.floor(x0 / span);
    const lastCol = Math.floor((x1 - edge) / span);
    const firstRow = Math.floor(y0 / span);
    const lastRow = Math.floor((y1 - edge) / span);

    // Positions are rounded to whole pixels, and neighbours share the rounded edge, so
    // there is never a gap or an overlap between two tiles.
    const screenX = (mx: number) => Math.round((view.left + (mx - w.x) * scaleX) * ratio);
    const screenY = (my: number) => Math.round((view.top + (my - w.y) * scaleY) * ratio);

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
          const parent = tileAt(level - up, parentCol, parentRow, view.coarser && up <= 2);
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

  return {
    paint,
    refresh(next: string) {
      stamp = next;
      for (const { key, url } of onScreen) {
        const tile = tiles.get(key);
        if (tile && tile.state !== 'loading') load(tile, url);
      }
    },
    retryFailed() {
      for (const [key, tile] of tiles) if (tile.state === 'failed') tiles.delete(key);
      failed = 0;
    },
    get ready() {
      return ready;
    },
    get failed() {
      return failed;
    },
  };
}
