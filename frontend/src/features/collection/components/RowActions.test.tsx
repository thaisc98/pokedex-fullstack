import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { admin, renderWithStore, trainer } from "@/test-utils/render";
import { RowActions } from "./RowActions";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("RowActions", () => {
  it("should_not_offer_delete_to_a_user", async () => {
    const { user } = renderWithStore(<RowActions pokedexId={25} name="pikachu" />, { signedInAs: trainer });

    await user.click(screen.getByRole("button", { name: "Actions for Pikachu" }));

    expect(await screen.findByRole("menuitem", { name: "Edit" })).toBeInTheDocument();
    expect(screen.queryByRole("menuitem", { name: "Delete" })).not.toBeInTheDocument();
  });

  it("should_ask_an_admin_to_confirm_a_delete_naming_the_record", async () => {
    const { user } = renderWithStore(<RowActions pokedexId={25} name="pikachu" />, { signedInAs: admin });

    await user.click(screen.getByRole("button", { name: "Actions for Pikachu" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent("Delete Pikachu from the collection?");
  });
});
