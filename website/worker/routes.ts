// The two addresses the website asks for the fallback picture:
//   /api/map-snapshot.json        when the newest snapshot was taken, and its address
//   /api/map-snapshot/<time>.jpg  that picture
// and one for checking the live site:
//   /api/map-view.json            how the map was framed at the last build
import type { MapView } from '../src/config/site.ts';
import { latestSnapshotTime, readSnapshot, type SnapshotStore } from './snapshots.ts';

const json = (body: unknown, status: number, cache: string) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': cache },
  });

/** Answers a snapshot request, or returns null when the address is not one of ours. */
export async function handleSnapshotRequest(url: URL, store: SnapshotStore, view?: MapView): Promise<Response | null> {
  if (url.pathname === '/api/map-view.json' && view) return json(view, 200, 'public, max-age=60');

  if (url.pathname === '/api/map-snapshot.json') {
    const takenAt = await latestSnapshotTime(store);
    if (takenAt === null) return json({ error: 'no snapshot yet' }, 404, 'public, max-age=60');
    // A new snapshot arrives every 30 minutes, so a minute of caching is plenty.
    return json({ takenAt, url: `/api/map-snapshot/${takenAt}.jpg` }, 200, 'public, max-age=60');
  }

  const match = /^\/api\/map-snapshot\/(\d{1,15})\.jpg$/.exec(url.pathname);
  if (match) {
    const picture = await readSnapshot(store, Number(match[1]));
    if (!picture) return json({ error: 'snapshot not found' }, 404, 'public, max-age=60');
    // The address contains the time, so this exact picture never changes.
    return new Response(picture, {
      headers: { 'content-type': 'image/jpeg', 'cache-control': 'public, max-age=86400, immutable' },
    });
  }

  if (url.pathname.startsWith('/api/')) return json({ error: 'not found' }, 404, 'no-store');
  return null;
}
