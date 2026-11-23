// The website's Worker. The site itself is static files (dist/), served without running
// this code. The Worker does two things, both about pictures of the live map:
//   - every 30 minutes (the cron trigger in wrangler.jsonc) it photographs the map with
//     Cloudflare's hosted browser and stores the pictures:
//       1. the whole world (snapshots.ts), deleting the ones that are too old;
//       2. once a day, the part of the world in the "Realismo" journey figure;
//       3. a few sheets of the site's own zoomable copy of the map (copy.ts).
//     The pages show these when the live map does not load;
//   - it answers /api/map-snapshot.json and the pictures' own addresses (routes.ts).
// Any other address that is not a file goes back to the static files, which answer with
// the 404 page.
import { journeyShot, sheetShot, worldShot, type MapView } from '../src/config/site.ts';
import generated from '../src/generated/map-view.json';
import { WrongZoom, withCamera, type Camera } from './capture.ts';
import { copyFraming, copyIndex, nextSheets, readSheets, readState, saveIndex, saveSheet, saveState, sheetName, type CopyState } from './copy.ts';
import { handleSnapshotRequest } from './routes.ts';
import { framingOf, isDue, JOURNEY, latestPicture, savePicture, WORLD } from './snapshots.ts';

const view: MapView = generated;
const reason = (error: unknown) => (error instanceof Error ? error.message : String(error));

/**
 * Cloudflare's free plan lets the hosted browser run 10 minutes a day. The whole-world
 * snapshot always comes first; the copy's sheets only get what is left, in small
 * portions: a few seconds per run, and nothing more once the day's use passes 7 minutes.
 */
const SHEETS_PER_RUN = 4;
const SHEET_TIME_PER_RUN = 5_000;
const BROWSER_TIME_PER_DAY = 7 * 60_000;

interface Env {
  ASSETS: Fetcher;
  BROWSER: BrowserRun;
  SNAPSHOTS: KVNamespace;
}

/** A map that stopped at zoom `error.got`: how many levels in from the whole world that is. */
const levelsIn = (error: WrongZoom) => Math.max(0, error.got - view.zoom);

/** The journey figure's picture, when there is none yet or it is a day old. */
async function takeJourney(camera: Camera, store: KVNamespace, state: CopyState): Promise<void> {
  for (;;) {
    const journey = journeyShot(view, state.deepest);
    const framing = framingOf(journey);
    if (!isDue(await latestPicture(store, JOURNEY), framing, Date.now())) return;
    try {
      const picture = await camera.shoot(journey, { quality: 88, thorough: true });
      await savePicture(store, JOURNEY, picture, Date.now(), framing);
      console.log(`journey picture saved (${journey.width} x ${journey.height} at zoom ${journey.zoom}, ${picture.byteLength} bytes)`);
      return;
    } catch (error) {
      // The map does not zoom this far: remember that, and take the picture at the
      // deepest level it does reach.
      if (!(error instanceof WrongZoom) || levelsIn(error) >= state.deepest) throw error;
      state.deepest = levelsIn(error);
    }
  }
}

/** A few more sheets of the copy of the map, most wanted first. */
async function takeSheets(camera: Camera, store: KVNamespace, state: CopyState): Promise<void> {
  // The store itself says which sheets exist, so sheets saved by a run that was cut
  // short are not forgotten.
  const sheets = await readSheets(store, state.framing);
  const started = Date.now();
  let taken = 0;
  try {
    for (const ref of nextSheets(sheets, state.deepest, started).slice(0, SHEETS_PER_RUN)) {
      if (Date.now() - started > SHEET_TIME_PER_RUN) break;
      try {
        const picture = await camera.shoot(sheetShot(view, ref.level, ref.col, ref.row), { quality: 85, thorough: true });
        const takenAt = Date.now();
        await saveSheet(store, ref, picture, takenAt, state.framing);
        sheets.set(sheetName(ref), takenAt);
        taken++;
      } catch (error) {
        if (!(error instanceof WrongZoom)) throw error;
        // The map does not zoom this far: the copy ends one level above.
        state.deepest = Math.min(ref.level - 1, levelsIn(error));
        console.log(`map copy: the map stops at zoom ${error.got}; the copy ends at level ${state.deepest}`);
        break;
      }
    }
  } finally {
    await saveIndex(store, copyIndex(view, sheets, state.deepest));
    console.log(`map copy: ${taken} sheets taken, ${sheets.size} in store`);
  }
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const answer = await handleSnapshotRequest(new URL(request.url), env.SNAPSHOTS, view);
    return answer ?? env.ASSETS.fetch(request);
  },

  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    const store = env.SNAPSHOTS;
    const world = worldShot(view);
    try {
      // A plain request first: if the map is down there is nothing to photograph, and
      // the hosted browser is not started at all.
      const answer = await fetch(world.url, { signal: AbortSignal.timeout(8000) });
      if (!answer.ok) throw new Error(`the map answered ${answer.status}`);
    } catch (error) {
      // The map being down is the case all this exists for: everything stored stays.
      console.error(`map snapshot failed: ${reason(error)}`);
      return;
    }

    const state = await readState(store, copyFraming(view), Date.now());
    const started = Date.now();
    try {
      await withCamera(env.BROWSER, async (camera) => {
        try {
          const picture = await camera.shoot(world);
          const deleted = await savePicture(store, WORLD, picture, Date.now());
          console.log(`map snapshot saved (${picture.byteLength} bytes); deleted ${deleted.length} old`);
        } catch (error) {
          console.error(`map snapshot failed: ${reason(error)}`);
          return;
        }
        // If one of these fails, what is stored stays and the next run tries again.
        try {
          await takeJourney(camera, store, state);
        } catch (error) {
          console.error(`journey picture failed: ${reason(error)}`);
        }
        try {
          if (state.browserMs + (Date.now() - started) < BROWSER_TIME_PER_DAY) await takeSheets(camera, store, state);
        } catch (error) {
          console.error(`map copy failed: ${reason(error)}`);
        }
      });
    } catch (error) {
      console.error(`hosted browser failed: ${reason(error)}`);
    } finally {
      state.browserMs += Date.now() - started;
      await saveState(store, state);
    }
  },
} satisfies ExportedHandler<Env>;
