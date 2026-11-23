// Reads the live map's settings before every build (and before `npm run dev`) and
// writes what the website needs to src/generated/map-view.json:
//
//   tiles   how to fetch the map's own picture tiles, so the page can draw the map
//           itself (src/scripts/live-map.ts): sharp at every size, with deep zoom.
//           The arithmetic is the same as in Dynmap's own page (web/js/hdmap.js and
//           dynmaputils.js). A few tiles are fetched to make sure the addresses are
//           right; only then is `verified` true and the page uses them.
//   frame   how to show Dynmap's page so the whole world exactly fills a frame (one
//           Dynmap zoom level, a width and a height). The map snapshots are taken with
//           this, and the page falls back to it when the tiles are not verified.
//
// If the map cannot be reached (it is down, or the build machine has no access to it),
// the values in src/config/map-view.default.json are used, so a build never fails
// because of the map.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

// MAP_URL lets a test point this at a stand-in map.
const MAP_URL = process.env.MAP_URL ?? 'https://mapa.geoanarchy.net/';
const WORLD = 'earth';
const LAYER = 'flat';
/** The playable rectangle, in blocks (config/site.ts has the same numbers). */
const BOUNDS = { minX: -18432, maxX: 18431, minZ: -9216, maxZ: 9215 };
/** The height used for the centre of the view; a top-down map ignores it. */
const Y = 64;
const TARGET_WIDTH = 1100;

const defaults = JSON.parse(readFileSync(new URL('../src/config/map-view.default.json', import.meta.url), 'utf8'));
const outDir = new URL('../src/generated/', import.meta.url);

/**
 * The file name of one tile, exactly as Dynmap's page builds it (hdmap.js getTileName
 * with dynmaputils.js getTileInfo). `zoom` is the page's zoom level (0 = zoomed all
 * the way out) and (col, row) the tile's place in the grid at that zoom.
 */
export function tileName(tiles, zoom, col, row) {
  const level = Math.max(0, tiles.nativeZoom - zoom);
  const scale = 2 ** level;
  const x = scale * col;
  const y = -(scale * row);
  const prefix = level === 0 ? '' : `${'z'.repeat(level)}_`;
  return `${tiles.prefix}/${x >> 5}_${y >> 5}/${prefix}${x}_${y}.${tiles.format}`;
}

/** Everything the website needs from a Dynmap configuration, or a thrown Error. */
export function viewFromConfiguration(configuration) {
  const world = configuration?.worlds?.find((w) => w.name === WORLD);
  if (!world) throw new Error(`world "${WORLD}" is not in the map's settings`);
  const map = world.maps?.find((m) => m.name === LAYER);
  if (!map) throw new Error(`layer "${LAYER}" is not in world "${WORLD}"`);
  const m = map.worldtomap;
  if (!Array.isArray(m) || m.length < 6) throw new Error('the layer has no worldtomap numbers');

  // Dynmap's HDProjection.fromLocationToLatLng, as pixels at zoom level 0 with y
  // pointing down (Leaflet's simple projection: x = lng, y = -lat).
  const nativeZoom = map.mapzoomout ?? 0;
  const tileSize = 128 << (map.tilescale ?? 0);
  const zoomOut = 2 ** nativeZoom;
  const px = (x, z) => (m[0] * x + m[1] * Y + m[2] * z) / zoomOut;
  const py = (x, z) => (tileSize - (m[3] * x + m[4] * Y + m[5] * z)) / zoomOut;
  const corners = [
    [BOUNDS.minX, BOUNDS.minZ],
    [BOUNDS.maxX + 1, BOUNDS.minZ],
    [BOUNDS.minX, BOUNDS.maxZ + 1],
    [BOUNDS.maxX + 1, BOUNDS.maxZ + 1],
  ];
  const xs = corners.map(([x, z]) => px(x, z));
  const ys = corners.map(([x, z]) => py(x, z));
  const rect = { x: Math.min(...xs), y: Math.min(...ys), width: Math.max(...xs) - Math.min(...xs), height: Math.max(...ys) - Math.min(...ys) };
  if (!(rect.width > 0 && rect.height > 0)) throw new Error('the world has no size on this layer');

  const maxZoom = nativeZoom + (map.mapzoomin ?? 0);
  let zoom = Math.max(0, Math.ceil(Math.log2(TARGET_WIDTH / rect.width) - 1e-9));
  zoom = Math.min(zoom, maxZoom);
  const width = Math.round(rect.width * 2 ** zoom);
  const height = Math.round(rect.height * 2 ** zoom);
  if (width < 400 || width > 4096 || height < 200 || height > 4096) {
    throw new Error(`the frame would be ${width} x ${height} pixels, which is not usable`);
  }
  return {
    source: 'detected',
    zoom,
    width,
    height,
    x: Math.round((BOUNDS.minX + BOUNDS.maxX + 1) / 2),
    z: Math.round((BOUNDS.minZ + BOUNDS.maxZ + 1) / 2),
    tiles: {
      base: new URL(`tiles/${WORLD}/`, MAP_URL).href,
      prefix: map.prefix ?? LAYER,
      format: map['image-format'] ?? 'png',
      tileSize,
      nativeZoom,
      // Where the world lies, in pixels at zoom level 0.
      world: rect,
      // A layer that changes between day and night has two sets of tiles; the page
      // does not follow the server's clock, so it does not use the tiles then.
      dayNight: Boolean(map.nightandday),
      verified: false,
    },
  };
}

