import { CssBaseline, ThemeProvider } from '@mui/material';
import { BrowserRouter } from 'react-router';
import { AuthProvider } from '../auth/AuthProvider';
import '../i18n';
import { RealtimeProvider } from '../realtime/RealtimeProvider';
import { AppRoutes } from '../routes/AppRoutes';
import { codeForgeTheme } from '../theme';

export const App = () => (
  <ThemeProvider theme={codeForgeTheme} defaultMode="dark">
    <CssBaseline />
    <BrowserRouter>
      <AuthProvider>
        <RealtimeProvider>
          <AppRoutes />
        </RealtimeProvider>
      </AuthProvider>
    </BrowserRouter>
  </ThemeProvider>
);
