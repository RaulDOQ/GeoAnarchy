// The store: its categories, the plans and their prices, the tags, and where each thing
// is sold at CentralCart (`checkout`, at the end). Change a value here and the "Loja"
// pages follow. The texts around them are in src/content/pt.ts, under `store`.

export const store = {
  /**
   * The store's categories, in the order of their tabs. Each one is a page under /loja/.
   * To add one: an entry here, its tab name in pt.store.categories, and its page in
   * src/pages/loja/ drawn inside the StoreShell component (src/components/store/).
   */
  categories: [
    { id: 'vip', path: '/loja/' },
    { id: 'skins', path: '/loja/skins/' },
    { id: 'tags', path: '/loja/tags/' },
  ],

  /** The monthly goal, in centavos. */
  goal: {
    /** What it takes to keep the server running for a month. */
    amount: 20000,
    /**
     * What has come in this month. Nothing can be bought yet, so nothing has. Until
     * payments are connected this number is kept by hand: raise it as money comes in,
     * and set it back to 0 when a new month starts.
     */
    raised: 0,
  },

  /** The plans, cheapest first. `monthly` is the price of one month, in centavos. */
  plans: [
    { id: 'geo-plus', name: 'Geo+', monthly: 990 },
    { id: 'geo-ultra', name: 'Geo Ultra', monthly: 1990 },
    { id: 'geo-ultra-plus', name: 'Geo Ultra+', monthly: 2990 },
  ],

  /** How often a plan can be paid, and the discount for paying more months at once (in percent). */
  periods: [
    { months: 1, off: 0 },
    { months: 3, off: 15 },
    { months: 6, off: 30 },
  ],
} as const;

export type Plan = (typeof store.plans)[number];
export type PlanId = Plan['id'];

/**
 * One tag, as GeoUltra has it in plugins/GeoUltra/presets.yml: the word, the plan that
 * includes it, and its colours (one colour, or two for a gradient from the first to the
 * second). Keep this list in step with presets.yml.
 */
export interface StoreTag {
  word: string;
  plan: PlanId;
  colors: readonly string[];
  bold?: boolean;
  /** This tag's own price, in centavos, where it differs from `tagPrice`. */
  price?: number;
}

/** What buying one tag on its own costs, for good, in centavos. */
export const tagPrice = 790;

/** What one tag costs, in centavos. */
export function tagPriceOf(tag: StoreTag): number {
  return tag.price ?? tagPrice;
}

/** The kinds of item a skin pack dresses, as GeoUltra names them, in the order they are shown. */
export const skinItems = {
  tools: ['sword', 'pickaxe', 'axe', 'shovel', 'hoe'],
  armour: ['helmet', 'chestplate', 'leggings', 'boots'],
} as const;
export type SkinItem = (typeof skinItems)['tools' | 'armour'][number];

/**
 * The four ways to buy a pack's skins, each with its own price: one item's skin of
 * your choice, the sword and the tools, the armour, or the whole pack.
 */
export const skinOptions = [
  { id: 'single', items: 1 },
  { id: 'tools', items: skinItems.tools.length },
  { id: 'armour', items: skinItems.armour.length },
  { id: 'pack', items: skinItems.tools.length + skinItems.armour.length },
] as const;
export type SkinOptionId = (typeof skinOptions)[number]['id'];

/** What each way of buying skins costs, in centavos, for every pack (a pack can set its own `prices`). */
export const skinPrices: Record<SkinOptionId, number> = {
  single: 1990,
  tools: 3990,
  armour: 5999,
  pack: 7599,
};

/** What one way of buying a pack's skins costs, in centavos. */
export function skinPrice(pack: SkinPack, option: SkinOptionId): number {
  return pack.prices?.[option] ?? skinPrices[option];
}

/**
 * One skin pack, as GeoUltra has it (config.yml, skins.packs): its name, the plan that
 * includes it, and the colour it is shown in. The icons are in public/loja/skins/<id>/,
 * one per item; in-game screenshots go in src/assets/skins/<id>/ and show up by themselves.
 */
export interface SkinPack {
  id: string;
  name: string;
  plan: PlanId;
  color: string;
  /** This pack's own prices, in centavos, where they differ from `skinPrices`. */
  prices?: Partial<Record<SkinOptionId, number>>;
}

/** Every pack, cheapest plan first. */
export const skinPacks: readonly SkinPack[] = [
  { id: 'silvestre', name: 'Silvestre', plan: 'geo-plus', color: '#69d37a' },
  { id: 'glacial', name: 'Glacial', plan: 'geo-plus', color: '#5fd6f0' },
  { id: 'draconico', name: 'Dracônico', plan: 'geo-ultra', color: '#ff8150' },
  { id: 'abissal', name: 'Abissal', plan: 'geo-ultra', color: '#c897ff' },
  { id: 'celestial', name: 'Celestial', plan: 'geo-ultra-plus', color: '#f5c542' },
];

