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
  // The wiki's articles are Markdown. Their code blocks are commands a player types, so
  // they are left as plain text for the site's own styles (src/styles/wiki.css).
  markdown: {
    syntaxHighlight: false,
  },
});
