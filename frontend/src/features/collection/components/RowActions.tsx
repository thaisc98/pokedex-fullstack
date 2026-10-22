"use client";

import { Button, Dropdown, Label, toast } from "@heroui/react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { selectIsAdmin } from "@/features/auth/authSlice";
import { isApiError } from "@/shared/api/apiError";
import { formatName } from "@/shared/lib/formatName";
import { problemCopy } from "@/shared/lib/problemCopy";
import { useAppSelector } from "@/store/hooks";
import { useSyncMutation } from "../collectionApi";
import { DeleteDialog } from "./DeleteDialog";

/** Row menu (ui-ux.md §5.3): View · Edit · Refresh from PokeAPI · Delete (ADMIN only). */
export function RowActions({ pokedexId, name }: { pokedexId: number; name: string }) {
  const router = useRouter();
  const isAdmin = useAppSelector(selectIsAdmin);
  const [sync] = useSyncMutation();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const display = formatName(name);

  const onAction = async (key: React.Key) => {
    switch (key) {
      case "view":
        router.push(`/collection/${pokedexId}`);
        break;
      case "edit":
        router.push(`/collection/${pokedexId}?edit=1`);
        break;
      case "refresh":
        try {
          await sync(pokedexId).unwrap();
          toast.success(`${display} refreshed from PokeAPI`, { description: "Your custom fields were kept." });
        } catch (error) {
          toast.danger("Couldn’t refresh", {
            description: isApiError(error) ? problemCopy(error.problem).message : undefined,
          });
        }
        break;
      case "delete":
        setConfirmOpen(true);
        break;
    }
  };

  return (
    <>
      <Dropdown>
        <Button isIconOnly size="sm" variant="ghost" aria-label={`Actions for ${display}`}>
          <svg aria-hidden="true" viewBox="0 0 24 24" className="size-5" fill="currentColor">
            <circle cx="5" cy="12" r="2" />
            <circle cx="12" cy="12" r="2" />
            <circle cx="19" cy="12" r="2" />
          </svg>
        </Button>
        <Dropdown.Popover placement="bottom end">
          <Dropdown.Menu aria-label={`Actions for ${display}`} onAction={(key) => void onAction(key)}>
            <Dropdown.Item id="view" textValue="View">
              <Label>View</Label>
            </Dropdown.Item>
            <Dropdown.Item id="edit" textValue="Edit">
              <Label>Edit</Label>
            </Dropdown.Item>
            <Dropdown.Item id="refresh" textValue="Refresh from PokeAPI">
              <Label>Refresh from PokeAPI</Label>
            </Dropdown.Item>
            {isAdmin ? (
              <Dropdown.Item id="delete" textValue="Delete" variant="danger">
                <Label>Delete</Label>
              </Dropdown.Item>
            ) : null}
          </Dropdown.Menu>
        </Dropdown.Popover>
      </Dropdown>
      {isAdmin && (
        <DeleteDialog pokedexId={pokedexId} name={name} isOpen={confirmOpen} onOpenChange={setConfirmOpen} />
      )}
    </>
  );
}
