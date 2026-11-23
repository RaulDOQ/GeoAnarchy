# GeoAnarchy website

The public site for the GeoAnarchy Minecraft server, in Brazilian Portuguese. It is a
static [Astro](https://astro.build) site, so the build output is plain HTML, CSS and
JavaScript that any static host can serve.

## Pages

| Address | File | What it is |
|---|---|---|
| `/` | `src/pages/index.astro` | Home: the pitch, the live map, the server's ideas, how to join, questions |
| `/sobre/` | `src/pages/sobre/index.astro` | Sobre nós. The server's story and team are still to be written. |
| `/loja/` | `src/pages/loja/index.astro` | Loja. Not open yet; the page says so. |
| `/wiki/` | `src/pages/wiki/index.astro` | GeoWiki: how to join, the world, how the server works |
| any other | `src/pages/404.astro` | Page not found |

The header tabs are the list `common.nav` in `src/content/pt.ts`: add a page there and it
appears in the header and the footer. Each page passes its id to the layout
(`<Base page="wiki">`) so its tab is highlighted.

## Commands

Run these inside `website/` (Node 22.12 or newer):

| Command | What it does |
|---|---|
| `npm install` | Installs dependencies |
| `npm run dev` | Local preview with live reload at `http://localhost:4321` |
| `npm run check` | Type-checks the pages, scripts and Worker, and runs the tests |
| `npm run build` | Frames the live map, runs the checks, then writes the finished site to `dist/` |
| `npm run preview` | Serves `dist/` locally, as a host would |

## Where things live

| Path | Contents |
|---|---|
| `src/config/site.ts` | Server addresses, the map address and its parameters, the Discord invite, world size. Change a value here and every page follows. |
| `src/content/pt.ts` | Every text on the site. |
| `src/styles/tokens.css` | Colours, type, spacing and motion values (white, blue and green theme). |
| `src/components/Logo.astro`, `Wordmark.astro` | The logo with the block Earth (header) and the name on its own (hero, closing section, footer, 404 page). |
| `src/components/PageHead.astro`, `Editions.astro` | The top of every page other than the home page, and the Java and Bedrock join cards (home page and GeoWiki). |
| `src/components/home/` | The home page's sections: hero and live map window, anarchy, the block map sheet, realism and its journey map, how to join, questions. `Closing.astro` ends every page. |
| `src/scripts/` | Browser scripts: the live map card (what it shows, zoom and move), its tilt, scroll entrances, copy buttons. |
| `worker/` | The Worker: map snapshots every 20 minutes and the addresses that serve them. |
| `scripts/detect-map.mjs` | Works out the live map's framing before each build. |
| `src/assets/earth.png` | The block world map (144 x 72 pixels, one per 2.5 degrees), shown enlarged with sharp edges. |
| `src/assets/logo.webp`, `logo-small.webp`, `wordmark.webp` | The logo artwork, trimmed: the full logo in two sizes and the name alone. `public/favicon.png`, `public/apple-touch-icon.png` and `public/og.png` (the picture shown when the site's link is shared) come from the same artwork. |
| `scripts/make-pixel-earth.mjs` | Draws `earth.png` from Natural Earth land data. Its header explains the options. |

## The block map

The world map on the page is a real map: `scripts/make-pixel-earth.mjs` tests every
2.5-degree cell of the Earth against Natural Earth coastlines and paints the land cells
green and the polar ice white. The result of that test is saved in
`scripts/earth-mask.txt`, so changing the colours is instant:

| Command | What it does |
|---|---|
| `npm run make:earth` | Repaints `src/assets/earth.png` from the saved mask |
| `npm run make:earth -- --remask` | Works the land mask out again first (about two minutes) |

The sea colour is not in the image: it is `--color-ocean` in `src/styles/tokens.css`.

## The live map

The hero shows the server's Dynmap site (`mapa.geoanarchy.net`) in a plain card: the
name above it, the map, and one line under it saying whether the map is live.

- **It fits.** The map is shown in a frame of exactly the world's size at one Dynmap
  zoom level, scaled to the card, so the whole world fills the card with no border.
  `scripts/detect-map.mjs` works out that frame before every build by asking the map
  for its settings; if the map cannot be reached it uses
  `src/config/map-view.default.json`. `/api/map-view.json` on the live site shows what
  the last build used.
- **Only the map.** Dynmap's own controls are switched off, and the visitor never
  touches Dynmap: a pad over the card takes the pointer and `src/scripts/live-map.ts`
  moves and enlarges the whole map. It can be zoomed in from the full view (click, then
  wheel and drag; two fingers on a phone; `+`, `−`, arrows and `Esc` on a keyboard), never
  out past it and never off the world. Until the map is clicked, the wheel scrolls the page.
- **Fallback.** Under the live map lies the newest snapshot, visible while the map loads.
  The page asks the map site for a small picture first; if that fails, the live map is
  not loaded, the snapshot stays and the line under the card gives its time. It tries
  again every minute.

### Snapshots

The Worker in `worker/` takes the snapshots. Every 20 minutes (the cron trigger in
`wrangler.jsonc`) it opens the map in Cloudflare's hosted browser at the same framing,
waits for the tiles and stores a JPEG in Cloudflare KV. It keeps the newest three and
deletes older ones. If the map is down it stores nothing, so the last good picture stays.

| Address | What it is |
|---|---|
| `/api/map-snapshot.json` | When the newest snapshot was taken, and its address |
| `/api/map-snapshot/<time>.jpg` | That picture |
| `/api/map-view.json` | How the map was framed at the last build |

On Cloudflare's free plan the hosted browser may run 10 minutes a day; a snapshot takes
a few seconds, so 72 a day fit, without much room. If snapshots stop late in the day,
that allowance ran out: change the cron line to `*/30 * * * *`, or use a paid plan.

`npm test` runs the snapshot tests (storage, deleting old ones, the addresses).
`npx wrangler dev` runs the built site with the Worker locally.

The map must allow being shown inside another site: if Dynmap or Cloudflare ever sends
an `X-Frame-Options` or `Content-Security-Policy: frame-ancestors` header for
`mapa.geoanarchy.net`, it has to permit the website's address.

## Before going public

`site.inDevelopment` in `src/config/site.ts` is `true`: the page shows a "site em
desenvolvimento" strip and asks search engines not to index it. Set it to `false` at
launch.

## Deploy on Cloudflare

The site runs as a Cloudflare Worker (`wrangler.jsonc`): the pages are static files in
`dist/`, and a small script takes the map snapshots (see "Snapshots" above). Cloudflare
builds and publishes it from GitHub every time `main` changes.

In the Cloudflare dashboard: **Workers & Pages → Create → Import a repository**, pick
`RaulDOQ/GeoAnarchy`, and set:

- **Project name:** `geoanarchy-website` (the name in `wrangler.jsonc`)
- **Production branch:** `main`
- **Path / root directory:** `website`
- **Build command:** `npm run build`
- **Deploy command:** `npx wrangler deploy`

The Node version comes from `.node-version`. There are no variables or secrets to add,
and the KV namespace for the snapshots is created by the first deploy.

To publish from a computer instead: `npm run build`, then `npx wrangler deploy` (it asks
you to sign in to Cloudflare the first time).

### Custom domain

Worker → **Settings → Domains & Routes → Add → Custom domain**. Read the note below
before adding `geoanarchy.net` itself.

`geoanarchy.net` is also the game address. Java players are sent to the game server by
the `_minecraft._tcp` SRV record, which keeps working when the website takes over the
name. Bedrock players are not: Bedrock connects straight to whatever `geoanarchy.net`
points at, so once that is the website, Bedrock needs its own name (for example
`jogar.geoanarchy.net`, DNS only, pointing at the game host). Change `bedrock.address`
in `src/config/site.ts` to match.
