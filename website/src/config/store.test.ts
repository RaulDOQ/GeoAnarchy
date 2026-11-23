// Run with `npm test`: the store's prices.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { goalProgress, periodPrice, reais, store } from './store.ts';

test('the three plans and what a month costs', () => {
  assert.deepEqual(
    store.plans.map((plan) => [plan.name, reais(plan.monthly)]),
    [['Geo+', '7,99'], ['Geo Ultra', '17,99'], ['Geo Ultra+', '27,99']],
  );
});

test('three months take 20% off, six months 40%', () => {
  const table = store.plans.map((plan) => store.periods.map((period) => reais(periodPrice(plan.monthly, period).total)));
  assert.deepEqual(table, [
    ['7,99', '19,18', '28,76'],
    ['17,99', '43,18', '64,76'],
    ['27,99', '67,18', '100,76'],
  ]);
});

test('the price per month falls with every longer period', () => {
  const perMonth = store.plans.map((plan) => store.periods.map((period) => reais(periodPrice(plan.monthly, period).perMonth)));
  assert.deepEqual(perMonth, [
    ['7,99', '6,39', '4,79'],
    ['17,99', '14,39', '10,79'],
    ['27,99', '22,39', '16,79'],
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
