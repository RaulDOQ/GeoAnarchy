// Takes one picture of the live map with Cloudflare's hosted browser (Browser Run):
// it opens the given address (the same one the website embeds) at the map's size,
// waits for the map tiles and returns a JPEG.
import puppeteer from '@cloudflare/puppeteer';
import type { MapView } from '../src/config/site.ts';

/** Map tiles that must have arrived before the picture counts as a real map. */
const MIN_TILES = 24;

export async function captureMap(binding: BrowserRun, url: string, view: MapView): Promise<Uint8Array> {
  const browser = await puppeteer.launch(binding);
  try {
    const page = await browser.newPage();
    await page.setViewport({ width: view.width, height: view.height, deviceScaleFactor: 1 });
    const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 20_000 });
    if (!response || !response.ok()) {
      throw new Error(`the map answered ${response ? response.status() : 'nothing'}`);
    }
    // Leaflet (the map library Dynmap uses) marks each tile that has arrived.
    await page.waitForFunction(`document.querySelectorAll('img.leaflet-tile-loaded').length >= ${MIN_TILES}`, { timeout: 10_000 });
    // A moment for the last tiles and their fade-in.
    await new Promise((resolve) => setTimeout(resolve, 800));
    return await page.screenshot({ type: 'jpeg', quality: 82 });
  } finally {
    await browser.close();
  }
}
