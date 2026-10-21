"use client";

import { Alert, Button } from "@heroui/react";
import { useRouter } from "next/navigation";
import { useTransition } from "react";
import { problemCopy, type ProblemDetail } from "@/shared/lib/problemCopy";

type ErrorStateProps = {
  problem?: Pick<ProblemDetail, "type" | "status" | "detail" | "correlationId">;
  /** Overrides the default retry (a server re-render of the current route). */
  onRetry?: () => void;
  /** A reference to show when there's no correlationId (e.g. an error digest). */
  reference?: string;
};

/**
 * Error state (docs/ui-ux.md §6.4/§7): friendly copy from the problem-type map,
 * a Retry button and the correlationId for support. Never shows stack traces.
 */
export function ErrorState({ problem, onRetry, reference }: ErrorStateProps) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const copy = problemCopy(problem);
  const ref = (typeof problem?.correlationId === "string" && problem.correlationId) || reference;

  const retry = () => {
    if (onRetry) return onRetry();
    startTransition(() => router.refresh());
  };

  return (
    <Alert status="danger" className="w-full">
      <Alert.Indicator />
      <Alert.Content className="gap-2">
        <Alert.Title>{copy.title}</Alert.Title>
        <Alert.Description>{copy.message}</Alert.Description>
        {ref && (
          <p className="text-xs text-muted">
            Reference: <code className="font-mono">{ref}</code>
          </p>
        )}
        <div>
          <Button size="sm" variant="secondary" onPress={retry} isPending={isPending}>
            Retry
          </Button>
        </div>
      </Alert.Content>
    </Alert>
  );
}
