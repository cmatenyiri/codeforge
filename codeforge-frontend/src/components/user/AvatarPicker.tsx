import { ButtonBase, FormHelperText, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { UserAvatar } from './UserAvatar';
import { AVATAR_IDS, type AvatarId } from './avatars';

type AvatarPickerProps = {
  value: AvatarId | null;
  onChange: (avatar: AvatarId) => void;
  /** Already-translated error text, shown beneath the grid. */
  error?: string;
  label?: string;
};

/** The ten-avatar chooser, used on both the register form and the profile page. */
export const AvatarPicker = ({ value, onChange, error, label }: AvatarPickerProps) => {
  const { t } = useTranslation();

  return (
    <Stack spacing={1}>
      <Typography variant="overline" sx={{ color: error ? 'error.main' : 'text.disabled' }}>
        {label ?? t('avatar.choose')}
      </Typography>

      <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }} role="radiogroup">
        {AVATAR_IDS.map((avatar) => (
          <ButtonBase
            key={avatar}
            role="radio"
            aria-checked={value === avatar}
            aria-label={avatar}
            onClick={() => {
              onChange(avatar);
            }}
            sx={{ borderRadius: 1.5 }}
          >
            <UserAvatar avatar={avatar} size={44} selected={value === avatar} />
          </ButtonBase>
        ))}
      </Stack>

      {error ? <FormHelperText error>{error}</FormHelperText> : null}
    </Stack>
  );
};
