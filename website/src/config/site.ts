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

  /** The block map (src/assets/earth.png): this many pixels for the whole world. */
  blockMap: { width: 144, height: 72 },

  /** The part of the world the journey figure in "Realismo" shows, in pixels of the
      block map (2.5 degrees each): 75°W to 45°E, 60°N to 30°S. */
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
}

/** The whole world: the picture the live map falls back to. */
export function worldShot(view: MapView): MapShot {
  return { url: mapEmbedUrl(view), width: view.width, height: view.height, zoom: view.zoom };
}

/** The journey figure is about 680 pixels wide on a large screen; twice that keeps it
    sharp on a high-density one. */
const JOURNEY_SHOT_WIDTH = 1300;

/**
 * The journey figure's part of the world, photographed on its own: zoomed in until it
 * is at least JOURNEY_SHOT_WIDTH pixels wide (cut from the whole-world picture it would
 * be a third of that, and blurred when shown).
 */
export function journeyShot(view: MapView): MapShot {
  const { journey, blockMap, world } = site;
  const across = journey.w / blockMap.width;
  const down = journey.h / blockMap.height;
  // Each zoom level in doubles the size. Never past the level where the tiles are at
  // full detail, when that is known.
  let steps = Math.max(0, Math.ceil(Math.log2(JOURNEY_SHOT_WIDTH / (view.width * across)) - 1e-9));
  if (view.tiles) steps = Math.min(steps, Math.max(0, view.tiles.nativeZoom - view.zoom));
  const zoom = view.zoom + steps;
  // Even sizes, so the centre of the picture is between two pixels, as Dynmap puts it.
  const even = (pixels: number) => 2 * Math.round(pixels / 2);
  return {
    url: mapEmbedUrl({
      ...view,
      zoom,
      x: Math.round(world.minX + ((journey.x + journey.w / 2) / blockMap.width) * world.width),
      z: Math.round(world.minZ + ((journey.y + journey.h / 2) / blockMap.height) * world.depth),
    }),
    width: even(view.width * 2 ** steps * across),
    height: even(view.height * 2 ** steps * down),
    zoom,
  };
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
