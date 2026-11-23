// The website's Worker. The site itself is static files (dist/), served without running
// this code. The Worker does two things, both about pictures of the live map:
//   - every 30 minutes (the cron trigger in wrangler.jsonc) it photographs the whole
//     map and stores the picture, deleting the ones that are too old. That is what the
//     live map shows when the map itself is down. Once a day it also photographs the
//     part of the world in the "Realismo" journey figure, zoomed in;
//   - it answers /api/map-snapshot.json and the pictures' own addresses (and
//     /api/map-view.json, which says how the map was framed at the last build).
// Any other address that is not a file goes back to the static files, which answer with
// the 404 page.
import { journeyShot, worldShot, type MapView } from '../src/config/site.ts';
import generated from '../src/generated/map-view.json';
import { captureMap } from './capture.ts';
import { handleSnapshotRequest } from './routes.ts';
import { framingOf, isDue, JOURNEY, latestPicture, savePicture, WORLD } from './snapshots.ts';

const view: MapView = generated;
const reason = (error: unknown) => (error instanceof Error ? error.message : String(error));

interface Env {
  ASSETS: Fetcher;
  BROWSER: BrowserRun;
  SNAPSHOTS: KVNamespace;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const answer = await handleSnapshotRequest(new URL(request.url), env.SNAPSHOTS, view);
    return answer ?? env.ASSETS.fetch(request);
  },

  async scheduled(_controller: ScheduledController, env: Env): Promise<void> {
    const world = worldShot(view);
    try {
      // A plain request first: if the map is down there is nothing to photograph, and
      // the hosted browser (which has a daily time allowance) is not started at all.
      const answer = await fetch(world.url, { signal: AbortSignal.timeout(8000) });
      if (!answer.ok) throw new Error(`the map answered ${answer.status}`);
      const picture = await captureMap(env.BROWSER, world);
      const deleted = await savePicture(env.SNAPSHOTS, WORLD, picture, Date.now());
      console.log(`map snapshot saved (${picture.byteLength} bytes); deleted ${deleted.length} old`);
    } catch (error) {
      // The map being down is the case this exists for: keep the earlier snapshots.
      console.error(`map snapshot failed: ${reason(error)}`);
      return;
    }

    // The journey figure's picture, when there is none yet or it is a day old. If it
    // fails, the one before stays and the next run tries again.
    try {
      const journey = journeyShot(view);
      const framing = framingOf(journey);
      if (!isDue(await latestPicture(env.SNAPSHOTS, JOURNEY), framing, Date.now())) return;
      const picture = await captureMap(env.BROWSER, journey, { quality: 88, thorough: true });
      await savePicture(env.SNAPSHOTS, JOURNEY, picture, Date.now(), framing);
      console.log(`journey picture saved (${journey.width} x ${journey.height} at zoom ${journey.zoom}, ${picture.byteLength} bytes)`);
    } catch (error) {
      console.error(`journey picture failed: ${reason(error)}`);
    }
  },
} satisfies ExportedHandler<Env>;
