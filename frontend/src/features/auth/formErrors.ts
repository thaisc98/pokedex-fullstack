import type { FieldErrors, FieldValues } from "react-hook-form";
import { fieldErrors, isApiError, type ApiError } from "@/shared/api/apiError";
import { problemCopy } from "@/shared/lib/problemCopy";
import type { SummaryItem } from "@/shared/ui/ErrorSummary";

/** Flatten React Hook Form errors (nested + arrays) into summary items, in field order. */
export function summaryFromFormErrors(
  errors: FieldErrors<FieldValues>,
  idFor: (path: string) => string,
  prefix = "",
): SummaryItem[] {
  const items: SummaryItem[] = [];
  for (const [key, value] of Object.entries(errors)) {
    if (!value || key === "root") continue;
    const path = prefix ? `${prefix}.${key}` : key;
    if (typeof value === "object" && "message" in value && typeof value.message === "string" && value.message) {
      items.push({ fieldId: idFor(path), message: value.message });
    } else if (typeof value === "object") {
      items.push(...summaryFromFormErrors(value as FieldErrors<FieldValues>, idFor, path));
    }
  }
  return items;
}

/** Message for a failed auth request; `retryAfter` drives the 429 copy (ui-ux.md §5.5). */
export function authErrorMessage(error: unknown): string {
  if (!isApiError(error)) return problemCopy(undefined).message;
  if (error.status === 429) {
    return error.retryAfter !== undefined
      ? `Too many attempts. Try again in ${error.retryAfter} seconds.`
      : "Too many attempts. Please wait a moment and try again.";
  }
  if (error.problem.type === "/problems/invalid-credentials" || error.status === 401) {
    return "Email or password is incorrect.";
  }
  if (error.problem.type === "/problems/email-taken") {
    return "An account with this email already exists. Try logging in instead.";
  }
  const copy = problemCopy(error.problem);
  return copy.message;
}

export function serverFieldErrors(error: unknown): { field: string; message: string }[] {
  return isApiError(error) ? fieldErrors(error as ApiError) : [];
}
