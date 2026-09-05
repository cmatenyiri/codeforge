import LogoutRounded from '@mui/icons-material/LogoutRounded';
import PersonRounded from '@mui/icons-material/PersonRounded';
import { Box, ButtonBase, Divider, ListItemIcon, ListItemText, Menu, MenuItem, Typography } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useAuth } from '../../auth/use-auth';
import { paths } from '../../routes/paths';
import { UserAvatar } from './UserAvatar';
import { isAvatarId } from './avatars';

/** Avatar button in the top-right corner, opening the profile / sign-out menu. */
export const UserMenu = () => {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  if (!user || !isAvatarId(user.avatar)) {
    return null;
  }

  const close = () => {
    setAnchorEl(null);
  };

  return (
    <>
      <ButtonBase
        onClick={(event) => {
          setAnchorEl(event.currentTarget);
        }}
        aria-haspopup="menu"
        aria-expanded={anchorEl !== null}
        aria-label={t('userMenu.open')}
        sx={{ borderRadius: 1.5, p: 0.25 }}
      >
        <UserAvatar avatar={user.avatar} size={32} />
      </ButtonBase>

      <Menu
        anchorEl={anchorEl}
        open={anchorEl !== null}
        onClose={close}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { minWidth: 220, mt: 1 } } }}
      >
        <Box sx={{ px: 1.5, py: 1 }}>
          <Typography variant="subtitle2" noWrap>
            {user.username}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }} noWrap>
            {user.email}
          </Typography>
        </Box>
        <Divider sx={{ my: 0.5 }} />

        <MenuItem
          onClick={() => {
            close();
            void navigate(paths.profile);
          }}
        >
          <ListItemIcon>
            <PersonRounded fontSize="small" />
          </ListItemIcon>
          <ListItemText>{t('userMenu.profile')}</ListItemText>
        </MenuItem>

        <MenuItem
          onClick={() => {
            close();
            void logout();
          }}
        >
          <ListItemIcon>
            <LogoutRounded fontSize="small" />
          </ListItemIcon>
          <ListItemText>{t('auth.logout')}</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
};
