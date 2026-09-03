# Code Review: Issues & Fixes

## Frontend (React/TypeScript) - Issues Found

### 1. **Missing Error Boundary** (High Priority)
**Issue**: No error boundary wraps the app. Any component crash terminates the entire UI.
**Impact**: Users see a blank page on JS errors with no recovery path.
**Fix**: Create an ErrorBoundary component and wrap `<App>`.

```typescript
// frontend/src/ErrorBoundary.tsx
import { Component, ReactNode } from 'react';
import { Alert, Box, Button, Container } from '@mui/material';

type Props = { children: ReactNode };
type State = { hasError: boolean; error: Error | null };

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error) {
    console.error('Boundary caught:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm" sx={{ py: 4 }}>
          <Alert severity="error" sx={{ mb: 2 }}>
            An unexpected error occurred. Please refresh the page.
          </Alert>
          <Box>{this.state.error?.message}</Box>
          <Button variant="contained" onClick={() => window.location.reload()}>
            Reload
          </Button>
        </Container>
      );
    }
    return this.props.children;
  }
}
```

Then wrap App:
```typescript
// frontend/src/main.tsx
<ErrorBoundary>
  <Provider store={store}>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </Provider>
</ErrorBoundary>
```

---

### 2. **Dialog Closes on Submission Error** (Medium Priority)
**Issue**: In `SubmitDialog`, when a request fails, the dialog still calls `onClose()` in the catch block.
**Current code** (e.g., `UpstreamDialog`):
```typescript
onSubmit={async () => { 
  await requestData('post', '/api/gateway/upstreams', form); 
  onClose();  // This runs even on error!
  onSaved(); 
}}
```
**Fix**: Restructure `SubmitDialog` to close only on successful submission:

```typescript
function SubmitDialog({ title, open, onClose, onSubmit, children }: ...) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setLoading(true);
    setError(null);
    try {
      await onSubmit();
      onClose(); // ✓ Only close on success
    } catch (requestError) {
      setError(apiErrorMessage(requestError)); // ✓ Show error, stay open
    } finally {
      setLoading(false);
    }
  }
  // ... rest unchanged
}
```

---

### 3. **No Request Cancellation on Component Unmount** (Medium Priority)
**Issue**: Multiple `useEndpoint` hooks and `useEffect` calls don't cancel in-flight requests when components unmount.
**Risk**: Memory leaks, "state update on unmounted component" warnings, stale state updates.
**Fix**: Use AbortController:

```typescript
function useEndpoint<T>(url: string, active = true): LoadState<T> & { refresh: () => Promise<void> } {
  const [state, setState] = useState<LoadState<T>>({ data: null, loading: active, error: null });
  const abortControllerRef = useRef<AbortController | null>(null);

  const refresh = useCallback(async () => {
    if (!active) return;
    
    // Cancel previous request
    abortControllerRef.current?.abort();
    abortControllerRef.current = new AbortController();

    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const data = await requestData<T>('get', url, { 
        signal: abortControllerRef.current.signal 
      });
      setState({ data, loading: false, error: null });
    } catch (error) {
      if (error instanceof Error && error.name !== 'AbortError') {
        setState({ data: null, loading: false, error: apiErrorMessage(error) });
      }
    }
  }, [active, url]);

  useEffect(() => {
    void refresh();
    return () => abortControllerRef.current?.abort(); // ✓ Cleanup on unmount
  }, [refresh]);

  return { ...state, refresh };
}
```

Update `requestData` to accept `signal`:
```typescript
export async function requestData<T>(
  method: 'get' | 'post' | 'put' | 'delete', 
  url: string, 
  body?: unknown,
  options?: { signal?: AbortSignal }
): Promise<T> {
  const response = await api.request<ApiEnvelope<T>>({
    method,
    url,
    data: body,
    signal: options?.signal
  });
  return response.data.data;
}
```

---

### 4. **No Auto-Logout on 401/403** (Medium Priority)
**Issue**: If backend returns 401 (unauthorized), the interceptor doesn't clear auth state. Stale tokens get reused.
**Fix**: Add interceptor response handler:

```typescript
// frontend/src/api.ts
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(storageKey);
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

---

### 5. **Type Safety: `AnyRecord` Overuse** (Low Priority)
**Issue**: All API responses use `AnyRecord` instead of specific types.
**Impact**: No IDE autocomplete, runtime type errors possible.
**Fix**: Define response DTOs:

```typescript
// frontend/src/types/api.ts
export interface GatewayRoute {
  id: string;
  routeKey: string;
  name: string;
  pathPattern: string;
  allowedMethods: string[];
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface Upstream {
  id: string;
  name: string;
  baseUrl: string;
  status: 'ACTIVE' | 'INACTIVE';
  timeoutMs: number;
  retryCount: number;
}

// Then use:
const routes = useEndpoint<PageResponse<GatewayRoute>>(`/api/gateway/routes?size=${pageSize}`);
```

---

## Docker Issues

### **Frontend Dockerfile**
1. **Missing CMD**: Container has no explicit CMD/ENTRYPOINT. Should be:
   ```dockerfile
   CMD ["nginx", "-g", "daemon off;"]
   ```

2. **HEALTHCHECK uses `wget`**: Not installed in Alpine nginx. Fix:
   ```dockerfile
   HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
     CMD nc -zv 127.0.0.1 80 || exit 1
   ```
   Or install curl instead of wget.

**Fixed Frontend Dockerfile:**
```dockerfile
FROM node:22-alpine AS build

WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html postcss.config.js tailwind.config.js tsconfig.json tsconfig.node.json vite.config.ts ./
COPY src src
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD test -f /var/run/nginx.pid || exit 1
```

---

### **docker-compose.yml Issues**

1. **Backend Healthcheck Fragile**: TCP probe is unreliable. Change to:
   ```yaml
   healthcheck:
     test: ["CMD", "curl", "-f", "http://127.0.0.1:8080/actuator/health"]
     interval: 15s
     timeout: 5s
     retries: 20
     start_period: 45s
   ```

2. **Unpinned Image Tags**: Add patch versions for reproducibility:
   ```yaml
   postgres:
     image: postgres:16.4-alpine    # ← Pinned to 16.4
   redis:
     image: redis:7.2-alpine        # ← Pinned to 7.2
   kafka:
     image: apache/kafka:3.7.2      # ← Already pinned ✓
   ```

3. **Kafka Single-Node Risk**: With `OFFSETS_TOPIC_REPLICATION_FACTOR: 1`, container loss = data loss. Add to README:
   > **⚠️ Warning**: This Kafka setup is single-node for development. Do NOT use in production. For production, set `OFFSETS_TOPIC_REPLICATION_FACTOR: 3`.

---

## Summary of Fixes

| Issue | Severity | Fix | Time |
|-------|----------|-----|------|
| No Error Boundary | High | Create ErrorBoundary.tsx | 10 min |
| Dialog closes on error | Medium | Restructure SubmitDialog | 5 min |
| No request cancellation | Medium | Add AbortController | 15 min |
| No auto-logout on 401 | Medium | Add response interceptor | 5 min |
| Type safety (AnyRecord) | Low | Define DTO types | 20 min |
| Frontend Dockerfile | Medium | Add CMD, fix HEALTHCHECK | 5 min |
| docker-compose.yml | Low | Pin image versions, docs | 5 min |

**Total impact of fixes**: Improved error handling, prevented memory leaks, better security, and type safety.
