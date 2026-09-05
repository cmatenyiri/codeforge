import TerminalRounded from '@mui/icons-material/TerminalRounded';
import { Alert, Box, Container, Paper, Stack, Typography } from '@mui/material';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { LanguageSwitcher } from '../LanguageSwitcher';

type AuthLayoutProps = {
  title: string;
  subtitle: string;
  /** A general (non-field) failure, already translated. Rendered above the form. */
  errorMessage?: string;
  children: ReactNode;
  footer: ReactNode;
};

/** The shared frame for sign in and sign up. */
export const AuthLayout = ({ title, subtitle, errorMessage, children, footer }: AuthLayoutProps) => {
  const { t } = useTranslation();

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas', display: 'flex', flexDirection: 'column' }}>
      <Stack direction="row" sx={{ justifyContent: 'flex-end', p: 2 }}>
        <LanguageSwitcher />
      </Stack>

      <Container maxWidth="sm" sx={{ flex: 1, display: 'grid', placeItems: 'center', pb: 10 }}>
        <Stack spacing={3} sx={{ width: '100%', maxWidth: 420 }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', justifyContent: 'center' }}>
            <Box
              sx={{
                width: 34,
                height: 34,
                borderRadius: 1.5,
                background: (theme) => theme.palette.brand.gradient,
                display: 'grid',
                placeItems: 'center',
                color: 'common.white',
              }}
            >
              <TerminalRounded sx={{ fontSize: 20 }} />
            </Box>
            <Typography sx={{ fontWeight: 700, letterSpacing: '-0.02em', fontSize: '1.125rem' }}>
              Code
              <Box component="span" sx={{ color: 'brand.ember' }}>
                Forge
              </Box>
            </Typography>
          </Stack>

          <Paper variant="outlined" sx={{ p: { xs: 2.5, sm: 4 } }}>
            <Stack spacing={0.5} sx={{ mb: 3 }}>
              <Typography variant="h3">{title}</Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {subtitle}
              </Typography>
            </Stack>

            {errorMessage ? (
              <Alert severity="error" sx={{ mb: 2.5 }}>
                {errorMessage}
              </Alert>
            ) : null}

            {children}
          </Paper>

          <Typography variant="body2" sx={{ color: 'text.secondary', textAlign: 'center' }}>
            {footer}
          </Typography>

          <Typography variant="overline" sx={{ color: 'text.disabled', textAlign: 'center' }}>
            {t('common.tagline')}
          </Typography>
        </Stack>
      </Container>
    </Box>
  );
};
