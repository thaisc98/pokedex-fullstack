"use client";

import { Button, Skeleton, toast } from "@heroui/react";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/features/auth/useSession";
import { useSyncMutation } from "@/features/collection/collectionApi";
import { isApiError } from "@/shared/api/apiError";
import { problemCopy } from "@/shared/lib/problemCopy";

type Props = { pokedexId: number; name: string };

/**
 * US03 sync (ui-ux.md §4): signed out → log in and come back; signed in → POST /collection.
 * 201 "Added", 200 "Refreshed; custom fields kept", 503 inline error with Retry.
 */
export function AddToCollectionButton({ pokedexId, name }: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const { ready: checked, user } = useSession();
  const [sync, { isLoading }] = useSyncMutation();
  const [inCollection, setInCollection] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!checked) return <Skeleton className="h-11 w-48 rounded-xl" />;

  if (!user) {
    return (
      <div className="flex flex-col items-start gap-1.5">
        <Button onPress={() => router.push(`/login?from=${encodeURIComponent(pathname)}`)}>
          <PlusIcon />
          Add to collection
        </Button>
        <p className="text-xs text-muted">You’ll be asked to log in first.</p>
      </div>
    );
  }

  const onSync = async () => {
    setError(null);
    try {
      const { created } = await sync(pokedexId).unwrap();
      setInCollection(true);
      if (created) {
        const id = toast.success(`${name} added to your collection`, {
          actionProps: {
            children: "View",
            onPress: () => {
              toast.close(id);
              router.push(`/collection/${pokedexId}`);
            },
          },
        });
      } else {
        toast.success("Refreshed from PokeAPI", { description: "Your custom fields were kept." });
      }
    } catch (e) {
      setError(isApiError(e) ? problemCopy(e.problem).message : problemCopy(undefined).message);
    }
  };

  return (
    <div className="flex flex-col items-start gap-1.5">
      <Button
        variant={inCollection ? "secondary" : "primary"}
        onPress={onSync}
        isPending={isLoading}
        isDisabled={isLoading}
      >
        {inCollection ? <CheckIcon /> : <PlusIcon />}
        {inCollection ? "In collection · Refresh" : "Add to collection"}
      </Button>
      {error && (
        <p role="alert" className="flex items-center gap-2 text-sm text-danger">
          {error}
          <Button size="sm" variant="ghost" onPress={onSync}>
            Retry
          </Button>
        </p>
      )}
    </div>
  );
}

function PlusIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d="M12 5v14M5 12h14" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12l5 5L20 7" />
    </svg>
  );
}
