import TerminalRounded from '@mui/icons-material/TerminalRounded';
import { AppBar, Box, Tab, Tabs, Toolbar, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router';
import { useAuth } from '../../auth/use-auth';
import { paths } from '../../routes/paths';
import { LanguageSwitcher } from '../LanguageSwitcher';
import { UserMenu } from '../user/UserMenu';
import { ColorModeToggle } from './ColorModeToggle';

const NAV = [
  { to: paths.problems, labelKey: 'nav.problems' },
  { to: paths.contests, labelKey: 'nav.contests' },
  { to: paths.interviews, labelKey: 'nav.interviews' },
  { to: paths.leaderboard, labelKey: 'nav.leaderboard' },
  { to: paths.home, labelKey: 'nav.home' },
] as const;

/** Only ever rendered for an administrator; the route and the API refuse everyone else. */
const ADMIN_NAV = [
  { to: paths.adminProblems, labelKey: 'nav.admin' },
  { to: paths.adminContests, labelKey: 'nav.adminContests' },
] as const;

/** The signed-in chrome: brand, primary nav, language and the account menu. */
export const AppHeader = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const { user } = useAuth();

  const items = user?.role === 'ADMIN' ? [...NAV, ...ADMIN_NAV] : [...NAV];

  // Matched by prefix so the solving page and the interview workspace keep their
  // section highlighted rather than dropping the indicator on a subpage. Home is
  // the exception: every path starts with "/".
  const active = items.find((item) =>
    item.to === paths.home ? location.pathname === paths.home : location.pathname.startsWith(item.to),
  );

  return (
    <AppBar position="sticky">
      <Toolbar sx={{ gap: 2 }}>
        <Box
          component={Link}
          to={paths.problems}
          sx={{ display: 'flex', alignItems: 'center', gap: 1.25, textDecoration: 'none', color: 'inherit' }}
        >
          <Box
            sx={{
              width: 28,
              height: 28,
              borderRadius: 1.25,
              background: (theme) => theme.palette.brand.gradient,
              display: 'grid',
              placeItems: 'center',
              color: 'common.white',
            }}
          >
            <TerminalRounded sx={{ fontSize: 17 }} />
          </Box>
          <Typography sx={{ fontWeight: 700, letterSpacing: '-0.02em', fontSize: '0.9375rem' }}>
            Code
            <Box component="span" sx={{ color: 'brand.ember' }}>
              Forge
            </Box>
          </Typography>
        </Box>

        <Tabs value={active?.to ?? false} sx={{ alignSelf: 'stretch', display: { xs: 'none', sm: 'flex' } }}>
          {items.map((item) => (
            <Tab key={item.to} value={item.to} component={Link} to={item.to} label={t(item.labelKey)} />
          ))}
        </Tabs>

        <Box sx={{ flex: 1 }} />
        <LanguageSwitcher />
        <ColorModeToggle />
        <UserMenu />
      </Toolbar>
    </AppBar>
  );
};
