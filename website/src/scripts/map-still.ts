// A still picture of part of the server's real map (the journey figure in the
// "Realismo" section). It is not interactive and is drawn once.
//
// The figure already shows the block map. On top of it this script puts, as they
// become available, the same region cut from the newest map snapshot (worker/), and
// then the map's own tiles for that region, which are sharp. If the live map is down
// or the snapshot service has nothing yet, what is underneath simply stays.
import { createTilePainter, readTileSource } from './tile-painter';

export function initMapStill(figure: HTMLElement): void {
  const canvas = figure.querySelector<HTMLCanvasElement>('canvas[data-still-tiles]');
  const snapshot = figure.querySelector<HTMLImageElement>('img[data-still-snapshot]');
  const box = canvas?.parentElement;
  // The region, as fractions of the whole world: left, top, right, bottom.
  const [x0, y0, x1, y1] = (figure.dataset.region ?? '').split(',').map(Number);
  if (!canvas || !snapshot || !box || !(x1 > x0) || !(y1 > y0)) return;

  async function showSnapshot(): Promise<void> {
    const infoUrl = figure.dataset.snapshotInfo;
    if (!infoUrl) return;
    try {
      const response = await fetch(infoUrl);
      if (!response.ok) return;
      const info = (await response.json()) as { url?: unknown };
      if (typeof info.url !== 'string') return;
      snapshot!.addEventListener('load', () => figure.classList.add('has-snapshot'), { once: true });
      snapshot!.src = info.url;
    } catch {
      // No snapshot service (for example in local development).
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

  const start = () => {
    void showSnapshot();
    showTiles();
  };
  if ('IntersectionObserver' in window) {
    const near = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        near.disconnect();
        start();
      },
      { rootMargin: '400px' },
    );
    near.observe(figure);
  } else {
    start();
  }
}
