import { screen, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { server } from "@/mocks/node";
import { API, renderWithStore, withCsrfCookie } from "@/test-utils/render";
import { LoginForm } from "./LoginForm";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, push: vi.fn() }),
  useSearchParams: () => new URLSearchParams("from=/pokemon/25"),
}));

async function submit(user: ReturnType<typeof renderWithStore>["user"]) {
  await user.type(screen.getByLabelText(/Email/), "ash@example.com");
  await user.type(screen.getByLabelText(/Password/), "wrong-password-123");
  await user.click(screen.getByRole("button", { name: "Log in" }));
}

beforeEach(() => {
  replace.mockClear();
  withCsrfCookie();
});

describe("LoginForm", () => {
  it("should_show_one_generic_message_for_bad_credentials", async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(
          { type: "/problems/invalid-credentials", title: "Unauthorized", status: 401 },
          { status: 401 },
        ),
      ),
    );
    const { user } = renderWithStore(<LoginForm />, { signedInAs: null });

    await submit(user);

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Email or password is incorrect.");
    await waitFor(() => expect(alert).toHaveFocus());
    expect(replace).not.toHaveBeenCalled();
  });

  it("should_say_when_to_try_again_after_a_429", async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json(
          { type: "/problems/rate-limited", title: "Too Many Requests", status: 429 },
          { status: 429, headers: { "Retry-After": "42" } },
        ),
      ),
    );
    const { user } = renderWithStore(<LoginForm />, { signedInAs: null });

    await submit(user);

    expect(await screen.findByRole("alert")).toHaveTextContent("Too many attempts. Try again in 42 seconds.");
  });

  it("should_go_back_to_the_safe_from_path_after_logging_in", async () => {
    server.use(
      http.post(`${API}/auth/login`, () =>
        HttpResponse.json({
          user: { id: "u", email: "ash@example.com", displayName: "Ash", role: "USER" },
          expiresAt: "2099-01-01T00:00:00Z",
        }),
      ),
    );
    const { user, store } = renderWithStore(<LoginForm />, { signedInAs: null });

    await submit(user);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/pokemon/25"));
    expect(store.getState().auth.user?.displayName).toBe("Ash");
  });
});
