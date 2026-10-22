import { z } from "zod";

/**
 * The one place the app imports Zod from. `jitless` turns off Zod 4's compiled validators,
 * and with them its `new Function("")` feature probe: our CSP has no 'unsafe-eval', so the
 * probe was reported as a CSP violation (Lighthouse "inspector-issues") even though Zod
 * caught the error. Configured here, at module load, before any schema is parsed.
 */
z.config({ jitless: true });

export { z };