/** The tiles under five points of the world, at the zoom level the page starts at. */
export function sampleTiles(view) {
  const t = view.tiles;
  const zoom = Math.min(t.nativeZoom, Math.max(0, Math.ceil(Math.log2(TARGET_WIDTH / t.world.width))));
  const points = [[0.5, 0.5], [0.2, 0.3], [0.8, 0.3], [0.2, 0.7], [0.8, 0.7]];
  return points.map(([fx, fy]) => {
    const col = Math.floor(((t.world.x + fx * t.world.width) * 2 ** zoom) / t.tileSize);
    const row = Math.floor(((t.world.y + fy * t.world.height) * 2 ** zoom) / t.tileSize);
    return t.base + tileName(t, zoom, col, row);
  });
}

async function detect() {
  const response = await fetch(new URL('up/configuration', MAP_URL), { signal: AbortSignal.timeout(8000) });
  if (!response.ok) throw new Error(`the map answered ${response.status}`);
  const view = viewFromConfiguration(await response.json());

  if (view.tiles.dayNight) {
    console.log('[map] the layer has day and night tiles; the page will show Dynmap in a frame instead');
    return view;
  }
  const answers = await Promise.all(
    sampleTiles(view).map(async (url) => {
      try {
        const tile = await fetch(url, { signal: AbortSignal.timeout(8000) });
        return { url, ok: tile.ok && (tile.headers.get('content-type') ?? '').startsWith('image/'), status: tile.status };
      } catch (error) {
        return { url, ok: false, status: error.message };
      }
    }),
  );
  const failed = answers.filter((a) => !a.ok);
  view.tiles.verified = failed.length === 0;
  if (view.tiles.verified) console.log(`[map] tiles verified (${answers.length} of ${answers.length} answered), for example ${answers[0].url}`);
  else console.log(`[map] tiles NOT verified; the page will show Dynmap in a frame instead. Failed: ${failed.map((a) => `${a.url} (${a.status})`).join(', ')}`);
  return view;
}

// Only when run directly (so the function above can be imported without side effects).
//   node scripts/detect-map.mjs            ask the live map, fall back to the defaults
//   node scripts/detect-map.mjs --offline  after `npm install`: write the defaults if
//                                          there is no file yet, so checks can run
if (import.meta.url === `file://${process.argv[1]}`) {
  const target = new URL('map-view.json', outDir);
  const offline = process.argv.includes('--offline');
  if (!(offline && existsSync(target))) {
    let view = defaults;
    if (!offline) {
      try {
        view = await detect();
        console.log(`[map] frame from the live map: ${view.width} x ${view.height} px at zoom ${view.zoom}`);
      } catch (error) {
        console.log(`[map] could not read the live map (${error.message}); using the default frame ${defaults.width} x ${defaults.height} px at zoom ${defaults.zoom}`);
      }
    }
    mkdirSync(outDir, { recursive: true });
    writeFileSync(target, JSON.stringify(view, null, 2) + '\n');
  }
}
