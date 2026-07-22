import { PayloadAction, configureStore, createSlice } from '@reduxjs/toolkit';

export type CurrentUser = {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  role: string;
  status: string;
};

type StoredAuth = {
  accessToken: string;
  refreshToken: string;
  user: CurrentUser;
};

type AuthState = {
  accessToken: string | null;
  refreshToken: string | null;
  user: CurrentUser | null;
  darkMode: boolean;
};

const storageKey = 'adaptive-gateway.auth';
const themeKey = 'adaptive-gateway.dark-mode';

function loadAuth(): StoredAuth | null {
  const raw = localStorage.getItem(storageKey);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    localStorage.removeItem(storageKey);
    return null;
  }
}

const existing = loadAuth();

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    accessToken: existing?.accessToken ?? null,
    refreshToken: existing?.refreshToken ?? null,
    user: existing?.user ?? null,
    darkMode: localStorage.getItem(themeKey) === 'true'
  } satisfies AuthState,
  reducers: {
    signedIn(state, action: PayloadAction<StoredAuth>) {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.user = action.payload.user;
      localStorage.setItem(storageKey, JSON.stringify(action.payload));
    },
    signedOut(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      localStorage.removeItem(storageKey);
    },
    userLoaded(state, action: PayloadAction<CurrentUser>) {
      state.user = action.payload;
      if (state.accessToken && state.refreshToken) {
        localStorage.setItem(
          storageKey,
          JSON.stringify({
            accessToken: state.accessToken,
            refreshToken: state.refreshToken,
            user: action.payload
          })
        );
      }
    },
    themeToggled(state) {
      state.darkMode = !state.darkMode;
      localStorage.setItem(themeKey, String(state.darkMode));
    }
  }
});

export const { signedIn, signedOut, userLoaded, themeToggled } = authSlice.actions;

export const store = configureStore({
  reducer: {
    auth: authSlice.reducer
  }
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
