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
});
