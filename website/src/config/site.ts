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
    /** Dynmap URL parameters for the map embedded in the home page (a clean view:
        no side panel, chat or controls; the full map opens in its own tab). */
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

/** The embedded map's address, with its parameters. */
export function mapEmbedUrl(): string {
  const query = new URLSearchParams(site.map.embedParams).toString();
  return query ? `${site.map.url}?${query}` : site.map.url;
}
