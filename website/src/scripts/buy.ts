// The store's "Comprar" buttons and the buy window (src/components/store/BuyDialog.astro).
//
// A button carries what it sells (data-buy: a BuySpec from src/config/store.ts). The window
// asks for the buyer's nick, name and e-mail, creates the order at CentralCart through its
// public Storefront API, shows the Pix to pay right on the page, then asks CentralCart every
// few seconds whether it was paid. CentralCart's plugin on the server delivers in the game.
//
// The price in the window is CentralCart's own, the one charged. The choices inside a
// product (pack, item, tag) are found in CentralCart's catalogue by their Valor.

import type { BuySpec } from '../config/store';
import { copyText, showToast } from './copy';

interface Option {
  id: number;
  name: string;
  value: string;
}
interface Field {
  name: string;
  type?: string;
  required?: boolean;
  options?: Option[];
}
interface Product {
  id: number;
  enabled?: boolean;
  pricing?: { price?: number };
  stock?: { available?: boolean };
  fields?: Field[];
  variations?: Product[];
}
interface Gateway {
  gateway: string;
  require_document?: boolean;
  default_provider?: string;
  provider_requirements?: Record<string, { document?: boolean }>;
}
interface CheckoutReply {
  pix_code?: string;
  qr_code?: string;
  return_url?: string;
  checkout_url?: string;
  order_id?: string | number;
  order_token?: string;
  status?: string;
}
interface Texts {
  notes: Record<string, string>;
  deliverTo: string;
  done: string;
  doneNoNick: string;
  pay: string;
  paying: string;
  error: string;
  unavailable: string;
  pixCopied: string;
  pixCopyFailed: string;
}

type Step = 'form' | 'pix' | 'done' | 'failed' | 'message';

