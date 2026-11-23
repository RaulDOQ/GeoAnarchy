// A still picture of part of the server's real map (the journey figure in the
// "Realismo" section). It is not interactive.
//
// The picture in the page is the newest photograph of exactly this part of the map,
// which the Worker takes zoomed in and keeps (worker/), so it is there when the live
// map is down. This script:
//   - shows it once it has loaded. If there is none yet (the first half hour after the
//     site is first deployed), the whole-world picture stands in, enlarged; that one
//     is only a third as sharp;
//   - draws the map's own tiles for this part on top, when the page may use them (see
//     scripts/detect-map.mjs).
import { snapshotInfo } from './map-copy';
import { createTilePainter, readTileSource } from './tile-painter';

export function initMapStill(figure: HTMLElement): void {
  const canvas = figure.querySelector<HTMLCanvasElement>('canvas[data-still-tiles]');
  const photo = figure.querySelector<HTMLImageElement>('img[data-still-photo]');
  const box = canvas?.parentElement;
  // The region, as fractions of the whole world: left, top, right, bottom.
  const [x0, y0, x1, y1] = (figure.dataset.region ?? '').split(',').map(Number);
  if (!canvas || !photo || !box || !(x1 > x0) || !(y1 > y0)) return;

  /** No photograph of this part yet: the whole world, shown so only this part is in the frame. */
  async function showWorldInstead(): Promise<void> {
    const info = figure.dataset.snapshotInfo ? await snapshotInfo(figure.dataset.snapshotInfo) : null;
    if (typeof info?.url !== 'string') return;
    figure.classList.add('cuts-world');
    photo!.addEventListener('load', () => figure.classList.add('has-photo'), { once: true });
    photo!.src = info.url;
  }

  function showPhoto(): void {
    const arrived = () => figure.classList.add('has-photo');
    // The picture may have loaded, or failed, before this script ran.
    if (photo!.complete && photo!.naturalWidth) arrived();
    else if (photo!.complete && photo!.getAttribute('src')) void showWorldInstead();
    else {
      photo!.addEventListener('load', arrived, { once: true });
      photo!.addEventListener('error', () => void showWorldInstead(), { once: true });
    }
  }

  function showTiles(): void {
    const source = readTileSource(figure);
    if (!source) return;
    let queued = false;
    const paint = () => {
      if (queued) return;
      queued = true;
      requestAnimationFrame(() => {
        queued = false;
        const width = box!.clientWidth;
        const height = box!.clientHeight;
        const mapWidth = width / (x1 - x0);
        const mapHeight = height / (y1 - y0);
        painter?.paint({ width, height, left: -x0 * mapWidth, top: -y0 * mapHeight, mapWidth, mapHeight, coarser: false });
      });
    };
    const painter = createTilePainter(canvas!, source, {
      onTile: (ok) => ok && figure.classList.add('has-tiles'),
      onChange: paint,
    });
    if (!painter) return;
    new ResizeObserver(paint).observe(box!);
    paint();
  }

  showPhoto();
  // The tiles are only asked for when the figure is about to be seen.
  if ('IntersectionObserver' in window) {
    const near = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        near.disconnect();
        showTiles();
      },
      { rootMargin: '400px' },
    );
    near.observe(figure);
  } else {
    showTiles();
  }
}
