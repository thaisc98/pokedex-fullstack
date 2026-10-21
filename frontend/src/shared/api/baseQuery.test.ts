import { http, HttpResponse } from "msw";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { server } from "@/mocks/node";
import { makeStore } from "@/store/makeStore";
import { signedIn } from "@/features/auth/authSlice";
import { collectionApi } from "@/features/collection/collectionApi";
import type { ApiError } from "./apiError";

const API = "http://localhost:3000/api/v1";

function setCookie(name: string, value: string) {
  document.cookie = `${name}=${value}; path=/`;
}
function clearCookie(name: string) {
  document.cookie = `${name}=; path=/; max-age=0`;
}

const item = {
  id: 25,
  snapshot: { id: 25, name: "pikachu", sprite: "", category: "", mass: 6, abilities: [], stats: [] },
  custom: { localizedNames: [], tags: [] },
  version: 0,
  syncedAt: "2026-10-21T00:00:00Z",
  updatedAt: "2026-10-21T00:00:00Z",
};

beforeEach(() => {
  window.history.replaceState({}, "", "http://localhost:3000/");
});
afterEach(() => {
  clearCookie("XSRF-TOKEN");
  clearCookie("pokedex_signed_in");
});

describe("baseQuery", () => {
  it("should_send_the_current_xsrf_cookie_on_writes_and_read_it_fresh_each_time", async () => {
    const seen: (string | null)[] = [];
    server.use(
      http.post(`${API}/collection`, ({ request }) => {
        seen.push(request.headers.get("X-XSRF-TOKEN"));
        setCookie("XSRF-TOKEN", `rotated-${seen.length}`); // Spring issues a new token per write
        return HttpResponse.json(item, { status: 201 });
      }),
    );
    setCookie("XSRF-TOKEN", "first");
    const store = makeStore();

    await store.dispatch(collectionApi.endpoints.sync.initiate(25));
    await store.dispatch(collectionApi.endpoints.sync.initiate(25));

    expect(seen).toEqual(["first", "rotated-1"]);
  });

  it("should_fetch_a_csrf_token_first_when_there_is_no_cookie", async () => {
    let header: string | null = null;
    server.use(
      http.get(`${API}/auth/csrf`, () => {
        setCookie("XSRF-TOKEN", "fresh");
        return HttpResponse.json({ headerName: "X-XSRF-TOKEN", token: "fresh" });
      }),
      http.post(`${API}/collection`, ({ request }) => {
        header = request.headers.get("X-XSRF-TOKEN");
        return HttpResponse.json(item, { status: 201 });
      }),
    );
    const store = makeStore();

    const result = await store.dispatch(collectionApi.endpoints.sync.initiate(25));

    expect(header).toBe("fresh");
    expect(result.data).toEqual({ item, created: true });
  });

  it("should_retry_once_with_a_new_token_after_a_csrf_403", async () => {
    let attempts = 0;
    server.use(
      http.get(`${API}/auth/csrf`, () => {
        setCookie("XSRF-TOKEN", "renewed");
        return HttpResponse.json({ headerName: "X-XSRF-TOKEN", token: "renewed" });
      }),
      http.post(`${API}/collection`, ({ request }) => {
        attempts += 1;
        if (request.headers.get("X-XSRF-TOKEN") !== "renewed") {
          return HttpResponse.json(
            { type: "/problems/csrf-token-invalid", title: "Forbidden", status: 403 },
            { status: 403 },
          );
        }
        return HttpResponse.json(item, { status: 200 });
      }),
    );
    setCookie("XSRF-TOKEN", "stale");
    const store = makeStore();

    const result = await store.dispatch(collectionApi.endpoints.sync.initiate(25));

    expect(attempts).toBe(2);
    expect(result.data).toEqual({ item, created: false });
  });

  it("should_not_retry_a_403_that_is_not_about_csrf", async () => {
    let attempts = 0;
    server.use(
      http.delete(`${API}/collection/25`, () => {
        attempts += 1;
        return HttpResponse.json(
          { type: "/problems/forbidden", title: "Forbidden", status: 403 },
          { status: 403 },
        );
      }),
    );
    setCookie("XSRF-TOKEN", "t");
    const store = makeStore();

    const result = await store.dispatch(collectionApi.endpoints.deleteCollected.initiate(25));

    expect(attempts).toBe(1);
    expect((result.error as ApiError).problem.type).toBe("/problems/forbidden");
  });

  it("should_end_the_session_on_401", async () => {
    server.use(
      http.get(`${API}/collection/25`, () =>
        HttpResponse.json({ type: "/problems/unauthorized", title: "Unauthorized", status: 401 }, { status: 401 }),
      ),
    );
    setCookie("pokedex_signed_in", "1");
    const store = makeStore();
    store.dispatch(
      signedIn({
        user: { id: "u", email: "a@b.c", displayName: "Ash", role: "USER" },
        expiresAt: "2099-01-01T00:00:00Z",
      }),
    );

    await store.dispatch(collectionApi.endpoints.getCollected.initiate(25));

    expect(store.getState().auth.user).toBeNull();
    expect(document.cookie).not.toContain("pokedex_signed_in=1");
  });

  it("should_expose_retry_after_on_429", async () => {
    server.use(
      http.post(`${API}/collection`, () =>
        HttpResponse.json(
          { type: "/problems/rate-limited", title: "Too many", status: 429 },
          { status: 429, headers: { "Retry-After": "42" } },
        ),
      ),
    );
    setCookie("XSRF-TOKEN", "t");
    const store = makeStore();

    const result = await store.dispatch(collectionApi.endpoints.sync.initiate(25));

    expect(result.error).toMatchObject({ status: 429, retryAfter: 42 });
  });
});
