// Draws the blocky world map used on the site: one pixel per 2.5 degrees of the real
// Earth (144 x 72 pixels), written to src/assets/earth.png. The pages show it enlarged
// with sharp pixel edges, so every pixel reads as a block.
//
// Land comes from Natural Earth (public domain) through the world-atlas package.
// Working out which pixels are land takes a couple of minutes, so the answer is kept
// in scripts/earth-mask.txt ("#" land, "." sea) and reused. The sea is left
// transparent (the page colours it); land is green with a little variation; the
// Antarctic and Greenland ice is white.
//
//   npm run make:earth             repaint the PNG from the saved mask (instant)
//   npm run make:earth -- --remask work the land mask out again first
//
// Both files are committed, so a normal build does not need this script.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { deflateSync } from 'node:zlib';
import { geoBounds, geoContains } from 'd3-geo';
import { feature } from 'topojson-client';

const COLS = 144;
const ROWS = 72;
const STEP = 360 / COLS;
/** Samples per side inside each pixel, and the share of them that must be land. */
const SAMPLES = 4;
const LAND_SHARE = 0.34;

const GREENS = [
  [70, 208, 138],
  [56, 194, 124],
  [92, 220, 154],
];
const ICE = [
  [255, 255, 255],
  [233, 242, 252],
];

const maskFile = new URL('./earth-mask.txt', import.meta.url);

// The data is stitched across the 180th meridian and the poles (Russia and Antarctica
// are whole shapes), so the test has to work on the sphere: d3-geo's geoContains does.
// Each polygon's bounds come first, to skip the ones nowhere near the point.
function computeMask() {
  const require = createRequire(import.meta.url);
  const topology = JSON.parse(readFileSync(require.resolve('world-atlas/land-50m.json'), 'utf8'));
  const land = feature(topology, topology.objects.land);
  const shapes = [];
  const geometries = land.type === 'FeatureCollection' ? land.features.map((f) => f.geometry) : [land.geometry];
  for (const g of geometries) {
    const list = g.type === 'Polygon' ? [g.coordinates] : g.coordinates;
    for (const coordinates of list) {
      const polygon = { type: 'Polygon', coordinates };
      const [[west, south], [east, north]] = geoBounds(polygon);
      shapes.push({ polygon, west, south, east, north });
    }
  }

  function isLand(lon, lat) {
    for (const s of shapes) {
      if (lat < s.south || lat > s.north) continue;
      // Bounds that cross the 180th meridian have west > east.
      const inLon = s.west <= s.east ? lon >= s.west && lon <= s.east : lon >= s.west || lon <= s.east;
      if (!inLon) continue;
      if (geoContains(s.polygon, [lon, lat])) return true;
    }
    return false;
  }

  const rows = [];
  for (let row = 0; row < ROWS; row++) {
    let line = '';
    for (let col = 0; col < COLS; col++) {
      let hits = 0;
      for (let sy = 0; sy < SAMPLES; sy++) {
        for (let sx = 0; sx < SAMPLES; sx++) {
          const lon = -180 + (col + (sx + 0.5) / SAMPLES) * STEP;
          const lat = 90 - (row + (sy + 0.5) / SAMPLES) * STEP;
          if (isLand(lon, lat)) hits++;
        }
      }
      line += hits / (SAMPLES * SAMPLES) >= LAND_SHARE ? '#' : '.';
    }
    rows.push(line);
  }
  return rows;
}

let mask;
if (existsSync(maskFile) && !process.argv.includes('--remask')) {
  mask = readFileSync(maskFile, 'utf8').trim().split('\n');
} else {
  mask = computeMask();
  writeFileSync(maskFile, mask.join('\n') + '\n');
}
if (mask.length !== ROWS || mask.some((line) => line.length !== COLS)) {
  throw new Error('earth-mask.txt does not match the map size; run with --remask');
}

/** A steady pseudo-random number 0..1 for a pixel, so the map is the same every run. */
function noise(x, y) {
  let h = (x * 374761393 + y * 668265263) >>> 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177) >>> 0;
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}

const isIce = (lon, lat) => lat < -60 || (lat > 60 && lon > -74 && lon < -11 && !(lat < 66.5 && lon > -30));

const pixels = Buffer.alloc(COLS * ROWS * 4);
let landCount = 0;
for (let row = 0; row < ROWS; row++) {
  for (let col = 0; col < COLS; col++) {
    if (mask[row][col] !== '#') continue;
    landCount++;
    const lon = -180 + (col + 0.5) * STEP;
    const lat = 90 - (row + 0.5) * STEP;
    const r = noise(col, row);
    const [red, green, blue] = isIce(lon, lat) ? ICE[r < 0.5 ? 0 : 1] : GREENS[r < 0.6 ? 0 : r < 0.82 ? 1 : 2];
    const i = (row * COLS + col) * 4;
    pixels[i] = red;
    pixels[i + 1] = green;
    pixels[i + 2] = blue;
    pixels[i + 3] = 255;
  }
}

// A minimal PNG writer (8-bit RGBA, no filtering).
const crcTable = new Uint32Array(256).map((_, n) => {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  return c >>> 0;
});
function crc32(buffer) {
  let c = 0xffffffff;
  for (const byte of buffer) c = crcTable[(c ^ byte) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const out = Buffer.alloc(8 + data.length + 4);
  out.writeUInt32BE(data.length, 0);
  body.copy(out, 4);
  out.writeUInt32BE(crc32(body), 8 + data.length);
  return out;
}
const header = Buffer.alloc(13);
header.writeUInt32BE(COLS, 0);
header.writeUInt32BE(ROWS, 4);
header[8] = 8; // bit depth
header[9] = 6; // colour type: RGBA
const raw = Buffer.alloc(ROWS * (COLS * 4 + 1));
for (let row = 0; row < ROWS; row++) {
  pixels.copy(raw, row * (COLS * 4 + 1) + 1, row * COLS * 4, (row + 1) * COLS * 4);
}
const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  chunk('IHDR', header),
  chunk('IDAT', deflateSync(raw, { level: 9 })),
  chunk('IEND', Buffer.alloc(0)),
]);

const target = new URL('../src/assets/earth.png', import.meta.url);
writeFileSync(target, png);
console.log(`earth.png: ${COLS} x ${ROWS} pixels, ${landCount} land, ${png.length} bytes`);
