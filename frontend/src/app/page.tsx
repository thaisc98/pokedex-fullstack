import { Card, Typography } from "@heroui/react";

/**
 * P0 placeholder. In P5 this becomes `redirect('/pokemon')` (docs/ui-ux.md §3.1).
 */
export default function Home() {
  return (
    <main
      id="content"
      className="mx-auto flex min-h-dvh w-full max-w-2xl items-center px-4 py-8"
    >
      <Card className="w-full">
        <Card.Header>
          <Typography.Heading level={1}>Pokédex</Typography.Heading>
          <Card.Description>
            The project skeleton is ready. The catalog arrives in a later phase.
          </Card.Description>
        </Card.Header>
      </Card>
    </main>
  );
}