/** Every tag, cheapest plan first. Geo+ one colour, Geo Ultra bold, Geo Ultra+ bold gradients. */
export const tags: readonly StoreTag[] = [
  { word: 'CRIA', plan: 'geo-plus', colors: ['#55ff55'] },
  { word: 'MITO', plan: 'geo-plus', colors: ['#ffaa00'] },
  { word: 'TRYHARD', plan: 'geo-plus', colors: ['#ff5555'] },
  { word: 'NÔMADE', plan: 'geo-plus', colors: ['#d2b48c'] },
  { word: 'REBELDE', plan: 'geo-plus', colors: ['#ff7043'] },
  { word: 'PIRATA', plan: 'geo-plus', colors: ['#5aa9ff'] },
  { word: 'SABONETE', plan: 'geo-plus', colors: ['#7fe3ff'] },
  { word: 'CALABRESO', plan: 'geo-plus', colors: ['#ff8a65'] },
  { word: '67', plan: 'geo-plus', colors: ['#ffd54f'] },
  { word: 'AURA', plan: 'geo-ultra', colors: ['#c77dff'], bold: true },
  { word: 'EGO', plan: 'geo-ultra', colors: ['#ff4fa3'], bold: true },
  { word: 'RESENHA', plan: 'geo-ultra', colors: ['#ffd23f'], bold: true },
  { word: 'GOTH', plan: 'geo-ultra', colors: ['#d94f70'], bold: true },
  { word: 'CHEFE', plan: 'geo-ultra', colors: ['#ffb300'], bold: true },
  { word: 'MONSTRO', plan: 'geo-ultra', colors: ['#76ff03'], bold: true },
  { word: 'SIGMA', plan: 'geo-ultra-plus', colors: ['#e0e0e0', '#8ab4ff'], bold: true },
  { word: 'ALPHA', plan: 'geo-ultra-plus', colors: ['#ff512f', '#dd2476'], bold: true },
  { word: 'ÍCONE', plan: 'geo-ultra-plus', colors: ['#c77dff', '#ff4fa3'], bold: true },
  { word: 'MOGGER', plan: 'geo-ultra-plus', colors: ['#00f5a0', '#00d9f5'], bold: true },
  { word: 'RIZZ', plan: 'geo-ultra-plus', colors: ['#ff6a88', '#ffc371'], bold: true },
  { word: 'GOAT', plan: 'geo-ultra-plus', colors: ['#f7971e', '#ffd200'], bold: true },
  { word: 'PRESIDENTE', plan: 'geo-ultra-plus', colors: ['#3ddc84', '#ffd23f'], bold: true },
  { word: 'MONARCA', plan: 'geo-ultra-plus', colors: ['#f5c542', '#a855f7'], bold: true },
  { word: 'GENERAL', plan: 'geo-ultra-plus', colors: ['#9acd32', '#c2b280'], bold: true },
  { word: 'MAGNATA', plan: 'geo-ultra-plus', colors: ['#ffd700', '#fff6c2'], bold: true },
  { word: 'LENDA', plan: 'geo-ultra-plus', colors: ['#a6c1ee', '#fbc2eb'], bold: true },
  { word: 'FENÔMENO', plan: 'geo-ultra-plus', colors: ['#f953c6', '#b91d73'], bold: true },
  { word: 'IMBATÍVEL', plan: 'geo-ultra-plus', colors: ['#ff416c', '#ff4b2b'], bold: true },
  { word: 'INTOCÁVEL', plan: 'geo-ultra-plus', colors: ['#43e97b', '#38f9d7'], bold: true },
  { word: 'BOSS', plan: 'geo-ultra-plus', colors: ['#f12711', '#f5af19'], bold: true },
];
export type Period = (typeof store.periods)[number];

/** What one payment of a plan comes to, in centavos. */
export interface PeriodPrice {
  /** The amount charged each period: the monthly price times the months, less the discount. */
  total: number;
  /** What that is per month (rounded: it is for comparing, the total is what is charged). */
  perMonth: number;
  /** The same months at the monthly price, without the discount. */
  full: number;
}

export function periodPrice(monthly: number, period: { months: number; off: number }): PeriodPrice {
  const full = monthly * period.months;
  const total = Math.round((full * (100 - period.off)) / 100);
  return { total, perMonth: Math.round(total / period.months), full };
}

/** Where the month stands against its goal, in centavos. */
export interface GoalProgress {
  /** What is still missing (0 once the goal is reached). */
  left: number;
  /** What came in beyond the goal (0 until then). */
  over: number;
  /** How much of the goal is covered, 0 to 100. */
  percent: number;
}

export function goalProgress(goal: { amount: number; raised: number }): GoalProgress {
  const raised = Math.max(0, goal.raised);
  return {
    left: Math.max(0, goal.amount - raised),
    over: Math.max(0, raised - goal.amount),
    percent: goal.amount > 0 ? Math.min(100, (raised / goal.amount) * 100) : 100,
  };
}

