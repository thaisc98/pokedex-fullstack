import type { Metadata } from "next";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";

export const metadata: Metadata = { title: "Log in" };

/** Phase A placeholder so the header link isn't broken. Phase B: the real sign-in form. */
export default function LoginPage() {
  return (
    <EmptyState
      headingLevel={1}
      title="Log in"
      description="Signing in is coming soon. Until then, you can browse the whole catalog."
      action={<AppButtonLink href="/pokemon">Browse the catalog</AppButtonLink>}
    />
  );
}