/** A Java nick (3 to 16 letters, numbers or _), or a Bedrock one: Floodgate's dot in front. */
const NICK = /^(?:\.[A-Za-z0-9_]{1,15}|[A-Za-z0-9_]{3,16})$/;
/** CentralCart takes names of letters and simple separators only. Two words at least. */
const NAME = /^\p{L}+(?:[ '-]\p{L}+)+$/u;
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const ENDED = ['REJECTED', 'CANCELED', 'REFUNDED', 'CHARGEDBACK'];
const POLL_EVERY = 4000;
const POLL_FOR = 45 * 60 * 1000;
/** The last nick bought for, so the next purchase starts with it (this browser only). */
const NICK_KEY = 'geoanarchy-loja-nick';

class StoreError extends Error {}

const plain = (text: string) => text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
const money = (reais: number) => `R$ ${reais.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function remembered(): string {
  try {
    return localStorage.getItem(NICK_KEY) ?? '';
  } catch {
    return '';
  }
}
function remember(nick: string): void {
  try {
    localStorage.setItem(NICK_KEY, nick);
  } catch {
    // Private windows and blocked storage: the nick is just typed again next time.
  }
}

/** The order's id, from the reply or from the return address CentralCart gives back. */
function orderIdOf(reply: CheckoutReply): string | null {
  if (reply.order_id !== undefined && reply.order_id !== null && String(reply.order_id) !== '') return String(reply.order_id);
  if (!reply.return_url) return null;
  try {
    return new URL(reply.return_url).searchParams.get('order_id');
  } catch {
    return null;
  }
}

export function initBuy(): void {
  const dialog = document.querySelector<HTMLDialogElement>('[data-buy-dialog]');
  if (!dialog || typeof dialog.showModal !== 'function') return;
  const box = dialog;
  const api = box.dataset.api ?? '';
  const storeDomain = box.dataset.store ?? '';
  const texts = JSON.parse(box.dataset.texts ?? '{}') as Texts;

  const $ = <T extends Element = HTMLElement>(selector: string) => box.querySelector<T>(selector)!;
  const form = $<HTMLFormElement>('form[data-step="form"]');
  const title = $('[data-buy-title]');
  const price = $('[data-buy-price]');
  const note = $('[data-buy-note]');
  const formError = $('[data-buy-error]');
  const payButton = $<HTMLButtonElement>('[data-buy-pay]');
  const itemsBox = $<HTMLFieldSetElement>('[data-buy-items]');
  const documentBox = $('[data-buy-document]');
  const qrWrap = $('[data-pix-qr-wrap]');
  const qr = $<HTMLImageElement>('[data-pix-qr]');
  const frame = $<HTMLIFrameElement>('[data-pix-frame]');
  const codeWrap = $('[data-pix-code-wrap]');
  const code = $('[data-pix-code]');
  const steps = $('[data-pix-steps]');
  const deliver = $('[data-pix-deliver]');
  const doneText = $('[data-done-text]');
  const messageText = $('[data-message-text]');
  const input = (name: string) => form.elements.namedItem(name) as HTMLInputElement;

  let spec: BuySpec | null = null;
  let pixCode = '';
  let pollRun = 0;
  let needsDocument = false;

  // --------------------------------------------------------------- CentralCart

  async function call<T>(path: string, body?: unknown): Promise<T> {
    let response: Response;
    try {
      response = await fetch(api + path, {
        method: body === undefined ? 'GET' : 'POST',
        headers: body === undefined ? { 'x-store-domain': storeDomain } : { 'x-store-domain': storeDomain, 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new StoreError(texts.error);
    }
    let data: unknown = null;
    try {
      data = await response.json();
    } catch {
      data = null;
    }
    if (!response.ok) {
      const message = (data as { errors?: { message?: string }[] } | null)?.errors?.[0]?.message;
      throw new StoreError(message || texts.error);
    }
    return data as T;
  }

  let catalogue: Promise<Product[]> | null = null;
  function loadCatalogue(): Promise<Product[]> {
    catalogue ??= (async () => {
      const all: Product[] = [];
      for (let page = 1; page <= 10; page++) {
        const reply = await call<{ data?: Product[]; meta?: { last_page?: number } }>(`/webstore/package?include_variations=true&limit=100&page=${page}`);
        for (const product of reply.data ?? []) all.push(product, ...(product.variations ?? []));
        if (!reply.meta?.last_page || page >= reply.meta.last_page) break;
      }
      return all;
    })().catch((error) => {
      catalogue = null;
      throw error;
    });
    return catalogue;
  }

  let gateway: Promise<Gateway | null> | null = null;
  function loadPix(): Promise<Gateway | null> {
    gateway ??= call<Gateway[]>('/webstore/gateway')
      .then((list) => list.find((each) => each.gateway === 'PIX') ?? null)
      .catch((error) => {
        gateway = null;
        throw error;
      });
    return gateway;
  }

  function productOf(list: Product[], current: BuySpec): Product | null {
    const product = list.find((each) => each.id === current.product);
    return product && product.enabled !== false && product.stock?.available !== false ? product : null;
  }

  /** The product's choices for these Valores, by field name and option id; null when one is missing. */
  function choicesOf(product: Product, wanted: string[]): Record<string, string> | null {
    const chosen: Record<string, string> = {};
    for (const value of wanted) {
      const field = product.fields?.find((each) => each.options?.some((option) => plain(option.value) === value));
      const option = field?.options?.find((each) => plain(each.value) === value);
      if (!field || !option) return null;
      chosen[field.name] = String(option.id);
    }
    // A required choice the page doesn't make can't be bought from here.
    if (product.fields?.some((field) => field.required && !(field.name in chosen))) return null;
    return chosen;
  }

  // --------------------------------------------------------------- The window

  function show(step: Step): void {
    box.querySelectorAll<HTMLElement>('[data-step]').forEach((element) => {
      element.hidden = element.dataset.step !== step;
    });
    box.dataset.at = step;
    box.scrollTop = 0;
  }

  function openBox(): void {
    if (!box.open) box.showModal();
    box.scrollTop = 0;
  }

  function setError(name: string, wrong: boolean): void {
    const message = box.querySelector<HTMLElement>(`[data-error="${name}"]`);
    if (message) message.hidden = !wrong;
    const control = name === 'item' ? itemsBox : input(name);
    if (control) control.setAttribute('aria-invalid', String(wrong));
  }

  function failForm(message: string): void {
    formError.textContent = message;
    formError.hidden = false;
  }

  function busy(on: boolean): void {
    payButton.disabled = on;
    payButton.textContent = on ? texts.paying : texts.pay;
  }

  function message(text: string): void {
    messageText.textContent = text;
    show('message');
  }

  async function open(next: BuySpec): Promise<void> {
    spec = next;
    pollRun++;
    form.reset();
    formError.hidden = true;
    ['item', 'nick', 'name', 'email', 'document', 'terms'].forEach((name) => setError(name, false));
    busy(false);
    title.textContent = next.title;
    price.textContent = money(next.price / 100);
    note.textContent = texts.notes[next.kind] ?? '';
    input('nick').value = remembered();
    itemsBox.hidden = !next.pickItemOf;
    if (next.pickItemOf) {
      itemsBox.querySelectorAll<HTMLImageElement>('img[data-icon]').forEach((icon) => {
        icon.src = `/loja/skins/${next.pickItemOf}/${icon.dataset.icon}.png`;
      });
    }
    show('form');
    openBox();

    // CentralCart's catalogue: is it on sale, and at what price. The form is usable meanwhile.
    try {
      const [list, pix] = await Promise.all([loadCatalogue(), loadPix()]);
      if (spec !== next) return;
      const product = productOf(list, next);
      if (!product || !pix || (!next.pickItemOf && !choicesOf(product, next.choices))) {
        message(texts.unavailable);
        return;
      }
      const charged = product.pricing?.price;
      if (typeof charged === 'number') price.textContent = money(charged);
      needsDocument = Boolean(pix.require_document || pix.provider_requirements?.[pix.default_provider ?? '']?.document);
      documentBox.hidden = !needsDocument;
    } catch {
      // Offline for a moment: buying tries again.
    }
  }

  function checkForm(): { nick: string; name: string; email: string; document: string; coupon: string; item: string } | null {
    const nick = input('nick').value.trim();
    const name = input('name').value.trim().replace(/\s+/g, ' ');
    const email = input('email').value.trim();
    const document = input('document').value.replace(/\D/g, '');
    const coupon = input('coupon').value.trim();
    const item = (form.querySelector<HTMLInputElement>('input[name="item"]:checked')?.value ?? '').trim();
    const wrong: string[] = [];
    if (spec?.pickItemOf && !item) wrong.push('item');
    if (!NICK.test(nick)) wrong.push('nick');
    if (!NAME.test(name)) wrong.push('name');
    if (!EMAIL.test(email)) wrong.push('email');
    if (needsDocument && document.length !== 11) wrong.push('document');
    if (!input('terms').checked) wrong.push('terms');
    ['item', 'nick', 'name', 'email', 'document', 'terms'].forEach((field) => setError(field, wrong.includes(field)));
    if (wrong.length) {
      const first = wrong[0] === 'item' ? itemsBox.querySelector<HTMLInputElement>('input') : input(wrong[0]);
      first?.focus();
      return null;
    }
    return { nick, name, email, document, coupon, item };
  }

  async function buy(): Promise<void> {
    const current = spec;
    const filled = checkForm();
    if (!current || !filled) return;
    formError.hidden = true;
    busy(true);
    try {
      const [list] = await Promise.all([loadCatalogue(), loadPix()]);
      const product = productOf(list, current);
      const choices = product && choicesOf(product, [...current.choices, ...(filled.item ? [filled.item] : [])]);
      if (!product || !choices) throw new StoreError(texts.unavailable);

      const order: Record<string, unknown> = {
        gateway: 'PIX',
        client_name: filled.name,
        client_email: filled.email,
        client_identifier: filled.nick,
        fields: { client_identifier: filled.nick },
        cart: [{ package_id: product.id, quantity: 1, options: choices, fields: choices }],
        terms: true,
        return_url: location.origin + location.pathname,
      };
      if (filled.coupon) order.coupon = filled.coupon;
      if (filled.document) order.client_document = filled.document;

      const reply = await call<CheckoutReply>('/webstore/checkout', order);
      if (spec !== current) return;
      remember(filled.nick);
      const orderId = orderIdOf(reply);

      if (reply.status === 'APPROVED') {
        finish(filled.nick);
        return;
      }
      if (reply.pix_code) {
        showPix(reply.pix_code, reply.qr_code ?? '', filled.nick);
      } else if (orderId && reply.order_token) {
        // No Pix code in the reply: CentralCart's own payment page, inside this window.
        showPayPage(`https://${storeDomain}/payment/${encodeURIComponent(orderId)}?t=${encodeURIComponent(reply.order_token)}`, filled.nick);
      } else if (reply.checkout_url) {
        location.assign(reply.checkout_url);
        return;
      } else {
        throw new StoreError(texts.error);
      }
      if (orderId) poll(orderId, filled.nick);
    } catch (error) {
      failForm(error instanceof StoreError ? error.message : texts.error);
    } finally {
      busy(false);
    }
  }

  function showPix(pix: string, image: string, nick: string): void {
    pixCode = pix;
    code.textContent = pix;
    codeWrap.hidden = false;
    steps.hidden = false;
    frame.hidden = true;
    frame.removeAttribute('src');
    qrWrap.hidden = !image;
    if (image) qr.src = /^(data:|https?:)/.test(image) ? image : `data:image/png;base64,${image}`;
    deliver.textContent = texts.deliverTo.replace('{nick}', nick);
    show('pix');
  }

  function showPayPage(url: string, nick: string): void {
    pixCode = '';
    qrWrap.hidden = true;
    codeWrap.hidden = true;
    steps.hidden = true;
    frame.src = url;
    frame.hidden = false;
    deliver.textContent = texts.deliverTo.replace('{nick}', nick);
    show('pix');
  }

  /** Back from a payment page (a card, say): only the waiting, then the answer. */
  function showWaiting(): void {
    pixCode = '';
    qrWrap.hidden = true;
    codeWrap.hidden = true;
    steps.hidden = true;
    frame.hidden = true;
    deliver.textContent = '';
    show('pix');
  }

  function finish(nick: string): void {
    pollRun++;
    doneText.textContent = nick ? texts.done.replace('{nick}', nick) : texts.doneNoNick;
    show('done');
    openBox();
  }

  function poll(orderId: string, nick: string): void {
    const run = ++pollRun;
    const until = Date.now() + POLL_FOR;
    const tick = async () => {
      if (run !== pollRun) return;
      try {
        const { status } = await call<{ status?: string }>(`/webstore/order_status/${encodeURIComponent(orderId)}`);
        if (run !== pollRun) return;
        if (status === 'APPROVED') {
          finish(nick);
          return;
        }
        if (status && ENDED.includes(status)) {
          pollRun++;
          show('failed');
          openBox();
          return;
        }
      } catch {
        // A moment without connection: ask again on the next round.
      }
      if (Date.now() < until) window.setTimeout(tick, POLL_EVERY);
    };
    window.setTimeout(tick, POLL_EVERY);
  }

  // --------------------------------------------------------------- Wiring

  document.querySelectorAll<HTMLElement>('[data-buy]').forEach((button) => {
    button.addEventListener('click', (event) => {
      event.preventDefault();
      try {
        void open(JSON.parse(button.dataset.buy ?? '') as BuySpec);
      } catch {
        // A button with broken data: it stays a link to the store.
      }
    });
  });

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    void buy();
  });
  form.addEventListener('input', (event) => {
    const target = event.target as HTMLInputElement;
    if (target.getAttribute('aria-invalid') === 'true') setError(target.name, false);
    if (target.name === 'item') setError('item', false);
  });

  $('[data-pix-copy]').addEventListener('click', async () => {
    if (!pixCode) return;
    showToast((await copyText(pixCode)) ? texts.pixCopied : texts.pixCopyFailed);
  });
  box.querySelectorAll('[data-buy-close]').forEach((button) => button.addEventListener('click', () => box.close()));
  $('[data-buy-retry]').addEventListener('click', () => {
    if (spec) void open(spec);
  });
  // A click on the dimmed page around the window closes it.
  box.addEventListener('click', (event) => {
    if (event.target === box) box.close();
  });

  // Back from a payment page outside the site, CentralCart adds ?order_id= to this page.
  const params = new URLSearchParams(location.search);
  const returning = params.get('order_id');
  if (returning) {
    params.delete('order_id');
    const rest = params.toString();
    history.replaceState(null, '', location.pathname + (rest ? `?${rest}` : '') + location.hash);
    title.textContent = '';
    showWaiting();
    openBox();
    poll(returning, '');
  }
}
