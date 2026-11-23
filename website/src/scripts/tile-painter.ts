// Paints a map on a canvas from picture tiles.
//
// The tiles come from one of two places (a TileSource says which):
//   - the server's Dynmap site: the map's own tiles, live. scripts/detect-map.mjs
//     finds them at build time (dynmapTiles below);
//   - the site's own copy of the map: larger pictures the Worker has photographed and
//     stored, for when the live map does not load (map-copy.ts).
// Every tile is drawn snapped to whole screen pixels and its neighbours share the
// rounded edge, so no line ever shows between two tiles, at any size. Where a tile is
// missing or has not arrived, the same place is shown from a coarser tile.
// Used by the live map (live-map.ts) and by the still map picture (map-still.ts).

const MAX_LOADS = 24;

/** Where the tiles are and how they are laid out. */
export interface TileSource {
  /** The size of one tile, in its own pixels. */
  tileWidth: number;
  tileHeight: number;
  /** The deepest level there are tiles for (level 0 is zoomed all the way out). */
  maxLevel: number;
  /** The deepest level that has every one of its tiles, when that is less than maxLevel. */
  completeLevel?: number;
  /** Where the world lies, in pixels at level 0. */
  world: { x: number; y: number; width: number; height: number };
  /** The address of one tile, or null when there is none. */
  address(level: number, col: number, row: number): string | null;
  /** Enlarged past their real size, tiles are smoothed (photographs) rather than shown as crisp blocks. */
  smooth?: boolean;
  /** How many tile pictures stay in memory (large tiles: fewer). */
  keep?: number;
}

/** What scripts/detect-map.mjs found out about the live map's tiles (MapTiles in config/site.ts). */
interface DynmapSettings {
  base: string;
  prefix: string;
  format: string;
  tileSize: number;
  nativeZoom: number;
  world: { x: number; y: number; width: number; height: number };
}

/** The live map's own tiles, named exactly as Dynmap's page names them (see detect-map.mjs). */
export function dynmapTiles(settings: DynmapSettings): TileSource {
  return {
    tileWidth: settings.tileSize,
    tileHeight: settings.tileSize,
    maxLevel: settings.nativeZoom,
    world: settings.world,
    address(level, col, row) {
      const out = Math.max(0, settings.nativeZoom - level);
      const scale = 2 ** out;
      const x = scale * col;
      const y = -(scale * row);
      const prefix = out === 0 ? '' : `${'z'.repeat(out)}_`;
      return `${settings.base}${settings.prefix}/${x >> 5}_${y >> 5}/${prefix}${x}_${y}.${settings.format}`;
    },
  };
}

