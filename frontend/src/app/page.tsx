import { redirect } from "next/navigation";

/** `/` → `/pokemon` (docs/ui-ux.md §3.1). */
export default function Home() {
  redirect("/pokemon");
}
