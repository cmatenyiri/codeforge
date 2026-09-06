import DarkModeRounded from '@mui/icons-material/DarkModeRounded';
import LightModeRounded from '@mui/icons-material/LightModeRounded';
import SettingsBrightnessRounded from '@mui/icons-material/SettingsBrightnessRounded';
import { ListItemIcon, ListItemText, Menu, MenuItem, IconButton, Tooltip } from '@mui/material';
import { useColorScheme } from '@mui/material/styles';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * The three states MUI's color scheme actually has. "System" is not a third
 * theme but a deferral to the OS, which is why it needs its own entry rather
 * than being inferred from the other two.
 */
const MODES = [
  { value: 'light', labelKey: 'theme.light', Icon: LightModeRounded },
  { value: 'dark', labelKey: 'theme.dark', Icon: DarkModeRounded },
  { value: 'system', labelKey: 'theme.system', Icon: SettingsBrightnessRounded },
] as const;

/**
 * Light / dark / system switch for the app chrome.
 *
 * <p>The theme already ships both schemes as CSS variables, so choosing one is a
 * class swap on `<html>` rather than a React re-render — and MUI persists the
 * choice, so it survives a reload without any storage code here.
 */
export const ColorModeToggle = () => {
  const { t } = useTranslation();
  const { mode, setMode, systemMode } = useColorScheme();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  // Undefined for the first paint, before MUI has read the stored choice.
  // Rendering the button anyway — rather than nothing — keeps the toolbar from
  // shifting sideways a frame later.
  const resolved = mode === 'system' ? systemMode : mode;
  const Icon = resolved === 'light' ? LightModeRounded : DarkModeRounded;

  return (
    <>
      <Tooltip title={t('theme.toggle')}>
        <IconButton
          size="small"
          aria-label={t('theme.toggle')}
          aria-haspopup="menu"
          aria-expanded={anchorEl !== null}
          onClick={(event) => {
            setAnchorEl(event.currentTarget);
          }}
        >
          <Icon fontSize="small" />
        </IconButton>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        open={anchorEl !== null}
        onClose={() => {
          setAnchorEl(null);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { minWidth: 168, mt: 1 } } }}
      >
        {MODES.map((option) => (
          <MenuItem
            key={option.value}
            selected={mode === option.value}
            onClick={() => {
              setMode(option.value);
              setAnchorEl(null);
            }}
          >
            <ListItemIcon>
              <option.Icon fontSize="small" />
            </ListItemIcon>
            <ListItemText>{t(option.labelKey)}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
};
