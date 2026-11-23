// Run with `npm test`: the site's own copy of the map (copy.ts).
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { sheetShot, worldShot, type MapView } from '../src/config/site.ts';
import { COPY_LEVELS, copyFraming, copyIndex, nextSheets, readIndex, readSheet, readSheets, readState, saveIndex, saveSheet, saveState, sheetName, type Sheets } from './copy.ts';
import { memoryStore } from './memory-store.ts';
import { handleSnapshotRequest } from './routes.ts';
import { savePicture, WORLD } from './snapshots.ts';

const picture = (byte: number) => new Uint8Array([0xff, 0xd8, byte]);
const HOURS = 60 * 60 * 1000;
const view: MapView = { zoom: 2, width: 1152, height: 576, x: 0, z: 0, tiles: null };
const framing = copyFraming(view);
const t = Date.UTC(2026, 10, 23, 14, 0);

test('level 0 is the whole world; every level in halves what a sheet covers', () => {
  const frame = ({ width, height, zoom, x, z }: ReturnType<typeof sheetShot>) => ({ width, height, zoom, x, z });
  assert.deepEqual(frame(sheetShot(view, 0, 0, 0)), frame(worldShot(view)));
  // Level 1, top left: a quarter of the world, centred in the north-west quarter.
  assert.deepEqual(frame(sheetShot(view, 1, 0, 0)), { width: 1152, height: 576, zoom: 3, x: -9216, z: -4608 });
  // Level 2, bottom right of 4 x 4.
  assert.deepEqual(frame(sheetShot(view, 2, 3, 3)), { width: 1152, height: 576, zoom: 4, x: 13824, z: 6912 });
  // Neighbours touch: their centres are exactly one sheet apart (36864 blocks / 4).
  assert.equal(sheetShot(view, 2, 2, 0).x - sheetShot(view, 2, 1, 0).x, 9216);
  assert.equal(new URL(sheetShot(view, 2, 3, 3).url).searchParams.get('zoom'), '4');
});

test('missing sheets come first, shallow levels before deep ones, then the oldest', () => {
  const sheets: Sheets = new Map();
  const names = (count: number, deepest = COPY_LEVELS, now = t) => nextSheets(sheets, deepest, now).slice(0, count).map(sheetName);
  assert.deepEqual(names(5), ['1:0:0', '1:1:0', '1:0:1', '1:1:1', '2:0:0']);
  assert.equal(nextSheets(sheets, COPY_LEVELS, t).length, 4 + 16 + 64 + 256);
  assert.equal(nextSheets(sheets, 2, t).length, 4 + 16, 'a map that stops at level 2');
  assert.equal(nextSheets(sheets, 0, t).length, 0, 'a map that cannot zoom in at all');

  for (const name of ['1:0:0', '1:1:0', '1:0:1', '1:1:1']) sheets.set(name, t);
  sheets.set('1:1:0', t - 30 * HOURS);
  sheets.set('1:0:1', t - 20 * HOURS);
  // Level 2 is still missing, so it comes before renewing anything.
  assert.deepEqual(names(1), ['2:0:0']);
  // With everything there (a map that stops at level 1): the oldest first; fresh ones not at all.
  assert.deepEqual(names(9, 1), ['1:1:0', '1:0:1']);
  assert.deepEqual(names(9, 1, t + 12 * HOURS), ['1:1:0', '1:0:1', '1:0:0', '1:1:1']);
});

test('the store itself says which sheets exist; sheets of another framing do not count', async () => {
  const store = memoryStore();
  await saveSheet(store, { level: 1, col: 1, row: 0 }, picture(1), t, framing);
  await saveSheet(store, { level: 2, col: 3, row: 2 }, picture(2), t + 5000, framing);
  await saveSheet(store, { level: 1, col: 0, row: 0 }, picture(3), t, 'another framing');

  const sheets = await readSheets(store, framing);
  assert.deepEqual([...sheets], [['1:1:0', t], ['2:3:2', t + 5000]]);
  assert.deepEqual([...new Uint8Array((await readSheet(store, { level: 2, col: 3, row: 2 }))!)], [0xff, 0xd8, 2]);
  assert.equal(await readSheet(store, { level: 3, col: 0, row: 0 }), null);
  // The sheet of the other framing is photographed again, in the same place.
  assert.equal(sheetName(nextSheets(sheets, COPY_LEVELS, t)[0]), '1:0:0');
});

