// The site's own copy of the map: what the pages show, and let the visitor zoom into,
// when the live map does not load.
//
// It is a pyramid of pictures ("sheets"), all the same size in pixels. Level 0 is the
// whole world in one picture: that is the snapshot taken every 30 minutes
// (snapshots.ts). Each level further in has four times as many sheets and twice the
// detail: level 1 is 2 x 2 sheets, level 2 is 4 x 4, and so on down to COPY_LEVELS.
// The sheets of levels 1 and deeper are kept here. Every run of the Worker photographs
// a few of them (index.ts): first the ones that are missing, shallow levels first,
// then the oldest. So the copy fills up over the first days and is then renewed sheet
// by sheet; it is never deleted, and stays when the map is down.
//
// Keys in Cloudflare KV:
//   copy:<level>:<col>:<row>   one sheet (JPEG). Its metadata says when it was taken
//                              and with which framing of the map.
//   copy-index                 what the pages need to know to draw the copy (CopyIndex)
//   copy-state                 what the Worker remembers between runs (CopyState)
import type { MapView } from '../src/config/site.ts';
import type { SnapshotStore } from './snapshots.ts';

/**
 * The deepest level kept. Level 4 is 16 x 16 sheets: 16 times the detail of the
 * whole-world picture, 340 sheets in all with the levels above it. One level more
 * would be 1024 sheets on its own, which takes weeks to photograph a few at a time.
 */
export const COPY_LEVELS = 4;

const PREFIX = 'copy:';
const INDEX = 'copy-index';
const STATE = 'copy-state';
const HOUR = 60 * 60 * 1000;

export interface SheetRef {
  level: number;
  col: number;
  row: number;
}

/** When each sheet in the store was photographed (milliseconds), by sheetName. */
export type Sheets = Map<string, number>;

export const sheetName = ({ level, col, row }: SheetRef) => `${level}:${col}:${row}`;

/** How the map is framed: sheets taken with another framing do not fit this one. */
export const copyFraming = (view: MapView) => `${view.zoom}/${view.width}x${view.height}/${view.x},${view.z}`;

/** What the pages are told (inside /api/map-snapshot.json, under "copy"). */
export interface CopyIndex {
  framing: string;
  /** The size of every sheet, in pixels. */
  width: number;
  height: number;
  /**
   * levels[0] is level 1, levels[1] is level 2, ...: for each sheet, row by row from
   * the top left, when it was photographed in seconds, or 0 if there is none yet.
   */
  levels: number[][];
}

/** What the Worker remembers between runs. */
export interface CopyState {
  framing: string;
  /** The deepest level the map itself can show (found out by trying). */
  deepest: number;
  /** A day (UTC), and how long the hosted browser has run on it, in milliseconds. */
  day: string;
  browserMs: number;
}

/** Stores a sheet. */
export async function saveSheet(store: SnapshotStore, ref: SheetRef, picture: ArrayBuffer | ArrayBufferView, takenAt: number, framing: string): Promise<void> {
  await store.put(PREFIX + sheetName(ref), picture, { metadata: { takenAt, framing } });
}

/** A sheet's picture, or null if there is none. */
export function readSheet(store: SnapshotStore, ref: SheetRef): Promise<ArrayBuffer | null> {
  return store.get(PREFIX + sheetName(ref), 'arrayBuffer');
}

/** The sheets in the store that fit this framing, read from the store itself. */
export async function readSheets(store: SnapshotStore, framing: string): Promise<Sheets> {
  const sheets: Sheets = new Map();
  let cursor: string | undefined;
  do {
    const page = await store.list({ prefix: PREFIX, cursor });
    for (const key of page.keys) {
      const about = key.metadata as { takenAt?: unknown; framing?: unknown } | undefined;
      if (about?.framing === framing && typeof about.takenAt === 'number') sheets.set(key.name.slice(PREFIX.length), about.takenAt);
    }
    cursor = page.cursor;
  } while (cursor);
  return sheets;
}

/**
 * The sheets to photograph next, most wanted first: the missing ones (shallow levels
 * first, since they cover the most), then the ones older than `minAge`, oldest first.
 */
export function nextSheets(sheets: Sheets, deepest: number, now: number, minAge = 12 * HOUR): SheetRef[] {
  const missing: SheetRef[] = [];
  const old: { ref: SheetRef; takenAt: number }[] = [];
  for (let level = 1; level <= deepest; level++) {
    const parts = 2 ** level;
    for (let row = 0; row < parts; row++) {
      for (let col = 0; col < parts; col++) {
        const ref = { level, col, row };
        const takenAt = sheets.get(sheetName(ref));
        if (takenAt === undefined) missing.push(ref);
        else if (now - takenAt >= minAge) old.push({ ref, takenAt });
      }
    }
  }
  old.sort((a, b) => a.takenAt - b.takenAt);
  return [...missing, ...old.map((entry) => entry.ref)];
}

/** What the pages need to draw the copy. Levels with no sheet at all at the end are left out. */
export function copyIndex(view: MapView, sheets: Sheets, deepest: number): CopyIndex {
  const levels: number[][] = [];
  for (let level = 1; level <= deepest; level++) {
    const parts = 2 ** level;
    const times: number[] = [];
    for (let row = 0; row < parts; row++) {
      for (let col = 0; col < parts; col++) times.push(Math.floor((sheets.get(sheetName({ level, col, row })) ?? 0) / 1000));
    }
    levels.push(times);
  }
  while (levels.length && levels[levels.length - 1].every((time) => time === 0)) levels.pop();
  return { framing: copyFraming(view), width: view.width, height: view.height, levels };
}

/** Writes the index, unless the one in the store already says the same. */
export async function saveIndex(store: SnapshotStore, index: CopyIndex): Promise<boolean> {
  const text = JSON.stringify(index);
  if (JSON.stringify(await store.get(INDEX, 'json')) === text) return false;
  await store.put(INDEX, text);
  return true;
}

/** The index for this framing, or null when there is none (or it is for another framing). */
export async function readIndex(store: SnapshotStore, framing: string): Promise<CopyIndex | null> {
  const index = (await store.get(INDEX, 'json')) as Partial<CopyIndex> | null;
  if (!index || index.framing !== framing || !Array.isArray(index.levels)) return null;
  return index as CopyIndex;
}

const dayOf = (now: number) => new Date(now).toISOString().slice(0, 10);

/** What was remembered, started afresh for a new framing and counted from zero on a new day. */
export async function readState(store: SnapshotStore, framing: string, now: number): Promise<CopyState> {
  const saved = (await store.get(STATE, 'json')) as Partial<CopyState> | null;
  const sameFraming = saved?.framing === framing;
  const sameDay = saved?.day === dayOf(now);
  return {
    framing,
    deepest: sameFraming && typeof saved?.deepest === 'number' ? Math.min(COPY_LEVELS, Math.max(0, saved.deepest)) : COPY_LEVELS,
    day: dayOf(now),
    browserMs: sameDay && typeof saved?.browserMs === 'number' ? saved.browserMs : 0,
  };
}

export async function saveState(store: SnapshotStore, state: CopyState): Promise<void> {
  await store.put(STATE, JSON.stringify(state));
}
