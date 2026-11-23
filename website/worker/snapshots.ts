// Map pictures kept in Cloudflare KV. There are two kinds ("shelves"):
//
//   WORLD    the whole world, so the website has something to show when the live map
//            is down. A new one is taken every 30 minutes (index.ts).
//   JOURNEY  the part of the world in the "Realismo" journey figure, zoomed in. It is
//            a still picture, so one a day is plenty.
//
// Only the newest few of each kind are kept. Keys, for a shelf with prefix `p:`:
//   p:<time>          the picture (JPEG), <time> in milliseconds, zero-padded so that
//                     keys sort by age
//   <shelf.latest>    { "takenAt": <time>, "framing": "..." } for the newest picture

/** The part of Cloudflare KV this file uses (the tests pass a small stand-in). */
export interface SnapshotStore {
  get(key: string, type: 'json'): Promise<unknown>;
  get(key: string, type: 'arrayBuffer'): Promise<ArrayBuffer | null>;
  put(key: string, value: string | ArrayBuffer | ArrayBufferView): Promise<void>;
  delete(key: string): Promise<void>;
  list(options: { prefix: string }): Promise<{ keys: { name: string }[] }>;
}

export interface Shelf {
  /** The name in the picture's address: /api/map-<name>/<time>.jpg */
  name: string;
  prefix: string;
  latest: string;
  /** How many pictures stay. */
  keep: number;
}

/** The newest, and two before it (one hour at 30 minutes). */
export const WORLD: Shelf = { name: 'snapshot', prefix: 'snapshot:', latest: 'latest', keep: 3 };
/** The newest, and the one before it for a browser that is still showing it. */
export const JOURNEY: Shelf = { name: 'journey', prefix: 'journey:', latest: 'latest-journey', keep: 2 };

/** What is known about the newest picture on a shelf. */
export interface Latest {
  /** When it was taken, in milliseconds. */
  takenAt: number;
  /** What it shows (see `framingOf`), when that was recorded. */
  framing?: string;
}

const DAY = 24 * 60 * 60 * 1000;
const keyFor = (shelf: Shelf, takenAt: number) => `${shelf.prefix}${String(takenAt).padStart(15, '0')}`;

/** A short description of what a picture shows: it changes when the framing does. */
export const framingOf = (shot: { url: string; width: number; height: number }) => `${shot.width}x${shot.height} ${shot.url}`;

/** Stores a new picture, points the shelf's `latest` at it and deletes the ones that are too old. */
export async function savePicture(store: SnapshotStore, shelf: Shelf, picture: ArrayBuffer | ArrayBufferView, takenAt: number, framing?: string): Promise<string[]> {
  await store.put(keyFor(shelf, takenAt), picture);
  await store.put(shelf.latest, JSON.stringify({ takenAt, framing }));

  const { keys } = await store.list({ prefix: shelf.prefix });
  const old = keys
    .map((key) => key.name)
    .sort()
    .reverse()
    .slice(shelf.keep);
  await Promise.all(old.map((name) => store.delete(name)));
  return old;
}

/** The newest picture on a shelf, or null if there is none yet. */
export async function latestPicture(store: SnapshotStore, shelf: Shelf): Promise<Latest | null> {
  const latest = (await store.get(shelf.latest, 'json')) as { takenAt?: unknown; framing?: unknown } | null;
  if (typeof latest?.takenAt !== 'number') return null;
  return { takenAt: latest.takenAt, framing: typeof latest.framing === 'string' ? latest.framing : undefined };
}

/** The picture taken at that time, or null if it never existed or was deleted. */
export function readPicture(store: SnapshotStore, shelf: Shelf, takenAt: number): Promise<ArrayBuffer | null> {
  return store.get(keyFor(shelf, takenAt), 'arrayBuffer');
}

/**
 * True when a still picture should be taken again: there is none, it was framed
 * differently (the site changed what it shows), or it is more than a day old.
 */
export function isDue(latest: Latest | null, framing: string, now: number, maxAge = DAY): boolean {
  return !latest || latest.framing !== framing || now - latest.takenAt >= maxAge;
}
