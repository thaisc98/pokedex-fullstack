import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { UserDto } from "@/shared/api/types";

/**
 * Who is signed in (architecture.md §3.3). Holds the user and the token expiry only:
 * the token itself is in an httpOnly cookie and never reaches JavaScript.
 * `checked` is false until we know (it avoids flashing "Log in" for a signed-in user).
 */
export type AuthState = {
  user: UserDto | null;
  expiresAt: string | null;
  checked: boolean;
};

const initialState: AuthState = { user: null, expiresAt: null, checked: false };

export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    signedIn(state, action: PayloadAction<{ user: UserDto; expiresAt: string }>) {
      state.user = action.payload.user;
      state.expiresAt = action.payload.expiresAt;
      state.checked = true;
    },
    userLoaded(state, action: PayloadAction<UserDto>) {
      state.user = action.payload;
      state.checked = true;
    },
    sessionEnded(state) {
      state.user = null;
      state.expiresAt = null;
      state.checked = true;
    },
  },
  selectors: {
    selectUser: (state) => state.user,
    selectIsAdmin: (state) => state.user?.role === "ADMIN",
    selectAuthChecked: (state) => state.checked,
  },
});

export const { signedIn, userLoaded, sessionEnded } = authSlice.actions;
export const { selectUser, selectIsAdmin, selectAuthChecked } = authSlice.selectors;
