"use client";

import { toast } from "@heroui/react";
import { useRouter } from "next/navigation";
import { useCallback } from "react";
import { useLogoutMutation } from "./authApi";

/** Log out, then go to the catalog. Local state is cleared even if the request fails. */
export function useSignOut() {
  const router = useRouter();
  const [logout, { isLoading }] = useLogoutMutation();
  const signOut = useCallback(async () => {
    try {
      await logout().unwrap();
    } catch {
      // Already signed out on the server (401) or offline: the local session is gone anyway.
    }
    toast.success("You’re signed out");
    router.push("/pokemon");
    router.refresh();
  }, [logout, router]);
  return { signOut, isSigningOut: isLoading };
}
