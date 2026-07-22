import {
  Add,
  Assessment,
  AutoGraph,
  Brightness4,
  Brightness7,
  Delete,
  Key,
  Logout,
  ManageAccounts,
  Menu as MenuIcon,
  Refresh,
  Router,
  Security,
  Speed,
  Timeline,
  WarningAmber
} from '@mui/icons-material';
import {
  Alert,
  AppBar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  CssBaseline,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Switch,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  ThemeProvider,
  Toolbar,
  Tooltip,
  Typography,
  createTheme
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material/Select';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip as ChartTooltip, XAxis, YAxis } from 'recharts';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { apiErrorMessage, requestData, type PageResponse } from './api';
import { signedIn, signedOut, themeToggled, userLoaded, type RootState } from './store';
import { useDispatch, useSelector } from 'react-redux';

type AnyRecord = Record<string, unknown>;
type Toast = { type: 'success' | 'error'; message: string };
type LoadState<T> = { data: T | null; loading: boolean; error: string | null };

const drawerWidth = 248;
const pageSize = 20;

const navItems = [
  { label: 'Overview', path: '/dashboard', icon: <Assessment /> },
  { label: 'Gateway', path: '/gateway', icon: <Router /> },
  { label: 'Rate Limits', path: '/rate-limits', icon: <Speed /> },
  { label: 'Consumers', path: '/consumers', icon: <ManageAccounts /> },
  { label: 'Operations', path: '/operations', icon: <Timeline /> },
  { label: 'Adaptive', path: '/adaptive', icon: <AutoGraph /> },
  { label: 'Anomalies', path: '/anomalies', icon: <WarningAmber /> }
];

function useAppDispatch() {
  return useDispatch<typeof import('./store').store.dispatch>();
}

function useAppSelector<T>(selector: (state: RootState) => T) {
  return useSelector(selector);
}

function isPage<T>(value: unknown): value is PageResponse<T> {
  return Boolean(value && typeof value === 'object' && 'content' in value);
}

function pageContent<T>(page: PageResponse<T> | T[] | null): T[] {
  if (!page) {
    return [];
  }
  return Array.isArray(page) ? page : page.content;
}

function shortValue(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.join(', ');
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function formatDate(value: unknown): string {
  if (!value || typeof value !== 'string') {
    return '-';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function statusColor(value: unknown): 'default' | 'success' | 'warning' | 'error' | 'info' {
  const normalized = String(value ?? '').toUpperCase();
  if (['ACTIVE', 'ALLOW', 'HEALTHY', 'LOW', 'PUBLISHED'].includes(normalized)) {
    return 'success';
  }
  if (['INACTIVE', 'REVOKED', 'DISABLED', 'BLOCK'].includes(normalized)) {
    return 'error';
  }
  if (['MEDIUM', 'PENDING', 'DEGRADED'].includes(normalized)) {
    return 'warning';
  }
  if (['HIGH', 'CRITICAL'].includes(normalized)) {
    return 'error';
  }
  return 'default';
}

function useEndpoint<T>(url: string, active = true): LoadState<T> & { refresh: () => Promise<void> } {
  const [state, setState] = useState<LoadState<T>>({ data: null, loading: active, error: null });

  const refresh = useCallback(async () => {
    if (!active) {
      return;
    }
    setState((current) => ({ ...current, loading: true, error: null }));
    try {
      const data = await requestData<T>('get', url);
      setState({ data, loading: false, error: null });
    } catch (error) {
      setState({ data: null, loading: false, error: apiErrorMessage(error) });
    }
  }, [active, url]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return { ...state, refresh };
}

function DataPanel<T extends AnyRecord>({
  title,
  columns,
  state,
  actions,
  empty = 'No records returned.'
}: {
  title: string;
  columns: { key: keyof T & string; label: string; date?: boolean; chip?: boolean }[];
  state: LoadState<PageResponse<T> | T[]>;
  actions?: React.ReactNode;
  empty?: string;
}) {
  const rows = pageContent<T>(state.data);

  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, overflow: 'hidden' }}>
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ px: 2, py: 1.5 }}>
        <Typography variant="h6">{title}</Typography>
        <Stack direction="row" spacing={1}>
          {actions}
        </Stack>
      </Stack>
      <Divider />
      {state.loading ? (
        <Stack alignItems="center" justifyContent="center" sx={{ minHeight: 180 }}>
          <CircularProgress size={28} />
        </Stack>
      ) : state.error ? (
        <Alert severity="error" sx={{ borderRadius: 0 }}>
          {state.error}
        </Alert>
      ) : rows.length === 0 ? (
        <Box sx={{ px: 2, py: 5, color: 'text.secondary' }}>{empty}</Box>
      ) : (
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell key={column.key}>{column.label}</TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row, index) => (
                <TableRow key={String(row.id ?? index)} hover>
                  {columns.map((column) => {
                    const value = column.date ? formatDate(row[column.key]) : shortValue(row[column.key]);
                    return (
                      <TableCell key={column.key} sx={{ maxWidth: 320, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {column.chip ? <Chip size="small" color={statusColor(row[column.key])} label={value} /> : value}
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
      {isPage<T>(state.data) && (
        <Box sx={{ px: 2, py: 1, color: 'text.secondary', typography: 'caption' }}>
          {state.data.totalElements} total records
        </Box>
      )}
    </Paper>
  );
}

function PageTitle({ title, action }: { title: string; action?: React.ReactNode }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ xs: 'stretch', sm: 'center' }} justifyContent="space-between" spacing={2} sx={{ mb: 2 }}>
      <Typography variant="h4" sx={{ fontWeight: 700 }}>{title}</Typography>
      {action}
    </Stack>
  );
}

function LoginPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const auth = await requestData<{
        accessToken: string;
        refreshToken: string;
        user: import('./store').CurrentUser;
      }>('post', '/api/auth/login', { usernameOrEmail, password });
      dispatch(signedIn(auth));
      navigate('/dashboard', { replace: true });
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <ThemeFrame>
      <CssBaseline />
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'background.default', px: 2 }}>
        <Paper component="form" onSubmit={submit} variant="outlined" sx={{ width: '100%', maxWidth: 420, p: 3, borderRadius: 2 }}>
          <Stack spacing={2.25}>
            <Box>
              <Typography variant="h4" sx={{ fontWeight: 700 }}>Adaptive Gateway</Typography>
              <Typography color="text.secondary">Operations console</Typography>
            </Box>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField label="Username or email" value={usernameOrEmail} onChange={(event) => setUsernameOrEmail(event.target.value)} required autoFocus />
            <TextField label="Password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
            <Button type="submit" variant="contained" size="large" disabled={loading} startIcon={loading ? <CircularProgress color="inherit" size={18} /> : <Security />}>
              Sign in
            </Button>
          </Stack>
        </Paper>
      </Box>
    </ThemeFrame>
  );
}

function ProtectedRoute({ children }: { children: React.ReactElement }) {
  const accessToken = useAppSelector((state) => state.auth.accessToken);
  const dispatch = useAppDispatch();

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    requestData<import('./store').CurrentUser>('get', '/api/auth/me')
      .then((user) => dispatch(userLoaded(user)))
      .catch(() => dispatch(signedOut()));
  }, [accessToken, dispatch]);

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function ThemeFrame({ children }: { children: React.ReactNode }) {
  const darkMode = useAppSelector((state) => state.auth.darkMode);
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: darkMode ? 'dark' : 'light',
          primary: { main: '#1f6feb' },
          secondary: { main: '#0f766e' },
          background: {
            default: darkMode ? '#111827' : '#f6f8fb',
            paper: darkMode ? '#182033' : '#ffffff'
          }
        },
        shape: { borderRadius: 8 },
        typography: {
          fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
          button: { textTransform: 'none', fontWeight: 700 }
        },
        components: {
          MuiButton: { styleOverrides: { root: { borderRadius: 8 } } },
          MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } }
        }
      }),
    [darkMode]
  );

  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}

