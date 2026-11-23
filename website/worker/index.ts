// The website's Worker. The site itself is static files (dist/), served without running
// this code. The Worker does two things for the live map's fallback picture:
//   - every 20 minutes (the cron trigger in wrangler.jsonc) it takes a snapshot of the
//     live map and stores it, deleting the ones that are too old;
//   - it answers /api/map-snapshot.json and /api/map-snapshot/<time>.jpg (and
//     /api/map-view.json, which says how the map was framed at the last build).
// Any other address that is not a file goes back to the static files, which answer with
// the 404 page.
import { mapEmbedUrl } from '../src/config/site.ts';
import view from '../src/generated/map-view.json';
import { captureMap } from './capture.ts';
import { handleSnapshotRequest } from './routes.ts';
import { saveSnapshot } from './snapshots.ts';

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
    try {
      const url = mapEmbedUrl(view);
      // A plain request first: if the map is down there is nothing to photograph, and
      // the hosted browser (which has a daily time allowance) is not started at all.
      const answer = await fetch(url, { signal: AbortSignal.timeout(8000) });
      if (!answer.ok) throw new Error(`the map answered ${answer.status}`);
      const picture = await captureMap(env.BROWSER, url, view);
      const deleted = await saveSnapshot(env.SNAPSHOTS, picture, Date.now());
      console.log(`map snapshot saved (${picture.byteLength} bytes); deleted ${deleted.length} old`);
    } catch (error) {
      // The map being down is the case this exists for: keep the earlier snapshots.
      console.error(`map snapshot failed: ${error instanceof Error ? error.message : String(error)}`);
    }
  },
} satisfies ExportedHandler<Env>;
