// The GeoWiki's articles are Markdown files: src/content/wiki/<wiki>/<section>/<article>.md.
// <wiki> is "jogador" (players) or "pais" (country founders). src/config/wiki.ts turns
// them into addresses and the side menu.
import { defineCollection } from 'astro:content';
import { glob } from 'astro/loaders';
import { z } from 'astro/zod';

const wiki = defineCollection({
  loader: glob({ pattern: '**/*.md', base: './src/content/wiki' }),
  schema: z.object({
    /** The article's name, shown as its heading and in the side menu. */
    title: z.string(),
    /** One or two sentences under the heading; also the page's description. */
    summary: z.string(),
    /** Position inside its section, lowest first. */
    order: z.number(),
  }),
});

export const collections = { wiki };
