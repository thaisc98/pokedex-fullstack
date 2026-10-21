import { describe, expect, it } from "vitest";
import { catalogHref, pageItems, parseCatalogParams } from "./catalogParams";

describe("parseCatalogParams", () => {
  it("should_default_to_page_1_and_size_20", () => {
    expect(parseCatalogParams({})).toEqual({ page: 1, size: 20 });
  });

  it("should_read_page_and_size_from_the_url", () => {
    expect(parseCatalogParams({ page: "3", size: "50" })).toEqual({ page: 3, size: 50 });
  });

  it.each([
    [{ page: "0" }, { page: 1, size: 20 }],
    [{ page: "-2" }, { page: 1, size: 20 }],
    [{ page: "abc" }, { page: 1, size: 20 }],
    [{ page: "2.5" }, { page: 1, size: 20 }],
    [{ size: "0" }, { page: 1, size: 20 }],
    [{ size: "101" }, { page: 1, size: 20 }],
    [{ page: ["4", "5"] }, { page: 4, size: 20 }],
  ])("should_fall_back_to_defaults_for_invalid_input_%#", (input, expected) => {
    expect(parseCatalogParams(input)).toEqual(expected);
  });
});

describe("catalogHref", () => {
  it("should_omit_default_values", () => {
    expect(catalogHref({ page: 1, size: 20 })).toBe("/pokemon");
  });

  it("should_include_page_and_non_default_size", () => {
    expect(catalogHref({ page: 2, size: 20 })).toBe("/pokemon?page=2");
    expect(catalogHref({ page: 3, size: 50 })).toBe("/pokemon?page=3&size=50");
    expect(catalogHref({ page: 1, size: 50 })).toBe("/pokemon?size=50");
  });
});

describe("pageItems", () => {
  it("should_list_every_page_when_there_are_few", () => {
    expect(pageItems(2, 5)).toEqual([1, 2, 3, 4, 5]);
  });

  it("should_collapse_distant_pages_into_ellipses", () => {
    expect(pageItems(1, 66)).toEqual([1, 2, "ellipsis-end", 66]);
    expect(pageItems(10, 66)).toEqual([1, "ellipsis-start", 9, 10, 11, "ellipsis-end", 66]);
    expect(pageItems(66, 66)).toEqual([1, "ellipsis-start", 65, 66]);
  });

  it("should_not_use_an_ellipsis_for_a_single_skipped_page", () => {
    expect(pageItems(4, 66)).toEqual([1, 2, 3, 4, 5, "ellipsis-end", 66]);
  });

  it("should_return_nothing_for_zero_pages", () => {
    expect(pageItems(1, 0)).toEqual([]);
  });
});
