import type { Metadata } from "next";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";

export const metadata: Metadata = { title: "Register" };

/** Phase A placeholder so the header link isn't broken. Phase B: the real registration form. */
export default function RegisterPage() {
  return (
    <EmptyState
      headingLevel={1}
      title="Create an account"
      description="Registration is coming soon. Until then, you can browse the whole catalog."
      action={<AppButtonLink href="/pokemon">Browse the catalog</AppButtonLink>}
    />
  );
}
