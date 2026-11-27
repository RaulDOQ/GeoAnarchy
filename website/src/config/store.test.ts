// Run with `npm test`: the store's prices.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  checkout,
  choiceValue,
  goalProgress,
  onSale,
  periodPrice,
  reais,
  skinBuy,
  skinItems,
  skinItemValue,
  skinOptions,
  skinPacks,
  skinPrice,
  store,
  tagBuy,
  tagPriceOf,
  tags,
  vipBuy,
} from './store.ts';

test('the store has VIP, Skins and Tags categories, each a page under /loja/', () => {
  assert.deepEqual(store.categories.map((category) => category.id), ['vip', 'skins', 'tags']);
  assert.ok(store.categories.every((category) => category.path.startsWith('/loja/') && category.path.endsWith('/')));
});

test('five skin packs, nine items each, sold four ways', () => {
  assert.deepEqual(skinPacks.map((pack) => pack.id), ['silvestre', 'glacial', 'draconico', 'abissal', 'celestial']);
  assert.deepEqual(skinPacks.map((pack) => pack.plan), ['geo-plus', 'geo-plus', 'geo-ultra', 'geo-ultra', 'geo-ultra-plus']);
  assert.equal(skinItems.tools.length + skinItems.armour.length, 9);
  assert.deepEqual(skinOptions.map((option) => [option.id, option.items]), [['single', 1], ['tools', 5], ['armour', 4], ['pack', 9]]);
});

test('skins cost 19,90 one item, 39,90 sword and tools, 59,99 armour, 75,99 the whole pack', () => {
  for (const pack of skinPacks) {
    assert.deepEqual(skinOptions.map((option) => reais(skinPrice(pack, option.id))), ['19,90', '39,90', '59,99', '75,99'], pack.id);
  }
});

test('a tag on its own costs 7,90', () => {
  assert.ok(tags.every((tag) => reais(tagPriceOf(tag)) === '7,90'));
});

test('the tags are the 30 of the game: 9 with Geo+, 6 more with Geo Ultra, 15 more with Geo Ultra+', () => {
  const count = (plan: string) => tags.filter((tag) => tag.plan === plan).length;
  assert.deepEqual([count('geo-plus'), count('geo-ultra'), count('geo-ultra-plus')], [9, 6, 15]);
  assert.equal(new Set(tags.map((tag) => tag.word)).size, tags.length, 'no tag twice');
  for (const tag of tags) {
    assert.equal(tag.word, tag.word.toUpperCase(), tag.word + ' is written in capitals');
    assert.ok(tag.colors.every((color) => /^#[0-9a-f]{6}$/.test(color)), tag.word);
    // Geo+ one colour, Geo Ultra bold, Geo Ultra+ a bold gradient: the same as in the game.
    assert.equal(tag.colors.length, tag.plan === 'geo-ultra-plus' ? 2 : 1, tag.word);
    assert.equal(tag.bold === true, tag.plan !== 'geo-plus', tag.word);
  }
});

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

test('the store at CentralCart is named by its domain, and every product id is a whole number or not there yet', () => {
  assert.equal(checkout.storeDomain, 'geoanarchy.centralcart.ai');
  assert.ok(checkout.api.startsWith('https://'));
  const { vip, skins, tag } = checkout.products;
  assert.deepEqual(Object.keys(vip).sort(), store.plans.map((plan) => plan.id).sort());
  for (const plan of store.plans) {
    assert.deepEqual(Object.keys(vip[plan.id]).map(Number), store.periods.map((period) => period.months), plan.id);
  }
  assert.deepEqual(Object.keys(skins), skinOptions.map((option) => option.id));
  const ids = [...Object.values(vip).flatMap((months) => Object.values(months)), ...Object.values(skins), tag];
  assert.ok(ids.every((id) => id === null || (Number.isInteger(id) && id > 0)));
  assert.equal(new Set(ids.filter((id) => id !== null)).size, ids.filter((id) => id !== null).length, 'no product used twice');
});

test('a choice is the word without accents in small letters, and every tag has its own', () => {
  assert.equal(choiceValue('NÔMADE'), 'nomade');
  assert.equal(choiceValue('ÍCONE'), 'icone');
  assert.equal(choiceValue('IMBATÍVEL'), 'imbativel');
  assert.equal(choiceValue('67'), '67');
  const values = tags.map((tag) => choiceValue(tag.word));
  assert.ok(values.every((value) => /^[a-z0-9]+$/.test(value)), values.join(' '));
  assert.equal(new Set(values).size, values.length);
});

test('the skin items go by the names GeoUltra\'s /geoultra skin dar takes', () => {
  assert.deepEqual(
    [...skinItems.tools, ...skinItems.armour].map((item) => skinItemValue[item]),
    ['espada', 'picareta', 'machado', 'pa', 'enxada', 'capacete', 'peitoral', 'calca', 'botas'],
  );
});

test('a button sells what is on sale at CentralCart, at the page\'s price, and nothing else', () => {
  const ultra = store.plans[1];
  const sixMonths = store.periods[2];
  const plan = vipBuy(ultra, sixMonths, 'Geo Ultra, 6 meses');
  assert.equal(plan === null, checkout.products.vip[ultra.id][6] === null);
  if (plan) assert.equal(plan.price, periodPrice(ultra.monthly, sixMonths).total);

  const sigma = tags.find((tag) => tag.word === 'SIGMA');
  assert.ok(sigma);
  const tag = tagBuy(sigma, 'Tag SIGMA');
  assert.equal(tag === null, checkout.products.tag === null);
  if (tag) assert.deepEqual([tag.kind, tag.choices, tag.price], ['tag', ['sigma'], 790]);

  const single = skinBuy(skinPacks[0], 'single', 'Skin avulsa');
  if (single) assert.deepEqual([single.choices, single.pickItemOf], [['silvestre'], 'silvestre']);
  const whole = skinBuy(skinPacks[4], 'pack', 'Pacote inteiro');
  if (whole) assert.deepEqual([whole.choices, whole.pickItemOf, whole.price], [['celestial'], undefined, 7599]);

  assert.equal(onSale('tags'), checkout.products.tag !== null);
});
