import { Chip, type ChipProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { type Difficulty } from '../../api/types';
import { DIFFICULTY_LABEL_KEY, DIFFICULTY_TOKEN } from './difficulty';

/** Difficulty badge, coloured from `palette.difficulty.*`. */
export const DifficultyChip = ({ difficulty, ...props }: { difficulty: Difficulty } & ChipProps) => {
  const { t } = useTranslation();
  const token = DIFFICULTY_TOKEN[difficulty];

  return (
    <Chip
      {...props}
      label={t(DIFFICULTY_LABEL_KEY[difficulty])}
      sx={{ color: `difficulty.${token}`, backgroundColor: `difficulty.${token}Bg` }}
    />
  );
};
