import { AppButtonLink } from "@/shared/ui/AppLink";
import { EmptyState } from "@/shared/ui/EmptyState";

/** App-wide 404 for unknown routes. */
export default function NotFound() {
  return (
    <EmptyState
      headingLevel={1}
      title="Page not found"
      description="The page you’re looking for doesn’t exist or has moved."
      action={<AppButtonLink href="/pokemon">Browse the catalog</AppButtonLink>}
    />
  );
}
