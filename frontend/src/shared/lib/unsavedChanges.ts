import { useEffect, useSyncExternalStore } from "react";

/**
 * Tracks whether any form on the page has unsaved input (browser only). Used so the session
 * expiry never throws away an edit silently. Not Redux: it is UI-only and changes per keystroke.
 */
const dirtyForms = new Set<symbol>();
const listeners = new Set<() => void>();

function emit() {
  for (const listener of listeners) listener();
}

export function hasUnsavedChanges(): boolean {
  return dirtyForms.size > 0;
}

export function subscribeUnsavedChanges(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

/** A form reports its dirty state; cleared automatically when the form unmounts. */
export function useReportUnsavedChanges(isDirty: boolean) {
  useEffect(() => {
    if (!isDirty) return undefined;
    const key = Symbol("dirty-form");
    dirtyForms.add(key);
    emit();
    return () => {
      dirtyForms.delete(key);
      emit();
    };
  }, [isDirty]);
}

export function useHasUnsavedChanges(): boolean {
  return useSyncExternalStore(subscribeUnsavedChanges, hasUnsavedChanges, () => false);
}
