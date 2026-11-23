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
