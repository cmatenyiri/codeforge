import { Box } from '@mui/material';
import { AVATAR_DESIGNS, type AvatarId } from './avatars';

type UserAvatarProps = {
  avatar: AvatarId;
  size?: number;
  /** Draws a ring around the tile — used to mark the current selection. */
  selected?: boolean;
};

/** Renders one catalogue avatar as a gradient tile with its mark on top. */
export const UserAvatar = ({ avatar, size = 32, selected = false }: UserAvatarProps) => {
  const design = AVATAR_DESIGNS[avatar];

  return (
    <Box
      sx={{
        width: size,
        height: size,
        borderRadius: `${Math.round(size * 0.28)}px`,
        flexShrink: 0,
        display: 'grid',
        placeItems: 'center',
        background: `linear-gradient(135deg, ${design.from} 0%, ${design.to} 100%)`,
        boxShadow: selected ? (theme) => `0 0 0 2px ${theme.palette.primary.main}` : 'none',
        transition: 'box-shadow 120ms ease',
      }}
    >
      <Box
        component="svg"
        viewBox="0 0 24 24"
        aria-hidden
        sx={{ width: size * 0.6, height: size * 0.6, display: 'block' }}
      >
        <path
          d={design.path}
          fill="none"
          stroke="rgba(10, 13, 19, 0.78)"
          strokeWidth={2}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </Box>
    </Box>
  );
};
