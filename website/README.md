# GeoAnarchy website

The public site for the GeoAnarchy Minecraft server: one page in Brazilian Portuguese
with the pitch, a live map of the server, how to join and common questions. It is a
static [Astro](https://astro.build) site, so the build output is plain HTML, CSS and
JavaScript that any static host can serve.

## Commands

Run these inside `website/` (Node 22.12 or newer):

| Command | What it does |
|---|---|
| `npm install` | Installs dependencies |
| `npm run dev` | Local preview with live reload at `http://localhost:4321` |
| `npm run check` | Type-checks the pages and scripts |
| `npm run build` | Type-checks, then writes the finished site to `dist/` |
| `npm run preview` | Serves `dist/` locally, as a host would |

## Where things live

| Path | Contents |
|---|---|
| `src/config/site.ts` | Server addresses, the map address and its parameters, the Discord invite, world size. Change a value here and every page follows. |
| `src/content/pt.ts` | Every text on the site. |
| `src/styles/tokens.css` | Colours, type, spacing and motion values (white, blue and green theme). |
| `src/components/Logo.astro` | The logo, shown in the header, the footer and the closing section. |
| `src/components/home/` | The page sections: hero and live map window, anarchy, the block map sheet, realism and its journey map, how to join, questions, closing. |
| `src/scripts/` | Browser scripts: live map loading and pointer guard, window tilt, scroll entrances, copy buttons. |
| `src/assets/earth.png` | The block world map (144 x 72 pixels, one per 2.5 degrees), shown enlarged with sharp edges. |
| `src/assets/logo.webp`, `logo-small.webp`, `wordmark.webp` | The logo artwork, trimmed: the full logo in two sizes and the name alone. `public/favicon.png` and `public/apple-touch-icon.png` come from the same artwork. |
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

The hero shows the server's Dynmap site (`mapa.geoanarchy.net`) in an iframe, styled as
a floating window.

- It loads only when the window is near the screen.
- A guard sits over the map so scrolling the page never zooms it by accident. A click or
  tap hands the pointer to the map; leaving the window, or tapping outside it, gives the
  page its scroll back.
- If the map has not answered after 15 seconds, a link to open it directly appears.
- The Dynmap URL parameters (world, layer, hidden panels) are in `site.map.embedParams`.

The map must allow being shown inside another site: if Dynmap or Cloudflare ever sends
an `X-Frame-Options` or `Content-Security-Policy: frame-ancestors` header for
`mapa.geoanarchy.net`, it has to permit the website's address.

## Before going public

`site.inDevelopment` in `src/config/site.ts` is `true`: the page shows a "site em
desenvolvimento" strip and asks search engines not to index it. Set it to `false` at
launch.

## Deploy on Cloudflare

The site runs as a Cloudflare Worker that only serves the static files in `dist/`
(`wrangler.jsonc`). Cloudflare builds and publishes it from GitHub every time `main`
changes.

In the Cloudflare dashboard: **Workers & Pages → Create → Import a repository**, pick
`RaulDOQ/GeoAnarchy`, and set:

- **Project name:** `geoanarchy-website` (the name in `wrangler.jsonc`)
- **Production branch:** `main`
- **Path / root directory:** `website`
- **Build command:** `npm run build`
- **Deploy command:** `npx wrangler deploy`

The Node version comes from `.node-version`. There are no variables or secrets to add.

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
