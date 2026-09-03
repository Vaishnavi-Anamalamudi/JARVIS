import { Component, ReactNode } from 'react';
import { Alert, Box, Button, Container, Stack, Typography } from '@mui/material';
import CrashIcon from '@mui/icons-material/ErrorOutline';

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

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error);
    console.error('Error info:', errorInfo.componentStack);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            minHeight: '100vh',
            display: 'grid',
            placeItems: 'center',
            bgcolor: 'background.default',
            px: 2
          }}
        >
          <Container maxWidth="sm">
            <Stack spacing={3} alignItems="center" textAlign="center">
              <CrashIcon sx={{ fontSize: 64, color: 'error.main' }} />
              <Stack spacing={1}>
                <Typography variant="h4" sx={{ fontWeight: 700 }}>
                  Oops! Something went wrong
                </Typography>
                <Typography color="text.secondary">
                  An unexpected error occurred. This has been logged.
                </Typography>
              </Stack>
              <Alert severity="error" sx={{ width: '100%', wordBreak: 'break-word' }}>
                {this.state.error?.message || 'Unknown error'}
              </Alert>
              <Stack direction="row" spacing={1}>
                <Button variant="contained" onClick={this.handleReset}>
                  Try Again
                </Button>
                <Button variant="outlined" onClick={() => (window.location.href = '/')}>
                  Go Home
                </Button>
              </Stack>
            </Stack>
          </Container>
        </Box>
      );
    }
    return this.props.children;
  }
}
