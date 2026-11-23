// The map window's slow 3D drift: it sways a few degrees on its own and settles flat
// while the visitor hovers, focuses or explores it, so the map is easy to use and
// sharp. Springs keep every change smooth. It writes the CSS variables --rx, --ry and
// --ty on the window and --sx, --sy on the figure (for the shadow). With reduced motion
// the window simply stays flat.
const reduced = matchMedia('(prefers-reduced-motion: reduce)');

type Axis = 'rx' | 'ry' | 'ty';
type Pose = Record<Axis, number>;

const axes: Axis[] = ['rx', 'ry', 'ty'];
const TAU = Math.PI * 2;
const stiffness = 52;
const damping = 2 * Math.sqrt(stiffness) * 0.96;
/** Seconds the drift takes to come back after the visitor leaves. */
const fadeIn = 1.8;

const smooth = (t: number) => {
  const x = Math.min(1, Math.max(0, t));
  return x * x * (3 - 2 * x);
};

/** The idle sway at time t (seconds): slow, overlapping waves. */
function drift(t: number): Pose {
  return {
    ry: -5 - 3 * Math.cos((TAU * t) / 17) + 1.2 * Math.sin((TAU * t) / 6.7),
    rx: 3.2 + 1.3 * Math.sin((TAU * t) / 21),
    ty: 4 * Math.sin((TAU * t) / 9.5),
  };
}

export interface Tilt {
  /** Holds the window flat (true) or lets it drift again (false). */
  hold(flat: boolean): void;
}

export function initTilt(figure: HTMLElement, win: HTMLElement): Tilt {
  const pose: Pose = { ...drift(0) };
  const speed: Pose = { rx: 0, ry: 0, ty: 0 };
  const start = performance.now();
  let last = start;
  let raf = 0;
  let visible = false;
  let hovered = false;
  let focused = false;
  let held = false;
  let resumeAt = 0;
  let amount = 1;
  const engaged = () => hovered || focused || held;
  const running = () => !reduced.matches && visible && !document.hidden;

  function write() {
    win.style.setProperty('--rx', `${pose.rx.toFixed(3)}deg`);
    win.style.setProperty('--ry', `${pose.ry.toFixed(3)}deg`);
    win.style.setProperty('--ty', `${pose.ty.toFixed(2)}px`);
    figure.style.setProperty('--sx', `${(-pose.ry * 1.8).toFixed(2)}px`);
    figure.style.setProperty('--sy', `${(pose.ty * 0.6).toFixed(2)}px`);
  }

  function frame(now: number) {
    raf = 0;
    const dt = Math.min(0.05, (now - last) / 1000);
    last = now;
    if (engaged()) amount = 0;
    else if (now >= resumeAt) amount = Math.min(1, amount + dt / fadeIn);
    const k = smooth(amount);
    const idle = drift((now - start) / 1000);
    let moving = false;
    for (const a of axes) {
      const target = idle[a] * k;
      const force = stiffness * (target - pose[a]) - damping * speed[a];
      speed[a] += force * dt;
      pose[a] += speed[a] * dt;
      if (Math.abs(speed[a]) > 2e-3 || Math.abs(target - pose[a]) > 2e-3) moving = true;
    }
    // Resting flat under the pointer: snap to exactly flat (a sharp map) and stop.
    if (!moving && engaged()) {
      for (const a of axes) pose[a] = speed[a] = 0;
      write();
      return;
    }
    write();
    if (running()) raf = requestAnimationFrame(frame);
  }

  const wake = () => {
    if (raf || !running()) return;
    last = performance.now();
    raf = requestAnimationFrame(frame);
  };
  const release = (ms: number) => {
    resumeAt = performance.now() + ms;
    wake();
  };

  win.addEventListener('pointerenter', (e) => {
    if (e.pointerType === 'touch') return;
    hovered = true;
    wake();
  });
  win.addEventListener('pointerleave', (e) => {
    if (e.pointerType === 'touch') return;
    hovered = false;
    if (!engaged()) release(1200);
  });
  win.addEventListener('focusin', () => {
    focused = true;
    wake();
  });
  win.addEventListener('focusout', (e) => {
    if (win.contains(e.relatedTarget as Node | null)) return;
    focused = false;
    if (!engaged()) release(1200);
  });
  new IntersectionObserver((entries) => {
    visible = entries.some((entry) => entry.isIntersecting);
    wake();
  }).observe(figure);
  document.addEventListener('visibilitychange', wake);

  const applyPreference = () => {
    figure.classList.toggle('is-static', reduced.matches);
    if (reduced.matches) {
      cancelAnimationFrame(raf);
      raf = 0;
      for (const a of axes) pose[a] = speed[a] = 0;
      write();
    } else {
      wake();
    }
  };
  reduced.addEventListener('change', applyPreference);
  applyPreference();

  return {
    hold(flat: boolean) {
      held = flat;
      if (flat) wake();
      else if (!engaged()) release(600);
    },
  };
}
