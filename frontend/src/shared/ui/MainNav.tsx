"use client";

import { Button, Chip, Drawer, Separator } from "@heroui/react";
import { usePathname } from "next/navigation";
import { useSession } from "@/features/auth/useSession";
import { useSignOut } from "@/features/auth/useSignOut";
import { AppButtonLink, AppLink } from "./AppLink";

type NavItem = { href: string; label: string };

/** Catalog always; Collection only when signed in (ui-ux.md §3.2). */
function useNavItems(): NavItem[] {
  const { user } = useSession();
  return user
    ? [
        { href: "/pokemon", label: "Catalog" },
        { href: "/collection", label: "Collection" },
      ]
    : [{ href: "/pokemon", label: "Catalog" }];
}

function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function MainNav({ className = "" }: { className?: string }) {
  const pathname = usePathname();
  const items = useNavItems();
  return (
    <nav aria-label="Main" className={className}>
      <ul className="flex items-center gap-1">
        {items.map((item) => {
          const active = isActive(pathname, item.href);
          return (
            <li key={item.href}>
              <AppLink
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`rounded-lg px-3 py-2 text-sm font-medium no-underline hover:bg-default hover:no-underline ${
                  active ? "text-foreground underline decoration-accent decoration-2 underline-offset-8" : "text-muted"
                }`}
              >
                {item.label}
              </AppLink>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

/** Below 768 px the nav collapses into a ☰ button that opens a HeroUI Drawer (ui-ux.md §3.2). */
export function MobileMenu({ className = "" }: { className?: string }) {
  const pathname = usePathname();
  const items = useNavItems();
  const { user } = useSession();
  const { signOut } = useSignOut();
  return (
    <div className={className}>
      <Drawer>
        <Button isIconOnly variant="ghost" aria-label="Open menu">
          <svg aria-hidden="true" viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <path d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </Button>
        <Drawer.Backdrop>
          <Drawer.Content placement="right">
            <Drawer.Dialog aria-label="Menu">
              {({ close }) => (
                <>
                  <Drawer.CloseTrigger aria-label="Close menu" />
                  <Drawer.Header>
                    <Drawer.Heading>Menu</Drawer.Heading>
                  </Drawer.Header>
                  <Drawer.Body>
                    <nav aria-label="Main">
                      <ul className="flex flex-col gap-1">
                        {items.map((item) => {
                          const active = isActive(pathname, item.href);
                          return (
                            <li key={item.href}>
                              <AppLink
                                href={item.href}
                                onPress={close}
                                aria-current={active ? "page" : undefined}
                                className={`flex w-full rounded-lg px-3 py-3 font-medium no-underline hover:bg-default hover:no-underline ${
                                  active ? "bg-default text-foreground" : "text-foreground"
                                }`}
                              >
                                {item.label}
                              </AppLink>
                            </li>
                          );
                        })}
                      </ul>
                    </nav>
                    <Separator className="my-4" />
                    {user ? (
                      <div className="flex flex-col gap-3">
                        <div className="flex flex-col gap-1">
                          <span className="font-semibold">{user.displayName}</span>
                          <span className="text-xs text-muted">{user.email}</span>
                          <Chip size="sm" color={user.role === "ADMIN" ? "accent" : "default"} className="self-start">
                            {user.role === "ADMIN" ? "Admin" : "Trainer"}
                          </Chip>
                        </div>
                        <Button
                          variant="secondary"
                          fullWidth
                          onPress={() => {
                            close();
                            void signOut();
                          }}
                        >
                          Log out
                        </Button>
                      </div>
                    ) : (
                      <div className="flex flex-col gap-2">
                        <AppButtonLink href="/login" variant="secondary" className="w-full" onClick={close}>
                          Log in
                        </AppButtonLink>
                        <AppButtonLink href="/register" className="w-full" onClick={close}>
                          Register
                        </AppButtonLink>
                      </div>
                    )}
                  </Drawer.Body>
                </>
              )}
            </Drawer.Dialog>
          </Drawer.Content>
        </Drawer.Backdrop>
      </Drawer>
    </div>
  );
}
