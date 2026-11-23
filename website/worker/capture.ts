// Photographs the live map with Cloudflare's hosted browser (Browser Run). It opens
// Dynmap's own page, without its controls, at the picture's size, waits for the map
// tiles and returns a JPEG. One browser serves a whole run (index.ts): starting it is
// the slow part, and its running time is what Cloudflare's free plan limits.
import puppeteer from '@cloudflare/puppeteer';
import type { MapShot } from '../src/config/site.ts';

/** Map tiles that must have arrived before the picture counts as a real map. */
const MIN_TILES = 24;

export interface CaptureOptions {
  /** JPEG quality, 0 to 100. */
  quality?: number;
  /**
   * For a picture that is kept for a long time: make sure the page is at the zoom
   * level asked for, and wait until no tile is still on its way.
   */
  thorough?: boolean;
}

export interface Camera {
  shoot(shot: MapShot, options?: CaptureOptions): Promise<Uint8Array>;
}

/** Dynmap's page opened at another zoom level than asked: it stops at its deepest one. */
export class WrongZoom extends Error {
  constructor(
    readonly asked: number,
    readonly got: number,
  ) {
    super(`the map opened at zoom ${got}, not ${asked}`);
  }
}

const TILES_ARRIVED_COUNT = `document.querySelectorAll('img.leaflet-tile-loaded').length`;
/** True in the page when Dynmap has no tile left to wait for (arrived or failed). */
const TILES_SETTLED = `(() => {
  const layer = window.dynmap && window.dynmap.maptype;
  if (layer && typeof layer.isLoading === 'function') return !layer.isLoading();
  return ${TILES_ARRIVED_COUNT} >= document.querySelectorAll('img.leaflet-tile').length;
})()`;
/** The share of the page's tiles that have arrived, 0 to 1. */
const TILES_ARRIVED = `(() => {
  const all = document.querySelectorAll('img.leaflet-tile').length;
  return all ? ${TILES_ARRIVED_COUNT} / all : 0;
})()`;
/** The zoom level Dynmap's page is at, or null when the page does not say. */
const PAGE_ZOOM = `(() => {
  const map = window.dynmap && window.dynmap.map;
  return map && typeof map.getZoom === 'function' ? map.getZoom() : null;
})()`;
/**
 * Moves the map in a page that is already open, the way Dynmap itself places the view
 * when it starts (map.js). Much quicker than loading the page again. Answers false
 * when this page cannot do it, or cannot say when its tiles have arrived.
 */
const moveTo = (shot: MapShot) => `(() => {
  const d = window.dynmap;
  const layer = d && d.maptype;
  if (!d || !d.map || !layer || typeof layer.isLoading !== 'function' || typeof d.getProjection !== 'function') return false;
  d.map.setView(d.getProjection().fromLocationToLatLng({ x: ${shot.x}, y: 64, z: ${shot.z} }), ${shot.zoom}, { animate: false });
  return true;
})()`;

const pause = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/** Starts the hosted browser, lends `work` a camera and closes the browser afterwards. */
export async function withCamera<T>(binding: BrowserRun, work: (camera: Camera) => Promise<T>): Promise<T> {
  const browser = await puppeteer.launch(binding);
  try {
    let page: Awaited<ReturnType<typeof browser.newPage>> | null = null;
    let size = '';

    const shoot = async (shot: MapShot, options: CaptureOptions = {}): Promise<Uint8Array> => {
      const wanted = `${shot.width}x${shot.height}`;
      const open = page !== null;
      const tab = (page ??= await browser.newPage());
      // A page that already shows the map at this size only has to move.
      let moved = false;
      if (open && size === wanted && options.thorough) {
        moved = (await tab.evaluate(moveTo(shot)).catch(() => false)) === true;
      }
      if (!moved) {
        if (size !== wanted) {
          await tab.setViewport({ width: shot.width, height: shot.height, deviceScaleFactor: 1 });
          size = wanted;
        }
        const response = await tab.goto(shot.url, { waitUntil: 'domcontentloaded', timeout: 20_000 });
        if (!response || !response.ok()) {
          throw new Error(`the map answered ${response ? response.status() : 'nothing'}`);
        }
        // Leaflet (the map library Dynmap uses) marks each tile that has arrived.
        await tab.waitForFunction(`${TILES_ARRIVED_COUNT} >= ${MIN_TILES}`, { timeout: 10_000 });
      }
      if (options.thorough) {
        // Past its deepest zoom level the picture would show more of the world than
        // was asked for.
        const zoom: unknown = await tab.evaluate(PAGE_ZOOM);
        if (typeof zoom === 'number' && zoom !== shot.zoom) throw new WrongZoom(shot.zoom, zoom);
        try {
          await tab.waitForFunction(TILES_SETTLED, { timeout: 15_000 });
        } catch {
          // Still waiting after 15 seconds: a few stragglers are fine, holes are not.
          const arrived = Number(await tab.evaluate(TILES_ARRIVED));
          if (!(arrived >= 0.9)) throw new Error(`only ${Math.round(arrived * 100)}% of the map tiles arrived`);
        }
      }
      // A moment for the last tiles and their fade-in.
      await pause(moved ? 450 : 800);
      return await tab.screenshot({ type: 'jpeg', quality: options.quality ?? 82 });
    };

    return await work({ shoot });
  } finally {
    await browser.close();
  }
}
