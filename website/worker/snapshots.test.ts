// Run with `npm test`. A small in-memory stand-in plays the part of Cloudflare KV.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { journeyShot, worldShot, type MapView } from '../src/config/site.ts';
import { handleSnapshotRequest } from './routes.ts';
import { framingOf, isDue, JOURNEY, latestPicture, readPicture, savePicture, WORLD, type SnapshotStore } from './snapshots.ts';

function memoryStore(): SnapshotStore & { data: Map<string, string | ArrayBuffer> } {
  const data = new Map<string, string | ArrayBuffer>();
  return {
    data,
    async get(key: string, type: 'json' | 'arrayBuffer') {
      const value = data.get(key);
      if (value === undefined) return null;
      if (type === 'json') return JSON.parse(value as string);
      return value as ArrayBuffer;
    },
    async put(key, value) {
      data.set(key, typeof value === 'string' ? value : ArrayBuffer.isView(value) ? (value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength) as ArrayBuffer) : value);
    },
    async delete(key) {
      data.delete(key);
    },
    async list({ prefix }) {
      return { keys: [...data.keys()].filter((name) => name.startsWith(prefix)).map((name) => ({ name })) };
    },
  } as SnapshotStore & { data: Map<string, string | ArrayBuffer> };
}

const picture = (byte: number) => new Uint8Array([0xff, 0xd8, byte]);
const MINUTES = 60_000;
const HOURS = 60 * MINUTES;
/** The map as the build finds it when it cannot ask: the whole world is 1152 pixels wide at zoom 2. */
const view: MapView = { zoom: 2, width: 1152, height: 576, x: 0, z: 0, tiles: null };

test('there is no snapshot before the first one', async () => {
  const store = memoryStore();
  assert.equal(await latestPicture(store, WORLD), null);
  const response = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store);
  assert.equal(response?.status, 404);
});

test('the newest snapshot is served, with its time', async () => {
  const store = memoryStore();
  const t = Date.UTC(2026, 10, 23, 14, 0);
  await savePicture(store, WORLD, picture(1), t);
  assert.equal((await latestPicture(store, WORLD))?.takenAt, t);

  const info = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store);
  assert.equal(info?.status, 200);
  const body = (await info!.json()) as { takenAt: number; url: string };
  assert.deepEqual(body, { takenAt: t, url: `/api/map-snapshot/${t}.jpg` });

  const image = await handleSnapshotRequest(new URL(`https://site${body.url}`), store);
  assert.equal(image?.status, 200);
  assert.equal(image?.headers.get('content-type'), 'image/jpeg');
  assert.deepEqual([...new Uint8Array(await image!.arrayBuffer())], [0xff, 0xd8, 1]);
});

test('snapshots that are too old are deleted, the newest are kept', async () => {
  const store = memoryStore();
  const start = Date.UTC(2026, 10, 23, 14, 0);
  const times = [0, 30, 60, 90, 120].map((m) => start + m * MINUTES);
  let deleted: string[] = [];
  for (const [i, t] of times.entries()) deleted = await savePicture(store, WORLD, picture(i), t);

  assert.equal(deleted.length, 1, 'each new snapshot pushes one old one out');
  const kept = [...store.data.keys()].filter((key) => key.startsWith('snapshot:'));
  assert.equal(kept.length, WORLD.keep);
  assert.equal(await readPicture(store, WORLD, times[0]), null);
  assert.equal(await readPicture(store, WORLD, times[1]), null);
  for (const t of times.slice(2)) assert.ok(await readPicture(store, WORLD, t), 'a recent snapshot is still there');
  assert.equal((await latestPicture(store, WORLD))?.takenAt, times[4]);

  const gone = await handleSnapshotRequest(new URL(`https://site/api/map-snapshot/${times[0]}.jpg`), store);
  assert.equal(gone?.status, 404);
});

test('a failed capture changes nothing: the earlier snapshot stays the latest', async () => {
  const store = memoryStore();
  const t = Date.UTC(2026, 10, 23, 14, 0);
  await savePicture(store, WORLD, picture(7), t);
  // index.ts catches the capture error and never calls savePicture.
  assert.equal((await latestPicture(store, WORLD))?.takenAt, t);
  assert.ok(await readPicture(store, WORLD, t));
});

test('other addresses are left to the static site', async () => {
  const store = memoryStore();
  assert.equal(await handleSnapshotRequest(new URL('https://site/wiki/'), store), null);
  assert.equal(await handleSnapshotRequest(new URL('https://site/nao-existe'), store), null);
  const unknownApi = await handleSnapshotRequest(new URL('https://site/api/outra-coisa'), store);
  assert.equal(unknownApi?.status, 404);
  const badName = await handleSnapshotRequest(new URL('https://site/api/map-snapshot/abc.jpg'), store);
  assert.equal(badName?.status, 404);
  const unknownKind = await handleSnapshotRequest(new URL('https://site/api/map-outro/123.jpg'), store);
  assert.equal(unknownKind?.status, 404);
});

