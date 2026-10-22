import { act, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useReportUnsavedChanges } from "@/shared/lib/unsavedChanges";
import { renderWithStore, trainer } from "@/test-utils/render";
import { signedIn } from "./authSlice";
import { SessionExpiry, WARNING_LEAD_MS } from "./SessionExpiry";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  usePathname: () => window.location.pathname,
}));

const toast = vi.hoisted(() => ({ warning: vi.fn() }));
vi.mock("@heroui/react", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@heroui/react")>()),
  toast,
}));

const MINUTE = 60_000;
const NOW = Date.parse("2026-10-22T12:00:00Z");
const inMinutes = (m: number) => new Date(NOW + m * MINUTE).toISOString();

function signIn(store: ReturnType<typeof renderWithStore>["store"], expiresAt: string) {
  act(() => {
    store.dispatch(signedIn({ user: trainer, expiresAt }));
  });
}

const advance = (ms: number) =>
  act(() => {
    vi.advanceTimersByTime(ms);
  });

function DirtyForm() {
  useReportUnsavedChanges(true);
  return <p>editing</p>;
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout", "Date"] });
  vi.setSystemTime(NOW);
  window.history.replaceState({}, "", "/collection/25?edit=1");
  document.cookie = "pokedex_signed_in=1; path=/";
  replace.mockClear();
  toast.warning.mockClear();
});

afterEach(() => {
  vi.useRealTimers();
  document.cookie = "pokedex_signed_in=; path=/; max-age=0";
});

describe("SessionExpiry", () => {
  it("should_warn_two_minutes_before_expiry_with_a_log_in_again_link_back_here", () => {
    const { store } = renderWithStore(<SessionExpiry />);
    signIn(store, inMinutes(10));

    advance(8 * MINUTE - 1000);
    expect(screen.queryByText(/Your session ends in 2 minutes/)).not.toBeInTheDocument();

    advance(1000);
    expect(screen.getByRole("status")).toHaveTextContent("Your session ends in 2 minutes. Save your changes.");
    expect(screen.getByRole("link", { name: "Log in again" })).toHaveAttribute(
      "href",
      "/login?from=%2Fcollection%2F25%3Fedit%3D1",
    );
  });

  it("should_sign_out_and_go_to_login_at_expiry_on_a_protected_page", () => {
    const { store } = renderWithStore(<SessionExpiry />);
    signIn(store, inMinutes(10));

    advance(10 * MINUTE);

    expect(store.getState().auth.user).toBeNull();
    expect(document.cookie).not.toContain("pokedex_signed_in=1");
    expect(toast.warning).toHaveBeenCalledWith("You’ve been signed out", expect.anything());
    expect(replace).toHaveBeenCalledWith("/login?from=%2Fcollection%2F25%3Fedit%3D1");
  });

  it("should_not_redirect_away_from_unsaved_input_at_expiry", () => {
    const { store } = renderWithStore(
      <>
        <SessionExpiry />
        <DirtyForm />
      </>,
    );
    signIn(store, inMinutes(3));

    advance(3 * MINUTE);

    expect(store.getState().auth.user).toBeNull();
    expect(replace).not.toHaveBeenCalled();
    expect(screen.getByRole("status")).toHaveTextContent("You’ve been signed out");
    expect(screen.getByText("editing")).toBeInTheDocument();
  });

  it("should_catch_up_when_the_tab_wakes_after_the_session_ended", () => {
    const { store } = renderWithStore(<SessionExpiry />);
    signIn(store, inMinutes(10));

    // The device slept: the clock moved on but no timer fired.
    vi.setSystemTime(NOW + 30 * MINUTE);
    act(() => {
      document.dispatchEvent(new Event("visibilitychange"));
    });
    advance(0);

    expect(store.getState().auth.user).toBeNull();
    expect(replace).toHaveBeenCalledWith("/login?from=%2Fcollection%2F25%3Fedit%3D1");
  });

  it("should_never_warn_a_visitor", () => {
    renderWithStore(<SessionExpiry />, { signedInAs: null });

    advance(120 * MINUTE);

    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("should_re_arm_the_timers_after_a_new_login", () => {
    const { store } = renderWithStore(<SessionExpiry />);
    signIn(store, inMinutes(3));
    advance(1 * MINUTE + 1000);
    expect(screen.getByText(/Your session ends in 2 minutes/)).toBeInTheDocument();

    // Logged in again: a fresh 60-minute session.
    signIn(store, inMinutes(61));
    expect(screen.queryByText(/Your session ends in 2 minutes/)).not.toBeInTheDocument();

    advance(10 * MINUTE); // the old session's end passes: nothing happens
    expect(store.getState().auth.user).not.toBeNull();
    expect(replace).not.toHaveBeenCalled();

    advance(49 * MINUTE); // T-2min of the new session
    expect(screen.getByText(/Your session ends in 2 minutes/)).toBeInTheDocument();
    expect(WARNING_LEAD_MS).toBe(2 * MINUTE);
  });
});