function Shell() {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.user);
  const darkMode = useAppSelector((state) => state.auth.darkMode);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [toast, setToast] = useState<Toast | null>(null);

  async function logout() {
    try {
      await requestData('post', '/api/auth/logout');
    } catch {
      // Local sign-out still clears browser state when the server token is already gone.
    }
    dispatch(signedOut());
    navigate('/login', { replace: true });
  }

  const drawer = (
    <Stack sx={{ height: '100%' }}>
      <Box sx={{ px: 2, py: 2.5 }}>
        <Typography variant="h6" sx={{ fontWeight: 800 }}>Adaptive Gateway</Typography>
        <Typography variant="body2" color="text.secondary">{user?.username ?? 'Administrator'}</Typography>
      </Box>
      <Divider />
      <Stack component="nav" spacing={0.5} sx={{ p: 1 }}>
        {navItems.map((item) => (
          <Button
            key={item.path}
            component={Link}
            to={item.path}
            startIcon={item.icon}
            color={location.pathname === item.path ? 'primary' : 'inherit'}
            variant={location.pathname === item.path ? 'contained' : 'text'}
            sx={{ justifyContent: 'flex-start' }}
            onClick={() => setMobileOpen(false)}
          >
            {item.label}
          </Button>
        ))}
      </Stack>
      <Box sx={{ flex: 1 }} />
      <Divider />
      <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ p: 1.5 }}>
        <Tooltip title="Toggle color mode">
          <IconButton onClick={() => dispatch(themeToggled())}>
            {darkMode ? <Brightness7 /> : <Brightness4 />}
          </IconButton>
        </Tooltip>
        <Tooltip title="Sign out">
          <IconButton onClick={logout}>
            <Logout />
          </IconButton>
        </Tooltip>
      </Stack>
    </Stack>
  );

  return (
    <ThemeFrame>
      <CssBaseline />
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
        <AppBar position="fixed" color="inherit" elevation={0} sx={{ display: { sm: 'none' }, borderBottom: 1, borderColor: 'divider' }}>
          <Toolbar>
            <IconButton edge="start" onClick={() => setMobileOpen(true)}><MenuIcon /></IconButton>
            <Typography variant="h6">Gateway Console</Typography>
          </Toolbar>
        </AppBar>
        <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
          <Drawer variant="temporary" open={mobileOpen} onClose={() => setMobileOpen(false)} ModalProps={{ keepMounted: true }} sx={{ display: { xs: 'block', sm: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth } }}>
            {drawer}
          </Drawer>
          <Drawer variant="permanent" sx={{ display: { xs: 'none', sm: 'block' }, '& .MuiDrawer-paper': { width: drawerWidth, boxSizing: 'border-box' } }} open>
            {drawer}
          </Drawer>
        </Box>
        <Box component="main" sx={{ flexGrow: 1, width: { sm: `calc(100% - ${drawerWidth}px)` }, pt: { xs: 8, sm: 0 } }}>
          <Container maxWidth="xl" sx={{ py: 3 }}>
            <Routes>
              <Route path="/dashboard" element={<Dashboard notify={setToast} />} />
              <Route path="/gateway" element={<GatewayPage notify={setToast} />} />
              <Route path="/rate-limits" element={<RateLimitsPage notify={setToast} />} />
              <Route path="/consumers" element={<ConsumersPage notify={setToast} currentUserId={user?.id ?? ''} />} />
              <Route path="/operations" element={<OperationsPage />} />
              <Route path="/adaptive" element={<AdaptivePage notify={setToast} />} />
              <Route path="/anomalies" element={<AnomaliesPage notify={setToast} />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Container>
        </Box>
      </Box>
      <Snackbar open={Boolean(toast)} autoHideDuration={4500} onClose={() => setToast(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}>
        <Alert severity={toast?.type ?? 'success'} variant="filled" onClose={() => setToast(null)}>{toast?.message}</Alert>
      </Snackbar>
    </ThemeFrame>
  );
}

function Dashboard({ notify }: { notify: (toast: Toast) => void }) {
  const health = useEndpoint<AnyRecord>('/api/system/health');
  const routes = useEndpoint<PageResponse<AnyRecord>>(`/api/gateway/routes?size=${pageSize}`);
  const requests = useEndpoint<PageResponse<AnyRecord>>(`/api/operations/requests?size=${pageSize}`);
  const alerts = useEndpoint<PageResponse<AnyRecord>>(`/api/alerts?size=${pageSize}`);
  const metrics = useEndpoint<PageResponse<AnyRecord>>(`/api/adaptive-learning/route-metrics?size=${pageSize}`);
  const metricRows = pageContent(metrics.data);

  return (
    <>
      <PageTitle title="Overview" action={<IconButtonPanel onClick={() => { void Promise.all([health.refresh(), routes.refresh(), requests.refresh(), alerts.refresh(), metrics.refresh()]); notify({ type: 'success', message: 'Dashboard refreshed' }); }} icon={<Refresh />} label="Refresh" />} />
      <Box className="grid gap-3 md:grid-cols-4" sx={{ mb: 3 }}>
        <Stat label="System" value={health.loading ? 'Loading' : health.error ? 'Unavailable' : shortValue(health.data?.status ?? health.data?.overallStatus ?? 'Online')} />
        <Stat label="Routes" value={String(routes.data?.totalElements ?? 0)} />
        <Stat label="Requests" value={String(requests.data?.totalElements ?? 0)} />
        <Stat label="Alerts" value={String(alerts.data?.totalElements ?? 0)} />
      </Box>
      <Box className="grid gap-3 lg:grid-cols-2">
        <Paper variant="outlined" sx={{ borderRadius: 2, p: 2, minHeight: 300 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>Route Throughput</Typography>
          {metrics.loading ? (
            <Stack alignItems="center" justifyContent="center" sx={{ minHeight: 220 }}><CircularProgress size={28} /></Stack>
          ) : metrics.error ? (
            <Alert severity="error">{metrics.error}</Alert>
          ) : metricRows.length === 0 ? (
            <Box sx={{ color: 'text.secondary', py: 8 }}>No metric records returned.</Box>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={metricRows.map((row) => ({ name: shortValue(row.routeKey ?? row.routeName), rps: Number(row.requestsPerSecond ?? 0), blocked: Number(row.blockedRate ?? 0) }))}>
                <XAxis dataKey="name" />
                <YAxis />
                <ChartTooltip />
                <Line type="monotone" dataKey="rps" stroke="#1f6feb" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="blocked" stroke="#0f766e" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </Paper>
        <DataPanel title="Recent Alerts" columns={[{ key: 'severity', label: 'Severity', chip: true }, { key: 'routeKey', label: 'Route' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Created', date: true }]} state={alerts} />
      </Box>
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Paper variant="outlined" sx={{ borderRadius: 2, p: 2, minHeight: 96 }}>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography variant="h4" sx={{ fontWeight: 800, mt: 0.75 }}>{value}</Typography>
    </Paper>
  );
}

function IconButtonPanel({ onClick, icon, label }: { onClick: () => void; icon: React.ReactNode; label: string }) {
  return (
    <Tooltip title={label}>
      <IconButton color="primary" onClick={onClick}>{icon}</IconButton>
    </Tooltip>
  );
}

function GatewayPage({ notify }: { notify: (toast: Toast) => void }) {
  const upstreams = useEndpoint<PageResponse<AnyRecord>>(`/api/gateway/upstreams?size=${pageSize}`);
  const routes = useEndpoint<PageResponse<AnyRecord>>(`/api/gateway/routes?size=${pageSize}`);
  const [upstreamOpen, setUpstreamOpen] = useState(false);
  const [routeOpen, setRouteOpen] = useState(false);

  async function refreshRoutes() {
    try {
      await requestData('post', '/api/gateway/routes/refresh');
      notify({ type: 'success', message: 'Gateway routes refreshed' });
    } catch (error) {
      notify({ type: 'error', message: apiErrorMessage(error) });
    }
  }

  return (
    <>
      <PageTitle title="Gateway" action={<Stack direction="row" spacing={1}><IconButtonPanel onClick={refreshRoutes} icon={<Refresh />} label="Refresh routes" /><IconButtonPanel onClick={() => setUpstreamOpen(true)} icon={<Add />} label="Add upstream" /><IconButtonPanel onClick={() => setRouteOpen(true)} icon={<Router />} label="Add route" /></Stack>} />
      <Box className="grid gap-3 xl:grid-cols-2">
        <DataPanel title="Upstream Services" columns={[{ key: 'name', label: 'Name' }, { key: 'baseUrl', label: 'Base URL' }, { key: 'status', label: 'Status', chip: true }, { key: 'timeoutMs', label: 'Timeout' }, { key: 'retryCount', label: 'Retries' }]} state={upstreams} />
        <DataPanel title="Routes" columns={[{ key: 'routeKey', label: 'Key' }, { key: 'name', label: 'Name' }, { key: 'pathPattern', label: 'Path' }, { key: 'allowedMethods', label: 'Methods' }, { key: 'status', label: 'Status', chip: true }]} state={routes} />
      </Box>
      <UpstreamDialog open={upstreamOpen} onClose={() => setUpstreamOpen(false)} onSaved={() => { void upstreams.refresh(); notify({ type: 'success', message: 'Upstream saved' }); }} />
      <RouteDialog open={routeOpen} upstreams={pageContent(upstreams.data)} onClose={() => setRouteOpen(false)} onSaved={() => { void routes.refresh(); notify({ type: 'success', message: 'Route saved' }); }} />
    </>
  );
}

function UpstreamDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ name: '', baseUrl: '', healthCheckPath: '/actuator/health', status: 'ACTIVE', timeoutMs: 5000, retryCount: 1 });
  return (
    <SubmitDialog title="Add Upstream" open={open} onClose={onClose} onSubmit={async () => { await requestData('post', '/api/gateway/upstreams', form); onClose(); onSaved(); }}>
      <TextField label="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
      <TextField label="Base URL" value={form.baseUrl} onChange={(event) => setForm({ ...form, baseUrl: event.target.value })} required />
      <TextField label="Health path" value={form.healthCheckPath} onChange={(event) => setForm({ ...form, healthCheckPath: event.target.value })} />
      <SelectField label="Status" value={form.status} values={['ACTIVE', 'INACTIVE']} onChange={(status) => setForm({ ...form, status })} />
      <TextField label="Timeout ms" type="number" value={form.timeoutMs} onChange={(event) => setForm({ ...form, timeoutMs: Number(event.target.value) })} />
      <TextField label="Retry count" type="number" value={form.retryCount} onChange={(event) => setForm({ ...form, retryCount: Number(event.target.value) })} />
    </SubmitDialog>
  );
}

function RouteDialog({ open, upstreams, onClose, onSaved }: { open: boolean; upstreams: AnyRecord[]; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ upstreamServiceId: '', routeKey: '', name: '', pathPattern: '/', allowedMethods: 'GET', stripPrefix: 0, priority: 0, status: 'ACTIVE' });
  return (
    <SubmitDialog title="Add Route" open={open} onClose={onClose} onSubmit={async () => { await requestData('post', '/api/gateway/routes', { ...form, allowedMethods: form.allowedMethods.split(',').map((item) => item.trim()).filter(Boolean), predicates: [] }); onClose(); onSaved(); }}>
      <SelectField label="Upstream" value={form.upstreamServiceId} values={upstreams.map((item) => String(item.id))} labels={Object.fromEntries(upstreams.map((item) => [String(item.id), shortValue(item.name)]))} onChange={(upstreamServiceId) => setForm({ ...form, upstreamServiceId })} />
      <TextField label="Route key" value={form.routeKey} onChange={(event) => setForm({ ...form, routeKey: event.target.value })} required />
      <TextField label="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
      <TextField label="Path pattern" value={form.pathPattern} onChange={(event) => setForm({ ...form, pathPattern: event.target.value })} required />
      <TextField label="Allowed methods" value={form.allowedMethods} onChange={(event) => setForm({ ...form, allowedMethods: event.target.value })} required />
      <TextField label="Strip prefix" type="number" value={form.stripPrefix} onChange={(event) => setForm({ ...form, stripPrefix: Number(event.target.value) })} />
      <TextField label="Priority" type="number" value={form.priority} onChange={(event) => setForm({ ...form, priority: Number(event.target.value) })} />
      <SelectField label="Status" value={form.status} values={['ACTIVE', 'INACTIVE']} onChange={(status) => setForm({ ...form, status })} />
    </SubmitDialog>
  );
}

function RateLimitsPage({ notify }: { notify: (toast: Toast) => void }) {
  const policies = useEndpoint<PageResponse<AnyRecord>>(`/api/rate-limit/policies?size=${pageSize}`);
  const assignments = useEndpoint<PageResponse<AnyRecord>>(`/api/rate-limit/assignments?size=${pageSize}`);
  const routes = useEndpoint<PageResponse<AnyRecord>>(`/api/gateway/routes?size=${pageSize}`);
  const [policyOpen, setPolicyOpen] = useState(false);
  const [assignmentOpen, setAssignmentOpen] = useState(false);

  return (
    <>
      <PageTitle title="Rate Limits" action={<Stack direction="row" spacing={1}><IconButtonPanel onClick={() => setPolicyOpen(true)} icon={<Add />} label="Add policy" /><IconButtonPanel onClick={() => setAssignmentOpen(true)} icon={<Speed />} label="Assign policy" /></Stack>} />
      <Box className="grid gap-3 xl:grid-cols-2">
        <DataPanel title="Policies" columns={[{ key: 'name', label: 'Name' }, { key: 'algorithm', label: 'Algorithm' }, { key: 'maxRequests', label: 'Max' }, { key: 'windowSeconds', label: 'Window' }, { key: 'strictnessFactor', label: 'Strictness' }, { key: 'status', label: 'Status', chip: true }]} state={policies} />
        <DataPanel title="Assignments" columns={[{ key: 'policyName', label: 'Policy' }, { key: 'routeKey', label: 'Route' }, { key: 'priority', label: 'Priority' }, { key: 'status', label: 'Status', chip: true }, { key: 'validFrom', label: 'Valid from', date: true }]} state={assignments} />
      </Box>
      <PolicyDialog open={policyOpen} onClose={() => setPolicyOpen(false)} onSaved={() => { void policies.refresh(); notify({ type: 'success', message: 'Policy saved' }); }} />
      <AssignmentDialog open={assignmentOpen} policies={pageContent(policies.data)} routes={pageContent(routes.data)} onClose={() => setAssignmentOpen(false)} onSaved={() => { void assignments.refresh(); notify({ type: 'success', message: 'Assignment saved' }); }} />
    </>
  );
}

function PolicyDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ name: '', algorithm: 'FIXED_WINDOW', windowSeconds: 60, maxRequests: 100, bucketCapacity: 100, refillTokens: 10, refillPeriodSeconds: 10, strictnessFactor: 1, adaptiveEnabled: true, status: 'ACTIVE' });
  return (
    <SubmitDialog title="Add Policy" open={open} onClose={onClose} onSubmit={async () => { await requestData('post', '/api/rate-limit/policies', form); onClose(); onSaved(); }}>
      <TextField label="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
      <SelectField label="Algorithm" value={form.algorithm} values={['FIXED_WINDOW', 'TOKEN_BUCKET']} onChange={(algorithm) => setForm({ ...form, algorithm })} />
      <TextField label="Window seconds" type="number" value={form.windowSeconds} onChange={(event) => setForm({ ...form, windowSeconds: Number(event.target.value) })} />
      <TextField label="Max requests" type="number" value={form.maxRequests} onChange={(event) => setForm({ ...form, maxRequests: Number(event.target.value) })} />
      <TextField label="Bucket capacity" type="number" value={form.bucketCapacity} onChange={(event) => setForm({ ...form, bucketCapacity: Number(event.target.value) })} />
      <TextField label="Refill tokens" type="number" value={form.refillTokens} onChange={(event) => setForm({ ...form, refillTokens: Number(event.target.value) })} />
      <TextField label="Refill period seconds" type="number" value={form.refillPeriodSeconds} onChange={(event) => setForm({ ...form, refillPeriodSeconds: Number(event.target.value) })} />
      <TextField label="Strictness factor" type="number" value={form.strictnessFactor} onChange={(event) => setForm({ ...form, strictnessFactor: Number(event.target.value) })} />
      <Stack direction="row" alignItems="center" justifyContent="space-between"><Typography>Adaptive</Typography><Switch checked={form.adaptiveEnabled} onChange={(event) => setForm({ ...form, adaptiveEnabled: event.target.checked })} /></Stack>
      <SelectField label="Status" value={form.status} values={['ACTIVE', 'INACTIVE']} onChange={(status) => setForm({ ...form, status })} />
    </SubmitDialog>
  );
}

function AssignmentDialog({ open, policies, routes, onClose, onSaved }: { open: boolean; policies: AnyRecord[]; routes: AnyRecord[]; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ policyId: '', routeId: '', priority: 0, status: 'ACTIVE', validFrom: new Date().toISOString(), validUntil: '' });
  return (
    <SubmitDialog title="Assign Policy" open={open} onClose={onClose} onSubmit={async () => { await requestData('post', '/api/rate-limit/assignments', { ...form, validUntil: form.validUntil || null }); onClose(); onSaved(); }}>
      <SelectField label="Policy" value={form.policyId} values={policies.map((item) => String(item.id))} labels={Object.fromEntries(policies.map((item) => [String(item.id), shortValue(item.name)]))} onChange={(policyId) => setForm({ ...form, policyId })} />
      <SelectField label="Route" value={form.routeId} values={routes.map((item) => String(item.id))} labels={Object.fromEntries(routes.map((item) => [String(item.id), shortValue(item.routeKey ?? item.name)]))} onChange={(routeId) => setForm({ ...form, routeId })} />
      <TextField label="Priority" type="number" value={form.priority} onChange={(event) => setForm({ ...form, priority: Number(event.target.value) })} />
      <TextField label="Valid from" value={form.validFrom} onChange={(event) => setForm({ ...form, validFrom: event.target.value })} />
      <TextField label="Valid until" value={form.validUntil} onChange={(event) => setForm({ ...form, validUntil: event.target.value })} />
      <SelectField label="Status" value={form.status} values={['ACTIVE', 'INACTIVE']} onChange={(status) => setForm({ ...form, status })} />
    </SubmitDialog>
  );
}

