// A still picture of part of the server's real map (the journey figure in the
// "Realismo" section). It is not interactive and is drawn once.
//
// The figure already shows the block map. On top of it this script puts, as they
// become available:
//   - a photograph of exactly this part of the map, which the Worker takes zoomed in
//     (worker/). Until the first one exists, the whole-world picture stands in,
//     enlarged; that one is only a third as sharp;
//   - the map's own tiles for this part, when the page may use them (see
//     scripts/detect-map.mjs).
// If the live map is down or the Worker has nothing yet, what is underneath stays.
import { createTilePainter, readTileSource } from './tile-painter';

export function initMapStill(figure: HTMLElement): void {
  const canvas = figure.querySelector<HTMLCanvasElement>('canvas[data-still-tiles]');
  const photo = figure.querySelector<HTMLImageElement>('img[data-still-photo]');
  const box = canvas?.parentElement;
  // The region, as fractions of the whole world: left, top, right, bottom.
  const [x0, y0, x1, y1] = (figure.dataset.region ?? '').split(',').map(Number);
  if (!canvas || !photo || !box || !(x1 > x0) || !(y1 > y0)) return;

  async function showPhoto(): Promise<void> {
    const infoUrl = figure.dataset.snapshotInfo;
    if (!infoUrl) return;
    try {
      const response = await fetch(infoUrl);
      if (!response.ok) return;
      const info = (await response.json()) as { url?: unknown; journey?: { url?: unknown } };
      const region = typeof info.journey?.url === 'string' ? info.journey.url : null;
      const world = typeof info.url === 'string' ? info.url : null;
      // `cuts-world`: the picture is the whole world, shown so only the region is in the frame.
      const show = (address: string, wholeWorld: boolean) => {
        figure.classList.toggle('cuts-world', wholeWorld);
        photo!.src = address;
      };
      photo!.addEventListener('load', () => figure.classList.add('has-photo'), { once: true });
      if (region) {
        if (world) photo!.addEventListener('error', () => show(world, true), { once: true });
        show(region, false);
      } else if (world) {
        show(world, true);
      }
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
    void showPhoto();
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
