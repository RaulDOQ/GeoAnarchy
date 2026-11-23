// The site's own copy of the map, as the pages use it.
//
// The Worker photographs the map and stores the pictures (worker/copy.ts): the whole
// world in one picture, and deeper levels cut into sheets of the same size, each level
// twice as detailed as the one above. /api/map-snapshot.json says what exists. The
// pages draw from this copy when the live map does not load, so there is always a real
// map to look at and to zoom into, even if it is some hours or days old.
import type { TileSource } from './tile-painter';

/** What /api/map-snapshot.json answers (worker/routes.ts). */
export interface SnapshotInfo {
  /** The newest whole-world picture: when it was taken (milliseconds) and its address. */
  takenAt?: number;
  url?: string;
  /** The journey figure's picture. */
  journey?: { takenAt?: number; url?: string };
  /** The deeper sheets: their size, and for each level when each sheet was taken (seconds; 0 = none). */
  copy?: { width: number; height: number; levels: number[][] };
}

const asked = new Map<string, Promise<SnapshotInfo | null>>();

/** Asks once per page, however many parts of the page want to know. */
export function snapshotInfo(address: string): Promise<SnapshotInfo | null> {
  let answer = asked.get(address);
  if (!answer) {
    answer = fetch(address)
      .then((response) => (response.ok ? (response.json() as Promise<SnapshotInfo>) : null))
      // No snapshot service (for example in local development).
      .catch(() => null);
    asked.set(address, answer);
  }
  return answer;
}

/**
 * The copy as something the tile painter can draw, or null when there is no picture at
 * all yet. `width` and `height` are the whole-world picture's size.
 */
export function copyTiles(info: SnapshotInfo, width: number, height: number): TileSource | null {
  const world = typeof info.url === 'string' ? info.url : null;
  const levels = info.copy && info.copy.width === width && info.copy.height === height ? info.copy.levels : [];
  if (!world && !levels.length) return null;
  // The copy fills up over its first days, shallow levels first. Visitors may zoom as
  // far as the deepest level that is whole, so they never zoom into a blur; deeper
  // sheets that already exist are still used where they are.
  let complete = 0;
  while (complete < levels.length && levels[complete].every(Boolean)) complete++;
  return {
    tileWidth: width,
    tileHeight: height,
    maxLevel: levels.length,
    completeLevel: complete,
    world: { x: 0, y: 0, width, height },
    address(level, col, row) {
      if (level === 0) return world;
      const time = levels[level - 1]?.[row * 2 ** level + col];
      return time ? `/api/map-copy/${level}/${col}_${row}.jpg?t=${time}` : null;
    },
    smooth: true,
    // A sheet is a large picture: a few dozen cover any view with room to spare.
    keep: 40,
  };
}
