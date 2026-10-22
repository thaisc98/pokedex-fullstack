"use client";

import { Alert } from "@heroui/react";
import { useEffect, useRef } from "react";

export type SummaryItem = { fieldId?: string; message: string };

type ErrorSummaryProps = {
  items: SummaryItem[];
  /** Change this (e.g. a submit counter) to move focus to the summary again. */
  focusKey: number;
  title?: string;
};

/**
 * Error summary at the top of a form (ui-ux.md §4 US04, §6.4): receives focus on a failed
 * submit and links each message to its field.
 */
export function ErrorSummary({ items, focusKey, title }: ErrorSummaryProps) {
  const ref = useRef<HTMLDivElement>(null);
  const focusedFor = useRef(0);
  const hasItems = items.length > 0;
  useEffect(() => {
    // Focus once per failed submit. The errors can arrive a render after the submit counter
    // (React Hook Form updates formState separately), so wait until there is something to show.
    if (hasItems && focusKey > 0 && focusedFor.current !== focusKey) {
      focusedFor.current = focusKey;
      ref.current?.focus();
    }
  }, [focusKey, hasItems]);

  if (items.length === 0) return null;
  // A single form-level message (e.g. "Email or password is incorrect.") is the whole summary.
  if (items.length === 1 && !items[0].fieldId) {
    return (
      <div ref={ref} tabIndex={-1} role="alert" className="rounded-2xl outline-none focus-visible:ring-2 focus-visible:ring-focus">
        <Alert status="danger">
          <Alert.Indicator />
          <Alert.Content>
            <Alert.Title>{items[0].message}</Alert.Title>
          </Alert.Content>
        </Alert>
      </div>
    );
  }
  const heading =
    title ?? (items.length === 1 ? "There is 1 problem with this form" : `There are ${items.length} problems with this form`);

  return (
    <div ref={ref} tabIndex={-1} className="rounded-2xl outline-none focus-visible:ring-2 focus-visible:ring-focus">
      <Alert status="danger">
        <Alert.Indicator />
        <Alert.Content>
          <Alert.Title>{heading}</Alert.Title>
          <ul className="mt-1 flex list-disc flex-col gap-1 pl-5 text-sm">
            {items.map((item, i) => (
              <li key={`${item.fieldId ?? "form"}-${i}`}>
                {item.fieldId ? (
                  <a
                    href={`#${item.fieldId}`}
                    className="underline underline-offset-2"
                    onClick={(event) => {
                      const field = document.getElementById(item.fieldId!);
                      if (field) {
                        event.preventDefault();
                        field.focus();
                      }
                    }}
                  >
                    {item.message}
                  </a>
                ) : (
                  item.message
                )}
              </li>
            ))}
          </ul>
        </Alert.Content>
      </Alert>
    </div>
  );
}
