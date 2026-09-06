import TerminalRounded from '@mui/icons-material/TerminalRounded';
import { AppBar, Box, Tab, Tabs, Toolbar, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router';
import { paths } from '../../routes/paths';
import { LanguageSwitcher } from '../LanguageSwitcher';
import { UserMenu } from '../user/UserMenu';
import { ColorModeToggle } from './ColorModeToggle';

const NAV = [
  { to: paths.problems, labelKey: 'nav.problems' },
  { to: paths.home, labelKey: 'nav.home' },
] as const;

/** The signed-in chrome: brand, primary nav, language and the account menu. */
export const AppHeader = () => {
  const { t } = useTranslation();
  const location = useLocation();

  const value = location.pathname.startsWith(paths.problems) ? 0 : location.pathname === paths.home ? 1 : false;

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

        <Tabs value={value} sx={{ alignSelf: 'stretch', display: { xs: 'none', sm: 'flex' } }}>
          {NAV.map((item) => (
            <Tab key={item.to} component={Link} to={item.to} label={t(item.labelKey)} />
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
