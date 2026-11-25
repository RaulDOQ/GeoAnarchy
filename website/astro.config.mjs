// @ts-check
import { defineConfig } from 'astro/config';

// Static site: every page is prerendered to plain HTML, CSS and JS in dist/.
export default defineConfig({
  site: 'https://geoanarchy.net',
  output: 'static',
  trailingSlash: 'always',
  build: {
    format: 'directory',
  },
  // Articles that moved. The founders' country articles were in the players' wiki until
  // 2026-11-25; old links (shared on Discord, say) still lead to them.
  redirects: {
    '/wiki/paises/tutorial': '/wiki/pais/paises/tutorial/',
    '/wiki/paises/fundar-e-evoluir': '/wiki/pais/paises/fundar-e-evoluir/',
    '/wiki/paises/territorio': '/wiki/pais/paises/territorio/',
    '/wiki/paises/guerras': '/wiki/pais/paises/guerras/',
  },
  // The wiki's articles are Markdown. Their code blocks are commands a player types, so
  // they are left as plain text for the site's own styles (src/styles/wiki.css).
  markdown: {
    syntaxHighlight: false,
  },
});
