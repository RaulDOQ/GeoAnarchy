// The store's plans and prices. Change a value here and the "Loja" page follows.
// The texts around them are in src/content/pt.ts, under `store`.

export const store = {
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