test('the journey picture is zoomed in on its part of the world', () => {
  const world = worldShot(view);
  assert.deepEqual({ width: world.width, height: world.height, zoom: world.zoom }, { width: 1152, height: 576, zoom: 2 });

  // A third of the world across and half of it down, two zoom levels in: four times
  // the pixels it has when cut from the whole-world picture (384 x 288).
  const journey = journeyShot(view);
  assert.deepEqual({ width: journey.width, height: journey.height, zoom: journey.zoom }, { width: 1536, height: 1152, zoom: 4 });
  // Centred on 15°W 15°N, the middle of 75°W..45°E and 60°N..30°S (1 block is about 1 km).
  const query = new URL(journey.url).searchParams;
  assert.deepEqual([query.get('zoom'), query.get('x'), query.get('z')], ['4', '-1536', '-1536']);
  assert.equal(query.get('nogui'), 'true');
});

test('the journey picture never asks for more detail than the map has', () => {
  const tiles = { base: 'https://map/tiles/earth/', prefix: 'flat', format: 'jpg', tileSize: 128, world: { x: 0, y: 0, width: 288, height: 144 }, dayNight: false, verified: true };
  assert.equal(journeyShot({ ...view, tiles: { ...tiles, nativeZoom: 9 } }).zoom, 4);
  const shallow = journeyShot({ ...view, tiles: { ...tiles, nativeZoom: 3 } });
  assert.deepEqual({ width: shallow.width, height: shallow.height, zoom: shallow.zoom }, { width: 768, height: 576, zoom: 3 });
});

test('the journey picture is taken when there is none, when its framing changes, and once a day', async () => {
  const store = memoryStore();
  const framing = framingOf(journeyShot(view));
  const t = Date.UTC(2026, 10, 23, 14, 0);
  assert.equal(isDue(await latestPicture(store, JOURNEY), framing, t), true, 'none yet');

  await savePicture(store, JOURNEY, picture(1), t, framing);
  const latest = await latestPicture(store, JOURNEY);
  assert.deepEqual(latest, { takenAt: t, framing });
  assert.equal(isDue(latest, framing, t + 30 * MINUTES), false, 'half an hour later');
  assert.equal(isDue(latest, framing, t + 23 * HOURS), false, 'not yet a day');
  assert.equal(isDue(latest, framing, t + 24 * HOURS), true, 'a day later');
  // The same world described one zoom level in is still the same picture ...
  assert.equal(isDue(latest, framingOf(journeyShot({ ...view, zoom: 3, width: 2304, height: 1152 })), t + 30 * MINUTES), false, 'same framing');
  // ... a map drawn at another size is not.
  assert.equal(isDue(latest, framingOf(journeyShot({ ...view, width: 1400, height: 700 })), t + 30 * MINUTES), true, 'framed differently');
});

test('the journey picture is offered next to the newest snapshot, and has its own address', async () => {
  const store = memoryStore();
  const t = Date.UTC(2026, 10, 23, 14, 0);
  await savePicture(store, WORLD, picture(1), t);
  await savePicture(store, JOURNEY, picture(2), t + 1000, 'framing');
  // Later whole-world snapshots do not push the journey picture out.
  for (const m of [30, 60, 90, 120]) await savePicture(store, WORLD, picture(3), t + m * MINUTES);

  const info = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store);
  const body = (await info!.json()) as { takenAt: number; url: string; journey: { takenAt: number; url: string } };
  assert.equal(body.url, `/api/map-snapshot/${t + 120 * MINUTES}.jpg`);
  assert.deepEqual(body.journey, { takenAt: t + 1000, url: `/api/map-journey/${t + 1000}.jpg` });

  const image = await handleSnapshotRequest(new URL(`https://site${body.journey.url}`), store);
  assert.equal(image?.headers.get('content-type'), 'image/jpeg');
  assert.deepEqual([...new Uint8Array(await image!.arrayBuffer())], [0xff, 0xd8, 2]);
  // A whole-world picture is not served under the journey's address, or the other way round.
  assert.equal((await handleSnapshotRequest(new URL(`https://site/api/map-journey/${t}.jpg`), store))?.status, 404);

  // A new journey picture keeps the one before it, for a page that is still showing it.
  await savePicture(store, JOURNEY, picture(4), t + 24 * HOURS, 'framing');
  await savePicture(store, JOURNEY, picture(5), t + 48 * HOURS, 'framing');
  assert.equal(await readPicture(store, JOURNEY, t + 1000), null);
  assert.ok(await readPicture(store, JOURNEY, t + 24 * HOURS));
  assert.ok(await readPicture(store, JOURNEY, t + 48 * HOURS));
});
