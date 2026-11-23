// Map snapshots: pictures of the live map kept in Cloudflare KV, so the website has
// something to show when the map itself is down. A new one is taken every 20 minutes
// (index.ts); only the newest few are kept.
//
// Keys:
//   snapshot:<time>  the picture (JPEG), <time> in milliseconds, zero-padded so that
//                    keys sort by age
//   latest           { "takenAt": <time> } for the newest picture

/** The part of Cloudflare KV this file uses (the tests pass a small stand-in). */
export interface SnapshotStore {
  get(key: string, type: 'json'): Promise<unknown>;
  get(key: string, type: 'arrayBuffer'): Promise<ArrayBuffer | null>;
  put(key: string, value: string | ArrayBuffer | ArrayBufferView): Promise<void>;
  delete(key: string): Promise<void>;
  list(options: { prefix: string }): Promise<{ keys: { name: string }[] }>;
}

/** How many snapshots stay: the newest, and two before it (one hour at 20 minutes). */
export const KEEP = 3;

const PREFIX = 'snapshot:';
const keyFor = (takenAt: number) => `${PREFIX}${String(takenAt).padStart(15, '0')}`;

/** Stores a new snapshot, points `latest` at it and deletes the ones that are too old. */
export async function saveSnapshot(store: SnapshotStore, picture: ArrayBuffer | ArrayBufferView, takenAt: number, keep = KEEP): Promise<string[]> {
  await store.put(keyFor(takenAt), picture);
  await store.put('latest', JSON.stringify({ takenAt }));

  const { keys } = await store.list({ prefix: PREFIX });
  const old = keys
    .map((key) => key.name)
    .sort()
    .reverse()
    .slice(keep);
  await Promise.all(old.map((name) => store.delete(name)));
  return old;
}

/** When the newest snapshot was taken (milliseconds), or null if there is none yet. */
export async function latestSnapshotTime(store: SnapshotStore): Promise<number | null> {
  const latest = (await store.get('latest', 'json')) as { takenAt?: unknown } | null;
  return typeof latest?.takenAt === 'number' ? latest.takenAt : null;
}

/** The snapshot taken at that time, or null if it never existed or was deleted. */
export function readSnapshot(store: SnapshotStore, takenAt: number): Promise<ArrayBuffer | null> {
  return store.get(keyFor(takenAt), 'arrayBuffer');
}
