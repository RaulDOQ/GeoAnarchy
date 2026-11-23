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
| `src/components/home/` | The page sections: hero, live map window, server, map facts, how to join, questions, closing. |
| `src/scripts/` | Browser scripts: live map loading and pointer guard, window tilt, scroll entrances, copy buttons. |

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

## Deploying

Any static host works. For Cloudflare Pages: root directory `website`, build command
`npm run build`, output directory `dist`.