function ConsumersPage({ notify, currentUserId }: { notify: (toast: Toast) => void; currentUserId: string }) {
  const consumers = useEndpoint<PageResponse<AnyRecord>>(`/api/consumers?size=${pageSize}`);
  const [consumerOpen, setConsumerOpen] = useState(false);
  const [credentialOpen, setCredentialOpen] = useState(false);
  const [rawKey, setRawKey] = useState<string | null>(null);

  return (
    <>
      <PageTitle title="Consumers" action={<Stack direction="row" spacing={1}><IconButtonPanel onClick={() => setConsumerOpen(true)} icon={<Add />} label="Add consumer" /><IconButtonPanel onClick={() => setCredentialOpen(true)} icon={<Key />} label="Create key" /></Stack>} />
      {rawKey && <Alert severity="info" sx={{ mb: 2 }} onClose={() => setRawKey(null)}>{rawKey}</Alert>}
      <DataPanel title="API Consumers" columns={[{ key: 'name', label: 'Name' }, { key: 'ownerUsername', label: 'Owner' }, { key: 'contactEmail', label: 'Contact' }, { key: 'environment', label: 'Environment', chip: true }, { key: 'status', label: 'Status', chip: true }]} state={consumers} />
      <ConsumerDialog open={consumerOpen} currentUserId={currentUserId} onClose={() => setConsumerOpen(false)} onSaved={() => { void consumers.refresh(); notify({ type: 'success', message: 'Consumer saved' }); }} />
      <CredentialDialog open={credentialOpen} consumers={pageContent(consumers.data)} onClose={() => setCredentialOpen(false)} onSaved={(apiKey) => { setRawKey(apiKey); notify({ type: 'success', message: 'Credential created' }); }} />
    </>
  );
}

