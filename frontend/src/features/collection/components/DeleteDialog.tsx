"use client";

import { AlertDialog, Button, toast } from "@heroui/react";
import { isApiError } from "@/shared/api/apiError";
import { formatName } from "@/shared/lib/formatName";
import { problemCopy } from "@/shared/lib/problemCopy";
import { useDeleteCollectedMutation } from "../collectionApi";

type Props = {
  pokedexId: number;
  name: string;
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onDeleted?: () => void;
};

/** ADMIN delete confirmation naming the record (ui-ux.md §7): focus trap, Esc closes. */
export function DeleteDialog({ pokedexId, name, isOpen, onOpenChange, onDeleted }: Props) {
  const [remove, { isLoading }] = useDeleteCollectedMutation();
  const display = formatName(name);

  const confirm = async () => {
    try {
      await remove(pokedexId).unwrap();
      toast.success(`${display} was removed from the collection`);
      onOpenChange(false);
      onDeleted?.();
    } catch (error) {
      toast.danger("Couldn’t delete", {
        description: isApiError(error) ? problemCopy(error.problem).message : undefined,
      });
    }
  };

  return (
    <AlertDialog.Backdrop isOpen={isOpen} onOpenChange={onOpenChange} isKeyboardDismissDisabled={false}>
      <AlertDialog.Container size="sm">
        <AlertDialog.Dialog>
          <AlertDialog.Header>
            <AlertDialog.Icon status="danger" />
            <AlertDialog.Heading>Delete {display} from the collection?</AlertDialog.Heading>
          </AlertDialog.Header>
          <AlertDialog.Body>
            <p>This removes the record and its custom fields for everyone. It can’t be undone.</p>
          </AlertDialog.Body>
          <AlertDialog.Footer>
            <Button variant="tertiary" slot="close" isDisabled={isLoading}>
              Cancel
            </Button>
            <Button variant="danger" onPress={confirm} isPending={isLoading} isDisabled={isLoading}>
              Delete
            </Button>
          </AlertDialog.Footer>
        </AlertDialog.Dialog>
      </AlertDialog.Container>
    </AlertDialog.Backdrop>
  );
}