test('the pages are told which sheets exist, level by level', async () => {
  const store = memoryStore();
  const sheets: Sheets = new Map([['1:1:0', t], ['2:3:2', t + 5000]]);
  const index = copyIndex(view, sheets, COPY_LEVELS);
  assert.equal(index.levels.length, 2, 'levels without any sheet at the end are left out');
  assert.deepEqual(index.levels[0], [0, t / 1000, 0, 0]);
  assert.equal(index.levels[1][2 * 4 + 3], t / 1000 + 5);
  assert.equal(index.levels[1].filter(Boolean).length, 1);
  assert.deepEqual({ width: index.width, height: index.height }, { width: 1152, height: 576 });

  assert.equal(await saveIndex(store, index), true);
  assert.equal(await saveIndex(store, index), false, 'the same index is not written twice');
  assert.deepEqual(await readIndex(store, framing), index);
  assert.equal(await readIndex(store, 'another framing'), null);
});

test('the Worker remembers how deep the map goes, and the browser time of the day', async () => {
  const store = memoryStore();
  const fresh = await readState(store, framing, t);
  assert.deepEqual(fresh, { framing, deepest: COPY_LEVELS, day: '2026-11-23', browserMs: 0 });

  await saveState(store, { ...fresh, deepest: 3, browserMs: 90_000 });
  assert.deepEqual(await readState(store, framing, t + 2 * HOURS), { framing, deepest: 3, day: '2026-11-23', browserMs: 90_000 });
  // A new day starts counting again; what is known about the map stays.
  assert.deepEqual(await readState(store, framing, t + 24 * HOURS), { framing, deepest: 3, day: '2026-11-24', browserMs: 0 });
  // A map framed differently is a map to find out about again.
  assert.equal((await readState(store, 'another framing', t)).deepest, COPY_LEVELS);
});

test('the copy is offered next to the newest snapshot, and each sheet has its own address', async () => {
  const store = memoryStore();
  await savePicture(store, WORLD, picture(1), t);
  const before = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store, view);
  assert.equal('copy' in ((await before!.json()) as object), false, 'no copy yet');

  await saveSheet(store, { level: 1, col: 1, row: 0 }, picture(9), t, framing);
  await saveIndex(store, copyIndex(view, await readSheets(store, framing), COPY_LEVELS));
  const info = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store, view);
  const body = (await info!.json()) as { url: string; copy: { width: number; height: number; levels: number[][] } };
  assert.deepEqual(body.copy, { width: 1152, height: 576, levels: [[0, t / 1000, 0, 0]] });

  const sheet = await handleSnapshotRequest(new URL(`https://site/api/map-copy/1/1_0.jpg?t=${t / 1000}`), store, view);
  assert.equal(sheet?.headers.get('content-type'), 'image/jpeg');
  assert.equal(sheet?.headers.get('cache-control'), 'public, max-age=31536000, immutable');
  assert.deepEqual([...new Uint8Array(await sheet!.arrayBuffer())], [0xff, 0xd8, 9]);

  for (const address of ['/api/map-copy/1/0_0.jpg', '/api/map-copy/1/2_0.jpg', '/api/map-copy/9/0_0.jpg', '/api/map-copy/0/0_0.jpg', '/api/map-copy/1/x.jpg']) {
    assert.equal((await handleSnapshotRequest(new URL(`https://site${address}`), store, view))?.status, 404, address);
  }
  // After a build that frames the map differently, the old copy is not offered.
  const other = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store, { ...view, width: 1400, height: 700 });
  assert.equal('copy' in ((await other!.json()) as object), false);
});
