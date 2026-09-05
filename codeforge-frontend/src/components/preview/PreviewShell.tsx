import DarkModeRounded from '@mui/icons-material/DarkModeRounded';
import LightModeRounded from '@mui/icons-material/LightModeRounded';
import LocalFireDepartmentRounded from '@mui/icons-material/LocalFireDepartmentRounded';
import NotificationsRounded from '@mui/icons-material/NotificationsRounded';
import SearchRounded from '@mui/icons-material/SearchRounded';
import TerminalRounded from '@mui/icons-material/TerminalRounded';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Chip,
  IconButton,
  InputAdornment,
  Stack,
  Tab,
  Tabs,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useColorScheme,
} from '@mui/material';
import { type ReactNode } from 'react';

export const sections = [
  { id: 'colors', label: 'Color' },
  { id: 'typography', label: 'Typography' },
  { id: 'surfaces', label: 'Surfaces' },
  { id: 'buttons', label: 'Actions' },
  { id: 'forms', label: 'Inputs' },
  { id: 'feedback', label: 'Feedback' },
  { id: 'data', label: 'Data display' },
  { id: 'navigation', label: 'Navigation' },
  { id: 'platform', label: 'In context' },
];

const Logo = () => (
  <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
    <Box
      sx={{
        width: 28,
        height: 28,
        borderRadius: 1.25,
        background: (theme) => theme.palette.brand.gradient,
        display: 'grid',
        placeItems: 'center',
        color: 'common.white',
        boxShadow: 2,
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
  </Stack>
);

const ModeToggle = () => {
  const { mode, setMode } = useColorScheme();
  const isDark = mode !== 'light';

  return (
    <Tooltip title={isDark ? 'Switch to light' : 'Switch to dark'}>
      <IconButton
        onClick={() => {
          setMode(isDark ? 'light' : 'dark');
        }}
      >
        {isDark ? <LightModeRounded fontSize="small" /> : <DarkModeRounded fontSize="small" />}
      </IconButton>
    </Tooltip>
  );
};

/** The app chrome, so the theme can be judged in situ rather than on a blank page. */
export const PreviewShell = ({ children }: { children: ReactNode }) => (
  <Box sx={{ minHeight: '100%', backgroundColor: 'surface.canvas' }}>
    <AppBar position="sticky">
      <Toolbar sx={{ gap: 2 }}>
        <Logo />
        <Tabs value={0} sx={{ display: { xs: 'none', md: 'flex' }, alignSelf: 'stretch' }}>
          <Tab label="Problems" />
          <Tab label="Interview" />
          <Tab label="Dashboard" />
        </Tabs>
        <Box sx={{ flex: 1 }} />
        <TextField
          placeholder="Search problems"
          sx={{ width: { xs: 0, sm: 200, lg: 260 }, display: { xs: 'none', sm: 'block' } }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRounded fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
        <Chip
          icon={<LocalFireDepartmentRounded />}
          label="17"
          sx={{ color: 'brand.ember', backgroundColor: 'brand.emberBg', '& .MuiChip-icon': { color: 'inherit' } }}
        />
        <ModeToggle />
        <Badge badgeContent={3} color="error">
          <IconButton>
            <NotificationsRounded fontSize="small" />
          </IconButton>
        </Badge>
        <Avatar sx={{ width: 30, height: 30, bgcolor: 'primary.main', color: 'primary.contrastText' }}>CS</Avatar>
      </Toolbar>
    </AppBar>
    {children}
  </Box>
);