function ConsumerDialog({ open, currentUserId, onClose, onSaved }: { open: boolean; currentUserId: string; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ ownerUserId: currentUserId, name: '', description: '', contactEmail: '', status: 'ACTIVE', environment: 'PRODUCTION' });
  useEffect(() => setForm((current) => ({ ...current, ownerUserId: currentUserId })), [currentUserId]);
  return (
    <SubmitDialog title="Add Consumer" open={open} onClose={onClose} onSubmit={async () => { await requestData('post', '/api/consumers', form); onClose(); onSaved(); }}>
      <TextField label="Owner user ID" value={form.ownerUserId} onChange={(event) => setForm({ ...form, ownerUserId: event.target.value })} required />
      <TextField label="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
      <TextField label="Description" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} multiline minRows={2} />
      <TextField label="Contact email" type="email" value={form.contactEmail} onChange={(event) => setForm({ ...form, contactEmail: event.target.value })} required />
      <SelectField label="Environment" value={form.environment} values={['PRODUCTION', 'SANDBOX']} onChange={(environment) => setForm({ ...form, environment })} />
      <SelectField label="Status" value={form.status} values={['ACTIVE', 'INACTIVE']} onChange={(status) => setForm({ ...form, status })} />
    </SubmitDialog>
  );
}

function CredentialDialog({ open, consumers, onClose, onSaved }: { open: boolean; consumers: AnyRecord[]; onClose: () => void; onSaved: (apiKey: string) => void }) {
  const [consumerId, setConsumerId] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  return (
    <SubmitDialog title="Create API Key" open={open} onClose={onClose} onSubmit={async () => { const result = await requestData<{ apiKey: string }>('post', `/api/consumers/${consumerId}/credentials`, { expiresAt: expiresAt || null }); onClose(); onSaved(result.apiKey); }}>
      <SelectField label="Consumer" value={consumerId} values={consumers.map((item) => String(item.id))} labels={Object.fromEntries(consumers.map((item) => [String(item.id), shortValue(item.name)]))} onChange={setConsumerId} />
      <TextField label="Expires at" value={expiresAt} onChange={(event) => setExpiresAt(event.target.value)} />
    </SubmitDialog>
  );
}

