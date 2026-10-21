"use client";

import { Avatar, Button, Chip, Dropdown, Header, Label, Separator, Skeleton } from "@heroui/react";
import { usePathname } from "next/navigation";
import { useAppSelector } from "@/store/hooks";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { selectAuthChecked, selectUser } from "./authSlice";
import { useSignOut } from "./useSignOut";

function initials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join("");
}

/** Header auth area (ui-ux.md §3.2): Log in / Register, or the user menu with role + Log out. */
export function UserMenu() {
  const checked = useAppSelector(selectAuthChecked);
  const user = useAppSelector(selectUser);
  const pathname = usePathname();
  const { signOut } = useSignOut();

  if (!checked) return <Skeleton className="h-9 w-32 rounded-full" />;

  if (!user) {
    const from = pathname && pathname !== "/login" && pathname !== "/register" ? `?from=${encodeURIComponent(pathname)}` : "";
    return (
      <div className="flex items-center gap-2">
        <AppButtonLink href={`/login${from}`} variant="ghost" size="sm">
          Log in
        </AppButtonLink>
        <AppButtonLink href="/register" variant="primary" size="sm">
          Register
        </AppButtonLink>
      </div>
    );
  }

  return (
    <Dropdown>
      <Button variant="ghost" size="sm" aria-label={`Account menu for ${user.displayName}`} className="gap-2 px-2">
        <Avatar size="sm">
          <Avatar.Fallback>{initials(user.displayName)}</Avatar.Fallback>
        </Avatar>
        <span className="max-w-32 truncate">{user.displayName}</span>
      </Button>
      <Dropdown.Popover placement="bottom end">
        <div className="flex flex-col gap-1 px-3 pt-3 pb-2">
          <span className="font-semibold">{user.displayName}</span>
          <span className="text-xs text-muted">{user.email}</span>
          <Chip size="sm" color={user.role === "ADMIN" ? "accent" : "default"} className="self-start">
            {user.role === "ADMIN" ? "Admin" : "Trainer"}
          </Chip>
        </div>
        <Separator />
        <Dropdown.Menu aria-label="Account" onAction={(key) => key === "logout" && void signOut()}>
          <Dropdown.Section>
            <Header className="sr-only">Account</Header>
            <Dropdown.Item id="logout" textValue="Log out">
              <Label>Log out</Label>
            </Dropdown.Item>
          </Dropdown.Section>
        </Dropdown.Menu>
      </Dropdown.Popover>
    </Dropdown>
  );
}
