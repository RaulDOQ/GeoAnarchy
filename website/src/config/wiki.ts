// The GeoWiki's shape. There are two wikis: one for players and one for country founders.
// Each has sections of articles, which are the Markdown files in src/content/wiki, laid
// out as <wiki>/<section>/<article>.md. This file turns them into addresses, the side
// menu and the "previous / next" links.
import { getCollection, type CollectionEntry } from 'astro:content';
import { pt } from '../content/pt';

export type WikiId = 'jogador' | 'pais';
export type WikiArticle = CollectionEntry<'wiki'>;

/** Where each wiki starts. */
export const wikiBase: Record<WikiId, string> = {
  jogador: '/wiki/',
  pais: '/wiki/pais/',
};

/** The order of the sections in each wiki's side menu. A new folder also needs a name in pt.ts (`wiki.sections`). */
const sectionOrder: Record<WikiId, readonly string[]> = {
  jogador: ['economia'],
  pais: ['economia'],
};

interface Place {
  wiki: WikiId;
  section: string;
  slug: string;
}

function place(article: WikiArticle): Place {
  const [wiki, section, slug] = article.id.split('/');
  if ((wiki !== 'jogador' && wiki !== 'pais') || !section || !slug) {
    throw new Error(`Wiki article in the wrong folder: src/content/wiki/${article.id}.md (expected <jogador|pais>/<section>/<article>.md)`);
  }
  return { wiki, section, slug };
}

export const articleWiki = (article: WikiArticle): WikiId => place(article).wiki;

export function sectionTitle(section: string): string {
  return (pt.wiki.sections as Record<string, string>)[section] ?? section;
}

export const articleSectionTitle = (article: WikiArticle): string => sectionTitle(place(article).section);

/** The part of the address after /wiki/: "economia/dinheiro", or "pais/economia/criar-moeda". */
export function articlePath(article: WikiArticle): string {
  const { wiki, section, slug } = place(article);
  return wiki === 'jogador' ? `${section}/${slug}` : `pais/${section}/${slug}`;
}

export const articleHref = (article: WikiArticle): string => `/wiki/${articlePath(article)}/`;

/** A wiki's articles in reading order: section by section, then by each article's `order`. */
export async function wikiArticles(wiki: WikiId): Promise<WikiArticle[]> {
  const order = sectionOrder[wiki];
  const rank = (section: string) => {
    const index = order.indexOf(section);
    return index < 0 ? order.length : index;
  };
  const articles = (await getCollection('wiki')).filter((article) => place(article).wiki === wiki);
  return articles.sort((a, b) => {
    const [pa, pb] = [place(a), place(b)];
    return rank(pa.section) - rank(pb.section) || pa.section.localeCompare(pb.section) || a.data.order - b.data.order;
  });
}

export async function allWikiArticles(): Promise<WikiArticle[]> {
  return [...(await wikiArticles('jogador')), ...(await wikiArticles('pais'))];
}

/** A wiki's articles of one section, in order. */
export async function sectionArticles(wiki: WikiId, section: string): Promise<WikiArticle[]> {
  return (await wikiArticles(wiki)).filter((article) => place(article).section === section);
}

export interface WikiLink {
  label: string;
  href: string;
  current: boolean;
}

export interface WikiGroup {
  title: string;
  links: WikiLink[];
}

/** The side menu of a wiki. `current` is the address of the page being shown. */
export async function wikiMenu(wiki: WikiId, current?: string): Promise<WikiGroup[]> {
  const groups: WikiGroup[] = [];
  if (wiki === 'jogador') {
    // The first articles live on the wiki's front page.
    const c = pt.wiki;
    groups.push({
      title: c.sections.start,
      links: [c.join, c.world, c.rules].map((article) => ({
        label: article.title,
        href: `${wikiBase.jogador}#${article.id}`,
        current: false,
      })),
    });
  }
  for (const article of await wikiArticles(wiki)) {
    const title = articleSectionTitle(article);
    let group = groups.find((candidate) => candidate.title === title);
    if (!group) {
      group = { title, links: [] };
      groups.push(group);
    }
    const href = articleHref(article);
    group.links.push({ label: article.data.title, href, current: href === current });
  }
  return groups;
}

/** The articles before and after this one in its wiki. */
export async function wikiNeighbours(article: WikiArticle): Promise<{ previous?: WikiArticle; next?: WikiArticle }> {
  const articles = await wikiArticles(articleWiki(article));
  const index = articles.findIndex((candidate) => candidate.id === article.id);
  return { previous: articles[index - 1], next: articles[index + 1] };
}