/** Centavos as Brazilians write money, without the currency sign: 799 is "7,99". */
export function reais(centavos: number): string {
  return (centavos / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// ------------------------------------------------------------------ Buying

/**
 * Each thing on sale and its product's id at CentralCart (in its panel, or from
 * GET /webstore/package). null: not created there yet, and the page says "Em breve"
 * instead of "Comprar". VIP plans by the months paid at once.
 */
export interface CheckoutProducts {
  vip: Record<PlanId, Record<number, number | null>>;
  skins: Record<SkinOptionId, number | null>;
  tag: number | null;
}

/**
 * Where purchases happen: the GeoAnarchy store at CentralCart. The pages call its public
 * Storefront API from the visitor's browser (src/scripts/buy.ts). There is no key: the
 * store is named by its domain. The buyer pays by Pix on the page itself, and CentralCart's
 * plugin on the server runs the product's command once the payment is approved (or when
 * the player next joins, if they were offline).
 *
 * The choices inside a product (which pack, which item, which tag) are found by their
 * "Valor" at CentralCart, which must be what these pages use: the pack's id (silvestre),
 * the item's name in GeoUltra (espada, see skinItemValue) and the tag's word without
 * accents, in small letters (nomade, see choiceValue).
 */
export const checkout = {
  api: 'https://api.centralcart.io/v1',
  /** The store's own address at CentralCart: it names the store to the API. */
  storeDomain: 'geoanarchy.centralcart.ai',
  /** The store's terms, which the buyer accepts before paying. */
  termsUrl: 'https://geoanarchy.centralcart.ai/terms',
  products: {
    vip: {
      'geo-plus': { 1: 1148243, 3: 1148246, 6: 1148251 },
      'geo-ultra': { 1: 1148253, 3: 1148258, 6: 1148260 },
      'geo-ultra-plus': { 1: 1148262, 3: 1148265, 6: 1148266 },
    },
    skins: { single: 1148269, tools: 1148271, armour: 1148274, pack: 1148278 },
    tag: 1148089,
  } as CheckoutProducts,
};

/** A word as CentralCart's choices name it: without accents, in small letters ("NÔMADE" is "nomade"). */
export function choiceValue(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}

/** Each skin item's name in GeoUltra's commands (/geoultra skin dar), which is also its Valor at CentralCart. */
export const skinItemValue: Record<SkinItem, string> = {
  sword: 'espada',
  pickaxe: 'picareta',
  axe: 'machado',
  shovel: 'pa',
  hoe: 'enxada',
  helmet: 'capacete',
  chestplate: 'peitoral',
  leggings: 'calca',
  boots: 'botas',
};

/** One "Comprar" button: what it buys, and how the buy dialog shows it. */
export interface BuySpec {
  /** The product's id at CentralCart. */
  product: number;
  kind: 'vip' | 'skin' | 'tag';
  /** What the dialog calls it: "Geo Ultra, 3 meses", "Tag SIGMA". */
  title: string;
  /** The choices inside the product, by their Valor at CentralCart. */
  choices: string[];
  /** A single skin of this pack: the buyer picks the item in the dialog. */
  pickItemOf?: string;
  /** The page's price, in centavos, shown until CentralCart's own arrives. */
  price: number;
}

/** The button for paying a plan for some months at once, or null while that product isn't on sale. */
export function vipBuy(plan: Plan, period: Period, title: string): BuySpec | null {
  const product = checkout.products.vip[plan.id]?.[period.months] ?? null;
  return product === null ? null : { product, kind: 'vip', title, choices: [], price: periodPrice(plan.monthly, period).total };
}

/** The button for one way of buying a pack's skins, or null while that product isn't on sale. */
export function skinBuy(pack: SkinPack, option: SkinOptionId, title: string): BuySpec | null {
  const product = checkout.products.skins[option];
  if (product === null) return null;
  const spec: BuySpec = { product, kind: 'skin', title, choices: [pack.id], price: skinPrice(pack, option) };
  if (option === 'single') spec.pickItemOf = pack.id;
  return spec;
}

/** The button for buying one tag for good, or null while tags aren't on sale. */
export function tagBuy(tag: StoreTag, title: string): BuySpec | null {
  const product = checkout.products.tag;
  return product === null ? null : { product, kind: 'tag', title, choices: [choiceValue(tag.word)], price: tagPriceOf(tag) };
}

/** True once something in a category can be bought. */
export function onSale(category: 'vip' | 'skins' | 'tags'): boolean {
  const { vip, skins, tag } = checkout.products;
  if (category === 'tags') return tag !== null;
  const ids = category === 'vip' ? Object.values(vip).flatMap((months) => Object.values(months)) : Object.values(skins);
  return ids.some((id) => id !== null);
}
