import { describe, expect, it } from "vitest";
import { z } from "./zod";

describe("shared zod", () => {
  it("should_run_jitless_so_no_new_function_probe_hits_the_csp", () => {
    expect(z.config().jitless).toBe(true);
  });
});
