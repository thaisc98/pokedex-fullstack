import { describe, expect, it } from "vitest";
import { problemCopy } from "./problemCopy";

describe("problemCopy", () => {
  it("should_map_a_known_problem_type_to_friendly_copy", () => {
    const copy = problemCopy({ type: "/problems/upstream-unavailable", status: 503 });
    expect(copy.title).toBe("PokeAPI is unavailable");
    expect(copy.message).toMatch(/try again/i);
  });

  it("should_map_not_found", () => {
    expect(problemCopy({ type: "/problems/not-found", status: 404 }).title).toBe(
      "We couldn’t find that Pokémon",
    );
  });

  it("should_fall_back_to_the_detail_for_an_unknown_type", () => {
    const copy = problemCopy({
      type: "/problems/something-new",
      status: 418,
      detail: "Short and stout.",
    });
    expect(copy.title).toBe("Something went wrong");
    expect(copy.message).toBe("Short and stout.");
  });

  it("should_fall_back_on_status_when_type_is_unknown_and_there_is_no_detail", () => {
    expect(problemCopy({ type: "about:blank", status: 503 }).title).toBe(
      "PokeAPI is unavailable",
    );
    expect(problemCopy({ type: "about:blank", status: 500 }).title).toBe("Something went wrong");
  });

  it("should_handle_a_missing_problem", () => {
    expect(problemCopy(undefined).title).toBe("Something went wrong");
  });

  it("should_cover_every_problem_type_in_the_contract", () => {
    const types = [
      "validation-error",
      "unauthorized",
      "invalid-credentials",
      "forbidden",
      "csrf-token-invalid",
      "not-found",
      "email-taken",
      "rate-limited",
      "version-conflict",
      "payload-too-large",
      "unsupported-media-type",
      "upstream-unavailable",
      "internal-error",
    ];
    for (const t of types) {
      expect(problemCopy({ type: `/problems/${t}`, status: 400 }).title).not.toBe(
        "Something went wrong",
      );
    }
  });
});
