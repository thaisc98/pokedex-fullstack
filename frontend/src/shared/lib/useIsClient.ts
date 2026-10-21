import { useSyncExternalStore } from "react";

/**
 * Hydration does not re-check `getSnapshot`, so a component that hydrates with `false`
 * would keep it until something else re-renders it. Notifying once right after subscribing
 * makes React compare again and switch to `true`.
 */
const subscribeOnce = (onChange: () => void) => {
  const id = setTimeout(onChange, 0);
  return () => clearTimeout(id);
};

/** False during SSR and hydration, true afterwards. No effect + setState needed. */
export function useIsClient(): boolean {
  return useSyncExternalStore(
    subscribeOnce,
    () => true,
    () => false,
  );
}