function OperationsPage() {
  const [tab, setTab] = useState(0);
  const requests = useEndpoint<PageResponse<AnyRecord>>(`/api/operations/requests?size=${pageSize}`);
  const auditLogs = useEndpoint<PageResponse<AnyRecord>>(`/api/operations/audit-logs?size=${pageSize}`);
  return (
    <>
      <PageTitle title="Operations" />
      <Paper variant="outlined" sx={{ borderRadius: 2, mb: 2 }}><Tabs value={tab} onChange={(_, value: number) => setTab(value)}><Tab label="Requests" /><Tab label="Audit" /></Tabs></Paper>
      {tab === 0 ? (
        <DataPanel title="Gateway Requests" columns={[{ key: 'routeKey', label: 'Route' }, { key: 'method', label: 'Method' }, { key: 'path', label: 'Path' }, { key: 'responseStatus', label: 'Status' }, { key: 'outcome', label: 'Outcome', chip: true }, { key: 'createdAt', label: 'Created', date: true }]} state={requests} />
      ) : (
        <DataPanel title="Audit Logs" columns={[{ key: 'actorUsername', label: 'Actor' }, { key: 'action', label: 'Action' }, { key: 'resourceType', label: 'Resource' }, { key: 'resourceId', label: 'Resource ID' }, { key: 'createdAt', label: 'Created', date: true }]} state={auditLogs} />
      )}
    </>
  );
}

