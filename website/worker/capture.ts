// Takes one picture of the live map with Cloudflare's hosted browser (Browser Run):
// it opens the given address (Dynmap's own page, without its controls) at the
// picture's size, waits for the map tiles and returns a JPEG.
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

/** True in the page when Dynmap has no tile left to wait for (arrived or failed). */
const TILES_SETTLED = `(() => {
  const layer = window.dynmap && window.dynmap.maptype;
  if (layer && typeof layer.isLoading === 'function') return !layer.isLoading();
  return document.querySelectorAll('img.leaflet-tile-loaded').length >= document.querySelectorAll('img.leaflet-tile').length;
})()`;
/** The share of the page's tiles that have arrived, 0 to 1. */
const TILES_ARRIVED = `(() => {
  const all = document.querySelectorAll('img.leaflet-tile').length;
  return all ? document.querySelectorAll('img.leaflet-tile-loaded').length / all : 0;
})()`;
/** The zoom level Dynmap's page is at, or null when the page does not say. */
const PAGE_ZOOM = `(() => {
  const map = window.dynmap && window.dynmap.map;
  return map && typeof map.getZoom === 'function' ? map.getZoom() : null;
})()`;

export async function captureMap(binding: BrowserRun, shot: MapShot, options: CaptureOptions = {}): Promise<Uint8Array> {
  const browser = await puppeteer.launch(binding);
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: shot.width, height: shot.height, deviceScaleFactor: 1 });
    const response = await page.goto(shot.url, { waitUntil: 'domcontentloaded', timeout: 20_000 });
    if (!response || !response.ok()) {
      throw new Error(`the map answered ${response ? response.status() : 'nothing'}`);
    }
    // Leaflet (the map library Dynmap uses) marks each tile that has arrived.
    await page.waitForFunction(`document.querySelectorAll('img.leaflet-tile-loaded').length >= ${MIN_TILES}`, { timeout: 10_000 });
    if (options.thorough) {
      // Dynmap quietly stops at its deepest zoom level; the picture would then show
      // more of the world than was asked for.
      const zoom: unknown = await page.evaluate(PAGE_ZOOM);
      if (typeof zoom === 'number' && zoom !== shot.zoom) {
        throw new Error(`the map opened at zoom ${zoom}, not ${shot.zoom}`);
      }
      try {
        await page.waitForFunction(TILES_SETTLED, { timeout: 15_000 });
      } catch {
        // Still waiting after 15 seconds: a few stragglers are fine, holes are not.
        const arrived = Number(await page.evaluate(TILES_ARRIVED));
        if (!(arrived >= 0.9)) throw new Error(`only ${Math.round(arrived * 100)}% of the map tiles arrived`);
      }
    }
    // A moment for the last tiles and their fade-in.
    await new Promise((resolve) => setTimeout(resolve, 800));
    return await page.screenshot({ type: 'jpeg', quality: options.quality ?? 82 });
  } finally {
    await browser.close();
  }
}
