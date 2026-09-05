import { Box, Chip, Container, Link as MuiLink, Paper, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useAuth } from '../auth/use-auth';
import { AppHeader } from '../components/layout/AppHeader';
import { paths } from '../routes/paths';

/**
 * Placeholder landing page for a signed-in user.
 *
 * <p>Exists only so the login flow has somewhere to land; the real dashboard,
 * problems list and interview pages replace it.
 */
export const HomePage = () => {
  const { t } = useTranslation();
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <Box sx={{ minHeight: '100vh', backgroundColor: 'surface.canvas' }}>
      <AppHeader />

      <Container maxWidth="md" sx={{ py: 6 }}>
        <Stack spacing={3}>
          <Typography variant="h1">{t('home.welcome', { username: user.username })}</Typography>

          <Paper variant="outlined" sx={{ p: 3 }}>
            <Stack spacing={2}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                <Typography variant="overline" sx={{ color: 'text.disabled' }}>
                  {t('home.signedInAs')}
                </Typography>
                <Typography variant="mono">{user.username}</Typography>
                <Typography variant="mono" sx={{ color: 'text.secondary' }}>
                  {user.email}
                </Typography>
                <Chip label={user.role} color={user.role === 'ADMIN' ? 'primary' : 'default'} />
              </Stack>

              <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                {t('home.placeholder')}
              </Typography>

              <Box>
                <MuiLink component={Link} to={paths.themePreview}>
                  {t('home.themePreview')}
                </MuiLink>
              </Box>
            </Stack>
          </Paper>
        </Stack>
      </Container>
    </Box>
  );
};
