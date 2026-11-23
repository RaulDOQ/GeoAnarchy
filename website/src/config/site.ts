// Everything about the server that the pages show: addresses, links and map facts.
// Change a value here and every page follows.

export const site = {
  name: 'GeoAnarchy',
  url: 'https://geoanarchy.net',

  /** Shows the "site em desenvolvimento" strip and keeps the site out of search engines. */
  inDevelopment: true,

  java: {
    address: 'geoanarchy.net',
    /** Oldest Java client the server is meant to accept (ViaBackwards). */
    minVersion: '1.12',
    /** The version the server itself runs. */
    serverVersion: '1.21.11',
  },

  bedrock: {
    address: 'geoanarchy.net',
    port: 10109,
  },

  map: {
    /** The public Dynmap site. */
    url: 'https://mapa.geoanarchy.net/',
    host: 'mapa.geoanarchy.net',
    /** Dynmap URL parameters for the map embedded in the home page: the layer, and no
        side panel, chat, compass or controls. Only the map itself is shown. */
    embedParams: {
      worldname: 'earth',
      mapname: 'flat',
      nogui: 'true',
      nopanel: 'true',
      hidechat: 'true',
      nocompass: 'true',
    } as Record<string, string>,
  },

  /** Invite found in the 2025 plugin source (/discord command). Confirm it still works. */
  discordUrl: 'https://discord.gg/XBwg3WWyqA',

  /** The Earth world, as installed (1:1000 map, playable rectangle). */
  world: {
    scale: '1:1000',
    width: 36864,
    depth: 18432,
    chunks: 2654208,
    minX: -18432,
    maxX: 18431,
    minZ: -9216,
    maxZ: 9215,
  },

  /** The world as a grid of cells of 2.5 degrees: the journey figure is laid out on it. */
  grid: { width: 144, height: 72 },

  /** The part of the world the journey figure in "Realismo" shows, in cells of that
      grid: 75°W to 45°E, 60°N to 30°S. */
  journey: { x: 42, y: 12, w: 48, h: 36 },
} as const;

/** The map's own picture tiles, as scripts/detect-map.mjs found them. */
export interface MapTiles {
  /** Where the tiles are, ending in a slash. */
  base: string;
  prefix: string;
  format: string;
  tileSize: number;
  /** The zoom level at which tiles are at full detail (0 is zoomed all the way out). */
  nativeZoom: number;
  /** Where the world lies, in pixels at zoom level 0. */
  world: { x: number; y: number; width: number; height: number };
  /** True when the layer has separate day and night tiles (the page does not use those). */
  dayNight: boolean;
  /** True when the build fetched a few tiles and they answered. */
  verified: boolean;
}

/** What scripts/detect-map.mjs writes to src/generated/map-view.json. */
export interface MapView {
  /** The tiles, or null when the map could not be read at build time. */
  tiles?: MapTiles | null;
  /** Dynmap zoom level. */
  zoom: number;
  /** Size of the world at that zoom level, in pixels. */
  width: number;
  height: number;
  /** The block at the centre. */
  x: number;
  z: number;
}

/** The tiles, when the pages may draw the map from them: found, checked, one set only. */
export function drawableTiles(view: MapView): MapTiles | null {
  return view.tiles?.verified && !view.tiles.dayNight ? view.tiles : null;
}

/** One photograph of the map, taken from Dynmap's own page (worker/capture.ts). */
export interface MapShot {
  /** The page to open. */
  url: string;
  /** The size of the picture, in pixels. */
  width: number;
  height: number;
  /** The Dynmap zoom level the page must be at. */
  zoom: number;
  /** The block at the centre of the picture. */
  x: number;
  z: number;
}

/** A photograph of the map `steps` zoom levels in from the whole-world view. */
function shotAt(view: MapView, steps: number, x: number, z: number, width: number, height: number): MapShot {
  const zoom = view.zoom + steps;
  return { url: mapEmbedUrl({ ...view, zoom, x, z }), width, height, zoom, x, z };
}

/** The whole world: the picture the live map falls back to. */
export function worldShot(view: MapView): MapShot {
  return shotAt(view, 0, view.x, view.z, view.width, view.height);
}

/**
 * One sheet of the site's own copy of the map (worker/copy.ts). Level 0 is the whole
 * world in one picture; each level further in has four times as many sheets, every one
 * the same size in pixels, so twice the detail. (col, row) counts from the top left.
 */
export function sheetShot(view: MapView, level: number, col: number, row: number): MapShot {
  const { world } = site;
  const parts = 2 ** level;
  const x = Math.round(world.minX + ((col + 0.5) / parts) * world.width);
  const z = Math.round(world.minZ + ((row + 0.5) / parts) * world.depth);
  return shotAt(view, level, x, z, view.width, view.height);
}

/** The journey figure is about 680 pixels wide on a large screen; twice that keeps it
    sharp on a high-density one. */
const JOURNEY_SHOT_WIDTH = 1300;

/**
 * The journey figure's part of the world, photographed on its own: zoomed in until it
 * is at least JOURNEY_SHOT_WIDTH pixels wide (cut from the whole-world picture it would
 * be a third of that, and blurred when shown). `deepest` is how many zoom levels in the
 * map can go, when that has been found out.
 */
export function journeyShot(view: MapView, deepest = Infinity): MapShot {
  const { journey, grid, world } = site;
  const across = journey.w / grid.width;
  const down = journey.h / grid.height;
  // Each zoom level in doubles the size. Never past the level where the tiles are at
  // full detail, when that is known.
  let steps = Math.min(deepest, Math.max(0, Math.ceil(Math.log2(JOURNEY_SHOT_WIDTH / (view.width * across)) - 1e-9)));
  if (view.tiles) steps = Math.min(steps, Math.max(0, view.tiles.nativeZoom - view.zoom));
  // Even sizes, so the centre of the picture is between two pixels, as Dynmap puts it.
  const even = (pixels: number) => 2 * Math.round(pixels / 2);
  return shotAt(
    view,
    steps,
    Math.round(world.minX + ((journey.x + journey.w / 2) / grid.width) * world.width),
    Math.round(world.minZ + ((journey.y + journey.h / 2) / grid.height) * world.depth),
    even(view.width * 2 ** steps * across),
    even(view.height * 2 ** steps * down),
  );
}

/** The embedded map's address: the layer, no controls, and the given framing. */
export function mapEmbedUrl(view: MapView): string {
  const query = new URLSearchParams({
    ...site.map.embedParams,
    zoom: String(view.zoom),
    x: String(view.x),
    y: '64',
    z: String(view.z),
  }).toString();
  return `${site.map.url}?${query}`;
}