/** The live map's tiles, from the settings an element carries in data-tiles, if any. */
export function readTileSource(element: HTMLElement): TileSource | null {
  try {
    return element.dataset.tiles ? dynmapTiles(JSON.parse(element.dataset.tiles) as DynmapSettings) : null;
  } catch {
    return null;
  }
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
  /** Also fetch coarser tiles to stand in for ones that have not arrived yet. */
  coarser: boolean;
  /** Only fetch the tiles this view needs; leave the canvas as it is. */
  fetchOnly?: boolean;
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

/**
 * A painter for `canvas`, or null when the browser cannot draw on it. `onTile` is
 * called for every tile that arrives (true) or fails (false); `onChange` when a new
 * painting would look different, because a tile arrived.
 */
export function createTilePainter(canvas: HTMLCanvasElement, source: TileSource, hooks: { onTile?: (ok: boolean) => void; onChange: () => void }): TilePainter | null {
  const context = canvas.getContext('2d');
  if (!context) return null;

  const keep = source.keep ?? 600;
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
          hooks.onChange();
        }
        waiting.shift()?.();
      };
      image.onload = () => done(true);
      image.onerror = () => done(false);
      image.src = stamp ? `${url}${url.includes('?') ? '&' : '?'}t=${stamp}` : url;
    };
    if (loading < MAX_LOADS) start();
    else waiting.push(start);
  }

  /** The tile at that place if there is one; `fetch` asks for it when it is not here yet. */
  function tileAt(level: number, col: number, row: number, fetch: boolean): Tile | undefined {
    const key = `${level}:${col}:${row}`;
    let tile = tiles.get(key);
    if (!tile && fetch) {
      const url = source.address(level, col, row);
      if (!url) return undefined;
      tile = { image: new Image(), state: 'loading', used: pass };
      tiles.set(key, tile);
      load(tile, url);
    }
    if (tile) tile.used = pass;
    return tile;
  }

  function paint(view: PaintView): void {
    if (!context || !view.width || !view.height) return;
    pass++;
    const draw = !view.fetchOnly;
    const ratio = Math.min(window.devicePixelRatio || 1, 2);
    const pixelWidth = Math.round(view.width * ratio);
    const pixelHeight = Math.round(view.height * ratio);
    if (draw) {
      if (canvas.width !== pixelWidth || canvas.height !== pixelHeight) {
        canvas.width = pixelWidth;
        canvas.height = pixelHeight;
      }
      context.setTransform(1, 0, 0, 1, 0, 0);
      context.clearRect(0, 0, pixelWidth, pixelHeight);
    }

    const w = source.world;
    // CSS pixels per map pixel (map pixels are counted at level 0).
    const scaleX = view.mapWidth / w.width;
    const scaleY = view.mapHeight / w.height;
    // The level whose tiles are at least as sharp as the screen needs.
    const level = Math.min(source.maxLevel, Math.max(0, Math.ceil(Math.log2(scaleX * ratio) - 0.25)));
    const spanX = source.tileWidth / 2 ** level;
    const spanY = source.tileHeight / 2 ** level;
    // Past the tiles' real size, map tiles show as crisp blocks rather than a blur.
    context.imageSmoothingEnabled = source.smooth || scaleX * ratio <= 2 ** level * 1.01;
    context.imageSmoothingQuality = 'high';

    // The part of the world on the canvas, in map pixels.
    const x0 = Math.max(w.x, w.x - view.left / scaleX);
    const x1 = Math.min(w.x + w.width, w.x + (view.width - view.left) / scaleX);
    const y0 = Math.max(w.y, w.y - view.top / scaleY);
    const y1 = Math.min(w.y + w.height, w.y + (view.height - view.top) / scaleY);
    const edge = 1e-7;
    const firstCol = Math.floor(x0 / spanX);
    const lastCol = Math.floor((x1 - edge) / spanX);
    const firstRow = Math.floor(y0 / spanY);
    const lastRow = Math.floor((y1 - edge) / spanY);

    // Positions are rounded to whole pixels, and neighbours share the rounded edge, so
    // there is never a gap or an overlap between two tiles.
    const screenX = (mx: number) => Math.round((view.left + (mx - w.x) * scaleX) * ratio);
    const screenY = (my: number) => Math.round((view.top + (my - w.y) * scaleY) * ratio);

    onScreen = [];
    if (draw) {
      context.save();
      context.beginPath();
      context.rect(screenX(w.x), screenY(w.y), screenX(w.x + w.width) - screenX(w.x), screenY(w.y + w.height) - screenY(w.y));
      context.clip();
    }
    for (let row = firstRow; row <= lastRow; row++) {
      for (let col = firstCol; col <= lastCol; col++) {
        const dx = screenX(col * spanX);
        const dy = screenY(row * spanY);
        const dw = screenX((col + 1) * spanX) - dx;
        const dh = screenY((row + 1) * spanY) - dy;
        const tile = tileAt(level, col, row, true);
        const url = tile && source.address(level, col, row);
        if (url) onScreen.push({ key: `${level}:${col}:${row}`, url });
        if (tile?.state === 'ready') {
          if (draw) context.drawImage(tile.image, dx, dy, dw, dh);
          continue;
        }
        // Not here: show the same place from a coarser tile. When this tile is only
        // on its way, coarser ones are fetched if the view asks for that; when there
        // is none (or it failed), the nearest coarser one that exists is fetched.
        const gone = !tile || tile.state === 'failed';
        let asked = false;
        for (let up = 1; up <= level; up++) {
          const parts = 2 ** up;
          const parentCol = Math.floor(col / parts);
          const parentRow = Math.floor(row / parts);
          const fetch = !asked && (gone || (view.coarser && up <= 2));
          const parent = tileAt(level - up, parentCol, parentRow, fetch);
          if (parent && parent.state !== 'failed') asked = true;
          if (parent?.state !== 'ready') continue;
          if (draw) {
            const sw = parent.image.naturalWidth / parts;
            const sh = parent.image.naturalHeight / parts;
            context.drawImage(parent.image, (col - parentCol * parts) * sw, (row - parentRow * parts) * sh, sw, sh, dx, dy, dw, dh);
          }
          break;
        }
      }
    }
    if (draw) context.restore();

    // Forget tiles that have not been used for a while.
    if (tiles.size > keep) {
      const old = [...tiles.entries()].filter(([, tile]) => tile.state !== 'loading').sort((a, b) => a[1].used - b[1].used);
      for (const [key] of old.slice(0, tiles.size - keep)) tiles.delete(key);
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
