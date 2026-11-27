// Minecraft text formatting for the GeoWiki's colour tool: a text made of parts, each with
// one colour, a gradient or a rainbow, and styles (bold, italic...). It is written out in the
// three formats the server's plugins read, and laid out letter by letter for the preview.

export type Mode = 'solid' | 'gradient' | 'rainbow';
export type Format = 'mini' | 'amp' | 'bukkit';

export interface Part {
  text: string;
  mode: Mode;
  /** Hex colours ("#55ff55"): one for a solid colour, two or more for a gradient. */
  colors: string[];
  bold: boolean;
  italic: boolean;
  underline: boolean;
  strike: boolean;
  obfuscated: boolean;
}

/** The 16 Minecraft colours: their "&" code, MiniMessage name and hex. */
export const PALETTE = [
  { code: '0', name: 'black', hex: '#000000' },
  { code: '1', name: 'dark_blue', hex: '#0000aa' },
  { code: '2', name: 'dark_green', hex: '#00aa00' },
  { code: '3', name: 'dark_aqua', hex: '#00aaaa' },
  { code: '4', name: 'dark_red', hex: '#aa0000' },
  { code: '5', name: 'dark_purple', hex: '#aa00aa' },
  { code: '6', name: 'gold', hex: '#ffaa00' },
  { code: '7', name: 'gray', hex: '#aaaaaa' },
  { code: '8', name: 'dark_gray', hex: '#555555' },
  { code: '9', name: 'blue', hex: '#5555ff' },
  { code: 'a', name: 'green', hex: '#55ff55' },
  { code: 'b', name: 'aqua', hex: '#55ffff' },
  { code: 'c', name: 'red', hex: '#ff5555' },
  { code: 'd', name: 'light_purple', hex: '#ff55ff' },
  { code: 'e', name: 'yellow', hex: '#ffff55' },
  { code: 'f', name: 'white', hex: '#ffffff' },
] as const;

const STYLES = [
  { key: 'bold', amp: 'l', mini: 'b' },
  { key: 'italic', amp: 'o', mini: 'i' },
  { key: 'underline', amp: 'n', mini: 'u' },
  { key: 'strike', amp: 'm', mini: 'st' },
  { key: 'obfuscated', amp: 'k', mini: 'obf' },
] as const;

export function normalize(hex: string): string {
  const clean = hex.trim().toLowerCase().replace(/^#/, '');
  return /^[0-9a-f]{6}$/.test(clean) ? `#${clean}` : '#ffffff';
}

function paletteOf(hex: string) {
  return PALETTE.find((color) => color.hex === normalize(hex));
}

function rgb(hex: string): [number, number, number] {
  const value = normalize(hex).slice(1);
  return [0, 2, 4].map((i) => parseInt(value.slice(i, i + 2), 16)) as [number, number, number];
}

function toHex([r, g, b]: number[]): string {
  return `#${[r, g, b].map((v) => Math.round(Math.max(0, Math.min(255, v))).toString(16).padStart(2, '0')).join('')}`;
}

/** The colour at a point (0 to 1) of a gradient through these colours. */
function blend(colors: string[], t: number): string {
  if (colors.length === 1) return normalize(colors[0]);
  const position = t * (colors.length - 1);
  const index = Math.min(Math.floor(position), colors.length - 2);
  const local = position - index;
  const a = rgb(colors[index]);
  const b = rgb(colors[index + 1]);
  return toHex(a.map((v, i) => v + (b[i] - v) * local));
}

/** The colour at a point (0 to 1) of a rainbow. */
function rainbow(t: number): string {
  const h = t * 6;
  const x = 1 - Math.abs((h % 2) - 1);
  const [r, g, b] = h < 1 ? [1, x, 0] : h < 2 ? [x, 1, 0] : h < 3 ? [0, 1, x] : h < 4 ? [0, x, 1] : h < 5 ? [x, 0, 1] : [1, 0, x];
  return toHex([r * 255, g * 255, b * 255]);
}

/** The letters of a part, each with its colour. */
export function letters(part: Part): { char: string; color: string }[] {
  const chars = Array.from(part.text);
  const count = chars.length;
  return chars.map((char, i) => {
    const t = count <= 1 ? 0 : i / (count - 1);
    const color = part.mode === 'solid' ? normalize(part.colors[0] ?? '#ffffff')
      : part.mode === 'rainbow' ? rainbow(t)
      : blend(part.colors.length ? part.colors : ['#ffffff'], t);
    return { char, color };
  });
}

function ampColor(hex: string, format: 'amp' | 'bukkit'): string {
  const named = paletteOf(hex);
  if (named) return `&${named.code}`;
  const digits = normalize(hex).slice(1);
  return format === 'amp' ? `&#${digits.toUpperCase()}` : `&x${Array.from(digits).map((d) => `&${d}`).join('')}`;
}

function ampStyles(part: Part): string {
  return STYLES.filter((s) => part[s.key]).map((s) => `&${s.amp}`).join('');
}

/** "&" codes. A colour code resets the styles, so they are written again after every colour. */
function toAmp(parts: Part[], format: 'amp' | 'bukkit'): string {
  let out = '';
  for (const part of parts) {
    if (!part.text) continue;
    const styles = ampStyles(part);
    if (part.mode === 'solid') {
      out += ampColor(part.colors[0] ?? '#ffffff', format) + styles + part.text;
      continue;
    }
    // A space needs no colour of its own, unless a line is drawn under or through it.
    const lined = part.underline || part.strike;
    for (const { char, color } of letters(part)) {
      out += char === ' ' && !lined ? char : ampColor(color, format) + styles + char;
    }
  }
  return out;
}

/** A colour's name ("gold") when it is no longer than its hex: country names have little room. */
function miniColor(hex: string): string {
  const name = paletteOf(hex)?.name;
  const code = normalize(hex);
  return name && name.length <= code.length ? name : code;
}

/** MiniMessage, the format Lands keeps country names in. Tags at the very end are left open: it is shorter. */
function toMini(parts: Part[]): string {
  const filled = parts.filter((part) => part.text);
  return filled.map((part, index) => {
    const styles = STYLES.filter((s) => part[s.key]);
    let open = '';
    let close = '';
    if (part.mode === 'solid') {
      open = `<${miniColor(part.colors[0] ?? '#ffffff')}>`;
    } else if (part.mode === 'gradient') {
      open = `<gradient:${(part.colors.length ? part.colors : ['#ffffff']).map(miniColor).join(':')}>`;
      close = '</gradient>';
    } else {
      open = '<rainbow>';
      close = '</rainbow>';
    }
    open += styles.map((s) => `<${s.mini}>`).join('');
    close = styles.slice().reverse().map((s) => `</${s.mini}>`).join('') + close;
    const text = part.text.replace(/\\/g, '\\\\').replace(/</g, '\\<');
    return open + text + (index < filled.length - 1 ? close : '');
  }).join('');
}

export function render(parts: Part[], format: Format): string {
  return format === 'mini' ? toMini(parts) : toAmp(parts, format);
}

/** A darker copy of a colour, as Minecraft draws text shadows. */
export function shadow(hex: string): string {
  return toHex(rgb(hex).map((v) => v / 4));
}