function AdaptivePage({ notify }: { notify: (toast: Toast) => void }) {
  const metrics = useEndpoint<PageResponse<AnyRecord>>(`/api/adaptive-learning/route-metrics?size=${pageSize}`);
  const heatmap = useEndpoint<PageResponse<AnyRecord>>(`/api/adaptive-learning/traffic-heatmap?size=${pageSize}`);
  const adjustments = useEndpoint<PageResponse<AnyRecord>>(`/api/adaptive-learning/adjustments?size=${pageSize}`);

  async function runLearning() {
    try {
      await requestData('post', '/api/adaptive-learning/run');
      await Promise.all([metrics.refresh(), heatmap.refresh(), adjustments.refresh()]);
      notify({ type: 'success', message: 'Adaptive learning run completed' });
    } catch (error) {
      notify({ type: 'error', message: apiErrorMessage(error) });
    }
  }

  return (
    <>
      <PageTitle title="Adaptive Learning" action={<IconButtonPanel onClick={runLearning} icon={<AutoGraph />} label="Run learning" />} />
      <Box className="grid gap-3 xl:grid-cols-2">
        <DataPanel title="Route Metrics" columns={[{ key: 'routeKey', label: 'Route' }, { key: 'requestsPerSecond', label: 'RPS' }, { key: 'blockedRate', label: 'Blocked' }, { key: 'errorRate', label: 'Errors' }, { key: 'avgLatencyMs', label: 'Latency' }]} state={metrics} />
        <DataPanel title="Traffic Heatmap" columns={[{ key: 'routeKey', label: 'Route' }, { key: 'bucketStart', label: 'Bucket', date: true }, { key: 'requestCount', label: 'Requests' }, { key: 'blockedCount', label: 'Blocked' }, { key: 'errorCount', label: 'Errors' }]} state={heatmap} />
        <DataPanel title="Adjustments" columns={[{ key: 'routeKey', label: 'Route' }, { key: 'previousStrictnessFactor', label: 'Previous' }, { key: 'newStrictnessFactor', label: 'New' }, { key: 'reason', label: 'Reason' }, { key: 'createdAt', label: 'Created', date: true }]} state={adjustments} />
      </Box>
    </>
  );
}

