// Run with `npm test`. A small in-memory stand-in plays the part of Cloudflare KV.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { handleSnapshotRequest } from './routes.ts';
import { KEEP, latestSnapshotTime, readSnapshot, saveSnapshot, type SnapshotStore } from './snapshots.ts';

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

test('there is no snapshot before the first one', async () => {
  const store = memoryStore();
  assert.equal(await latestSnapshotTime(store), null);
  const response = await handleSnapshotRequest(new URL('https://site/api/map-snapshot.json'), store);
  assert.equal(response?.status, 404);
});

test('the newest snapshot is served, with its time', async () => {
  const store = memoryStore();
  const t = Date.UTC(2026, 10, 23, 14, 0);
  await saveSnapshot(store, picture(1), t);
  assert.equal(await latestSnapshotTime(store), t);

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
  const times = [0, 20, 40, 60, 80].map((m) => start + m * MINUTES);
  let deleted: string[] = [];
  for (const [i, t] of times.entries()) deleted = await saveSnapshot(store, picture(i), t);

  assert.equal(deleted.length, 1, 'each new snapshot pushes one old one out');
  const kept = [...store.data.keys()].filter((key) => key.startsWith('snapshot:'));
  assert.equal(kept.length, KEEP);
  assert.equal(await readSnapshot(store, times[0]), null);
  assert.equal(await readSnapshot(store, times[1]), null);
  for (const t of times.slice(2)) assert.ok(await readSnapshot(store, t), 'a recent snapshot is still there');
  assert.equal(await latestSnapshotTime(store), times[4]);

  const gone = await handleSnapshotRequest(new URL(`https://site/api/map-snapshot/${times[0]}.jpg`), store);
  assert.equal(gone?.status, 404);
});

test('a failed capture changes nothing: the earlier snapshot stays the latest', async () => {
  const store = memoryStore();
  const t = Date.UTC(2026, 10, 23, 14, 0);
  await saveSnapshot(store, picture(7), t);
  // index.ts catches the capture error and never calls saveSnapshot.
  assert.equal(await latestSnapshotTime(store), t);
  assert.ok(await readSnapshot(store, t));
});

test('other addresses are left to the static site', async () => {
  const store = memoryStore();
  assert.equal(await handleSnapshotRequest(new URL('https://site/wiki/'), store), null);
  assert.equal(await handleSnapshotRequest(new URL('https://site/nao-existe'), store), null);
  const unknownApi = await handleSnapshotRequest(new URL('https://site/api/outra-coisa'), store);
  assert.equal(unknownApi?.status, 404);
  const badName = await handleSnapshotRequest(new URL('https://site/api/map-snapshot/abc.jpg'), store);
  assert.equal(badName?.status, 404);
});
