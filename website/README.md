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
| `src/components/home/` | The home page's sections: hero, the live map, anarchy, realism and its journey map, how to join, questions. `Closing.astro` ends every page. |
| `src/scripts/` | Browser scripts: the live map (zoom and move), the tile painter it shares with the still map picture, the site's copy of the map as the pages read it, scroll entrances, copy buttons. |
| `worker/` | The Worker: every 30 minutes a map snapshot and a few sheets of the site's own copy of the map, the journey picture once a day, and the addresses that serve them. |
| `scripts/detect-map.mjs` | Finds and checks the live map's tiles before each build. |
| `src/assets/anarchy-chaos.webp`, `anarchy-order.webp` | The pictures in the two "Anarquia" panels. Replace a file to change a picture; without a file the panel shows blocks. |
| `src/assets/logo.webp`, `logo-small.webp`, `wordmark.webp` | The logo artwork, trimmed: the full logo in two sizes and the name alone. `public/favicon.png`, `public/apple-touch-icon.png` and `public/og.png` (the picture shown when the site's link is shared) come from the same artwork. |

## The live map

The first section of the home page is the live map: large, with the limits of the world
along its edges, rising into view as the visitor scrolls down from the hero.

- **The page draws the map itself.** `src/scripts/live-map.ts` fetches the map's own
  picture tiles from the server's Dynmap site (`mapa.geoanarchy.net`) and paints them on
  a canvas, snapped to whole pixels, at the level of detail the current zoom needs. So
  there are no lines between tiles, no Dynmap controls and no player markers, and the
  map can be zoomed in down to single blocks.
- **Finding the tiles.** `scripts/detect-map.mjs` runs before every build. It asks the map
  for its settings, works out where the world lies and how tiles are named (the same
  arithmetic as Dynmap's own page), and fetches a few tiles to check. Only when they
  answer does the page use them. `/api/map-view.json` on the live site shows what the
  last build found.
- **If the tiles are not verified** (the map was down during the build, or its settings
  changed in a way the script does not understand), the page shows Dynmap's own page in a
  frame instead, scaled so the world fills the card. Dynmap's page cannot be enlarged
  without blurring, so as soon as the visitor zooms in, the card shows the site's own
  copy of the map (see below) and the line under the card says so.
- **Using it.** A pad over the card takes the pointer: click, then wheel and drag; two
  fingers on a phone; `+`, `−`, arrows and `Esc` on a keyboard. The map can be zoomed in
  from the full view, never out past it or off the world. Until it is clicked, the wheel
  scrolls the page.
- **Staying current.** While the map is on screen in a visible tab, the tiles in view are
  fetched again every five minutes.
- **When the map does not load.** Under the map lies the newest snapshot, visible while
  the map loads. The page asks the map site for a small picture first; if that fails,
  nothing live is loaded and the card shows the site's own copy of the map: the real
  map, some hours or days old, sharp and zoomable in the same way. The line under the
  card gives the time of the newest picture. The page tries the live map again every
  minute and goes back to it when it answers.

The "Realismo" section shows part of the same real map as a still picture, with a long
journey drawn on it (`src/scripts/map-still.ts`). The picture is a photograph of that
part of the map which the Worker takes and keeps (the "journey picture", see below), so
it is there whether or not the live map is up; the map's own tiles are drawn over it when
the build has verified them. It is not interactive. Which part of the world it shows is
`site.journey` in `src/config/site.ts`.

No picture on the site is generated or drawn by hand: every map shown is the server's
own, live or as last photographed.

### The pictures the site keeps

The Worker in `worker/` takes three kinds of picture of the live map and keeps them in
Cloudflare KV, where they stay when the map is down.

**Snapshots.** Every 30 minutes (the cron trigger in
`wrangler.jsonc`) it opens Dynmap's page in Cloudflare's hosted browser, framed so the
world fills the picture, waits for the tiles and stores a JPEG in Cloudflare KV. It keeps the newest three and
deletes older ones. If the map is down it stores nothing, so the last good picture stays.

**The journey picture.** Once a day the same run also photographs only the part of the world
in the "Realismo" figure, two zoom levels further in (1536 x 1152 pixels with the usual
map; cut from the whole-world snapshot that part would be 384 x 288 and blurred when
shown). It is taken again at once when the site changes what the figure shows. The run
waits for every tile and stores nothing if the map opens at another zoom level than
asked, so a half-loaded or wrongly framed picture is never kept. Until the first journey
picture exists (up to 30 minutes after the first deploy), the figure shows that part of
the whole-world snapshot.

**The copy of the map** (`worker/copy.ts`). This is what makes the map zoomable when the
live one is down. It is a pyramid of pictures ("sheets"), all the size of the snapshot:

| Level | Sheets | Detail |
|---|---|---|
| 0 | 1 (the snapshot itself) | the whole world in one picture |
| 1 | 2 x 2 | twice the detail |
| 2 | 4 x 4 | 4 times |
| 3 | 8 x 8 | 8 times |
| 4 | 16 x 16 | 16 times |

Each run photographs up to four sheets: first the ones that are missing, shallow levels
first, then the oldest. So after the first deploy the copy fills up level by level (with a
map that answers quickly: level 2 within about three hours, level 3 within eleven, level 4
within two days) and is then renewed sheet by sheet, each about every two days. Nothing is ever deleted, so the copy stays
when the map is down. Visitors can zoom as far as the deepest level that is whole.

A complete copy at every zoom level of the live map would be hundreds of thousands of
tiles and several gigabytes, more than Cloudflare's free storage holds and far more than
the hosted browser could photograph; level 4 (`COPY_LEVELS` in `worker/copy.ts`) is the
practical limit on the free plan. If the map itself cannot zoom that far, the Worker
notices and the copy ends where the map does.

| Address | What it is |
|---|---|
| `/api/map-snapshot.json` | When the newest snapshot was taken and its address; the same for the journey picture, under `journey`; which sheets of the copy exist, under `copy` |
| `/api/map-snapshot/<time>.jpg` | A whole-world snapshot |
| `/api/map-journey/<time>.jpg`, `/api/map-journey/latest.jpg` | A journey picture; the newest one |
| `/api/map-copy/<level>/<col>_<row>.jpg` | One sheet of the copy |
| `/api/map-view.json` | What the last build found out about the map (tiles, framing) |

On Cloudflare's free plan the hosted browser may run 10 minutes a day. One browser
serves a whole run. The snapshot always comes first; the copy's sheets get at most five
seconds per run, and none once the day's use passes seven minutes (the Worker keeps
count), so the snapshots are never starved.

`npm test` runs the Worker's tests (storage, deleting old ones, the addresses, when the
journey picture is due and how it is framed, which sheets of the copy come next).
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
