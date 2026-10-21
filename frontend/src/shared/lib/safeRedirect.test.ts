import { describe, expect, it } from "vitest";
import { safeRedirect } from "./safeRedirect";

describe("safeRedirect", () => {
  it.each(["/collection", "/pokemon/25", "/collection?page=2&tag=starter"])(
    "should_accept_the_internal_path_%s",
    (path) => {
      expect(safeRedirect(path)).toBe(path);
    },
  );

  it.each([
    "https://evil.example/",
    "//evil.example/path",
    "/\\evil.example",
    "javascript:alert(1)",
    "pokemon/25",
    "/\t/evil.example",
    "",
    null,
    undefined,
  ])("should_fall_back_for_%s", (from) => {
    expect(safeRedirect(from)).toBe("/collection");
  });

  it("should_not_redirect_back_to_the_login_page", () => {
    expect(safeRedirect("/login?from=/collection")).toBe("/collection");
  });

  it("should_use_the_given_fallback", () => {
    expect(safeRedirect("https://evil.example", "/pokemon")).toBe("/pokemon");
  });
});
