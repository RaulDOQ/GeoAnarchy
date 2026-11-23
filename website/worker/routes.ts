// The addresses the website asks for map pictures:
//   /api/map-snapshot.json        the newest whole-world picture (when it was taken and
//                                 its address), and the same for the journey picture
//                                 under "journey" when there is one
//   /api/map-snapshot/<time>.jpg  a whole-world picture
//   /api/map-journey/<time>.jpg   a journey picture
// and one for checking the live site:
//   /api/map-view.json            how the map was framed at the last build
import type { MapView } from '../src/config/site.ts';
import { JOURNEY, latestPicture, readPicture, WORLD, type Shelf, type SnapshotStore } from './snapshots.ts';

const json = (body: unknown, status: number, cache: string) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': cache },
  });

const SHELVES: Shelf[] = [WORLD, JOURNEY];
const addressOf = (shelf: Shelf, takenAt: number) => `/api/map-${shelf.name}/${takenAt}.jpg`;

/** Answers a snapshot request, or returns null when the address is not one of ours. */
export async function handleSnapshotRequest(url: URL, store: SnapshotStore, view?: MapView): Promise<Response | null> {
  if (url.pathname === '/api/map-view.json' && view) return json(view, 200, 'public, max-age=60');

  if (url.pathname === '/api/map-snapshot.json') {
    const [world, journey] = await Promise.all([latestPicture(store, WORLD), latestPicture(store, JOURNEY)]);
    if (!world && !journey) return json({ error: 'no snapshot yet' }, 404, 'public, max-age=60');
    // A new snapshot arrives every 30 minutes, so a minute of caching is plenty.
    return json(
      {
        ...(world && { takenAt: world.takenAt, url: addressOf(WORLD, world.takenAt) }),
        ...(journey && { journey: { takenAt: journey.takenAt, url: addressOf(JOURNEY, journey.takenAt) } }),
      },
      200,
      'public, max-age=60',
    );
  }

  const match = /^\/api\/map-([a-z]+)\/(\d{1,15})\.jpg$/.exec(url.pathname);
  const shelf = match && SHELVES.find((candidate) => candidate.name === match[1]);
  if (match && shelf) {
    const picture = await readPicture(store, shelf, Number(match[2]));
    if (!picture) return json({ error: 'snapshot not found' }, 404, 'public, max-age=60');
    // The address contains the time, so this exact picture never changes.
    return new Response(picture, {
      headers: { 'content-type': 'image/jpeg', 'cache-control': 'public, max-age=86400, immutable' },
    });
  }

  if (url.pathname.startsWith('/api/')) return json({ error: 'not found' }, 404, 'no-store');
  return null;
}
