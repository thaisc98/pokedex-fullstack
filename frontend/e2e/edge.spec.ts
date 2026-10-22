import { expect, test } from "./support/fixtures";

/**
 * The nginx edge overwrites X-Forwarded-For with the real client address (docker/nginx/default.conf,
 * security.md §4.1), so a client can't pick a fresh rate-limit bucket by faking its IP (F-AUTH-3).
 *
 * Every request below claims a different client IP (X-Forwarded-For and X-Real-IP). If the edge passed
 * them through, each would get its own 3-per-minute registration bucket and never see a 429. Through the
 * edge they all share the one real bucket: at most 3 registrations succeed, and once the bucket is empty
 * a never-seen fake IP is still refused. The assertion does not depend on how much of the bucket earlier
 * registrations in this minute (the setup project's) already used.
 *
 * Runs in its own project after `setup` and before nothing else that registers, so it never starves
 * another test's registration.
 */
test("a spoofed X-Forwarded-For does not get its own registration bucket at the edge", async ({ request }) => {
  const statuses: number[] = [];
  for (let attempt = 1; attempt <= 5; attempt++) {
    await request.get("/api/v1/auth/csrf");
    const token = (await request.storageState()).cookies.find((c) => c.name === "XSRF-TOKEN")?.value ?? "";
    const fakeIp = `198.51.100.${attempt}`; // TEST-NET-2, one per attempt
    const response = await request.post("/api/v1/auth/register", {
      headers: { "X-XSRF-TOKEN": token, "X-Forwarded-For": fakeIp, "X-Real-IP": fakeIp },
      data: {
        email: `edge-${Date.now()}-${attempt}@example.com`,
        password: "Edge-Spoof-Check-2026",
        displayName: `Edge ${attempt}`,
      },
    });
    statuses.push(response.status());
  }

  const firstRefusal = statuses.indexOf(429);
  expect(firstRefusal, `statuses ${statuses.join(",")}: the shared bucket ran out`).toBeGreaterThanOrEqual(0);
  expect(firstRefusal, "at most 3 registrations per minute from one real client").toBeLessThanOrEqual(3);
  expect(statuses.slice(0, firstRefusal).every((s) => s === 201), `statuses ${statuses.join(",")}`).toBe(true);
  expect(
    statuses.slice(firstRefusal),
    "a new fake IP after the bucket is empty is still refused (no per-spoofed-IP bucket)",
  ).toEqual(statuses.slice(firstRefusal).map(() => 429));
});
