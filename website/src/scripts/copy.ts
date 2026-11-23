// Copy buttons: any [data-copy="text"] copies its text and says so in the page toast.
// The toast's wording comes from data attributes on the toast element (Base.astro).

let toastTimer = 0;

function showToast(text: string): void {
  const toast = document.querySelector<HTMLElement>('[data-site-toast]');
  if (!toast) return;
  toast.textContent = text;
  toast.classList.add('visible');
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove('visible'), 2600);
}

async function copyText(value: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(value);
    return true;
  } catch {
    // Older browsers, or a page that is not served over HTTPS.
    const field = document.createElement('textarea');
    field.value = value;
    field.setAttribute('readonly', '');
    field.style.cssText = 'position:fixed;top:0;left:0;opacity:0';
    document.body.append(field);
    field.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch {
      ok = false;
    }
    field.remove();
    return ok;
  }
}

export function initCopy(root: ParentNode = document): void {
  const toast = document.querySelector<HTMLElement>('[data-site-toast]');
  const okText = toast?.dataset.copied ?? '{value}';
  const failText = toast?.dataset.failed ?? '{value}';
  root.querySelectorAll<HTMLElement>('[data-copy]').forEach((button) => {
    let resetTimer = 0;
    button.addEventListener('click', async () => {
      const value = button.dataset.copy ?? '';
      const ok = await copyText(value);
      showToast((ok ? okText : failText).replace('{value}', value));
      if (!ok) return;
      button.classList.add('is-copied');
      window.clearTimeout(resetTimer);
      resetTimer = window.setTimeout(() => button.classList.remove('is-copied'), 1800);
    });
  });
}
