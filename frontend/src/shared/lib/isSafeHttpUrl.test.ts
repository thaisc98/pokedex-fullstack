import { describe, expect, it } from "vitest";
import { isSafeHttpUrl, isSpriteUrl } from "./isSafeHttpUrl";

describe("isSafeHttpUrl", () => {
  it("should_accept_http_and_https_urls", () => {
    expect(isSafeHttpUrl("https://example.com/a")).toBe(true);
    expect(isSafeHttpUrl("http://example.com/a")).toBe(true);
  });

  it.each([
    "javascript:alert(1)",
    "JaVaScRiPt:alert(1)",
    "data:text/html,<script>alert(1)</script>",
    "vbscript:msgbox",
    "//evil.example/x",
    "/relative/path",
    "not a url",
    "",
  ])("should_reject_%s", (url) => {
    expect(isSafeHttpUrl(url)).toBe(false);
  });

  it("should_reject_non_string_values", () => {
    expect(isSafeHttpUrl(undefined)).toBe(false);
    expect(isSafeHttpUrl(null)).toBe(false);
    expect(isSafeHttpUrl(42)).toBe(false);
  });

  it("should_only_accept_allow_listed_hosts_when_a_list_is_given", () => {
    const allowedHosts = ["raw.githubusercontent.com"];
    expect(isSafeHttpUrl("https://raw.githubusercontent.com/x.png", { allowedHosts })).toBe(true);
    expect(isSafeHttpUrl("https://evil.example/x.png", { allowedHosts })).toBe(false);
    expect(
      isSafeHttpUrl("https://raw.githubusercontent.com.evil.example/x.png", { allowedHosts }),
    ).toBe(false);
  });

  it("should_reject_urls_with_credentials", () => {
    expect(isSafeHttpUrl("https://user:pass@example.com/")).toBe(false);
  });
});

describe("isSpriteUrl", () => {
  it("should_accept_https_urls_under_the_pokeapi_sprites_path", () => {
    expect(
      isSpriteUrl("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png"),
    ).toBe(true);
  });

  it.each([
    "http://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
    "https://raw.githubusercontent.com/someone-else/repo/x.png",
    "https://raw.githubusercontent.com/PokeAPI/sprites/../../evil/x.png",
    "https://evil.example/PokeAPI/sprites/x.png",
  ])("should_reject_%s", (url) => {
    expect(isSpriteUrl(url)).toBe(false);
  });
});
