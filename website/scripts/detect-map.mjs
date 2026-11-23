// Works out how to frame the live map so the whole world exactly fills the map card,
// and writes the answer to src/generated/map-view.json. It runs before every build
// and before `npm run dev`.
//
// The live map is the server's Dynmap site. At Dynmap zoom level 0 the world is some
// number of pixels wide, and each level doubles it. This script asks the map for its
// settings (up/configuration), does the same arithmetic Dynmap's own page does, and
// picks the zoom level where the world is about TARGET_WIDTH pixels wide. The website
// then shows the map in a frame of exactly that size, scaled to fit the card.
//
// If the map cannot be reached (it is down, or the build machine has no access to it),
// the values in src/config/map-view.default.json are used instead, so a build never
// fails because of the map.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';

const MAP_URL = 'https://mapa.geoanarchy.net/';
const WORLD = 'earth';
const LAYER = 'flat';
/** The playable rectangle, in blocks (config/site.ts has the same numbers). */
const BOUNDS = { minX: -18432, maxX: 18431, minZ: -9216, maxZ: 9215 };
const TARGET_WIDTH = 1100;

const defaults = JSON.parse(readFileSync(new URL('../src/config/map-view.default.json', import.meta.url), 'utf8'));
const outDir = new URL('../src/generated/', import.meta.url);

/** The frame for a Dynmap configuration, or a thrown Error saying what was wrong. */
export function viewFromConfiguration(configuration) {
  const world = configuration?.worlds?.find((w) => w.name === WORLD);
  if (!world) throw new Error(`world "${WORLD}" is not in the map's settings`);
  const map = world.maps?.find((m) => m.name === LAYER);
  if (!map) throw new Error(`layer "${LAYER}" is not in world "${WORLD}"`);
  const m = map.worldtomap;
  if (!Array.isArray(m) || m.length < 6) throw new Error('the layer has no worldtomap numbers');

  // Dynmap's HDProjection.fromLocationToLatLng, as horizontal and vertical pixels at
  // zoom level 0. The height (y) is the same for every point, so it cancels out.
  const zoomOut = 2 ** (map.mapzoomout ?? 0);
  const px = (x, z) => (m[0] * x + m[2] * z) / zoomOut;
  const py = (x, z) => (m[3] * x + m[5] * z) / zoomOut;
  const corners = [
    [BOUNDS.minX, BOUNDS.minZ],
    [BOUNDS.maxX + 1, BOUNDS.minZ],
    [BOUNDS.minX, BOUNDS.maxZ + 1],
    [BOUNDS.maxX + 1, BOUNDS.maxZ + 1],
  ];
  const xs = corners.map(([x, z]) => px(x, z));
  const ys = corners.map(([x, z]) => py(x, z));
  const width0 = Math.max(...xs) - Math.min(...xs);
  const height0 = Math.max(...ys) - Math.min(...ys);
  if (!(width0 > 0 && height0 > 0)) throw new Error('the world has no size on this layer');

  const maxZoom = (map.mapzoomout ?? 0) + (map.mapzoomin ?? 0);
  let zoom = Math.max(0, Math.ceil(Math.log2(TARGET_WIDTH / width0) - 1e-9));
  zoom = Math.min(zoom, maxZoom);
  const width = Math.round(width0 * 2 ** zoom);
  const height = Math.round(height0 * 2 ** zoom);
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
  };
}

async function detect() {
  const response = await fetch(new URL('up/configuration', MAP_URL), { signal: AbortSignal.timeout(8000) });
  if (!response.ok) throw new Error(`the map answered ${response.status}`);
  return viewFromConfiguration(await response.json());
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
