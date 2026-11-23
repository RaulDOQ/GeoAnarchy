// The store's plans and prices. Change a value here and the "Loja" page follows.
// The texts around them are in src/content/pt.ts, under `store`.

export const store = {
  /** False while the plans cannot be bought yet: the page then says "Em breve". */
  open: false,

  /** The plans, cheapest first. `monthly` is the price of one month, in centavos. */
  plans: [
    { id: 'geo-plus', name: 'Geo+', monthly: 799 },
    { id: 'geo-ultra', name: 'Geo Ultra', monthly: 1799 },
    { id: 'geo-ultra-plus', name: 'Geo Ultra+', monthly: 2799 },
  ],

  /** How often a plan can be paid, and the discount for paying more months at once (in percent). */
  periods: [
    { months: 1, off: 0 },
    { months: 3, off: 20 },
    { months: 6, off: 40 },
  ],
} as const;

export type Plan = (typeof store.plans)[number];
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

/** Centavos as Brazilians write money, without the currency sign: 799 is "7,99". */
export function reais(centavos: number): string {
  return (centavos / 100).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
