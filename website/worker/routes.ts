// The addresses the website asks for map pictures:
//   /api/map-snapshot.json            everything the pages need to know: the newest
//                                     whole-world picture (when it was taken and its
//                                     address), the journey picture under "journey",
//                                     and the site's copy of the map under "copy"
//   /api/map-snapshot/<time>.jpg      a whole-world picture
//   /api/map-journey/<time>.jpg       a journey picture
//   /api/map-journey/latest.jpg       the newest journey picture
//   /api/map-copy/<level>/<col>_<row>.jpg   one sheet of the copy
// and one for checking the live site:
//   /api/map-view.json                how the map was framed at the last build
import type { MapView } from '../src/config/site.ts';
import { COPY_LEVELS, copyFraming, readIndex, readSheet } from './copy.ts';
import { JOURNEY, latestPicture, readPicture, WORLD, type Shelf, type SnapshotStore } from './snapshots.ts';

const json = (body: unknown, status: number, cache: string) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': cache },
  });
const jpeg = (picture: ArrayBuffer, cache: string) => new Response(picture, { headers: { 'content-type': 'image/jpeg', 'cache-control': cache } });
const missing = () => json({ error: 'snapshot not found' }, 404, 'public, max-age=60');

/** A picture whose address says when it was taken never changes. */
const FOREVER = 'public, max-age=31536000, immutable';
const SHELVES: Shelf[] = [WORLD, JOURNEY];
const addressOf = (shelf: Shelf, takenAt: number) => `/api/map-${shelf.name}/${takenAt}.jpg`;

/** Answers a snapshot request, or returns null when the address is not one of ours. */
export async function handleSnapshotRequest(url: URL, store: SnapshotStore, view?: MapView): Promise<Response | null> {
  if (url.pathname === '/api/map-view.json' && view) return json(view, 200, 'public, max-age=60');

  if (url.pathname === '/api/map-snapshot.json') {
    const [world, journey, copy] = await Promise.all([latestPicture(store, WORLD), latestPicture(store, JOURNEY), view ? readIndex(store, copyFraming(view)) : null]);
    if (!world && !journey) return json({ error: 'no snapshot yet' }, 404, 'public, max-age=60');
    // A new snapshot arrives every 30 minutes, so a minute of caching is plenty.
    return json(
      {
        ...(world && { takenAt: world.takenAt, url: addressOf(WORLD, world.takenAt) }),
        ...(journey && { journey: { takenAt: journey.takenAt, url: addressOf(JOURNEY, journey.takenAt) } }),
        ...(copy && { copy: { width: copy.width, height: copy.height, levels: copy.levels } }),
      },
      200,
      'public, max-age=60',
    );
  }

  const shelf = SHELVES.find((candidate) => url.pathname.startsWith(`/api/map-${candidate.name}/`));
  if (shelf) {
    const name = url.pathname.slice(`/api/map-${shelf.name}/`.length);
    if (name === 'latest.jpg') {
      const latest = await latestPicture(store, shelf);
      const picture = latest && (await readPicture(store, shelf, latest.takenAt));
      // The newest one changes, so this address is only kept for a few minutes.
      return picture ? jpeg(picture, 'public, max-age=300') : missing();
    }
    const time = /^(\d{1,15})\.jpg$/.exec(name);
    const picture = time && (await readPicture(store, shelf, Number(time[1])));
    return picture ? jpeg(picture, FOREVER) : missing();
  }

  const sheet = /^\/api\/map-copy\/(\d)\/(\d{1,3})_(\d{1,3})\.jpg$/.exec(url.pathname);
  if (sheet) {
    const [level, col, row] = sheet.slice(1).map(Number);
    const inside = level >= 1 && level <= COPY_LEVELS && col < 2 ** level && row < 2 ** level;
    const picture = inside ? await readSheet(store, { level, col, row }) : null;
    // The pages add ?t=<time> to the address, so a newer sheet gets a new address.
    return picture ? jpeg(picture, url.searchParams.has('t') ? FOREVER : 'public, max-age=300') : missing();
  }

  if (url.pathname.startsWith('/api/')) return json({ error: 'not found' }, 404, 'no-store');
  return null;
}
