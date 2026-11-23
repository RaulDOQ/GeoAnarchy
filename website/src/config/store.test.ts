// Run with `npm test`: the store's prices.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { periodPrice, reais, store } from './store.ts';

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

test('money is written the Brazilian way', () => {
  assert.equal(reais(799), '7,99');
  assert.equal(reais(10076), '100,76');
  assert.equal(reais(123456), '1.234,56');
});