function AnomaliesPage({ notify }: { notify: (toast: Toast) => void }) {
  const anomalies = useEndpoint<PageResponse<AnyRecord>>(`/api/anomalies?size=${pageSize}`);
  const snapshots = useEndpoint<PageResponse<AnyRecord>>(`/api/anomalies/snapshots?size=${pageSize}`);
  const alerts = useEndpoint<PageResponse<AnyRecord>>(`/api/alerts?size=${pageSize}`);

  async function runDetection() {
    try {
      await requestData('post', '/api/anomalies/run');
      await Promise.all([anomalies.refresh(), snapshots.refresh(), alerts.refresh()]);
      notify({ type: 'success', message: 'Anomaly detection completed' });
    } catch (error) {
      notify({ type: 'error', message: apiErrorMessage(error) });
    }
  }

  return (
    <>
      <PageTitle title="Anomalies" action={<IconButtonPanel onClick={runDetection} icon={<WarningAmber />} label="Run detection" />} />
      <Box className="grid gap-3 xl:grid-cols-2">
        <DataPanel title="Anomaly Records" columns={[{ key: 'severity', label: 'Severity', chip: true }, { key: 'routeKey', label: 'Route' }, { key: 'metricName', label: 'Metric' }, { key: 'observedValue', label: 'Observed' }, { key: 'thresholdValue', label: 'Threshold' }, { key: 'status', label: 'Status', chip: true }]} state={anomalies} />
        <DataPanel title="Stat Snapshots" columns={[{ key: 'routeKey', label: 'Route' }, { key: 'metricName', label: 'Metric' }, { key: 'meanValue', label: 'Mean' }, { key: 'standardDeviation', label: 'Std dev' }, { key: 'createdAt', label: 'Created', date: true }]} state={snapshots} />
        <DataPanel title="Alerts" columns={[{ key: 'severity', label: 'Severity', chip: true }, { key: 'routeKey', label: 'Route' }, { key: 'message', label: 'Message' }, { key: 'createdAt', label: 'Created', date: true }]} state={alerts} />
      </Box>
    </>
  );
}

function SelectField({ label, value, values, labels, onChange }: { label: string; value: string; values: string[]; labels?: Record<string, string>; onChange: (value: string) => void }) {
  return (
    <FormControl fullWidth>
      <InputLabel>{label}</InputLabel>
      <Select label={label} value={value} onChange={(event: SelectChangeEvent) => onChange(event.target.value)}>
        {values.map((item) => <MenuItem key={item} value={item}>{labels?.[item] ?? item}</MenuItem>)}
      </Select>
    </FormControl>
  );
}

function SubmitDialog({ title, open, onClose, onSubmit, children }: { title: string; open: boolean; onClose: () => void; onSubmit: () => Promise<void>; children: React.ReactNode }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    setLoading(true);
    setError(null);
    try {
      await onSubmit();
    } catch (requestError) {
      setError(apiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          {error && <Alert severity="error">{error}</Alert>}
          {children}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={loading} startIcon={loading ? <CircularProgress color="inherit" size={18} /> : undefined}>Save</Button>
      </DialogActions>
    </Dialog>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<ProtectedRoute><Shell /></ProtectedRoute>} />
    </Routes>
  );
}
