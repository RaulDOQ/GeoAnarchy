// Run with `npm test`: the store's prices.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { goalProgress, periodPrice, reais, store } from './store.ts';

test('the three plans and what a month costs', () => {
  assert.deepEqual(
    store.plans.map((plan) => [plan.name, reais(plan.monthly)]),
    [['Geo+', '9,90'], ['Geo Ultra', '19,90'], ['Geo Ultra+', '29,90']],
  );
});

test('three months take 15% off, six months 30%', () => {
  const table = store.plans.map((plan) => store.periods.map((period) => reais(periodPrice(plan.monthly, period).total)));
  assert.deepEqual(table, [
    ['9,90', '25,25', '41,58'],
    ['19,90', '50,75', '83,58'],
    ['29,90', '76,25', '125,58'],
  ]);
});

test('the price per month falls with every longer period', () => {
  const perMonth = store.plans.map((plan) => store.periods.map((period) => reais(periodPrice(plan.monthly, period).perMonth)));
  assert.deepEqual(perMonth, [
    ['9,90', '8,42', '6,93'],
    ['19,90', '16,92', '13,93'],
    ['29,90', '25,42', '20,93'],
  ]);
});

test('what is charged is never more than the stated discount allows', () => {
  for (const plan of store.plans) {
    for (const period of store.periods) {
      const price = periodPrice(plan.monthly, period);
      assert.equal(price.full, plan.monthly * period.months);
      // Rounded to the centavo, so within half a centavo of the exact figure.
      assert.ok(Math.abs(price.total - (price.full * (100 - period.off)) / 100) <= 0.5);
    }
  }
});

test('the monthly goal is R$ 200, and nothing has come in yet', () => {
  assert.equal(reais(store.goal.amount), '200,00');
  assert.deepEqual(goalProgress(store.goal), { left: 20000, over: 0, percent: 0 });
});

test('the goal counts down to zero, then counts what came in beyond it', () => {
  const goal = (raised: number) => goalProgress({ amount: 20000, raised });
  assert.deepEqual(goal(5000), { left: 15000, over: 0, percent: 25 });
  assert.deepEqual(goal(19999), { left: 1, over: 0, percent: 99.995 });
  assert.deepEqual(goal(20000), { left: 0, over: 0, percent: 100 });
  // Past the goal the bar stays full and the rest is what goes back into the server.
  assert.deepEqual(goal(26550), { left: 0, over: 6550, percent: 100 });
  assert.deepEqual(goal(-10), { left: 20000, over: 0, percent: 0 });
});

test('money is written the Brazilian way', () => {
  assert.equal(reais(799), '7,99');
  assert.equal(reais(10076), '100,76');
  assert.equal(reais(123456), '1.234,56');
});
