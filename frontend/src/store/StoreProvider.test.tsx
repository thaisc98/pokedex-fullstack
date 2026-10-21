import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { baseApi } from "@/shared/api/baseApi";
import type { AppStore } from "./makeStore";
import { useAppStore } from "./hooks";
import { StoreProvider } from "./StoreProvider";

function StoreProbe({ onStore }: { onStore: (store: AppStore) => void }) {
  onStore(useAppStore());
  return <p>probe</p>;
}

describe("StoreProvider", () => {
  it("should_render_its_children", () => {
    render(
      <StoreProvider>
        <h1>Pokédex</h1>
      </StoreProvider>,
    );

    expect(screen.getByRole("heading", { level: 1, name: "Pokédex" })).toBeInTheDocument();
  });

  it("should_provide_a_store_with_the_rtk_query_reducer_mounted", () => {
    let store: AppStore | undefined;
    render(
      <StoreProvider>
        <StoreProbe onStore={(s) => (store = s)} />
      </StoreProvider>,
    );

    expect(store?.getState()).toHaveProperty(baseApi.reducerPath);
  });

  it("should_create_a_separate_store_for_each_provider_instance", () => {
    const stores: AppStore[] = [];
    render(
      <>
        <StoreProvider>
          <StoreProbe onStore={(s) => stores.push(s)} />
        </StoreProvider>
        <StoreProvider>
          <StoreProbe onStore={(s) => stores.push(s)} />
        </StoreProvider>
      </>,
    );

    expect(new Set(stores).size).toBe(2);
  });

  it("should_keep_the_same_store_across_re_renders", () => {
    const stores: AppStore[] = [];
    const tree = (
      <StoreProvider>
        <StoreProbe onStore={(s) => stores.push(s)} />
      </StoreProvider>
    );
    const { rerender } = render(tree);
    rerender(
      <StoreProvider>
        <StoreProbe onStore={(s) => stores.push(s)} />
      </StoreProvider>,
    );

    expect(stores.length).toBeGreaterThanOrEqual(2);
    expect(new Set(stores).size).toBe(1);
  });
});
